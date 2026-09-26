package depollsoft.pitchperfect.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import depollsoft.compose.drawPlatform
import depollsoft.compose.rememberDrawable

/**
 * An icon drawn by its own Drawable, tinted and stretched to [size], so the vector drawables
 * rasterize as the platform draws them; Compose's vector painter anti-aliases their edges a
 * little differently.
 */
@Composable
fun DrawableIcon(
    @DrawableRes icon: Int,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    alpha: Float = 1f,
) {
    val drawable = rememberDrawable(icon, tint)
    // The icon's alpha is a truncated byte.
    val byteAlpha = (alpha * 255).toInt() / 255f
    Box(
        modifier
            .size(size)
            .drawBehind { drawPlatform(drawable, 0, 0, this.size.width.toInt(), this.size.height.toInt(), byteAlpha) },
    )
}
