package depollsoft.tagmaster.ui

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos

private const val MID_SCALE = 1.75f
private const val MAX_SCALE = 3f

/** PhotoView's double-tap zoom: 200ms on an AccelerateDecelerateInterpolator. */
private const val ZOOM_MILLIS = 200
private val AccelerateDecelerate = Easing { x -> (cos((x + 1) * PI) / 2 + 0.5).toFloat() }

/** How far a [ZoomableImage] is zoomed (1x fitted, up to 3x) and panned from center. */
@Stable
class ZoomState {
    var scale by mutableFloatStateOf(1f)
        internal set
    var offset by mutableStateOf(Offset.Zero)
        internal set
}

/**
 * A pinch-and-pan image, as PhotoView showed sheet music: fitted and centered at first, zoomed
 * from 1x to 3x by pinching or by double-tapping and dragging (down zooms in, as
 * ScaleGestureDetector's quick scale did), and stepped 1x → 1.75x → 3x → 1x by double-tapping. A
 * pan keeps gliding after the finger lifts. Panning keeps the image covering the view wherever it
 * is larger than it.
 */
@Composable
fun ZoomableImage(
    image: Bitmap,
    modifier: Modifier = Modifier,
    keepScreenOn: Boolean = false,
    state: ZoomState = remember(image) { ZoomState() },
) {
    val view = LocalView.current
    DisposableEffect(view, keepScreenOn) {
        val previous = view.keepScreenOn
        view.keepScreenOn = keepScreenOn
        onDispose { view.keepScreenOn = previous }
    }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val scope = rememberCoroutineScope()
    val paint = remember { Paint(Paint.FILTER_BITMAP_FLAG) }
    val motion = remember(state) { arrayOfNulls<Job>(1) }

    fun fitted(): RectF {
        // FIT_CENTER (Matrix.setRectToRect with CENTER): scaled to fit and centered.
        val fit = minOf(size.width.toFloat() / image.width, size.height.toFloat() / image.height)
        val width = image.width * fit
        val height = image.height * fit
        val left = (size.width - width) * 0.5f
        val top = (size.height - height) * 0.5f
        return RectF(left, top, left + width, top + height)
    }

    /** How far the image may move from center at [zoom] before it stops covering the view. */
    fun reach(zoom: Float): Offset {
        val base = fitted()
        return Offset(((base.width() * zoom - size.width) / 2f).coerceAtLeast(0f), ((base.height() * zoom - size.height) / 2f).coerceAtLeast(0f))
    }

    fun clamp(
        candidate: Offset,
        zoom: Float,
    ): Offset {
        val max = reach(zoom)
        return Offset(candidate.x.coerceIn(-max.x, max.x), candidate.y.coerceIn(-max.y, max.y))
    }

    /** Scales by [factor] about [point], keeping what is under it there. */
    fun zoomAbout(
        point: Offset,
        factor: Float,
    ) {
        val next = (state.scale * factor).coerceIn(1f, MAX_SCALE)
        val focus = point - Offset(size.width / 2f, size.height / 2f) - state.offset
        state.offset = clamp(state.offset - focus * (next / state.scale - 1f), next)
        state.scale = next
    }

    fun stepZoom(tap: Offset) {
        val target =
            when {
                state.scale < MID_SCALE - 0.01f -> MID_SCALE
                state.scale < MAX_SCALE - 0.01f -> MAX_SCALE
                else -> 1f
            }
        motion[0]?.cancel()
        motion[0] =
            scope.launch {
                val start = state.scale
                val startOffset = state.offset
                val focus = tap - Offset(size.width / 2f, size.height / 2f) - startOffset
                Animatable(0f).animateTo(1f, tween(ZOOM_MILLIS, easing = AccelerateDecelerate)) {
                    val current = start + (target - start) * value
                    state.scale = current
                    state.offset = clamp(startOffset - focus * (current / start - 1f), current)
                }
            }
    }

    fun fling(velocity: Offset) {
        motion[0]?.cancel()
        motion[0] =
            scope.launch {
                val max = reach(state.scale)
                val decay = splineBasedDecay<Float>(view.context.resources.displayMetrics.let { androidx.compose.ui.unit.Density(it.density) })
                coroutineScope {
                    val x = Animatable(state.offset.x).apply { updateBounds(-max.x, max.x) }
                    val y = Animatable(state.offset.y).apply { updateBounds(-max.y, max.y) }
                    launch { x.animateDecay(velocity.x, decay) { state.offset = Offset(value, state.offset.y) } }
                    launch { y.animateDecay(velocity.y, decay) { state.offset = Offset(state.offset.x, value) } }
                }
            }
    }

    Box(
        modifier
            .onSizeChanged { size = it }
            .pointerInput(image, state) {
                var lastTapUp = -1L
                var lastTap = Offset.Zero
                val doubleTapSlop = 100.dp.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    // A touch stops a glide or a zoom step, as PhotoView's did.
                    motion[0]?.cancel()
                    val secondTap =
                        lastTapUp >= 0 && down.uptimeMillis - lastTapUp <= viewConfiguration.doubleTapTimeoutMillis &&
                            (down.position - lastTap).getDistance() < doubleTapSlop
                    lastTapUp = -1L
                    val velocity = VelocityTracker()
                    velocity.addPosition(down.uptimeMillis, down.position)
                    var moving = false
                    var quickScale = false
                    var pinched = false
                    var travel = Offset.Zero
                    var lastY = down.position.y
                    var up = down
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.isEmpty()) {
                            up = event.changes.first()
                            break
                        }
                        if (pressed.size > 1) pinched = true
                        val zoom = event.calculateZoom()
                        val pan = event.calculatePan()
                        val centroid = event.calculateCentroid()
                        if (!moving) {
                            travel += pan
                            if (travel.getDistance() > viewConfiguration.touchSlop || abs(1f - zoom) > 0.02f) {
                                moving = true
                                // Double-tap and drag is a one-finger zoom.
                                quickScale = secondTap && !pinched
                            }
                        }
                        if (moving) {
                            if (quickScale) {
                                // ScaleGestureDetector's anchored scale: the span is twice the
                                // distance from where the second tap landed, below it zooming in.
                                val y = pressed.first().position.y
                                val previous = abs(lastY - down.position.y) * 2
                                val current = abs(y - down.position.y) * 2
                                if (previous > viewConfiguration.touchSlop) {
                                    val below = y > down.position.y
                                    val growing = current > previous
                                    val change = abs(1f - current / previous) * 0.5f
                                    zoomAbout(down.position, if (below == growing) 1f + change else 1f - change)
                                }
                                lastY = y
                            } else {
                                if (zoom != 1f) zoomAbout(centroid, zoom)
                                state.offset = clamp(state.offset + pan, state.scale)
                            }
                            event.changes.forEach { it.consume() }
                        }
                        if (pressed.size == 1) velocity.addPosition(pressed.first().uptimeMillis, pressed.first().position)
                    }
                    when {
                        !moving && secondTap -> stepZoom(down.position)
                        !moving -> {
                            lastTapUp = up.uptimeMillis
                            lastTap = down.position
                        }
                        !pinched && !quickScale && state.scale > 1f -> {
                            val speed = velocity.calculateVelocity()
                            fling(Offset(speed.x, speed.y))
                        }
                    }
                }
            }.drawBehind {
                if (size == IntSize.Zero) return@drawBehind
                val base = fitted()
                val width = base.width() * state.scale
                val height = base.height() * state.scale
                val left = base.centerX() - width / 2f + state.offset.x
                val top = base.centerY() - height / 2f + state.offset.y
                drawIntoCanvas { it.nativeCanvas.drawBitmap(image, null, RectF(left, top, left + width, top + height), paint) }
            },
    )
}
