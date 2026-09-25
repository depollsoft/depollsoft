package depollsoft.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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

    internal fun mouseEntered(timeout: Long) {
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
 * hides it when the mouse leaves. Touch and stylus pointers do not hover.
 */
fun Modifier.tooltipOnHover(state: LabelTooltipState): Modifier =
    pointerInput(state) {
        val timeout = viewConfiguration.longPressTimeoutMillis
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.changes.none { it.type == PointerType.Mouse }) continue
                when (event.type) {
                    PointerEventType.Enter -> state.mouseEntered(timeout)
                    PointerEventType.Exit -> state.dismiss()
                }
            }
        }
    }
