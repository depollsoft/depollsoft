package depollsoft.pitchperfect

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect

/**
 * The Pitch Pipe tab: the radial face, or the classic grid when Settings asks for it. Leaving the
 * tab, or the app, silences it, and so does changing face. The "notes play until pressed again"
 * setting is picked up as it changes (a change synced from another device included), and read
 * again whenever the tab or the app comes back.
 */
@Composable
fun PitchPipeScreen(
    model: PitchPipeModel,
    isCurrentPage: Boolean,
    state: PitchInstrumentState = rememberPitchInstrumentState(model),
    classicState: ClassicPitchPipeState = rememberClassicPitchPipeState(model),
    classic: Boolean = classicSetting(),
) {
    val face: InstrumentState<*> = if (classic) classicState else state
    LifecycleResumeEffect(face) {
        face.toggleMode = SettingsModel.toggleNotes
        onPauseOrDispose { face.stopAll() }
    }
    LaunchedEffect(face) { snapshotFlow { SettingsModel.toggleNotes }.collect { face.toggleMode = it } }
    OnPageVisibilityChange(isCurrentPage) { current ->
        if (current) face.toggleMode = SettingsModel.toggleNotes else face.stopAll()
    }
    if (classic) {
        ClassicPitchPipe(classicState, Modifier.fillMaxSize())
    } else {
        PitchInstrument(state, Modifier.fillMaxSize())
    }
}

/**
 * Whether Settings asks for the classic grid. Settings is another activity, so the setting is read
 * again whenever this one comes back, whether or not the preference store announced the change.
 */
@Composable
private fun classicSetting(): Boolean {
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        resumes++
        onPauseOrDispose {}
    }
    return resumes.let { SettingsModel.classicPitchPipe }
}
