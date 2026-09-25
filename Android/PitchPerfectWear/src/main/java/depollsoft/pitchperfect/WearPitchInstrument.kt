package depollsoft.pitchperfect

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect

/** Interaction state for [model]'s face, with the host view's haptics and motion setting. */
@Composable
fun rememberWearInstrumentState(model: PitchPipeModel): WearInstrumentState {
    val view = LocalView.current
    val context = LocalContext.current
    return remember(model) {
        WearInstrumentState(
            model,
            haptic = { view.performHapticFeedback(it) },
            reduceMotion = { systemAnimationsOff(context) },
        )
    }
}

/** The watch app's one screen: the pitch-pipe face for [model], filling the display. */
@Composable
fun WearPitchPipeScreen(
    model: PitchPipeModel,
    state: WearInstrumentState = rememberWearInstrumentState(model),
) {
    val focusRequester = remember { FocusRequester() }
    LifecycleResumeEffect(state) {
        state.toggleMode = SettingsModel.getToggleNotes()
        // The crown scrolls whichever node has focus.
        focusRequester.requestFocus()
        onPauseOrDispose { state.stopAll() }
    }
    WearPitchInstrument(state, Modifier.fillMaxSize().focusRequester(focusRequester))
}

/**
 * The radial pitch-pipe face. [WearInstrumentRenderer] paints it; fingers, the crown and a
 * screen reader drive [state]. A finger on a note or the range selector keeps the system's
 * swipe-to-dismiss away until it lifts, so sliding between notes never closes the app.
 */
@Composable
fun WearPitchInstrument(
    state: WearInstrumentState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val renderer = remember(context) { WearInstrumentRenderer(context) }
    PitchInstrumentFace(
        state,
        rangeLabels = stringResource(R.string.RangeLowDescription) to stringResource(R.string.RangeHighDescription),
        draw = renderer::draw,
        modifier =
            modifier
                .onRotaryScrollEvent {
                    // Compose reports the crown in pixels, opposite in sign to the raw scroll axis.
                    state.rotate(-it.verticalScrollPixels)
                    true
                }.focusable(),
    )
}
