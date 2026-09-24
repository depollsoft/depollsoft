package depollsoft.tagmaster.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Drag-to-reorder for one section of a lazy list, the way ItemTouchHelper reordered it: a drag
 * starts from a row's handle, the order is previewed on screen as the row passes its neighbours,
 * holding the row near the top or bottom edge scrolls the list, and nothing is committed until
 * the finger lifts. A change to the source while dragging cancels the drag rather than
 * committing a stale order.
 *
 * [keyOf] names the lazy-list key a section item is shown under, so rows in other sections are
 * never drop targets.
 */
@Stable
class ReorderState<K : Any>(
    private val listState: LazyListState,
    private val keyOf: (K) -> Any,
    private val commit: (baseline: List<K>, order: List<K>) -> Unit,
) {
    /** The order being shown while a drag is in progress; null when nothing is dragged. */
    var preview: List<K>? by mutableStateOf(null)
        private set

    var dragging: K? by mutableStateOf(null)
        private set

    private var baseline: List<K> = emptyList()

    /** How far the dragged row is drawn from where the preview order puts it, in pixels. */
    var offset by mutableFloatStateOf(0f)
        private set

    val isDragging: Boolean get() = dragging != null

    /** The rows to show: the preview while dragging, otherwise [source]. */
    fun order(source: List<K>): List<K> = preview ?: source

    fun start(
        item: K,
        source: List<K>,
    ) {
        if (dragging != null || source.size < 2 || item !in source) return
        baseline = source
        preview = source
        dragging = item
        offset = 0f
    }

    fun dragBy(delta: Float) {
        val item = dragging ?: return
        val order = preview ?: return
        offset += delta
        val items = listState.layoutInfo.visibleItemsInfo
        val index = order.indexOf(item)
        if (offset > 0 && index < order.lastIndex) {
            val next = items.firstOrNull { it.key == keyOf(order[index + 1]) } ?: return
            if (offset > next.size / 2f) {
                showOrder(order.toMutableList().apply { add(index + 1, removeAt(index)) })
                offset -= next.size
            }
        } else if (offset < 0 && index > 0) {
            val previous = items.firstOrNull { it.key == keyOf(order[index - 1]) } ?: return
            if (-offset > previous.size / 2f) {
                showOrder(order.toMutableList().apply { add(index - 1, removeAt(index)) })
                offset += previous.size
            }
        }
    }

    /** Where the list stood when the preview last changed, to be held by index; see [shownOrder]. */
    private var anchor: Pair<Int, Int>? = null

    private fun showOrder(order: List<K>) {
        anchor = listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        preview = order
    }

    /**
     * A lazy list keeps its first visible row in place by key; when that row trades places the
     * list would scroll after it and the dragged row would jump from the finger. Holds the scroll
     * position by index instead. It must run in the frame that composes the new order: asked for
     * any earlier, the list remeasures the old order and forgets the request.
     */
    internal fun holdScrollPosition() {
        val (index, offset) = anchor ?: return
        anchor = null
        listState.requestScrollToItem(index, offset)
    }

    /**
     * Scrolls the list while the dragged row is held in the top or bottom [edge] zone, faster the
     * deeper it sits, up to [maxStep] pixels a frame. Frames are only requested while there is
     * something to scroll, so an idle drag leaves the frame clock (and tests) alone.
     */
    suspend fun autoScroll(
        edge: Float,
        maxStep: Float,
    ) {
        while (true) {
            snapshotFlow { edgeScrollStep(edge, maxStep) != 0f }.first { it }
            while (true) {
                withFrameNanos { }
                val step = edgeScrollStep(edge, maxStep)
                if (step == 0f) break
                val scrolled = listState.scrollBy(step)
                // The row's slot moved with the content; keep the row under the finger.
                dragBy(scrolled)
            }
        }
    }

    private fun edgeScrollStep(
        edge: Float,
        maxStep: Float,
    ): Float {
        val item = dragging ?: return 0f
        val info = listState.layoutInfo
        val row = info.visibleItemsInfo.firstOrNull { it.key == keyOf(item) } ?: return 0f
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

    /** Commits the previewed order (the finger lifted). */
    fun drop() {
        val order = preview
        val start = baseline
        clear()
        if (order != null && order != start) commit(start, order)
    }

    /** Abandons the drag without committing; true when there was one. */
    fun cancel(): Boolean {
        val had = dragging != null
        clear()
        return had
    }

    /** Called when the source changes underneath; a drag in progress is abandoned. */
    fun sourceChanged(source: List<K>): Boolean = if (dragging != null && source != baseline) cancel() else false

    private fun clear() {
        dragging = null
        preview = null
        offset = 0f
        baseline = emptyList()
    }
}

/** The rows to show: the preview while dragging, holding the list's scroll position across swaps. */
@Composable
fun <K : Any> ReorderState<K>.shownOrder(source: List<K>): List<K> {
    val order = order(source)
    SideEffect { holdScrollPosition() }
    return order
}

/** The drag handle's gesture: pressing and dragging it moves [item]. */
fun <K : Any> Modifier.reorderHandle(
    state: ReorderState<K>,
    item: K,
    source: () -> List<K>,
    enabled: Boolean,
): Modifier =
    if (!enabled) {
        this
    } else {
        pointerInput(state, item) {
            coroutineScope {
                val edge = 48.dp.toPx()
                val maxStep = 12.dp.toPx()
                var scroller: Job? = null
                detectDragGestures(
                    onDragStart = {
                        state.start(item, source())
                        scroller = launch { state.autoScroll(edge, maxStep) }
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        state.dragBy(amount.y)
                    },
                    onDragEnd = {
                        scroller?.cancel()
                        state.drop()
                    },
                    onDragCancel = {
                        scroller?.cancel()
                        state.cancel()
                    },
                )
            }
        }
    }
