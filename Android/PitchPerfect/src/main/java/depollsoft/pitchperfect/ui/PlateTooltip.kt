package depollsoft.pitchperfect.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import depollsoft.compose.LabelTooltipState
import depollsoft.compose.rememberLabelTooltipState
import depollsoft.compose.tooltipOnHover

/**
 * [content] with a tooltip reading [label], shown on a long press and on mouse hover (see
 * [LabelTooltipState]). The content wires the long press it is handed into its own clickable
 * (`combinedClickable(onLongClick = …)`), which gives the press its buzz.
 */
@Composable
fun WithTooltip(
    label: String,
    modifier: Modifier = Modifier,
    state: LabelTooltipState = rememberLabelTooltipState(),
    content: @Composable BoxScope.(showTooltip: () -> Unit) -> Unit,
) {
    Box(modifier.tooltipOnHover(state)) {
        content(state::longPressed)
        if (state.visible) TooltipPopup(label, onDismiss = state::dismiss)
    }
}

@Composable
private fun TooltipPopup(
    label: String,
    onDismiss: () -> Unit,
) {
    val colors = plateColors
    val offset = with(LocalDensity.current) { TOOLTIP_OFFSET.roundToPx() }
    val shown = remember { Animatable(0f) }
    LaunchedEffect(Unit) { shown.animateTo(1f, tween(TOOLTIP_FADE_MILLIS)) }
    Popup(
        popupPositionProvider = remember(offset) { BelowOrAbove(offset) },
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = false),
    ) {
        // AppCompat's tooltip frame: the theme's inverse, a little translucent, 2dp corners.
        Box(
            Modifier
                .graphicsLayer { alpha = shown.value }
                .background(colors.ink.copy(alpha = 0.9f), RoundedCornerShape(2.dp))
                .padding(horizontal = 16.dp, vertical = 6.5.dp),
        ) {
            PlateText(label, plateText(14.sp, colors.ground), maxLines = 1)
        }
    }
}

/** Centred under the anchor, [offset] below it, or above it when there is no room below. */
private class BelowOrAbove(
    private val offset: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x =
            (anchorBounds.center.x - popupContentSize.width / 2)
                .coerceAtMost(windowSize.width - popupContentSize.width)
                .coerceAtLeast(0)
        val below = anchorBounds.bottom + offset
        val y =
            if (below + popupContentSize.height <= windowSize.height) {
                below
            } else {
                (anchorBounds.top - offset - popupContentSize.height).coerceAtLeast(0)
            }
        return IntOffset(x, y)
    }
}

private val TOOLTIP_OFFSET = 16.dp
private const val TOOLTIP_FADE_MILLIS = 150
