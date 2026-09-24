package depollsoft.pitchperfect.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * A drag of one lazy-list row by its handle, as ItemTouchHelper did it: the row follows the
 * finger, trades places with a neighbour once it passes that neighbour's middle, and holding it
 * near the top or bottom edge scrolls the list so it can travel the whole list in one drag.
 *
 * @param indexOf the dragged row's current index, from its key.
 * @param canMove whether the row at `from` may trade places with the one at `to`.
 * @param move trades two adjacent rows; not stored until [onDrop].
 * @param onDrop the finger lifted or the gesture ended: store the order once.
 */
class RowDrag(
    private val listState: LazyListState,
    private val indexOf: (Any) -> Int,
    private val canMove: (from: Int, to: Int) -> Boolean,
    private val move: (from: Int, to: Int) -> Unit,
    private val onDrop: () -> Unit,
) {
    /** The key of the row being dragged, or null. */
    var key by mutableStateOf<Any?>(null)
        private set

    /** How far the dragged row is drawn from its slot. */
    var offset by mutableFloatStateOf(0f)
        private set

    /** Follows one drag on the handle of the row with [rowKey]. */
    suspend fun track(
        scope: PointerInputScope,
        rowKey: Any,
    ) = coroutineScope {
        val edge = with(scope) { EDGE_ZONE_DP.dp.toPx() }
        val maxStep = with(scope) { MAX_SCROLL_DP_PER_FRAME.dp.toPx() }
        scope.awaitEachGesture {
            val down = awaitFirstDown()
            down.consume()
            key = rowKey
            offset = 0f
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
                key = null
                offset = 0f
                onDrop()
            }
        }
    }

    /** How far to scroll this frame: faster the deeper the dragged row sits in an edge zone. */
    private fun edgeScrollStep(
        edge: Float,
        maxStep: Float,
    ): Float {
        val info = listState.layoutInfo
        val row = info.visibleItemsInfo.firstOrNull { it.key == key } ?: return 0f
        val top = row.offset + offset
        val bottom = top + row.size
        val start = info.viewportStartOffset.toFloat()
        val end = info.viewportEndOffset.toFloat()
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
            listState.requestScrollToItem(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
        }
        move(from, to)
    }

    private companion object {
        const val EDGE_ZONE_DP = 48
        const val MAX_SCROLL_DP_PER_FRAME = 12
    }
}
