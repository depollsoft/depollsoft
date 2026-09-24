package depollsoft.pitchperfect

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect

/**
 * The Pitch Pipe tab. Leaving the tab, or the app, silences it; coming back picks up a changed
 * "notes play until pressed again" setting.
 */
@Composable
fun PitchPipeScreen(
    model: PitchPipeModel,
    isCurrentPage: Boolean,
    state: PitchInstrumentState = rememberPitchInstrumentState(model),
) {
    LifecycleResumeEffect(state) {
        state.toggleMode = SettingsModel.toggleNotes
        onPauseOrDispose { state.stopAll() }
    }
    OnPageVisibilityChange(isCurrentPage) { current -> if (!current) state.stopAll() }
    PitchInstrument(state, Modifier.fillMaxSize())
}
