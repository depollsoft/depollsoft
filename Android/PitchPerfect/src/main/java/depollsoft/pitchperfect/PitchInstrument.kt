package depollsoft.pitchperfect

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource

/** Interaction state for [model]'s face, with the host view's haptics and motion setting. */
@Composable
fun rememberPitchInstrumentState(model: PitchPipeModel): PitchInstrumentState {
    val view = LocalView.current
    val context = LocalContext.current
    return remember(model) {
        PitchInstrumentState(
            model,
            haptic = { view.performHapticFeedback(it) },
            reduceMotion = { systemAnimationsOff(context) },
        )
    }
}

/**
 * The radial pitch-pipe face. [PitchInstrumentRenderer] paints it; fingers and a screen reader
 * drive [state].
 */
@Composable
fun PitchInstrument(
    state: PitchInstrumentState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val renderer = remember(context) { PitchInstrumentRenderer(context) }
    PitchInstrumentFace(
        state,
        rangeLabels = stringResource(R.string.RangeLowDescription) to stringResource(R.string.RangeHighDescription),
        draw = renderer::draw,
        modifier = modifier.testTag(TestTags.PITCH_INSTRUMENT),
    )
}
