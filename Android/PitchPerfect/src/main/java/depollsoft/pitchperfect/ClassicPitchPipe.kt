package depollsoft.pitchperfect

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import depollsoft.pitchperfect.lib.Note

/**
 * The pitch pipe as the classic grid shows it: one octave, the pipe's first twelve notes (C to B,
 * or F to E), as the app had before the inclusive octave. The range is the pipe's own.
 */
class ClassicOctave(
    private val pipe: PitchPipe,
) : PitchPipe by pipe {
    override val notes: List<Note>
        get() = pipe.notes.let { it.subList(0, minOf(it.size, ClassicPitchPipeGeometry.CELLS)) }
}

/**
 * The classic grid's interaction state. Fingers, chords, slides, toggle mode and the screen reader
 * behave exactly as on the radial face; only the layout differs.
 *
 * @param density pixels per dp, for the grid's margins and range rows.
 */
class ClassicPitchPipeState(
    model: PitchPipe,
    density: Float,
    haptic: (Int) -> Unit = {},
    reduceMotion: () -> Boolean = { false },
) : InstrumentState<ClassicPitchPipeGeometry>(
        ClassicOctave(model),
        { width, height, cells -> ClassicPitchPipeGeometry(width, height, cells, density) },
        haptic,
        reduceMotion,
        tapEveryToggle = true,
        screenReaderFeedback = false,
    )

/** Interaction state for [model]'s classic grid, with the host view's haptics and motion setting. */
@Composable
fun rememberClassicPitchPipeState(model: PitchPipeModel): ClassicPitchPipeState {
    val view = LocalView.current
    val context = LocalContext.current
    val density = LocalDensity.current.density
    return remember(model, density) {
        ClassicPitchPipeState(
            model,
            density,
            haptic = { view.performHapticFeedback(it) },
            reduceMotion = { systemAnimationsOff(context) },
        )
    }
}

/**
 * The classic pitch pipe: the old grid of big buttons, twelve round the edge of a four-by-four
 * grid with the range choices in the middle. Settings switches it on in place of the radial face.
 */
@Composable
fun ClassicPitchPipe(
    state: ClassicPitchPipeState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val renderer = remember(context) { ClassicPitchPipeRenderer(context) }
    PitchInstrumentFace(
        state,
        rangeLabels = stringResource(R.string.ClassicCtoB) to stringResource(R.string.ClassicFtoE),
        draw = renderer::draw,
        modifier = modifier.testTag(TestTags.CLASSIC_PITCH_PIPE),
    )
}
