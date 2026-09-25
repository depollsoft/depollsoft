package depollsoft.tagmaster.ui

import depollsoft.compose.PlatformIcon
import depollsoft.compose.ViewAlign
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import depollsoft.tagmaster.ui.TagMasterType.withoutLineHeight

/** The Material 3 button styles the screens use. */
enum class ButtonStyle { Filled, Tonal, Outlined }

/**
 * A Material 3 button measured as MaterialButton measures: 48dp tall, the label (and an 18dp icon
 * placed just before it) centered, full-pill corners unless [shape] says otherwise. Like
 * MaterialButton it draws its background [inset] from the top and bottom of the 48dp target, so
 * the visible pill is 40dp while the whole 48dp stays touchable.
 */
@Composable
fun TagMasterButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ButtonStyle = ButtonStyle.Filled,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
    shape: Shape = RoundedCornerShape(50),
    inset: Dp = 4.dp,
) {
    val colors = TagMasterTheme.colors
    val disabledContent = colors.onSurface.copy(alpha = 0.38f)
    val (container, content, border) =
        when (style) {
            ButtonStyle.Filled ->
                Triple(
                    if (enabled) colors.primary else colors.onSurface.copy(alpha = 0.12f),
                    if (enabled) colors.onPrimary else disabledContent,
                    null,
                )
            ButtonStyle.Tonal ->
                Triple(
                    if (enabled) colors.secondaryContainer else colors.onSurface.copy(alpha = 0.12f),
                    if (enabled) colors.onSecondaryContainer else disabledContent,
                    null,
                )
            ButtonStyle.Outlined ->
                Triple(
                    Color.Transparent,
                    if (enabled) colors.primary else disabledContent,
                    BorderStroke(1.dp, if (enabled) colors.outline else colors.onSurface.copy(alpha = 0.12f)),
                )
        }
    Box(
        modifier
            .heightIn(min = 48.dp)
            .widthIn(min = 48.dp)
            .then(
                InsetShape(shape, inset).let { drawn ->
                    Modifier
                        .background(container, drawn)
                        .then(if (border != null) Modifier.border(border, drawn) else Modifier)
                        .clip(drawn)
                },
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(contentPadding),
        contentAlignment = ViewAlign.Center,
        // The row fills the content box so the icon and the label each center in it, the way
        // MaterialButton centers its compound icon and its text independently.
        propagateMinConstraints = true,
    ) {
        Row(horizontalArrangement = ViewAlign.CenterArrangement, verticalAlignment = ViewAlign.CenterVertically) {
            if (icon != null) PlatformIcon(icon, Modifier.padding(end = 8.dp), tint = content, size = 18.dp)
            ViewCenteredText(text, TagMasterType.labelLarge.withoutLineHeight(), content)
        }
    }
}

/** [shape] drawn [vertical] in from the top and bottom of the bounds, as MaterialButton's insets. */
private class InsetShape(
    private val shape: Shape,
    private val vertical: Dp,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val inset = with(density) { vertical.roundToPx() }.toFloat()
        val inner = shape.createOutline(Size(size.width, (size.height - 2 * inset).coerceAtLeast(0f)), layoutDirection, density)
        return when (inner) {
            is Outline.Rectangle -> Outline.Rectangle(inner.rect.translate(0f, inset))
            // A path, not Outline.Rounded: Modifier.border rebuilds a rounded outline from the
            // full size and would draw the stroke outside the inset.
            is Outline.Rounded ->
                inner.roundRect.let {
                    Outline.Generic(
                        Path().apply {
                            addRoundRect(
                                RoundRect(
                                    it.left,
                                    it.top + inset,
                                    it.right,
                                    it.bottom + inset,
                                    it.topLeftCornerRadius,
                                    it.topRightCornerRadius,
                                    it.bottomRightCornerRadius,
                                    it.bottomLeftCornerRadius,
                                ),
                            )
                        },
                    )
                }
            is Outline.Generic -> Outline.Generic(Path().apply { addPath(inner.path, androidx.compose.ui.geometry.Offset(0f, inset)) })
        }
    }
}
