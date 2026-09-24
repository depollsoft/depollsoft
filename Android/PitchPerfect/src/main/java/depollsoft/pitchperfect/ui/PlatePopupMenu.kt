package depollsoft.pitchperfect.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

/** One row of a [PlatePopupMenu]. */
class PopupMenuItem(
    val title: String,
    val enabled: Boolean = true,
    val testTag: String? = null,
    val onClick: () -> Unit = {},
)

/**
 * A PopupMenu as the View screens showed it: a plain white list hanging from the anchor's bottom
 * start corner, 48dp rows of 16sp text inset 16dp, as wide as the widest row and at least 196dp. Place it inside the
 * anchor's layout; it hangs from the anchor's bounds.
 */
@Composable
fun PlatePopupMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<PopupMenuItem>,
) {
    if (!expanded) return
    val colors = plateColors
    Popup(
        popupPositionProvider = BelowAnchor,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        Layout(
            content = {
                items.forEach { item ->
                    Box(
                        Modifier
                            .height(48.dp)
                            .then(if (item.testTag != null) Modifier.testTag(item.testTag) else Modifier)
                            .clickable(
                                enabled = item.enabled,
                                role = Role.Button,
                                indication = ripple(),
                                interactionSource = null,
                            ) {
                                onDismissRequest()
                                item.onClick()
                            }.padding(horizontal = 16.dp)
                            .centerVerticallyLikeViews(),
                    ) {
                        // A disabled row (the menu's heading) still reads in ink, as the View's did.
                        PlateText(item.title, plateText(16.sp, colors.ink), maxLines = 1)
                    }
                }
            },
            modifier = Modifier.shadow(8.dp).background(Color.White),
        ) { measurables, constraints ->
            val widest = measurables.maxOfOrNull { it.maxIntrinsicWidth(constraints.maxHeight) } ?: 0
            // AppCompat's popup menu rows are at least 196dp wide.
            val width = widest.coerceAtLeast(MIN_WIDTH.roundToPx()).coerceAtMost(constraints.maxWidth)
            val placeables = measurables.map { it.measure(Constraints.fixed(width, 48.dp.roundToPx())) }
            layout(width, placeables.sumOf { it.height }) {
                var y = 0
                placeables.forEach {
                    it.place(0, y)
                    y += it.height
                }
            }
        }
    }
}

/** Hangs the menu from the anchor's bottom start corner, kept on screen. */
private object BelowAnchor : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = anchorBounds.left.coerceAtMost(windowSize.width - popupContentSize.width).coerceAtLeast(0)
        val below = anchorBounds.bottom
        val y = if (below + popupContentSize.height <= windowSize.height) below else (anchorBounds.top - popupContentSize.height).coerceAtLeast(0)
        return IntOffset(x, y)
    }
}

private val MIN_WIDTH = 196.dp
