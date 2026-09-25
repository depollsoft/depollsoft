package depollsoft.pitchperfect

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import android.graphics.Rect
import android.provider.Settings
import android.view.View
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
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
    // The release capture (scripts/release/wear.py) waits for this resource id.
    WearPitchInstrument(
        state,
        Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }.testTag("pitchInstrument").focusRequester(focusRequester),
    )
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
    val view = LocalView.current
    val renderer = remember(context) { WearInstrumentRenderer(context) }

    val breathing = state.breathing
    LaunchedEffect(breathing) {
        if (!breathing) {
            state.breathePhase = 0f
            return@LaunchedEffect
        }
        // An infinite-animation frame loop, so tests and the system's animation policy can
        // tell it apart from work that will finish.
        val scale = animatorDurationScale(context)
        val start = withInfiniteAnimationFrameMillis { it }
        while (true) {
            withInfiniteAnimationFrameMillis { now -> state.breathePhase = WearInstrumentState.breathePhaseAt(now - start, scale) }
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
            .pointerInput(state, view) { trackFingers(state, view) }
            .drawBehind {
                drawIntoCanvas {
                    renderer.draw(it.nativeCanvas, state.geometry, state.notes, state.model.isFromFToF, state.breathePhase)
                }
            },
    ) {
        AccessibilityTargets(state)
    }
}

private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.trackFingers(
    state: WearInstrumentState,
    view: View,
) {
    awaitEachGesture {
        val first = awaitFirstDown(requireUnconsumed = false)
        // A finger on a cell or the range selector belongs to the face: sliding between notes
        // must not become the system's swipe-to-dismiss, which would close the app mid-note.
        // Consuming the events is not enough; the swipe is taken by the window above Compose.
        val onControl = state.geometry.rangeRowAt(first.position.x, first.position.y) != -1 ||
            state.geometry.cellAt(first.position.x, first.position.y) != -1
        if (onControl) view.parent?.requestDisallowInterceptTouchEvent(true)
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
            if (onControl) view.parent?.requestDisallowInterceptTouchEvent(false)
            state.endGesture()
        }
    }
}

/**
 * One invisible node per cell and range row, laid over what the renderer draws, so a screen
 * reader can find, read and activate each part of the face. Each covers the area a finger would
 * reach that part from, so exploring by touch finds what a tap would play. They carry semantics
 * only; fingers pass through to the face.
 */
@Composable
private fun AccessibilityTargets(state: WearInstrumentState) {
    val geometry = state.geometry
    val notes = state.notes
    for (index in 0 until minOf(geometry.cellCenters.size, notes.size)) {
        val note = notes[index]
        Target(geometry.cellTarget(index), NoteNames.spoken(note), selected = note.isPlaying) {
            state.accessibilityClick(index)
        }
    }
    val low = stringResource(R.string.RangeLowDescription)
    val high = stringResource(R.string.RangeHighDescription)
    Target(geometry.rangeTarget(0), low, selected = !state.model.isFromFToF) {
        state.selectRange(false)
        true
    }
    Target(geometry.rangeTarget(1), high, selected = state.model.isFromFToF) {
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
            .activatedByKeys(onClick)
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
