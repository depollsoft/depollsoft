package depollsoft.pitchperfect

import android.graphics.Rect
import android.provider.Settings
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
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
            reduceMotion = {
                Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
            },
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
 * screen reader drive [state].
 */
@Composable
fun WearPitchInstrument(
    state: WearInstrumentState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val renderer = remember(context) { WearInstrumentRenderer(context) }

    val breathing = state.breathing
    LaunchedEffect(breathing) {
        if (!breathing) {
            state.breathePhase = 0f
            return@LaunchedEffect
        }
        // An infinite-animation frame loop, so tests and the system's animation policy can
        // tell it apart from work that will finish.
        val start = withInfiniteAnimationFrameMillis { it }
        while (true) {
            withInfiniteAnimationFrameMillis { now -> state.breathePhase = WearInstrumentState.breathePhaseAt(now - start) }
        }
    }

    Box(
        modifier
            .onSizeChanged { state.resize(it.width, it.height) }
            .onRotaryScrollEvent {
                // Compose reports the crown in pixels, opposite in sign to the raw scroll axis.
                state.rotate(-it.verticalScrollPixels)
                true
            }.focusable()
            .pointerInput(state) { trackFingers(state) }
            .drawBehind {
                drawIntoCanvas {
                    renderer.draw(it.nativeCanvas, state.geometry, state.notes, state.model.isFromFToF, state.breathePhase)
                }
            },
    ) {
        AccessibilityTargets(state)
    }
}

private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.trackFingers(state: WearInstrumentState) {
    awaitEachGesture {
        val first = awaitFirstDown(requireUnconsumed = false)
        state.down(first.id.value, first.position.x, first.position.y, firstFinger = true)
        first.consume()
        try {
            while (true) {
                val event = awaitPointerEvent()
                val stillDown = event.changes.any { it.pressed }
                for (change in event.changes) {
                    when {
                        change.changedToDownIgnoreConsumed() ->
                            state.down(change.id.value, change.position.x, change.position.y, firstFinger = false)
                        change.changedToUpIgnoreConsumed() -> if (stillDown) state.up(change.id.value)
                        change.pressed && change.positionChanged() ->
                            state.move(change.id.value, change.position.x, change.position.y)
                    }
                    change.consume()
                }
                if (!stillDown) break
            }
        } finally {
            state.endGesture()
        }
    }
}

/**
 * One invisible node per cell and range row, laid over what the renderer draws, so a screen
 * reader can find, read and activate each part of the face. They carry semantics only; fingers
 * pass through to the face.
 */
@Composable
private fun AccessibilityTargets(state: WearInstrumentState) {
    val geometry = state.geometry
    val notes = state.notes
    val radius = geometry.cellRadius
    for (index in 0 until minOf(geometry.cellCenters.size, notes.size)) {
        val c = geometry.cellCenters[index]
        val note = notes[index]
        Target(
            Rect((c[0] - radius).toInt(), (c[1] - radius).toInt(), (c[0] + radius).toInt(), (c[1] + radius).toInt()),
            NoteNames.spoken(note),
            selected = note.isPlaying,
        ) { state.accessibilityClick(index) }
    }
    val low = stringResource(R.string.RangeLowDescription)
    val high = stringResource(R.string.RangeHighDescription)
    Target(Rect().also { geometry.rangeLowRect.roundOut(it) }, low, selected = !state.model.isFromFToF) {
        state.selectRange(false)
        true
    }
    Target(Rect().also { geometry.rangeHighRect.roundOut(it) }, high, selected = state.model.isFromFToF) {
        state.selectRange(true)
        true
    }
}

@Composable
private fun Target(
    bounds: Rect,
    description: String,
    selected: Boolean,
    onClick: () -> Boolean,
) {
    val density = LocalDensity.current
    Box(
        Modifier
            .offset { IntOffset(bounds.left, bounds.top) }
            .size(with(density) { bounds.width().coerceAtLeast(1).toDp() }, with(density) { bounds.height().coerceAtLeast(1).toDp() })
            .semantics {
                contentDescription = description
                role = Role.Button
                this.selected = selected
                onClick(action = onClick)
            },
    )
}
