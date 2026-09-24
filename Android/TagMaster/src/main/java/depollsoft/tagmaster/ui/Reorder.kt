package depollsoft.tagmaster.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Drag-to-reorder for one section of a lazy list, the way ItemTouchHelper reordered it: a drag
 * starts the moment a row's handle is touched and the row lifts, the order is previewed on screen
 * as the row passes its neighbours (they slide aside), holding the row near the top or bottom edge
 * scrolls the list, and nothing is committed until the finger lifts, when the row settles into its
 * slot. A change to the source while dragging cancels the drag rather than committing a stale order.
 *
 * [keyOf] names the lazy-list key a section item is shown under, so rows in other sections are
 * never drop targets. [commit] stores the new order and says whether it did.
 */
@Stable
class ReorderState<K : Any>(
    private val listState: LazyListState,
    private val keyOf: (K) -> Any,
    private val scope: CoroutineScope,
    private val commit: (baseline: List<K>, order: List<K>) -> Boolean,
) {
    /** Where drag feedback is felt; set from the composition that shows the list. */
    var haptics: HapticFeedback? = null

    /** The order being shown while a drag is in progress; null when nothing is dragged. */
    var preview: List<K>? by mutableStateOf(null)
        private set

    var dragging: K? by mutableStateOf(null)
        private set

    /** The row gliding from where the finger left it into its slot, after a drop. */
    var settling: K? by mutableStateOf(null)
        private set

    private var baseline: List<K> = emptyList()

    /** How far the dragged (or settling) row is drawn from where the order puts it, in pixels. */
    var offset by mutableFloatStateOf(0f)
        private set

    /** How far the row is lifted, 0 (resting) to 1 (held). */
    private val liftAnimation = Animatable(0f)
    val lift: Float get() = liftAnimation.value

    private var liftJob: Job? = null
    private var settleJob: Job? = null

    val isDragging: Boolean get() = dragging != null

    /** Whether [item]'s row is placed by the drag rather than by the list. */
    fun isMoving(item: K): Boolean = dragging == item || settling == item

    /** The rows to show: the preview while dragging, otherwise [source]. */
    fun order(source: List<K>): List<K> = preview ?: source

    fun start(
        item: K,
        source: List<K>,
    ): Boolean {
        finishSettle()
        if (dragging != null || source.size < 2 || item !in source) return false
        baseline = source
        preview = source
        dragging = item
        offset = 0f
        haptics?.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
        animateLift(1f)
        return true
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
                haptics?.performHapticFeedback(HapticFeedbackType.SegmentTick)
            }
        } else if (offset < 0 && index > 0) {
            val previous = items.firstOrNull { it.key == keyOf(order[index - 1]) } ?: return
            if (-offset > previous.size / 2f) {
                preview = order.toMutableList().apply { add(index - 1, removeAt(index)) }
                offset += previous.size
                haptics?.performHapticFeedback(HapticFeedbackType.SegmentTick)
            }
        }
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

    /** Commits the previewed order (the finger lifted) and settles the row into its slot. */
    fun drop() {
        val item = dragging ?: return
        val order = preview
        val start = baseline
        dragging = null
        preview = null
        baseline = emptyList()
        haptics?.performHapticFeedback(HapticFeedbackType.GestureEnd)
        val stored = order == null || order == start || commit(start, order)
        if (!stored) {
            // Refused (the list changed): the rows slide back, the dropped one with them.
            offset = 0f
            animateLift(0f)
            return
        }
        settling = item
        val from = offset
        settleJob =
            scope.launch {
                try {
                    launch { liftAnimation.animateTo(0f, tween(ListMotion.SETTLE_MILLIS, easing = FastOutSlowInEasing)) }
                    animate(from, 0f, animationSpec = tween(ListMotion.SETTLE_MILLIS, easing = FastOutSlowInEasing)) { value, _ -> offset = value }
                } finally {
                    if (settling == item) {
                        settling = null
                        offset = 0f
                    }
                }
            }
    }

    /** Abandons the drag (and any settle) without committing; true when there was a drag. */
    fun cancel(): Boolean {
        val had = dragging != null
        finishSettle()
        dragging = null
        preview = null
        offset = 0f
        baseline = emptyList()
        if (had) animateLift(0f)
        return had
    }

    /** Called when the source changes underneath; a drag in progress is abandoned. */
    fun sourceChanged(source: List<K>): Boolean = if (dragging != null && source != baseline) cancel() else false

    private fun finishSettle() {
        settleJob?.cancel()
        settleJob = null
        if (settling != null) {
            settling = null
            offset = 0f
            liftJob?.cancel()
            scope.launch { liftAnimation.snapTo(0f) }
        }
    }

    private fun animateLift(target: Float) {
        liftJob?.cancel()
        liftJob = scope.launch { liftAnimation.animateTo(target, tween(ListMotion.LIFT_MILLIS, easing = FastOutSlowInEasing)) }
    }
}

/** A [ReorderState] for [listState], animating in this composition and felt through its haptics. */
@Composable
fun <K : Any> rememberReorderState(
    listState: LazyListState,
    vararg inputs: Any?,
    keyOf: (K) -> Any,
    commit: (baseline: List<K>, order: List<K>) -> Boolean,
): ReorderState<K> {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val state = remember(listState, *inputs) { ReorderState(listState, keyOf, scope, commit) }
    state.haptics = haptics
    return state
}

/**
 * The rows to show: the preview while dragging, otherwise [source]. When the rows only trade
 * places, whether by a drag, a Move up/down or a sync, the list holds its scroll position by
 * index: a lazy list otherwise keeps its first visible row in place by key, and when that row
 * moves the list scrolls after it, taking the dragged row away from the finger or the moved row
 * off screen. The request must be made in the frame that composes the new order: asked for any
 * earlier, the list remeasures the old order and forgets it.
 */
@Composable
fun <K : Any> ReorderState<K>.shownOrder(
    source: List<K>,
    listState: LazyListState,
): List<K> {
    val order = order(source)
    val last = remember { arrayOfNulls<List<K>>(1) }
    SideEffect {
        val previous = last[0]
        last[0] = order
        if (previous != null && previous != order && previous.size == order.size && previous.toSet() == order.toSet()) {
            listState.requestScrollToItem(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
        }
    }
    return order
}

/**
 * Draws [item]'s row where a drag puts it: above its neighbours, following the finger, lifted
 * with a shadow over [surface]. Everything after this modifier (a divider, the content) travels
 * with the row.
 */
fun <K : Any> Modifier.reorderRow(
    state: ReorderState<K>,
    item: K,
    surface: Color,
): Modifier =
    if (!state.isMoving(item)) {
        this
    } else {
        this
            .zIndex(1f)
            .graphicsLayer {
                translationY = state.offset
                shadowElevation = ListMotion.liftedElevation.toPx() * state.lift
            }.drawBehind { drawRect(surface.copy(alpha = surface.alpha * state.lift)) }
    }

/**
 * The drag handle's gesture: touching it picks [item] up, and dragging moves it. The press is
 * reported to [interactionSource], so the handle can ripple as the View's ImageButton did.
 */
fun <K : Any> Modifier.reorderHandle(
    state: ReorderState<K>,
    item: K,
    source: () -> List<K>,
    enabled: Boolean,
    interactionSource: MutableInteractionSource? = null,
): Modifier =
    if (!enabled) {
        this
    } else {
        pointerInput(state, item) {
            val edge = 48.dp.toPx()
            val maxStep = 12.dp.toPx()
            coroutineScope {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    if (!state.start(item, source())) return@awaitEachGesture
                    down.consume()
                    val press = PressInteraction.Press(down.position)
                    interactionSource?.tryEmit(press)
                    val scroller = launch { state.autoScroll(edge, maxStep) }
                    var lifted = false
                    try {
                        while (isActive) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                lifted = true
                                break
                            }
                            val delta = change.position.y - change.previousPosition.y
                            change.consume()
                            if (delta != 0f) state.dragBy(delta)
                        }
                    } finally {
                        scroller.cancel()
                        interactionSource?.tryEmit(if (lifted) PressInteraction.Release(press) else PressInteraction.Cancel(press))
                        if (lifted) state.drop() else state.cancel()
                    }
                }
            }
        }
    }
