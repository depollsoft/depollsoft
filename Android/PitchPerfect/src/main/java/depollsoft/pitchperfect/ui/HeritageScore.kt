package depollsoft.pitchperfect.ui

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import depollsoft.pitchperfect.R

/** The decoded score artwork, once per screen density: each density has its own raster. */
internal object HeritageArtwork {
    private val bitmaps = mutableMapOf<Int, Bitmap>()

    fun get(resources: Resources): Bitmap? =
        synchronized(this) {
            val density = resources.displayMetrics.densityDpi
            bitmaps[density] ?: runCatching {
                BitmapFactory.decodeResource(resources, R.drawable.panobackground)
            }.getOrNull()?.also { bitmaps[density] = it }
        }
}

/** Paints the full-bleed, low-contrast score engraving behind every app surface. */
internal object HeritageScoreRenderer {
    fun render(
        resources: Resources,
        width: Int,
        height: Int,
        ground: Int,
        engraving: Int,
        hairline: Int,
    ): Bitmap? {
        val source = HeritageArtwork.get(resources) ?: return null
        if (width <= 0 || height <= 0) return null
        val tileHeight = width * (source.height.toFloat() / source.width.toFloat())
        if (tileHeight <= 0f) return null
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(ground)
        val grain = Paint().apply { color = hairline }
        var grainY = 0f
        while (grainY < height) {
            grain.alpha = 8 + ((grainY.toInt() * 31) % 14)
            canvas.drawLine(0f, grainY, width.toFloat(), grainY, grain)
            grainY += 4f
        }
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                alpha = 28
                colorFilter = PorterDuffColorFilter(engraving, PorterDuff.Mode.SRC_IN)
            }
        val sourceRect = Rect(0, 0, source.width, source.height)
        var tileTop = 0f
        while (tileTop < height) {
            canvas.drawBitmap(source, sourceRect, RectF(0f, tileTop, width.toFloat(), tileTop + tileHeight), paint)
            tileTop += tileHeight
        }
        // A hardware bitmap lives in GPU memory, so no frame re-uploads the full-screen ground.
        return runCatching { bitmap.copy(Bitmap.Config.HARDWARE, false) }
            .getOrNull()
            ?.also { bitmap.recycle() }
            ?: bitmap
    }
}

/** The score engraving, rendered once per size. */
fun Modifier.heritageScore(resources: Resources, colors: PlateColors): Modifier =
    drawWithCache {
        val rendered =
            HeritageScoreRenderer.render(
                resources,
                size.width.toInt(),
                size.height.toInt(),
                colors.ground.toArgb(),
                colors.inkSecondary.toArgb(),
                colors.hairline.toArgb(),
            )
        onDrawBehind {
            if (rendered != null) {
                drawIntoCanvas { it.nativeCanvas.drawBitmap(rendered, 0f, 0f, null) }
            } else {
                drawRect(colors.ground)
            }
        }
    }

/** A plate: the ground with its score engraving, and [content] over it. */
@Composable
fun PlateBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val resources = LocalContext.current.resources
    val colors = plateColors
    Box(modifier.fillMaxSize().heritageScore(resources, colors), content = content)
}
