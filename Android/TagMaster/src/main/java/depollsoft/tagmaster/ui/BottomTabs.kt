package depollsoft.tagmaster.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.roundToInt

/** One tab of a [BottomTabs] strip. */
data class TabItem(
    val label: String,
    @param:DrawableRes val icon: Int,
    val id: String,
)

/**
 * The charcoal page switcher at the bottom of Browse and the tag detail: equal slots (split as
 * LinearLayout splits leftover pixels), a 24dp icon over a label, and the accent indicator along
 * the top edge following [position] (the pager's page plus its scroll fraction).
 */
@Composable
fun BottomTabs(
    tabs: List<TabItem>,
    selected: Int,
    position: Float,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TagMasterTheme.colors
    Layout(
        modifier =
            modifier
                .fillMaxWidth()
                .background(colors.chrome)
                .selectableGroup()
                .drawWithContent {
                    drawContent()
                    val count = tabs.size
                    if (count == 0) return@drawWithContent
                    val slots = viewSlots(size.width.roundToInt(), count)
                    val page = position.coerceIn(0f, (count - 1).toFloat())
                    val index = page.toInt().coerceAtMost(count - 1)
                    val fraction = page - index
                    val inset = 8.dp.toPx() + 2.dp.toPx()
                    val left = lerp(slots[index].first + inset, slots.getOrElse(index + 1) { slots[index] }.first + inset, fraction)
                    val right =
                        lerp(
                            slots[index].second - inset,
                            slots.getOrElse(index + 1) { slots[index] }.second - inset,
                            fraction,
                        )
                    val height = 3.dp.toPx()
                    val radius = CornerRadius(3.dp.toPx())
                    val path =
                        Path().apply {
                            addRoundRect(
                                RoundRect(
                                    left = left,
                                    top = 0f,
                                    right = right,
                                    bottom = height,
                                    topLeftCornerRadius = radius,
                                    topRightCornerRadius = radius,
                                    bottomLeftCornerRadius = CornerRadius.Zero,
                                    bottomRightCornerRadius = CornerRadius.Zero,
                                ),
                            )
                        }
                    drawPath(path, colors.chromeAccent)
                },
        content = {
            tabs.forEachIndexed { index, tab ->
                val isSelected = index == selected
                val tint = if (isSelected) colors.onChrome else colors.tabTint
                Column(
                    Modifier
                        .selectable(isSelected, role = Role.Tab) { onSelect(index) }
                        .semantics { contentDescription = tab.label }
                        .testTag(tab.id)
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = ViewAlign.CenterHorizontally,
                ) {
                    Column(Modifier.clearAndSetSemantics { }, horizontalAlignment = ViewAlign.CenterHorizontally) {
                        PlatformIcon(tab.icon, tint = tint)
                        Text(
                            tab.label,
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            style = TagMasterType.labelMedium,
                            color = tint,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val slots = viewSlots(width, measurables.size)
        val placeables =
            measurables.mapIndexed { index, measurable ->
                measurable.measure(Constraints.fixedWidth(slots[index].second - slots[index].first))
            }
        val contentHeight = placeables.maxOfOrNull { it.height } ?: 0
        val height = max(72.dp.roundToPx(), contentHeight + 16.dp.roundToPx())
        layout(width, height) {
            placeables.forEachIndexed { index, placeable ->
                placeable.place(slots[index].first, (height - placeable.height) / 2)
            }
        }
    }
}

/** LinearLayout's weight split: each slot takes its share of what is left, rounded down. */
private fun viewSlots(
    width: Int,
    count: Int,
): List<Pair<Int, Int>> {
    val slots = mutableListOf<Pair<Int, Int>>()
    var remaining = width
    var left = 0
    for (index in 0 until count) {
        val share = remaining / (count - index)
        slots += left to left + share
        left += share
        remaining -= share
    }
    return slots
}

private fun lerp(
    start: Float,
    end: Float,
    fraction: Float,
) = start + (end - start) * fraction
