package depollsoft.tagmaster

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import bolts.Task
import depollsoft.tagmaster.barbershop.Tag
import depollsoft.tagmaster.ui.BarAction
import depollsoft.tagmaster.ui.CompactBarberPole
import depollsoft.tagmaster.ui.LocalSnackbars
import depollsoft.tagmaster.ui.PlatformIcon
import depollsoft.tagmaster.ui.ShowAs
import depollsoft.tagmaster.ui.Snackbars
import depollsoft.tagmaster.ui.TagMasterTheme
import depollsoft.tagmaster.ui.TagMasterTopBar
import depollsoft.tagmaster.ui.TagMasterType
import depollsoft.tagmaster.ui.TagMasterType.withoutLineHeight
import depollsoft.tagmaster.ui.ViewAlign
import depollsoft.tagmaster.ui.ZoomableImage
import depollsoft.tagmaster.ui.navigateUpOrHome
import depollsoft.tagmaster.ui.notePress
import depollsoft.tagmaster.ui.rememberNotePlayer
import depollsoft.tagmaster.ui.setTagMasterContent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * A tag's sheet music, zoomable, with the key note one press away. PDFs are rendered page by page
 * into one tall image; anything that cannot be shown here opens in another app.
 */
class SheetMusicActivity : AppCompatActivity() {
    var tag: Tag? by mutableStateOf(null)
        private set

    var image: Bitmap? by mutableStateOf(null)
        private set

    var rotation by mutableFloatStateOf(0f)
        private set

    var imageLoading by mutableStateOf(false)
        private set

    internal var snackbars: Snackbars? = null
    private val loadMutex = Mutex()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTagMasterContent { SheetMusicScreen(this) }
        val tagId = intent.getIntExtra("tagId", -1)
        Tag.loadTagById(tagId).continueWith({ task ->
            if (!isFinishing && !isDestroyed) {
                if (task.isFaulted || task.isCancelled) {
                    snackbars?.show(getString(R.string.failed_to_load_tag))
                } else {
                    tag = task.result
                }
            }
            null
        }, Task.UI_THREAD_EXECUTOR)
        loadImage()
    }

    internal fun rotate() {
        rotation = (rotation + 90f) % 360f
        loadImage()
    }

    private fun loadImage() {
        val imageRotation = rotation
        lifecycleScope.launch {
            loadMutex.withLock {
                imageLoading = true
                try {
                    val bitmap = withContext(Dispatchers.IO) { render(imageRotation) }
                    if (!isFinishing && !isDestroyed) {
                        if (bitmap == null) openExternally(fallback = true) else image = bitmap
                    }
                } finally {
                    imageLoading = false
                }
            }
        }
    }

    private suspend fun render(imageRotation: Float): Bitmap? =
        try {
            var result: Bitmap? = null
            if (intent.type == "application/pdf") {
                contentResolver.openFileDescriptor(requireNotNull(intent.data), "r").use { fd ->
                    PdfRenderer(requireNotNull(fd)).use { renderer ->
                        val dpi = minOf(resources.displayMetrics.densityDpi, 200)
                        for (pageIndex in 0 until renderer.pageCount) {
                            kotlin.coroutines.coroutineContext.ensureActive()
                            val rendered =
                                renderer.openPage(pageIndex).use { page ->
                                    val width = dpi * page.width / 72
                                    val height = dpi * page.height / 72
                                    require(width.toLong() * height * 4 <= MAX_BITMAP_SIZE)
                                    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
                                        it.eraseColor(Color.WHITE)
                                        page.render(it, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                    }
                                }
                            val pageBitmap = rotateBitmap(rendered, imageRotation)
                            if (pageBitmap !== rendered) rendered.recycle()
                            val previous = result
                            result =
                                if (previous == null) {
                                    pageBitmap
                                } else {
                                    appendBitmap(previous, pageBitmap).also {
                                        previous.recycle()
                                        pageBitmap.recycle()
                                    }
                                }
                        }
                    }
                }
            } else {
                contentResolver.openInputStream(requireNotNull(intent.data)).use { stream ->
                    val decoded = requireNotNull(BitmapFactory.decodeStream(stream))
                    result = rotateBitmap(decoded, imageRotation)
                    if (result !== decoded) decoded.recycle()
                }
            }
            result?.takeIf { it.byteCount <= MAX_BITMAP_SIZE }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }

    private fun appendBitmap(
        first: Bitmap,
        second: Bitmap,
    ): Bitmap {
        val width = maxOf(first.width, second.width)
        val height = first.height + second.height
        require(width.toLong() * height * 4 <= MAX_BITMAP_SIZE)
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(first, ((width - first.width) / 2).toFloat(), 0f, null)
        canvas.drawBitmap(second, ((width - second.width) / 2).toFloat(), first.height.toFloat(), null)
        return result
    }

    private fun rotateBitmap(
        bitmap: Bitmap,
        rotation: Float,
    ): Bitmap {
        if (rotation == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(rotation) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    internal fun openExternally(fallback: Boolean = false) {
        val toLaunch = Intent(intent).apply { component = null }
        try {
            startActivity(toLaunch)
            if (fallback) {
                // This activity finishes immediately, so a snackbar would disappear before it can be read.
                Toast.makeText(this, R.string.sheet_music_external_fallback, Toast.LENGTH_LONG).show()
                finish()
            }
        } catch (_: ActivityNotFoundException) {
            snackbars?.show(getString(R.string.sheet_music_no_external_app))
        }
    }

    companion object {
        const val MAX_BITMAP_SIZE = 1024 * 1024 * 100 // 100MiB
    }
}

@Composable
private fun SheetMusicScreen(activity: SheetMusicActivity) {
    activity.snackbars = LocalSnackbars.current
    val tag = activity.tag
    Column(Modifier.fillMaxSize()) {
        TagMasterTopBar(
            title = tag?.title ?: "",
            brandTitle = true,
            onNavigateUp = { activity.navigateUpOrHome() },
            actions =
                listOf(
                    BarAction("rotate", stringResource(R.string.rotate), R.drawable.ic_rotate, ShowAs.Always) { activity.rotate() },
                    BarAction("openExternally", stringResource(R.string.open_in_external_app), R.drawable.ic_launch, ShowAs.IfRoom) {
                        activity.openExternally()
                    },
                ),
        )
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            val image = activity.image
            if (image != null) {
                ZoomableImage(
                    image,
                    Modifier
                        .fillMaxSize()
                        .testTag("sheetMusicImage"),
                    keepScreenOn = SettingsModel.wakeLockOnSheetMusic,
                )
            }
            CompactBarberPole(
                activity.imageLoading && image == null,
                stringResource(R.string.detail_loading),
                Modifier.align(ViewAlign.Center),
            )
            if (tag?.keyNote != null) KeyFab(tag, Modifier.align(Alignment.BottomEnd))
        }
    }
}

/** The floating key button: sounds the key note while pressed, filled in the accent while it sounds. */
@Composable
private fun KeyFab(
    tag: Tag,
    modifier: Modifier,
) {
    val colors = TagMasterTheme.colors
    val player = rememberNotePlayer()
    val playing = tag.keyNote?.isPlaying == true
    val shape = RoundedCornerShape(16.dp)
    val content = if (playing) colors.onPrimary else colors.primary
    // ExtendedFloatingActionButton's layout: the icon 16dp from the start, the label centered in
    // what is left after the icon's 12dp padding and the 20dp end padding, at least 120dp wide.
    Layout(
        modifier =
            modifier
                .padding(end = 16.dp, bottom = 16.dp)
                .shadow(6.dp, shape)
                .background(if (playing) colors.primary else colors.sheetKeySurface, shape)
                .border(BorderStroke(1.dp, colors.primary), shape)
                .clip(shape)
                .notePress(player, { tag.keyNote }, description = stringResource(R.string.play_key_note, tag.writtenKey.orEmpty()))
                .testTag("keyButton"),
        content = {
            PlatformIcon(R.drawable.ic_key, tint = content)
            Text(tag.writtenKey.orEmpty(), style = TagMasterType.labelLarge.withoutLineHeight(), color = content, maxLines = 1)
        },
    ) { measurables, _ ->
        val icon = measurables[0].measure(Constraints())
        val label = measurables[1].measure(Constraints())
        val start = 16.dp.roundToPx()
        val textStart = start + icon.width + 12.dp.roundToPx()
        val end = 20.dp.roundToPx()
        val width = maxOf(120.dp.roundToPx(), textStart + label.width + end)
        val height = maxOf(48.dp.roundToPx(), maxOf(icon.height, label.height))
        layout(width, height) {
            icon.placeRelative(start, (height - icon.height) / 2)
            label.placeRelative(textStart + (width - end - textStart - label.width) / 2, (height - label.height) / 2)
        }
    }
}
