package depollsoft.tagmaster.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Drag-to-reorder for one section of a lazy list, the way ItemTouchHelper reordered it: a drag
 * starts from a row's handle, the order is previewed on screen as the row passes its neighbours,
 * and nothing is committed until the finger lifts. A change to the source while dragging cancels
 * the drag rather than committing a stale order.
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
                preview = order.toMutableList().apply { add(index + 1, removeAt(index)) }
                offset -= next.size
            }
        } else if (offset < 0 && index > 0) {
            val previous = items.firstOrNull { it.key == keyOf(order[index - 1]) } ?: return
            if (-offset > previous.size / 2f) {
                preview = order.toMutableList().apply { add(index - 1, removeAt(index)) }
                offset += previous.size
            }
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
            detectDragGestures(
                onDragStart = { state.start(item, source()) },
                onDrag = { change, amount ->
                    change.consume()
                    state.dragBy(amount.y)
                },
                onDragEnd = { state.drop() },
                onDragCancel = { state.cancel() },
            )
        }
    }
