package depollsoft.tagmaster.ui

import android.view.ViewConfiguration
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** What a [WithTooltip] anchor calls when it is long-pressed. */
@Stable
fun interface TooltipTrigger {
    fun longPressed()
}

/**
 * Names an icon-only control in a tooltip, as AppCompat's TooltipCompat did for toolbar actions,
 * the Up button and the overflow button: a long press (which the anchor's combinedClickable gives
 * the long-press haptic) shows it for 2.5 seconds, and a mouse resting on the control shows it
 * after the long-press timeout until the pointer leaves (at most 15 seconds). [content] wires the
 * trigger into its long click.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithTooltip(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable (TooltipTrigger) -> Unit,
) {
    val state = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()
    val showing = remember { arrayOfNulls<Job>(1) }

    fun showFor(millis: Long) {
        showing[0]?.cancel()
        showing[0] =
            scope.launch {
                try {
                    launch { state.show() }
                    delay(millis)
                } finally {
                    state.dismiss()
                }
            }
    }
    // combinedClickable already gives a long click its haptic.
    val trigger = remember { TooltipTrigger { showFor(LONG_PRESS_HIDE_MILLIS) } }
    // The caller's modifier (a layoutId, say) goes on this Box: TooltipBox does not put its own
    // modifier on the node a parent layout measures.
    Box(modifier) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
            tooltip = { PlainTooltip { Text(label, Modifier.testTag("tooltip")) } },
            state = state,
            focusable = false,
            enableUserInput = false,
        ) {
            Box(
                Modifier.pointerInput(Unit) {
                    awaitPointerEventScope {
                        var pending: Job? = null
                        while (true) {
                            val event = awaitPointerEvent()
                            when (event.type) {
                                PointerEventType.Enter -> {
                                    pending?.cancel()
                                    pending =
                                        scope.launch {
                                            delay(ViewConfiguration.getLongPressTimeout().toLong())
                                            showFor(HOVER_HIDE_MILLIS)
                                        }
                                }
                                PointerEventType.Exit -> {
                                    pending?.cancel()
                                    showing[0]?.cancel()
                                }
                            }
                        }
                    }
                },
            ) { content(trigger) }
    }
    }
}

private const val LONG_PRESS_HIDE_MILLIS = 2500L
private const val HOVER_HIDE_MILLIS = 15000L
