package depollsoft.pitchperfect.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Whether a control's tooltip is showing, and for how long. Long-pressing the control shows it
 * for 2.5 seconds; resting a mouse on it shows it after the long-press timeout until the pointer
 * leaves, as AppCompat's TooltipCompat did for toolbar and navigation icons.
 */
@Stable
class TooltipState {
    var visible by mutableStateOf(false)
        private set

    private var hide: Job? = null

    internal fun show(
        scope: kotlinx.coroutines.CoroutineScope,
        forMs: Long?,
    ) {
        visible = true
        hide?.cancel()
        hide =
            forMs?.let {
                scope.launch {
                    delay(it)
                    visible = false
                }
            }
    }

    fun dismiss() {
        hide?.cancel()
        visible = false
    }

    companion object {
        const val LONG_PRESS_SHOW_MS = 2_500L
        const val HOVER_SHOW_MS = 15_000L
    }
}

/**
 * [content] with a tooltip reading [label]. The content wires the long press it is handed to its
 * own clickable (`combinedClickable(onLongClick = …)`), so the press shows the tooltip with the
 * long-press buzz; a mouse resting on the content shows it too.
 */
@Composable
fun WithTooltip(
    label: String,
    modifier: Modifier = Modifier,
    state: TooltipState = remember { TooltipState() },
    content: @Composable BoxScope.(showTooltip: () -> Unit) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val longPressTimeout = LocalViewConfiguration.current.longPressTimeoutMillis
    val showOnPress: () -> Unit = {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        state.show(scope, TooltipState.LONG_PRESS_SHOW_MS)
    }
    Box(
        modifier.pointerInput(state) {
            awaitPointerEventScope {
                var pending: Job? = null
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.changes.none { it.type == PointerType.Mouse }) continue
                    when (event.type) {
                        PointerEventType.Enter -> {
                            pending?.cancel()
                            pending =
                                scope.launch {
                                    delay(longPressTimeout)
                                    state.show(scope, TooltipState.HOVER_SHOW_MS)
                                }
                        }
                        PointerEventType.Exit -> {
                            pending?.cancel()
                            state.dismiss()
                        }
                    }
                }
            }
        },
    ) {
        content(showOnPress)
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
    LaunchedEffect(Unit) { shown.animateTo(1f, tween(TOOLTIP_FADE_MS)) }
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
private const val TOOLTIP_FADE_MS = 150
