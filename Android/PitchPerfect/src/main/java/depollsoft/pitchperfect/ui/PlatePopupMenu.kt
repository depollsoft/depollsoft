package depollsoft.pitchperfect.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
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

/** Where a [PlatePopupMenu] opens: hanging below its anchor, or over it as an overflow menu. */
enum class MenuPlacement { BELOW_ANCHOR, OVER_ANCHOR_END }

/**
 * A PopupMenu as the View screens showed it: a plain white list hanging from the anchor's bottom
 * start corner, 48dp rows of 16sp text inset 16dp, as wide as the widest row and at least 196dp.
 * Place it inside the anchor's layout; it hangs from the anchor's bounds. It grows in from the
 * corner it opens from and fades away, as the platform's popup windows did.
 *
 * An overflow menu ([MenuPlacement.OVER_ANCHOR_END]) opens over its button, end-aligned, and
 * greys its disabled rows as the action bar's overflow did; a heading row in a plain popup
 * ([dimDisabled] false) reads in ink.
 */
@Composable
fun PlatePopupMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<PopupMenuItem>,
    placement: MenuPlacement = MenuPlacement.BELOW_ANCHOR,
    dimDisabled: Boolean = placement == MenuPlacement.OVER_ANCHOR_END,
) {
    val shown = remember { MutableTransitionState(false) }
    shown.targetState = expanded
    if (!shown.currentState && !shown.targetState) return
    val colors = plateColors
    val transition = rememberTransition(shown, label = "menu")
    val scale by transition.animateFloat(
        transitionSpec = { tween(if (targetState) MENU_IN_MS else MENU_OUT_MS, easing = LinearOutSlowInEasing) },
        label = "scale",
    ) { if (it) 1f else MENU_START_SCALE }
    val alpha by transition.animateFloat(
        transitionSpec = { tween(if (targetState) MENU_FADE_IN_MS else MENU_OUT_MS, easing = FastOutSlowInEasing) },
        label = "alpha",
    ) { if (it) 1f else 0f }
    var origin by remember { mutableStateOf(TransformOrigin(0f, 0f)) }
    val nudge = with(LocalDensity.current) { OVERFLOW_OFFSET.roundToPx() }
    val provider =
        remember(placement, nudge) {
            MenuPosition(placement, nudge) { origin = it }
        }
    Popup(
        popupPositionProvider = provider,
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
                        val ink = if (dimDisabled && !item.enabled) colors.ink.copy(alpha = DISABLED_ALPHA) else colors.ink
                        PlateText(item.title, plateText(16.sp, ink), maxLines = 1)
                    }
                }
            },
            modifier =
                Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                        transformOrigin = origin
                    }.shadow(8.dp)
                    .background(Color.White),
        ) { measurables, constraints ->
            val widest = measurables.maxOfOrNull { it.maxIntrinsicWidth(constraints.maxHeight) } ?: 0
            // AppCompat's popup menu rows are at least 196dp wide.
            val width = widest.coerceAtLeast(MIN_WIDTH.roundToPx()).coerceAtMost(constraints.maxWidth)
            val placeables = measurables.map { it.measure(Constraints.fixed(width, 48.dp.roundToPx())) }
            layout(width, placeables.sumOf { it.height }) {
                var y = 0
                placeables.forEach {
                    it.placeRelative(0, y)
                    y += it.height
                }
            }
        }
    }
}

/**
 * Places the menu, kept on screen, and reports which corner it grows from: below the anchor from
 * its start corner, or (an overflow) over the anchor from its end corner, nudged [nudge] inward as
 * the overflow style's -4dp horizontal offset did.
 */
private class MenuPosition(
    private val placement: MenuPlacement,
    private val nudge: Int,
    private val onOrigin: (TransformOrigin) -> Unit,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
        val maxY = (windowSize.height - popupContentSize.height).coerceAtLeast(0)
        val ltr = layoutDirection == LayoutDirection.Ltr
        return when (placement) {
            MenuPlacement.BELOW_ANCHOR -> {
                val x = (if (ltr) anchorBounds.left else anchorBounds.right - popupContentSize.width).coerceIn(0, maxX)
                val below = anchorBounds.bottom
                val fitsBelow = below + popupContentSize.height <= windowSize.height
                val y = if (fitsBelow) below else (anchorBounds.top - popupContentSize.height).coerceAtLeast(0)
                onOrigin(TransformOrigin(if (ltr) 0f else 1f, if (fitsBelow) 0f else 1f))
                IntOffset(x, y)
            }
            MenuPlacement.OVER_ANCHOR_END -> {
                val x =
                    (if (ltr) anchorBounds.right - popupContentSize.width - nudge else anchorBounds.left + nudge)
                        .coerceIn(0, maxX)
                val y = anchorBounds.top.coerceIn(0, maxY)
                onOrigin(TransformOrigin(if (ltr) 1f else 0f, 0f))
                IntOffset(x, y)
            }
        }
    }
}

private val MIN_WIDTH = 196.dp
private val OVERFLOW_OFFSET = 4.dp
private const val DISABLED_ALPHA = 0.38f

// Compose's DropdownMenu timings: grow in over 120ms (fading over the first 30), fade out in 75.
private const val MENU_IN_MS = 120
private const val MENU_FADE_IN_MS = 30
private const val MENU_OUT_MS = 75
private const val MENU_START_SCALE = 0.8f
