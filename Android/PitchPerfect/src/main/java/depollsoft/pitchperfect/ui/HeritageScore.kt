package depollsoft.pitchperfect.ui

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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

/**
 * The decoded score artwork, once per screen density: each density has its own raster.
 *
 * Only its alpha is kept. The engraving is drawn through an SRC_IN tint, which discards the
 * artwork's colour, and the full-colour raster is large: the xxhdpi one decodes to about 22MB on a
 * 420dpi phone, more than a 48MB Android 7 heap can spare alongside the app.
 */
internal object HeritageArtwork {
    private val masks = mutableMapOf<Int, Bitmap>()

    fun get(resources: Resources): Bitmap? =
        synchronized(this) {
            val density = resources.displayMetrics.densityDpi
            masks[density] ?: runCatching { decodeMask(resources) }.getOrNull()?.also { masks[density] = it }
        }

    private fun decodeMask(resources: Resources): Bitmap? {
        val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ALPHA_8 }
        val decoded = BitmapFactory.decodeResource(resources, R.drawable.panobackground, options) ?: return null
        if (decoded.config == Bitmap.Config.ALPHA_8) return decoded
        return decoded.extractAlpha().also { decoded.recycle() }
    }
}

/** Paints the full-bleed, low-contrast score engraving behind every app surface. */
fun Modifier.heritageScore(resources: Resources, colors: PlateColors): Modifier =
    drawWithCache {
        val width = size.width
        val height = size.height
        val source = HeritageArtwork.get(resources)
        val tileHeight = if (source == null) 0f else width * (source.height.toFloat() / source.width.toFloat())
        val grain = Paint().apply { color = colors.hairline.toArgb() }
        val engraving =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                alpha = 28
                colorFilter = PorterDuffColorFilter(colors.inkSecondary.toArgb(), PorterDuff.Mode.SRC_IN)
            }
        val sourceRect = if (source == null) null else Rect(0, 0, source.width, source.height)
        onDrawBehind {
            // Compose does not clip a node to its bounds, so nothing here may draw past them.
            drawRect(colors.ground)
            drawIntoCanvas {
                val canvas = it.nativeCanvas
                if (width <= 0f || height <= 0f || source == null || tileHeight <= 0f) return@drawIntoCanvas
                var grainY = 0f
                while (grainY < height) {
                    grain.alpha = 8 + ((grainY.toInt() * 31) % 14)
                    canvas.drawLine(0f, grainY, width, grainY, grain)
                    grainY += 4f
                }
                // The last tile runs past the bottom edge.
                canvas.save()
                canvas.clipRect(0f, 0f, width, height)
                var tileTop = 0f
                while (tileTop < height) {
                    canvas.drawBitmap(source, sourceRect, RectF(0f, tileTop, width, tileTop + tileHeight), engraving)
                    tileTop += tileHeight
                }
                canvas.restore()
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
