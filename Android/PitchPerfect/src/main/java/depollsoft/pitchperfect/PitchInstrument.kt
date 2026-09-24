package depollsoft.pitchperfect

import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import android.graphics.Rect
import android.provider.Settings
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset

/** Interaction state for [model]'s face, with the host view's haptics and motion setting. */
@Composable
fun rememberPitchInstrumentState(model: PitchPipeModel): PitchInstrumentState {
    val view = LocalView.current
    val context = LocalContext.current
    return remember(model) {
        PitchInstrumentState(
            model,
            haptic = { view.performHapticFeedback(it) },
            reduceMotion = {
                Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
            },
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

    val breathing = state.breathing
    LaunchedEffect(breathing) {
        if (!breathing) {
            state.breathePhase = 0f
            return@LaunchedEffect
        }
        // An infinite-animation frame loop, so tests and the system's animation policy can tell
        // it apart from work that will finish.
        val start = withInfiniteAnimationFrameMillis { it }
        while (true) {
            withInfiniteAnimationFrameMillis { now -> state.breathePhase = PitchInstrumentState.breathePhaseAt(now - start) }
        }
    }

    Box(
        modifier
            .testTag(TestTags.PITCH_INSTRUMENT)
            .onSizeChanged { state.resize(it.width, it.height) }
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


private suspend fun PointerInputScope.trackFingers(state: PitchInstrumentState) {
    awaitEachGesture {
        val first = awaitFirstDown(requireUnconsumed = false)
        // A finger on a cell or the range selector belongs to the instrument: consuming its
        // events keeps the tab pager from taking a slide between notes as a swipe.
        val owned = state.down(first.id.value, first.position.x, first.position.y, firstFinger = true)
        if (owned) first.consume()
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
                    if (owned) change.consume()
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
private fun AccessibilityTargets(state: PitchInstrumentState) {
    val geometry = state.geometry
    val notes = state.notes
    if (geometry.width == 0) return
    for (index in 0 until minOf(geometry.cellCenters.size, notes.size)) {
        val note = notes[index]
        Target(geometry.cellBounds(index), NoteNames.spoken(note), selected = note.isPlaying) {
            state.accessibilityClick(index)
        }
    }
    val low = stringResource(R.string.RangeLowDescription)
    val high = stringResource(R.string.RangeHighDescription)
    Target(geometry.rangeLowRect, low, selected = !state.model.isFromFToF) { state.accessibilitySelectRange(false) }
    Target(geometry.rangeHighRect, high, selected = state.model.isFromFToF) { state.accessibilitySelectRange(true) }
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
            .size(
                with(density) { bounds.width().coerceAtLeast(1).toDp() },
                with(density) { bounds.height().coerceAtLeast(1).toDp() },
            ).activatedByKeys(onClick)
            .semantics {
                contentDescription = description
                role = Role.Button
                this.selected = selected
                onClick(action = onClick)
            },
    )
}

/**
 * Lets a keyboard or D-pad reach a face target and activate it with Enter, the D-pad centre or
 * Space, as the View's ExploreByTouchHelper did. Like the View, no focus ring is drawn.
 */
private fun Modifier.activatedByKeys(onClick: () -> Boolean): Modifier =
    onKeyEvent { event ->
        event.type == KeyEventType.KeyUp &&
            event.key in listOf(Key.Enter, Key.NumPadEnter, Key.DirectionCenter, Key.Spacebar) &&
            onClick()
    }.focusable()
