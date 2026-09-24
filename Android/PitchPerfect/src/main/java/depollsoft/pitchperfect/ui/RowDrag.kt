package depollsoft.pitchperfect.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * A drag of one lazy-list row by its handle, as ItemTouchHelper did it: the row lifts as the
 * finger lands, follows it, trades places with a neighbour once it passes that neighbour's middle
 * (the neighbour slides aside), and holding it near the top or bottom edge scrolls the list so it
 * can travel the whole list in one drag. On release it settles into its slot.
 *
 * @param indexOf the dragged row's current index, from its key.
 * @param canMove whether the row at `from` may trade places with the one at `to`.
 * @param move trades two adjacent rows; not stored until [onDrop].
 * @param onDrop the finger lifted or the gesture ended: store the order once.
 * @param haptics ticks as the drag starts, as each neighbour is passed, and as it ends.
 */
class RowDrag(
    private val listState: LazyListState,
    private val indexOf: (Any) -> Int,
    private val canMove: (from: Int, to: Int) -> Boolean,
    private val move: (from: Int, to: Int) -> Unit,
    private val onDrop: () -> Unit,
    private val haptics: HapticFeedback? = null,
) {
    /** The key of the row being dragged or settling into its slot, or null. */
    var key by mutableStateOf<Any?>(null)
        private set

    /** Whether a finger still holds the row (false while it settles). */
    var held by mutableStateOf(false)
        private set

    /** How far the dragged row is drawn from its slot. */
    var offset by mutableFloatStateOf(0f)
        private set

    /** How far the row is lifted, from 0 (in the list) to 1 (held above it). */
    var lift by mutableFloatStateOf(0f)
        private set

    private var settle: Job? = null
    private var liftJob: Job? = null

    /** Follows one drag on the handle of the row with [rowKey]. */
    suspend fun track(
        scope: PointerInputScope,
        rowKey: Any,
    ) = coroutineScope {
        val gestures = this
        val edge = with(scope) { EDGE_ZONE_DP.dp.toPx() }
        val maxStep = with(scope) { MAX_SCROLL_DP_PER_FRAME.dp.toPx() }
        scope.awaitEachGesture {
            val down = awaitFirstDown()
            down.consume()
            // A new drag while the last row is still settling finishes that settle at once.
            settle?.cancel()
            settle = null
            key = rowKey
            held = true
            offset = 0f
            haptics?.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
            liftJob?.cancel()
            liftJob = launch { animateLift(1f, LIFT_MS) }
            val scroller =
                launch {
                    while (isActive) {
                        // Frames are only requested while there is something to scroll, so an
                        // idle drag leaves the frame clock (and tests) alone.
                        snapshotFlow { edgeScrollStep(edge, maxStep) != 0f }.first { it }
                        while (isActive) {
                            withFrameNanos { }
                            val step = edgeScrollStep(edge, maxStep)
                            if (step == 0f) break
                            val scrolled = listState.scrollBy(step)
                            // The slot moved with the content; keep the row under the finger.
                            offset += scrolled
                            swapIfPastANeighbour()
                        }
                    }
                }
            try {
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break
                    val delta = change.position.y - change.previousPosition.y
                    change.consume()
                    if (delta != 0f) {
                        offset += delta
                        swapIfPastANeighbour()
                    }
                }
            } finally {
                scroller.cancel()
                held = false
                haptics?.performHapticFeedback(HapticFeedbackType.GestureEnd)
                onDrop()
                liftJob?.cancel()
                if (gestures.isActive) {
                    settle = launch { settleInto(rowKey) }
                } else {
                    // The row left composition mid-drag: nothing is left to animate.
                    key = null
                    offset = 0f
                    lift = 0f
                }
            }
        }
    }

    /** The released row glides from where the finger left it to its slot, and sets down. */
    private suspend fun settleInto(rowKey: Any) {
        try {
            coroutineScope {
                launch { animateLift(0f, SETTLE_MS) }
                Animatable(offset).animateTo(0f, tween(SETTLE_MS, easing = FastOutSlowInEasing)) { offset = value }
            }
        } finally {
            if (key == rowKey && !held) {
                key = null
                offset = 0f
                lift = 0f
            }
        }
    }

    private suspend fun animateLift(
        target: Float,
        durationMs: Int,
    ) {
        Animatable(lift).animateTo(target, tween(durationMs, easing = FastOutSlowInEasing)) { lift = value }
    }

    /** How far to scroll this frame: faster the deeper the dragged row sits in an edge zone. */
    private fun edgeScrollStep(
        edge: Float,
        maxStep: Float,
    ): Float {
        if (!held) return 0f
        val info = listState.layoutInfo
        val row = info.visibleItemsInfo.firstOrNull { it.key == key } ?: return 0f
        val top = row.offset + offset
        val bottom = top + row.size
        // ItemTouchHelper scrolled once the row left the list's padded area: with the Songs
        // list's 90dp bottom padding, that is well above the screen's edge.
        val start = info.viewportStartOffset.toFloat() + info.beforeContentPadding
        val end = info.viewportEndOffset.toFloat() - info.afterContentPadding
        return when {
            top < start + edge && listState.canScrollBackward -> -maxStep * ((start + edge - top) / edge).coerceAtMost(1f)
            bottom > end - edge && listState.canScrollForward -> maxStep * ((bottom - (end - edge)) / edge).coerceAtMost(1f)
            else -> 0f
        }
    }

    private fun swapIfPastANeighbour() {
        val rowKey = key ?: return
        val index = indexOf(rowKey)
        val items = listState.layoutInfo.visibleItemsInfo
        val current = items.firstOrNull { it.index == index } ?: return
        val middle = current.offset + offset + current.size / 2f
        val next = items.firstOrNull { it.index == index + 1 }
        val previous = items.firstOrNull { it.index == index - 1 }
        if (offset > 0 && next != null && middle > next.offset + next.size / 2f && canMove(index, index + 1)) {
            swap(index, index + 1)
            offset -= next.size
        } else if (offset < 0 && previous != null && middle < previous.offset + previous.size / 2f && canMove(index, index - 1)) {
            swap(index, index - 1)
            offset += previous.size
        }
    }

    private fun swap(
        from: Int,
        to: Int,
    ) {
        // A lazy list keeps its first visible row in place by key; if that row is one of the two
        // trading places, the list would scroll to follow it and the row would jump from the
        // finger. Hold the scroll position by index instead.
        if (from == listState.firstVisibleItemIndex || to == listState.firstVisibleItemIndex) {
            listState.holdScrollPosition()
        }
        move(from, to)
        haptics?.performHapticFeedback(HapticFeedbackType.SegmentTick)
    }

    internal companion object {
        const val EDGE_ZONE_DP = 48
        const val MAX_SCROLL_DP_PER_FRAME = 12
        const val LIFT_MS = 150
        const val SETTLE_MS = 200

        /** How high a held row floats above the list. */
        val LIFT_ELEVATION = 6.dp
    }
}

/**
 * A row of a list [drag] can reorder: it animates in, out and into new places like the rest of
 * the app's lists, except while it is the dragged row, which follows the finger instead. The
 * dragged row draws above its neighbours on [surface], lifted by a shadow.
 */
fun LazyItemScope.reorderableRow(
    drag: RowDrag,
    rowKey: Any,
    surface: Color,
): Modifier {
    val moving = drag.key == rowKey
    return Modifier
        .animateItem(
            fadeInSpec = ListMotion.fade(),
            placementSpec = if (moving) null else ListMotion.placement(),
            fadeOutSpec = ListMotion.fade(),
        ).then(
            if (moving) {
                Modifier
                    .zIndex(1f)
                    .graphicsLayer {
                        translationY = drag.offset
                        shadowElevation = RowDrag.LIFT_ELEVATION.toPx() * drag.lift
                    }.background(surface.copy(alpha = surface.alpha * drag.lift))
            } else {
                Modifier
            },
        )
}

/**
 * Keeps the list where it is by index through the next change to its rows. A lazy list otherwise
 * keeps its first visible row in place by key, so moving that row (a sort, a move up or down)
 * would scroll the list after it, carrying what the person just moved out of view.
 */
fun LazyListState.holdScrollPosition() {
    requestScrollToItem(firstVisibleItemIndex, firstVisibleItemScrollOffset)
}
