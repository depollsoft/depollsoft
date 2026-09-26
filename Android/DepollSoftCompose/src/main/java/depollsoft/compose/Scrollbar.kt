package depollsoft.compose

import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A View's vertical scroll position in the units its scrollbar is computed from: the visible
 * [extent], the whole [range] and the [offset] of the visible part.
 */
data class ScrollExtent(
    val extent: Int,
    val range: Int,
    val offset: Int,
)

/**
 * RecyclerView's LinearLayoutManager with smooth scrollbars: sizes are estimated from the rows in
 * view, averaged over the rows the adapter has. Offsets are relative to the padded viewport.
 */
fun recyclerViewExtent(info: LazyListLayoutInfo): ScrollExtent? {
    val end = info.viewportEndOffset - info.afterContentPadding
    val visible = info.visibleItemsInfo.filter { it.offset < end && it.offset + it.size > 0 }
    if (visible.isEmpty() || info.totalItemsCount == 0) return null
    val first = visible.first()
    val last = visible.last()
    val laidOutArea = last.offset + last.size - first.offset
    val laidOutRange = abs(last.index - first.index) + 1
    val average = laidOutArea.toFloat() / laidOutRange
    return ScrollExtent(
        extent = min(end, laidOutArea),
        range = (average * info.totalItemsCount).toInt(),
        offset = (first.index * average - first.offset).roundToInt(),
    )
}

/**
 * ListView with smooth scrollbars: 100 units per row, less the parts of the first and last rows
 * that are out of view. [ListView] lays rows out to its full height when it does not clip to its
 * padding, so every composed row counts.
 */
fun listViewExtent(
    info: LazyListLayoutInfo,
    divider: Int = 0,
): ScrollExtent? {
    val rows = info.visibleItemsInfo
    if (rows.isEmpty()) return null
    val height = info.viewportSize.height
    val first = rows.first()
    val last = rows.last()
    // Rows after the first draw ListView's divider above themselves; a ListView child does not.
    val firstDivider = if (first.index > 0) divider else 0
    val firstHeight = first.size - firstDivider
    val lastHeight = last.size - if (last.index > 0) divider else 0
    val top = first.offset + firstDivider + info.beforeContentPadding
    val bottom = last.offset + last.size + info.beforeContentPadding
    var extent = rows.size * 100
    if (firstHeight > 0) extent += top * 100 / firstHeight
    if (lastHeight > 0) extent -= (bottom - height) * 100 / lastHeight
    val offset = if (firstHeight > 0) max(first.index * 100 - top * 100 / firstHeight, 0) else 0
    return ScrollExtent(extent, max(info.totalItemsCount * 100, 0), offset)
}

/**
 * The fade a View's scrollbar goes through: shown when the view appears (for 1.2 s) and whenever
 * it scrolls (for 0.3 s after the scroll stops), then faded out over 0.25 s. The delay runs on the
 * main looper, as ScrollabilityCache's does.
 */
private class ScrollbarFade {
    private val main = Handler(Looper.getMainLooper())
    var visible by mutableStateOf(false)
        private set
    var fading by mutableStateOf(false)
        private set
    private val startFade = Runnable { fading = true }

    fun wake(delay: Long) {
        main.removeCallbacks(startFade)
        fading = false
        visible = true
        main.postDelayed(startFade, delay)
    }

    fun hold() {
        main.removeCallbacks(startFade)
        fading = false
        visible = true
    }

    fun faded() {
        visible = false
        fading = false
    }

    fun stop() = main.removeCallbacks(startFade)
}

/**
 * Draws the platform's vertical scrollbar thumb over this element, inset by the scrolling
 * container's [top], [bottom] and [end] padding, exactly where and how a View with
 * `scrollbarStyle="insideOverlay"` draws it.
 */
@Composable
private fun Modifier.viewScrollbar(
    top: Dp,
    bottom: Dp,
    end: Dp = 0.dp,
    isScrolling: () -> Boolean,
    measure: () -> ScrollExtent?,
): Modifier {
    val context = LocalContext.current
    val density = LocalDensity.current
    val (thumb, thickness) =
        remember(context) {
            val attributes =
                context.obtainStyledAttributes(intArrayOf(android.R.attr.scrollbarThumbVertical, android.R.attr.scrollbarSize))
            try {
                // The thumb's color is a state list; an enabled View draws it enabled.
                val drawable: Drawable? =
                    attributes.getDrawable(0)?.mutate()?.apply { state = intArrayOf(android.R.attr.state_enabled) }
                drawable to attributes.getDimensionPixelSize(1, with(density) { 4.dp.roundToPx() })
            } finally {
                attributes.recycle()
            }
        }
    val fade = remember { ScrollbarFade() }
    val alpha = remember { Animatable(1f) }
    val scrolling by rememberUpdatedState(isScrolling)
    // A View awakens its scrollbars whenever its window becomes visible: when the screen first
    // shows, and again when it comes back from behind another.
    LifecycleStartEffect(fade) {
        fade.wake(INITIAL_DELAY)
        onStopOrDispose { fade.stop() }
    }
    LaunchedEffect(fade) {
        var wasScrolling = false
        snapshotFlow { scrolling() }.collect { now ->
            // Only a scroll ending shortens the fade; the first report, idle, keeps the initial
            // 1.2 seconds.
            if (now) fade.hold() else if (wasScrolling && fade.visible) fade.wake(SCROLL_DELAY)
            wasScrolling = now
        }
    }
    LaunchedEffect(fade.fading) {
        if (fade.fading) {
            alpha.animateTo(0f, tween(FADE_DURATION, easing = LinearEasing))
            fade.faded()
        } else {
            alpha.snapTo(1f)
        }
    }
    if (thumb == null) return this
    return drawWithContent {
        drawContent()
        if (!fade.visible) return@drawWithContent
        val values = measure() ?: return@drawWithContent
        if (values.extent <= 0 || values.range <= values.extent) return@drawWithContent
        val trackTop = top.roundToPx()
        val track = size.height.toInt() - trackTop - bottom.roundToPx()
        // ScrollBarUtils.getThumbLength / getThumbOffset.
        var length = (track.toFloat() * values.extent / values.range).roundToInt()
        if (length < thickness * 2) length = thickness * 2
        var offset = ((track - length).toFloat() * values.offset / (values.range - values.extent)).roundToInt()
        if (offset > track - length) offset = track - length
        // ScrollBarDrawable takes the fade as an integer alpha.
        val shown = (alpha.value * 255).toInt() / 255f
        // The thumb sits at the trailing edge: the right, or the left in a right-to-left layout.
        val left =
            if (layoutDirection == LayoutDirection.Rtl) end.roundToPx() else size.width.toInt() - end.roundToPx() - thickness
        drawPlatform(thumb, left, trackTop + offset, thickness, length, shown)
    }
}

/** RecyclerView's scrollbar for a list with [top] and [bottom] content padding. */
@Composable
fun Modifier.recyclerScrollbar(
    state: LazyListState,
    top: Dp = 0.dp,
    bottom: Dp = 0.dp,
): Modifier = viewScrollbar(top, bottom, isScrolling = { state.isScrollInProgress }) { recyclerViewExtent(state.layoutInfo) }

/**
 * ListView's scrollbar for a list with [top] and [bottom] content padding whose rows after the
 * first include a [divider] above them.
 */
@Composable
fun Modifier.listViewScrollbar(
    state: LazyListState,
    top: Dp = 0.dp,
    bottom: Dp = 0.dp,
    divider: Dp = 0.dp,
): Modifier {
    val dividerPx = with(LocalDensity.current) { divider.roundToPx() }
    return viewScrollbar(top, bottom, isScrolling = { state.isScrollInProgress }) { listViewExtent(state.layoutInfo, dividerPx) }
}

/**
 * ScrollView's scrollbar, for a [verticalScroll][androidx.compose.foundation.verticalScroll]
 * whose content carries [top], [bottom] and [end] padding (the ScrollView's own padding in the
 * layout it replaces; the thumb sits inside it). The range is the child's bottom edge; the extent
 * is the whole view.
 */
@Composable
fun Modifier.scrollViewScrollbar(
    state: ScrollState,
    top: Dp = 0.dp,
    bottom: Dp = 0.dp,
    end: Dp = 0.dp,
): Modifier {
    val density = LocalDensity.current
    return viewScrollbar(top, bottom, end, { state.isScrollInProgress }) {
        val viewport = state.viewportSize
        if (viewport <= 0) {
            null
        } else {
            val childBottom = viewport + state.maxValue - with(density) { bottom.roundToPx() }
            ScrollExtent(viewport, childBottom, state.value)
        }
    }
}

private const val INITIAL_DELAY = 1200L
private const val SCROLL_DELAY = 300L
private const val FADE_DURATION = 250

/**
 * Scrolls just far enough to show item [index] whole, the way RecyclerView's and ListView's
 * smoothScrollToPosition do: nothing when it is already in view, otherwise an animated scroll
 * that stops with the item's near edge inside the padded viewport.
 */
suspend fun LazyListState.revealItem(index: Int) {
    fun find() = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    // Item offsets start after the top padding; the box ends before the bottom padding.
    val end = layoutInfo.viewportSize.height - layoutInfo.beforeContentPadding - layoutInfo.afterContentPadding

    suspend fun settleOn(shown: androidx.compose.foundation.lazy.LazyListItemInfo) {
        when {
            shown.offset < 0 -> animateScrollBy(shown.offset.toFloat())
            shown.offset + shown.size > end -> animateScrollBy((shown.offset + shown.size - end).toFloat())
        }
    }
    find()?.let {
        settleOn(it)
        return
    }
    // Off screen: glide toward it by the rows' average height, as a smooth scroller does, then
    // line its near edge up. A list too long to estimate falls back to a jump.
    val visible = layoutInfo.visibleItemsInfo
    if (visible.isEmpty()) {
        scrollToItem(index)
        return
    }
    val average = visible.sumOf { it.size }.toFloat() / visible.size
    val distance =
        if (index > visible.last().index) {
            (index - visible.last().index) * average + (visible.last().offset + visible.last().size - end)
        } else {
            -((visible.first().index - index) * average - visible.first().offset)
        }
    animateScrollBy(distance)
    val shown = find()
    if (shown != null) {
        settleOn(shown)
    } else {
        val below = index > (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0)
        scrollToItem(index)
        if (below) {
            // Coming from above: settle with the item's bottom edge at the end of the viewport.
            val placed = find() ?: return
            val back = end - (placed.offset + placed.size)
            if (back > 0) scrollBy(-back.toFloat())
        }
    }
}
