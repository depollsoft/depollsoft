package depollsoft.tagmaster.ui

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import depollsoft.tagmaster.ui.TagMasterType.withoutLineHeight

/** The Material 3 button styles the screens use. */
enum class ButtonStyle { Filled, Tonal, Outlined }

/**
 * A Material 3 button measured as MaterialButton measures: 48dp tall, the label (and an 18dp icon
 * placed just before it) centered, full-pill corners unless [shape] says otherwise.
 */
@Composable
fun TagMasterButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ButtonStyle = ButtonStyle.Filled,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = if (icon != null) 16.dp else 24.dp, vertical = 10.dp),
    shape: Shape = RoundedCornerShape(50),
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
                    BorderStroke(1.dp, if (enabled) colors.outlineVariant else colors.onSurface.copy(alpha = 0.12f)),
                )
        }
    Box(
        modifier
            .heightIn(min = 48.dp)
            .widthIn(min = 48.dp)
            .clip(shape)
            .background(container, shape)
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(contentPadding),
        contentAlignment = ViewAlign.Center,
    ) {
        Row(verticalAlignment = ViewAlign.CenterVertically) {
            if (icon != null) PlatformIcon(icon, Modifier.padding(end = 8.dp), tint = content, size = 18.dp)
            Text(text, style = TagMasterType.labelLarge.withoutLineHeight(), color = content, maxLines = 1)
        }
    }
}
