package depollsoft.pitchperfect

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect

/**
 * The Pitch Pipe tab. Leaving the tab, or the app, silences it. The "notes play until pressed
 * again" setting is picked up as it changes (a change synced from another device included), and
 * read again whenever the tab or the app comes back.
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
    LaunchedEffect(state) { snapshotFlow { SettingsModel.toggleNotes }.collect { state.toggleMode = it } }
    OnPageVisibilityChange(isCurrentPage) { current ->
        if (current) state.toggleMode = SettingsModel.toggleNotes else state.stopAll()
    }
    PitchInstrument(state, Modifier.fillMaxSize())
}
