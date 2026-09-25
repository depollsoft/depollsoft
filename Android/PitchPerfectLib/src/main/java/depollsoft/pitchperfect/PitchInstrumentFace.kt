package depollsoft.pitchperfect

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.provider.Settings
import android.view.View
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import depollsoft.pitchperfect.lib.Note

/** Whether the system asks for animations to be switched off ("Animator duration scale" off). */
fun systemAnimationsOff(context: Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

/**
 * A radial pitch-pipe face. [draw] paints it on the platform canvas; fingers, keys and a screen
 * reader drive [state]. [rangeLabels] are what a screen reader says for the C-to-C and F-to-F rows.
 */
@Composable
fun <G : InstrumentGeometry> PitchInstrumentFace(
    state: InstrumentState<G>,
    rangeLabels: Pair<String, String>,
    draw: (canvas: Canvas, geometry: G, notes: List<Note>, fromFToF: Boolean, breathePhase: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val view = LocalView.current

    val breathing = state.breathing
    LaunchedEffect(breathing) {
        if (!breathing) {
            state.breathePhase = 0f
            return@LaunchedEffect
        }
        // An infinite-animation frame loop, so tests and the system's animation policy can tell
        // it apart from work that will finish.
        val scale = animatorDurationScale(context)
        val start = withInfiniteAnimationFrameMillis { it }
        while (true) {
            withInfiniteAnimationFrameMillis { now ->
                state.breathePhase = InstrumentState.breathePhaseAt(now - start, scale)
            }
        }
    }

    Box(
        modifier
            .onSizeChanged { state.resize(it.width, it.height) }
            .pointerInput(state, view) { trackFingers(state, view) }
            .drawBehind {
                drawIntoCanvas {
                    draw(it.nativeCanvas, state.geometry, state.notes, state.model.isFromFToF, state.breathePhase)
                }
            },
    ) {
        AccessibilityTargets(state, rangeLabels)
    }
}

private suspend fun PointerInputScope.trackFingers(
    state: InstrumentState<*>,
    view: View,
) {
    awaitEachGesture {
        val first = awaitFirstDown(requireUnconsumed = false)
        // A finger on a cell or the range selector belongs to the face. Consuming its events keeps
        // a Compose pager from taking a slide between notes as a swipe; the views above Compose
        // (the watch's swipe-to-dismiss) have to be told not to intercept it.
        val owned = state.down(first.id.value, first.position.x, first.position.y, firstFinger = true)
        if (owned) {
            first.consume()
            view.parent?.requestDisallowInterceptTouchEvent(true)
        }
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
            if (owned) view.parent?.requestDisallowInterceptTouchEvent(false)
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
private fun AccessibilityTargets(
    state: InstrumentState<*>,
    rangeLabels: Pair<String, String>,
) {
    val geometry = state.geometry
    val notes = state.notes
    if (geometry.width == 0) return
    for (index in 0 until minOf(geometry.cellCenters.size, notes.size)) {
        val note = notes[index]
        Target(geometry.cellTarget(index), NoteNames.spoken(note), selected = note.isPlaying) {
            state.accessibilityClick(index)
        }
    }
    Target(geometry.rangeTarget(0), rangeLabels.first, selected = !state.model.isFromFToF) {
        state.accessibilitySelectRange(false)
    }
    Target(geometry.rangeTarget(1), rangeLabels.second, selected = state.model.isFromFToF) {
        state.accessibilitySelectRange(true)
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
 * Space, as an ExploreByTouchHelper's virtual views are. No focus ring is drawn.
 */
private fun Modifier.activatedByKeys(onClick: () -> Boolean): Modifier =
    onKeyEvent { event ->
        event.type == KeyEventType.KeyUp &&
            event.key in listOf(Key.Enter, Key.NumPadEnter, Key.DirectionCenter, Key.Spacebar) &&
            onClick()
    }.focusable()
