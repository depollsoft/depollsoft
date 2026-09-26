package depollsoft.compose

import android.os.Build
import androidx.compose.animation.core.Animatable
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
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
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
 * Drag-to-reorder for one section of a lazy list, the way ItemTouchHelper reordered a
 * RecyclerView:
 *
 * - Touching a row's handle picks the row up: it lifts over [ListMotion.LIFT_MILLIS] and follows
 *   the finger.
 * - The row trades places with a neighbour once its far edge passes the neighbour's far edge
 *   (ItemTouchHelper.Callback.chooseDropTarget), and the neighbour slides aside. The new order is
 *   only previewed.
 * - Taking the row toward the list's padded top or bottom, and holding it within 48dp of that
 *   edge, scrolls the list, as ItemTouchHelper scrolled only while the drag headed that way. A row
 *   held still where it was picked up never scrolls, and nothing scrolls past its section's ends.
 * - Releasing it commits the previewed order once, if it changed, and the row settles from where
 *   it was drawn into its slot over [ListMotion.SETTLE_MILLIS]. A drop the owner refuses, or a
 *   drag abandoned by [cancel] (including one the system takes away), settles the row back into
 *   its slot in the source order the same way.
 *
 * The source is read when a drag starts; what it does while a drag is held is up to the owner.
 * Calling [sourceChanged] abandons the drag when the source moved on; not calling it keeps the
 * preview on screen until the drop, as the View screens that skipped refreshing mid-drag did.
 * Items gone from the source ([shownOrder]'s latest) drop out of the preview either way.
 *
 * [keyOf] names the lazy-list key an item is shown under, so rows outside the section are never
 * drop targets. [commit] stores a new order, if the owner accepts it; the rows settle to whatever
 * order the source then holds.
 */
@Stable
class ReorderState<K : Any> internal constructor(
    private val listState: LazyListState,
    private val scope: CoroutineScope,
    internal var keyOf: (K) -> Any,
    internal var commit: (baseline: List<K>, order: List<K>) -> Unit,
) {
    /** Where drag feedback is felt; set from the composition that shows the list. */
    var haptics: HapticFeedback? = null

    /** The order shown while a drag is held; null otherwise. */
    var preview: List<K>? by mutableStateOf(null)
        private set

    /** The item a finger holds. */
    var dragging: K? by mutableStateOf(null)
        private set

    /** The item gliding into its slot after a drop or a cancelled drag. */
    var settling: K? by mutableStateOf(null)
        private set

    private var baseline: List<K> = emptyList()

    /** How far the finger has taken the held row from its slot in [preview], in pixels. */
    var offset by mutableFloatStateOf(0f)
        private set

    /**
     * How far the finger itself has moved since the drag started, in pixels; scrolling that keeps
     * the row under the finger is not counted. Its sign says which edge the drag heads for.
     */
    private var travel by mutableFloatStateOf(0f)

    /** The items the owner's source last held, from [shownOrder]; null before it has run. */
    internal var present: Set<K>? = null

    private val liftAnimation = Animatable(0f)

    /** How far the moving row is lifted, 0 (resting) to 1 (held). */
    val lift: Float get() = liftAnimation.value

    /** The top of the moving row's slot, in root coordinates, as last laid out. */
    private var slotTop by mutableFloatStateOf(Float.NaN)

    /** Where the settling row was drawn when the drag ended, in root coordinates. */
    private var settleFrom = 0f

    /** How far the settling row has glided, 0 (where the finger left it) to 1 (in its slot). */
    private var settled by mutableFloatStateOf(1f)

    private var liftJob: Job? = null
    private var settleJob: Job? = null

    /** Whether [item]'s row is placed by the drag rather than by the list. */
    fun isMoving(item: K): Boolean = dragging == item || settling == item

    /** How far the moving row is drawn from its slot, in pixels. */
    val translation: Float
        get() =
            when {
                dragging != null -> offset
                settling != null && !slotTop.isNaN() -> (settleFrom - slotTop) * (1f - settled)
                else -> 0f
            }

    /** The rows to show: the preview while a drag is held, otherwise [source]. */
    fun order(source: List<K>): List<K> = preview ?: source

    /** Picks [item] up; false when there is nothing to reorder or a drag is already held. */
    fun start(
        item: K,
        source: List<K>,
    ): Boolean {
        val interrupted = endSettle()
        if (dragging != null || source.size < 2 || item !in source) {
            if (interrupted) animateLift(0f, from = 0f, millis = 0)
            return false
        }
        baseline = source.toList()
        preview = baseline
        dragging = item
        offset = 0f
        travel = 0f
        slotTop = Float.NaN
        haptics?.dragStarted()
        // The row a settle was carrying is set down at once, so this one lifts from rest rather
        // than from wherever that one's shadow had got to.
        animateLift(1f, from = if (interrupted) 0f else null)
        return true
    }

    /** The finger moved the held row by [delta] pixels. */
    fun fingerMoved(delta: Float) {
        if (dragging == null) return
        travel += delta
        dragBy(delta)
    }

    /** Moves the held row by [delta] pixels, trading places with each neighbour it passes. */
    fun dragBy(delta: Float) {
        val item = dragging ?: return
        offset += delta
        while (true) {
            val order = currentPreview() ?: return
            val index = order.indexOf(item)
            val items = listState.layoutInfo.visibleItemsInfo
            val to =
                if (offset > 0 && index < order.lastIndex) {
                    val next = items.firstOrNull { it.key == keyOf(order[index + 1]) } ?: return
                    if (offset <= next.size) return
                    offset -= next.size
                    // The slot moves now; the list only lays it out in the next frame, and a drop
                    // before then settles from here.
                    if (!slotTop.isNaN()) slotTop += next.size
                    index + 1
                } else if (offset < 0 && index > 0) {
                    val previous = items.firstOrNull { it.key == keyOf(order[index - 1]) } ?: return
                    if (-offset <= previous.size) return
                    offset += previous.size
                    if (!slotTop.isNaN()) slotTop -= previous.size
                    index - 1
                } else {
                    return
                }
            preview = order.toMutableList().apply { add(to, removeAt(index)) }
            haptics?.passed()
        }
    }

    /**
     * The preview without items the source no longer holds: a row deleted mid-drag is no longer
     * shown, so it is neither a neighbour to pass nor part of the order dropped.
     */
    private fun currentPreview(): List<K>? {
        val order = preview ?: return null
        val present = present ?: return order
        if (order.all { it in present }) return order
        return order.filter { it in present }.also { preview = it }
    }

    /**
     * Scrolls the list while the held row sits in the top or bottom [edge] zone of the list's
     * padded area, faster the deeper it sits, up to [maxStep] pixels a frame. Frames are only
     * requested while there is something to scroll, so an idle drag leaves the frame clock (and
     * tests) alone.
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
                if (!slotTop.isNaN()) slotTop -= scrolled
                dragBy(scrolled)
            }
        }
    }

    private fun edgeScrollStep(
        edge: Float,
        maxStep: Float,
    ): Float {
        val item = dragging ?: return 0f
        val order = preview ?: return 0f
        val info = listState.layoutInfo
        val row = info.visibleItemsInfo.firstOrNull { it.key == keyOf(item) } ?: return 0f
        val top = row.offset + offset
        val bottom = top + row.size
        // ItemTouchHelper scrolled once the row left the RecyclerView's padded area, so a list
        // padded clear of a floating button scrolls well above the screen's edge. It scrolled only
        // toward the edge the drag heads for, and a row at its section's end has nowhere further
        // to go: scrolling on would only carry its slot out of view and end the drag.
        val start = info.viewportStartOffset.toFloat() + info.beforeContentPadding
        val end = info.viewportEndOffset.toFloat() - info.afterContentPadding
        val index = order.indexOf(item)
        return when {
            travel < 0 && index > 0 && top < start + edge && listState.canScrollBackward ->
                -maxStep * ((start + edge - top) / edge).coerceAtMost(1f)
            travel > 0 && index < order.lastIndex && bottom > end - edge && listState.canScrollForward ->
                maxStep * ((bottom - (end - edge)) / edge).coerceAtMost(1f)
            else -> 0f
        }
    }

    /** The finger lifted: commits the previewed order if it changed, and settles the row. */
    fun drop() {
        val item = dragging ?: return
        val order = currentPreview() ?: baseline
        val start = baseline.let { base -> present?.let { p -> base.filter { it in p } } ?: base }
        haptics?.dragEnded()
        endDrag(item)
        // A drop where nothing moved stores nothing. A refused one leaves the source as it was,
        // and the rows slide back.
        if (order != start) commit(start, order)
    }

    /** Abandons the held drag without committing; true when there was one. */
    fun cancel(): Boolean {
        val item = dragging
        if (item == null) {
            finishSettle()
            return false
        }
        endDrag(item)
        return true
    }

    /** Abandons a held drag when [source] is no longer the order it started from. */
    fun sourceChanged(source: List<K>): Boolean = if (dragging != null && source != baseline) cancel() else false

    /** Where the moving row's slot was laid out; the row's settle glides toward it. */
    internal fun placed(top: Float) {
        slotTop = top
    }

    private fun endDrag(item: K) {
        settleFrom = slotTop + offset
        dragging = null
        preview = null
        baseline = emptyList()
        offset = 0f
        travel = 0f
        if (slotTop.isNaN()) {
            animateLift(0f)
            return
        }
        settling = item
        settled = 0f
        liftJob?.cancel()
        settleJob =
            scope.launch {
                try {
                    coroutineScope {
                        launch { liftAnimation.animateTo(0f, tween(ListMotion.SETTLE_MILLIS, easing = ListMotion.easing)) }
                        animate(0f, 1f, animationSpec = tween(ListMotion.SETTLE_MILLIS, easing = ListMotion.easing)) { value, _ -> settled = value }
                    }
                } finally {
                    if (settling == item) {
                        settling = null
                        settled = 1f
                    }
                }
            }
    }

    /** Ends a settle in its slot, and sets the row down; see [endSettle]. */
    private fun finishSettle() {
        if (endSettle()) animateLift(0f, from = 0f, millis = 0)
    }

    /** Stops a settle where it is and returns whether there was one; the caller sets the lift. */
    private fun endSettle(): Boolean {
        settleJob?.cancel()
        settleJob = null
        if (settling == null) return false
        settling = null
        settled = 1f
        return true
    }

    /**
     * Animates the lift to [target], first snapping it to [from] if given. Both happen in one job,
     * so a later call cannot cancel the snap before it has run and leave the lift mid-way.
     */
    private fun animateLift(
        target: Float,
        from: Float? = null,
        millis: Int = ListMotion.LIFT_MILLIS,
    ) {
        liftJob?.cancel()
        liftJob =
            scope.launch {
                if (from != null) liftAnimation.snapTo(from)
                if (millis == 0) liftAnimation.snapTo(target) else liftAnimation.animateTo(target, tween(millis, easing = ListMotion.easing))
            }
    }
}

// The gesture haptics came with Android 14. Earlier, a drag starts with the long-press buzz
// ItemTouchHelper gave it, and nothing else.
private val gestureHaptics = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

private fun HapticFeedback.dragStarted() =
    performHapticFeedback(if (gestureHaptics) HapticFeedbackType.GestureThresholdActivate else HapticFeedbackType.LongPress)

private fun HapticFeedback.passed() {
    if (gestureHaptics) performHapticFeedback(HapticFeedbackType.SegmentTick)
}

private fun HapticFeedback.dragEnded() {
    if (gestureHaptics) performHapticFeedback(HapticFeedbackType.GestureEnd)
}

/**
 * A [ReorderState] for [listState], animating in this composition and felt through its haptics.
 * [inputs] start a new state when they change; [keyOf] and [commit] always use their latest
 * values.
 */
@Composable
fun <K : Any> rememberReorderState(
    listState: LazyListState,
    vararg inputs: Any?,
    keyOf: (K) -> Any,
    commit: (baseline: List<K>, order: List<K>) -> Unit,
): ReorderState<K> {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val state = remember(listState, *inputs) { ReorderState(listState, scope, keyOf, commit) }
    state.keyOf = keyOf
    state.commit = commit
    state.haptics = haptics
    return state
}

/**
 * The rows to show: the preview while a drag is held, otherwise [source]. Whenever the rows only
 * trade places, by a drag, a sort, a Move up/down or a sync, the list holds its scroll position
 * by index: a lazy list otherwise keeps its first visible row in place by key, and when that row
 * moves the list scrolls after it, taking the dragged row from the finger or the moved row off
 * screen. The request is made in the frame that composes the new order; made any earlier, the
 * list remeasures the old order and forgets it.
 */
@Composable
fun <K : Any> ReorderState<K>.shownOrder(
    source: List<K>,
    listState: LazyListState,
): List<K> {
    val shown = source.toSet().also { present = it }
    val order = order(source).filter { it in shown }
    val last = remember { arrayOfNulls<List<K>>(1) }
    SideEffect {
        val previous = last[0]
        last[0] = order
        if (previous != null && previous != order && previous.size == order.size && previous.toSet() == order.toSet()) {
            listState.holdScrollPosition()
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
            // Placed before the translation, so this is the slot, not where the row is drawn.
            .onPlaced { state.placed(it.positionInRoot().y) }
            .zIndex(1f)
            .graphicsLayer {
                translationY = state.translation
                shadowElevation = ListMotion.liftedElevation.toPx() * state.lift
            }.drawBehind { drawRect(surface.copy(alpha = surface.alpha * state.lift)) }
    }

/**
 * The drag handle's gesture: touching it picks [item] up out of [source], and dragging moves it.
 * The press is reported to [interactionSource], so the handle can ripple as an ImageButton did.
 * A gesture the system takes away (a cancel) abandons the drag rather than dropping it.
 *
 * A new [source] or [interactionSource] takes effect for the next gesture without restarting one
 * in progress; a new [state] or [item] starts over.
 */
fun <K : Any> Modifier.reorderHandle(
    state: ReorderState<K>,
    item: K,
    source: () -> List<K>,
    enabled: Boolean,
    interactionSource: MutableInteractionSource? = null,
): Modifier = if (!enabled) this else this then ReorderHandleElement(state, item, source, interactionSource)

private class ReorderHandleElement<K : Any>(
    val state: ReorderState<K>,
    val item: K,
    val source: () -> List<K>,
    val interactionSource: MutableInteractionSource?,
) : ModifierNodeElement<ReorderHandleNode<K>>() {
    override fun create() = ReorderHandleNode(state, item, source, interactionSource)

    override fun update(node: ReorderHandleNode<K>) = node.update(state, item, source, interactionSource)

    override fun InspectorInfo.inspectableProperties() {
        name = "reorderHandle"
        properties["item"] = item
    }

    override fun equals(other: Any?): Boolean =
        other is ReorderHandleElement<*> &&
            other.state === state &&
            other.item == item &&
            other.source === source &&
            other.interactionSource === interactionSource

    override fun hashCode(): Int = ((state.hashCode() * 31 + item.hashCode()) * 31 + source.hashCode()) * 31 + interactionSource.hashCode()
}

private class ReorderHandleNode<K : Any>(
    var state: ReorderState<K>,
    var item: K,
    var source: () -> List<K>,
    var interactionSource: MutableInteractionSource?,
) : DelegatingNode() {
    private val pointer = delegate(SuspendingPointerInputModifierNode { track() })

    fun update(
        state: ReorderState<K>,
        item: K,
        source: () -> List<K>,
        interactionSource: MutableInteractionSource?,
    ) {
        val restart = state !== this.state || item != this.item
        this.state = state
        this.item = item
        this.source = source
        this.interactionSource = interactionSource
        if (restart) pointer.resetPointerInputHandler()
    }

    private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.track() {
        val edge = EDGE_ZONE.toPx()
        val maxStep = MAX_SCROLL_PER_FRAME.toPx()
        coroutineScope {
            awaitEachGesture {
                val down = awaitFirstDown()
                // This gesture keeps the state, item and interaction source it started with.
                val state = state
                val item = item
                val interactions = interactionSource
                if (!state.start(item, source())) return@awaitEachGesture
                down.consume()
                val press = PressInteraction.Press(down.position)
                interactions?.tryEmit(press)
                val scroller = launch { state.autoScroll(edge, maxStep) }
                var lifted = false
                try {
                    while (isActive) {
                        val event: PointerEvent = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            // A finger lifting arrives unconsumed; a cancel (the system taking
                            // the touch stream) arrives already consumed.
                            lifted = !change.isConsumed
                            break
                        }
                        val delta = change.position.y - change.previousPosition.y
                        change.consume()
                        if (delta != 0f) state.fingerMoved(delta)
                    }
                } finally {
                    scroller.cancel()
                    interactions?.tryEmit(if (lifted) PressInteraction.Release(press) else PressInteraction.Cancel(press))
                    if (lifted) state.drop() else state.cancel()
                }
            }
        }
    }
}

/**
 * Keeps the list where it is by index through the next change to its rows. A lazy list
 * otherwise keeps its first visible row in place by key, so moving that row would scroll the list
 * after it.
 */
fun LazyListState.holdScrollPosition() {
    requestScrollToItem(firstVisibleItemIndex, firstVisibleItemScrollOffset)
}

private val EDGE_ZONE = 48.dp
private val MAX_SCROLL_PER_FRAME = 12.dp
