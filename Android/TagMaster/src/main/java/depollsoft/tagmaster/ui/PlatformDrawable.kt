package depollsoft.tagmaster.ui

import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Loads a drawable resource the way an ImageView would, mutated so a tint stays local.
 *
 * Icons and the watermark are the app's vector drawables. Drawing the platform Drawable (rather
 * than a Compose ImageVector) keeps its rasterization, so an icon is pixel-for-pixel the one the
 * View screens showed.
 */
@Composable
fun rememberDrawable(
    @DrawableRes id: Int,
    tint: Color? = null,
): Drawable {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    return remember(id, tint, context, configuration.uiMode) {
        AppCompatResources.getDrawable(context, id)!!.mutate().also { drawable ->
            if (tint != null) drawable.setTint(tint.toArgb())
        }
    }
}

/** Draws [drawable] into [left]..[left]+[width] (pixels), as a View's background or image. */
fun DrawScope.drawPlatform(
    drawable: Drawable,
    left: Int,
    top: Int,
    width: Int,
    height: Int,
    alpha: Float = 1f,
) {
    drawable.setBounds(left, top, left + width, top + height)
    drawable.alpha = (alpha * 255).roundToInt()
    drawIntoCanvas { drawable.draw(it.nativeCanvas) }
}

/** An icon of [size] (24dp by default): an ImageView showing [id] with FIT_CENTER. */
@Composable
fun PlatformIcon(
    @DrawableRes id: Int,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    size: Dp = 24.dp,
    alpha: Float = 1f,
) {
    val drawable = rememberDrawable(id, tint)
    Box(
        modifier
            .size(size)
            .drawBehind {
                drawPlatform(drawable, 0, 0, this.size.width.roundToInt(), this.size.height.roundToInt(), alpha)
            },
    )
}

/**
 * ImageView's CENTER_INSIDE: the drawable at its intrinsic size, scaled down only when it does not
 * fit, centered with the same rounding ImageView applies.
 */
fun DrawScope.drawCenterInside(drawable: Drawable) {
    val viewWidth = size.width.roundToInt()
    val viewHeight = size.height.roundToInt()
    val width = drawable.intrinsicWidth
    val height = drawable.intrinsicHeight
    val scale =
        if (width <= viewWidth && height <= viewHeight) {
            1f
        } else {
            minOf(viewWidth.toFloat() / width, viewHeight.toFloat() / height)
        }
    val dx = ((viewWidth - width * scale) * 0.5f).roundToInt()
    val dy = ((viewHeight - height * scale) * 0.5f).roundToInt()
    drawIntoCanvas { canvas ->
        val native = canvas.nativeCanvas
        val save = native.save()
        native.translate(dx.toFloat(), dy.toFloat())
        native.scale(scale, scale)
        drawable.setBounds(0, 0, width, height)
        drawable.draw(native)
        native.restoreToCount(save)
    }
}
