package depollsoft.pitchperfect.ui

import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * An icon drawn by its own Drawable, tinted, at [size]. The vector drawables rasterize exactly as
 * they did in the View screens this way; Compose's vector painter anti-aliases their edges a
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
    val context = LocalContext.current
    val drawable = remember(icon, context) { AppCompatResources.getDrawable(context, icon)?.mutate() }
    Box(
        modifier
            .size(size)
            .drawBehind {
                val d = drawable ?: return@drawBehind
                d.setBounds(0, 0, this.size.width.toInt(), this.size.height.toInt())
                d.setTint(tint.toArgb())
                d.alpha = (alpha * 255).toInt()
                drawIntoCanvas { d.draw(it.nativeCanvas) }
            },
    )
}
