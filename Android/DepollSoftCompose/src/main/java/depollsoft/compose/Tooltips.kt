package depollsoft.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Whether an icon-only control's label tooltip is showing, as AppCompat's TooltipCompat showed it
 * for toolbar actions, Up, the overflow button and navigation items: a long press shows it for
 * 2.5 seconds, and a mouse resting on the control shows it after the long-press timeout until the
 * pointer leaves (at most 15 seconds). The long press's buzz is the anchor's own: Compose's
 * `combinedClickable` gives it one, so the tooltip adds none.
 */
@Stable
class LabelTooltipState internal constructor(
    private val scope: CoroutineScope,
) {
    var visible by mutableStateOf(false)
        private set

    private var hide: Job? = null
    private var hover: Job? = null

    /** The anchor was long-pressed. */
    fun longPressed() = showFor(LONG_PRESS_SHOW_MILLIS)

    fun dismiss() {
        hover?.cancel()
        hide?.cancel()
        visible = false
    }

    /**
     * The mouse came to (or moved on to a new spot on) the anchor: the tooltip shows once it has
     * rested there for [timeout]. Moving on restarts the wait, so sweeping across a toolbar
     * shows nothing.
     */
    internal fun mouseEntered(timeout: Long) {
        if (visible) return
        hover?.cancel()
        hover =
            scope.launch {
                delay(timeout)
                showFor(HOVER_SHOW_MILLIS)
            }
    }

    private fun showFor(millis: Long) {
        visible = true
        hide?.cancel()
        hide =
            scope.launch {
                delay(millis)
                visible = false
            }
    }

    companion object {
        const val LONG_PRESS_SHOW_MILLIS = 2500L
        const val HOVER_SHOW_MILLIS = 15000L
    }
}

@Composable
fun rememberLabelTooltipState(): LabelTooltipState {
    val scope = rememberCoroutineScope()
    return remember(scope) { LabelTooltipState(scope) }
}

/**
 * Shows [state]'s tooltip when a mouse rests on this element for the long-press timeout, and
 * hides it when the mouse leaves. As AppCompat's TooltipCompatHandler did, a move further than the
 * hover slop (half the touch slop) restarts the wait, so the mouse has to come to rest. Touch and
 * stylus pointers do not hover.
 */
fun Modifier.tooltipOnHover(state: LabelTooltipState): Modifier =
    pointerInput(state) {
        val timeout = viewConfiguration.longPressTimeoutMillis
        val slop = viewConfiguration.touchSlop / 2
        var anchor = Offset.Unspecified
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                val mouse = event.changes.firstOrNull { it.type == PointerType.Mouse } ?: continue
                when (event.type) {
                    PointerEventType.Enter -> {
                        anchor = mouse.position
                        state.mouseEntered(timeout)
                    }
                    PointerEventType.Move ->
                        if (!anchor.isSpecified || (mouse.position - anchor).getDistance() > slop) {
                            anchor = mouse.position
                            state.mouseEntered(timeout)
                        }
                    PointerEventType.Exit -> {
                        anchor = Offset.Unspecified
                        state.dismiss()
                    }
                }
            }
        }
    }
