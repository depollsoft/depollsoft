package depollsoft.tagmaster

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.view.Menu
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import bolts.Task
import com.bindroid.converters.BoolConverter
import com.bindroid.trackable.trackable
import com.bindroid.utils.Property
import com.bindroid.utils.WeakReflectedProperty
import com.bindroid.utils.bind
import com.bindroid.utils.bindTo
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.snackbar.Snackbar
import depollsoft.pitchperfect.lib.Note
import depollsoft.tagmaster.barbershop.Tag
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SheetMusicActivity : AppCompatActivity() {
    private val viewScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    var tag: Tag? by trackable()
    var drawable: Drawable? by trackable {
        viewScope.launch {
            if (!isFinishing && !isDestroyed) photoView.setImageDrawable(it)
        }
    }
    var rotation: Float by trackable(0f) { loadImage() }
    lateinit var photoView: PhotoView
    lateinit var keyButton: ExtendedFloatingActionButton
    private var touchInProgress = false
    private var playingNote: Note? by trackable()
    private val clearTouchState = Runnable { touchInProgress = false }
    private val stopNote =
        Runnable {
            playingNote?.stop()
            playingNote = null
        }

    @SuppressLint("ClickableViewAccessibility") // Non-touch clicks have their own bounded playback path.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sheetmusicview)
        setUpToolbar(true)
        photoView = findViewById(R.id.photoView)
        photoView.keepScreenOn = SettingsModel.wakeLockOnSheetMusic
        keyButton = findViewById(R.id.keyButton)
        keyButton.applyBottomInsetsAsMargin()
        keyButton.extend()
        bindTo(R.id.keyButton, "Activated", { playingNote?.isPlaying == true })
        bindTo(R.id.sheetMusicLoading, "Visibility", { drawable == null }, BoolConverter.get())
        bindTo(R.id.keyButton, "Visibility", { tag?.keyNote }, BoolConverter.get())
        bindTo(R.id.keyButton, "Text", { tag?.writtenKey })
        bindTo(R.id.keyButton, "ContentDescription", {
            getString(R.string.play_key_note, tag?.writtenKey.orEmpty())
        })
        keyButton.setOnTouchListener { _, event ->
            if (!keyButton.isEnabled) return@setOnTouchListener false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    keyButton.removeCallbacks(clearTouchState)
                    keyButton.removeCallbacks(stopNote)
                    stopNote.run()
                    touchInProgress = true
                    playingNote = tag?.keyNote
                    playingNote?.play()
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_OUTSIDE -> {
                    stopNote.run()
                    // Run after the click posted by View's ACTION_UP handling.
                    keyButton.post { keyButton.post(clearTouchState) }
                }
            }
            false
        }
        keyButton.setOnClickListener {
            if (!touchInProgress) {
                keyButton.removeCallbacks(stopNote)
                stopNote.run()
                playingNote = tag?.keyNote
                playingNote?.play()
                keyButton.postDelayed(stopNote, 1500)
            }
        }
        bind(
            WeakReflectedProperty(this, "Title"),
            Property({
                tag?.title?.makeTitleString(this)
            }, null, CharSequence::class.java),
        )
        val tagId = intent.getIntExtra("tagId", -1)
        Tag.loadTagById(tagId).continueWith({ task ->
            if (!isFinishing && !isDestroyed) {
                if (task.isFaulted || task.isCancelled) {
                    Snackbar.make(photoView, R.string.failed_to_load_tag, Snackbar.LENGTH_LONG).show()
                } else {
                    tag = task.result
                }
            }
            null
        }, Task.UI_THREAD_EXECUTOR)
        loadImage()
    }

    override fun onSupportNavigateUp() = navigateUpOrHome()

    override fun onStop() {
        keyButton.removeCallbacks(stopNote)
        keyButton.removeCallbacks(clearTouchState)
        stopNote.run()
        touchInProgress = false
        super.onStop()
    }

    override fun onDestroy() {
        viewScope.cancel()
        super.onDestroy()
    }

    private fun rotate() {
        rotation = (rotation + 90f) % 360f
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

    private val loadMutex = Mutex()

    private fun loadImage() {
        val imageRotation = rotation
        viewScope.launch {
            loadMutex.withLock {
                val bitmap =
                    withContext(Dispatchers.IO) {
                        try {
                            var result: Bitmap? = null
                            if (intent.type == "application/pdf") {
                                contentResolver.openFileDescriptor(requireNotNull(intent.data), "r").use { fd ->
                                    PdfRenderer(requireNotNull(fd)).use { renderer ->
                                        val dpi = minOf(resources.displayMetrics.densityDpi, 200)
                                        for (pageIndex in 0 until renderer.pageCount) {
                                            ensureActive()
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
                    }
                if (!isFinishing && !isDestroyed) {
                    if (bitmap == null) {
                        openExternally(fallback = true)
                    } else {
                        drawable = bitmap.toDrawable(resources)
                    }
                }
            }
        }
    }

    private fun openExternally(fallback: Boolean = false) {
        val toLaunch = Intent(intent).apply { component = null }
        try {
            startActivity(toLaunch)
            if (fallback) {
                // This activity finishes immediately, so a Snackbar would disappear before it can be read.
                Toast.makeText(this, R.string.sheet_music_external_fallback, Toast.LENGTH_LONG).show()
                finish()
            }
        } catch (_: ActivityNotFoundException) {
            Snackbar.make(photoView, R.string.sheet_music_no_external_app, Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.sheetmusicmenu, menu)
        menu.findItem(R.id.rotateButton)?.setOnMenuItemClickListener {
            rotate()
            true
        }
        menu.findItem(R.id.launchButton)?.setOnMenuItemClickListener {
            openExternally()
            true
        }
        return super.onCreateOptionsMenu(menu)
    }

    companion object {
        const val MAX_BITMAP_SIZE = 1024 * 1024 * 100 // 100MiB
    }
}
