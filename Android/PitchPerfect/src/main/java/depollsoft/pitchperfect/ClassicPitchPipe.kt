package depollsoft.pitchperfect

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
 * behave exactly as on the radial face; only the layout differs. [context] gives the grid its
 * density and the range labels' widths.
 */
class ClassicPitchPipeState(
    model: PitchPipe,
    context: Context,
    haptic: (Int) -> Unit = {},
    reduceMotion: () -> Boolean = { false },
    onNoteStarted: () -> Unit = {},
) : InstrumentState<ClassicPitchPipeGeometry>(
        ClassicOctave(model),
        classicGeometry(context),
        haptic,
        reduceMotion,
        tapEveryToggle = true,
        screenReaderFeedback = false,
        onNoteStarted = onNoteStarted,
    )

private fun classicGeometry(context: Context): (Int, Int, Int) -> ClassicPitchPipeGeometry {
    val density = context.resources.displayMetrics.density
    val labelWidths = ClassicPitchPipeRenderer.rangeLabelWidths(context)
    return { width, height, cells -> ClassicPitchPipeGeometry(width, height, cells, density, labelWidths) }
}

/** Interaction state for [model]'s classic grid, with the host view's haptics and motion setting. */
@Composable
fun rememberClassicPitchPipeState(model: PitchPipeModel): ClassicPitchPipeState {
    val view = LocalView.current
    val context = LocalContext.current
    return remember(model, context) {
        ClassicPitchPipeState(
            model,
            context,
            haptic = { view.performHapticFeedback(it) },
            reduceMotion = { systemAnimationsOff(context) },
            onNoteStarted = { PitchPerfectAnalytics.pitchPlayed(PitchPerfectAnalytics.Source.CLASSIC_PITCH_PIPE) },
        )
    }
}

/**
 * The classic pitch pipe: the old grid of big buttons, twelve round the edge of a four-by-four
 * grid, with the readout and the range choices in the middle. Settings switches it on in place of
 * the radial face.
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
        rangeLabels = stringResource(R.string.ClassicRangeLowDescription) to stringResource(R.string.ClassicRangeHighDescription),
        draw = renderer::draw,
        modifier = modifier.testTag(TestTags.CLASSIC_PITCH_PIPE),
    )
}
