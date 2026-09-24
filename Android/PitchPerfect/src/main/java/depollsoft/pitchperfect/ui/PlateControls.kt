package depollsoft.pitchperfect.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

/** A section header (`TextAppearance.Plate.SectionHeader`): letter-spaced caps in secondary ink. */
@Composable
fun PlateSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    PlateText(
        text.uppercase(),
        // The style named monospace, but its Material parent's app:fontFamily (sans-serif) wins.
        style = plateText(12.sp, plateColors.inkSecondary, letterSpacing = 0.14f),
        modifier = modifier,
    )
}

/**
 * A two-position mode switch (`Widget.Plate.ModeButton` in a toggle group): outlined segments
 * sharing their middle edge, the chosen one filled with the lit accent.
 *
 * @param options each segment's label and its screen-reader description.
 */
@Composable
fun PlateModeToggle(
    options: List<Pair<String, String>>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = plateColors
    Layout(
        content = {
            options.forEachIndexed { index, (label, description) ->
                val chosen = index == selected
                val shape =
                    when (index) {
                        0 -> RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp)
                        options.lastIndex -> RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp)
                        else -> RoundedCornerShape(0.dp)
                    }
                Box(
                    Modifier
                        // The chosen segment is drawn over its neighbour's shared edge.
                        .zIndex(if (chosen) 1f else 0f)
                        .heightIn(min = 40.dp)
                        .widthIn(min = 72.dp)
                        .clip(shape)
                        .background(if (chosen) colors.accent else colors.surface, shape)
                        .border(1.dp, colors.hairline, shape)
                        .clickable(role = Role.RadioButton, indication = ripple(), interactionSource = null) {
                            if (!chosen) onSelect(index)
                        }.semantics {
                            contentDescription = description
                            this.selected = chosen
                        }.padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    PlateText(
                        label.uppercase(),
                        style = plateText(12.sp, if (chosen) colors.onAccent else colors.inkSecondary, PlateFonts.oswald, letterSpacing = 0.12f),
                        maxLines = 1,
                    )
                }
            }
        },
        modifier = modifier,
    ) { measurables, constraints ->
        // Segments overlap by their 1dp stroke, as the toggle group's negative margins made them.
        val overlap = 1.dp.roundToPx()
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0)) }
        val height = placeables.maxOf { it.height }
        val width = placeables.sumOf { it.width } - overlap * (placeables.size - 1)
        layout(width, height) {
            var x = 0
            placeables.forEach {
                it.place(x, 0)
                x += it.width - overlap
            }
        }
    }
}
