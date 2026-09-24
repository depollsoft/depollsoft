package depollsoft.tagmaster.ui

import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException

/**
 * A Material 3 slider drawn the way MDC 1.14's `Slider` draws itself, so a screen that had one
 * keeps its exact look: a 16dp track inset 22dp from each side (its ends extending 8dp past the
 * first and last value), split around a 4dp by 44dp bar thumb by a 6dp gap from the thumb's
 * center, with 2dp inside corners, and a 4dp stop dot at the end of the inactive track. Colors are
 * MDC's own `m3_slider_*` state lists, so the disabled look matches as well.
 *
 * Touch works as MDC's slider does inside a scrolling page: a drag claims the slider only once it
 * has moved sideways past the touch slop (a vertical drag scrolls the page instead), a tap sets the
 * value where the finger lifts, the bar thumb narrows to half its width while held, and a gesture
 * taken away mid-drag puts the value back. Screen readers adjust it with
 * the set-progress action; a keyboard or D-pad focuses it (with a highlight) and moves it with the
 * arrow keys. In a right-to-left layout it runs from the right.
 */
@Composable
fun ViewSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    enabled: Boolean = true,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val paints = remember(context, configuration.uiMode) { SliderPaints(context) }
    val currentValue by rememberUpdatedState(value)
    val keyed = remember { FloatArray(1) }
    keyed[0] = value
    val change by rememberUpdatedState(onValueChange)
    val span = valueRange.endInclusive - valueRange.start
    val geometry = remember { SliderGeometry() }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    var pressed by remember { mutableStateOf(false) }
    val interactions = remember { MutableInteractionSource() }
    val hasFocus by interactions.collectIsFocusedAsState()
    // Only keyboard focus shows; a slider never takes focus from a touch.
    val focused = hasFocus && LocalInputModeManager.current.inputMode == InputMode.Keyboard
    Box(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(value.coerceIn(valueRange), valueRange)
                if (enabled) {
                    setProgress { target ->
                        change(target.coerceIn(valueRange))
                        true
                    }
                } else {
                    disabled()
                }
            }.then(
                if (!enabled) {
                    Modifier
                } else {
                    Modifier
                        .onKeyEvent { event ->
                            val step = keyStep(event, span, rtl) ?: return@onKeyEvent false
                            if (event.type == KeyEventType.KeyDown) {
                                // Several presses can land before the new value comes back in.
                                keyed[0] = (keyed[0] + step).coerceIn(valueRange)
                                change(keyed[0])
                            }
                            true
                        }.focusable(interactionSource = interactions)
                        .pointerInput(valueRange, rtl) {
                            fun valueAt(x: Float): Float {
                                // From this node's own size: drawing may not have happened yet.
                                val side = SIDE_PADDING.toPx()
                                val along = ((x - side) / (size.width - side * 2)).coerceIn(0f, 1f)
                                return valueRange.start + (if (rtl) 1f - along else along) * span
                            }
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                val before = currentValue
                                val claimed = awaitHorizontalTouchSlopOrCancellation(down.id) { moved, _ -> moved.consume() }
                                if (claimed == null) {
                                    // A tap sets the value where it lifts; a scroll took the gesture otherwise.
                                    val up = currentEvent.changes.firstOrNull { it.id == down.id }
                                    if (up != null && !up.pressed && !up.isConsumed) {
                                        up.consume()
                                        change(valueAt(up.position.x))
                                    }
                                    return@awaitEachGesture
                                }
                                pressed = true
                                change(valueAt(claimed.position.x))
                                try {
                                    val completed =
                                        horizontalDrag(claimed.id) { moved ->
                                            moved.consume()
                                            change(valueAt(moved.position.x))
                                        }
                                    if (!completed) change(before)
                                } catch (e: CancellationException) {
                                    change(before)
                                    throw e
                                } finally {
                                    pressed = false
                                }
                            }
                        }
                },
            ).drawBehind {
                geometry.update(size.width, this)
                val fraction = if (span > 0f) ((currentValue - valueRange.start) / span).coerceIn(0f, 1f) else 0f
                drawIntoCanvas {
                    val canvas = it.nativeCanvas
                    val save = canvas.save()
                    // Right to left, the whole slider mirrors: it starts at the right.
                    if (rtl) canvas.scale(-1f, 1f, size.width / 2f, 0f)
                    paints.draw(canvas, geometry, size.height, fraction, enabled, pressed, focused, this)
                    canvas.restoreToCount(save)
                }
            },
    )
}

/**
 * How far an arrow key moves the value, or null for a key the slider ignores. Like MDC's slider
 * with no step size: one unit a press, and a twentieth of the range while the key repeats.
 */
private fun keyStep(
    event: KeyEvent,
    span: Float,
    rtl: Boolean,
): Float? {
    val direction =
        when (event.key) {
            Key.DirectionRight -> if (rtl) -1 else 1
            Key.DirectionLeft -> if (rtl) 1 else -1
            Key.Plus, Key.Equals, Key.NumPadAdd -> 1
            Key.Minus, Key.NumPadSubtract -> -1
            else -> return null
        }
    val increment = if (event.nativeKeyEvent.repeatCount > 0) maxOf(1f, span / 20f) else 1f
    return direction * increment
}

/** Where the track's first and last values sit, in pixels. */
private class SliderGeometry {
    var start = 0f
    var end = 0f

    fun update(
        width: Float,
        density: androidx.compose.ui.unit.Density,
    ) {
        val side = with(density) { SIDE_PADDING.toPx() }
        start = side
        end = width - side
    }
}

private class SliderPaints(
    context: android.content.Context,
) {
    private val activeTrack = ContextCompat.getColorStateList(context, com.google.android.material.R.color.m3_slider_active_track_color)!!
    private val inactiveTrack = ContextCompat.getColorStateList(context, com.google.android.material.R.color.m3_slider_inactive_track_color)!!
    private val thumb = ContextCompat.getColorStateList(context, com.google.android.material.R.color.m3_slider_thumb_color)!!
    private val inactiveStop = ContextCompat.getColorStateList(context, com.google.android.material.R.color.m3_slider_inactive_tick_marks_color)!!
    private val focusHighlight =
        android.util.TypedValue().let { value ->
            context.theme.resolveAttribute(androidx.appcompat.R.attr.colorControlHighlight, value, true)
            if (value.resourceId != 0) ContextCompat.getColor(context, value.resourceId) else value.data
        }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rect = RectF()

    fun draw(
        canvas: android.graphics.Canvas,
        geometry: SliderGeometry,
        height: Float,
        fraction: Float,
        enabled: Boolean,
        pressed: Boolean,
        focused: Boolean,
        density: androidx.compose.ui.unit.Density,
    ) {
        val state = if (enabled) intArrayOf(android.R.attr.state_enabled) else intArrayOf()
        with(density) {
            val trackHeight = TRACK_HEIGHT.toPx()
            val outer = trackHeight / 2
            val inside = INSIDE_CORNER.toPx()
            val gap = THUMB_GAP.toPx()
            val top = (height - trackHeight) / 2
            val bottom = top + trackHeight
            val center = geometry.start + fraction * (geometry.end - geometry.start)

            // Active track: from the rounded start to the gap before the thumb.
            val activeLeft = geometry.start - outer
            val activeRight = center - gap
            if (activeRight - activeLeft > inside * 2) {
                track(canvas, activeLeft, top, activeRight, bottom, outer, inside, activeTrack.getColorForState(state, 0))
            }
            // Inactive track: from the gap after the thumb to the rounded end.
            val inactiveLeft = center + gap
            val inactiveRight = geometry.end + outer
            if (inactiveRight - inactiveLeft > inside * 2) {
                track(canvas, inactiveLeft, top, inactiveRight, bottom, inside, outer, inactiveTrack.getColorForState(state, 0))
                // The stop indicator marks the last value while the thumb is clear of it.
                if (geometry.end - inactiveLeft > STOP.toPx()) {
                    paint.color = inactiveStop.getColorForState(state, 0)
                    canvas.drawCircle(geometry.end, (top + bottom) / 2, STOP.toPx() / 2, paint)
                }
            }
            // The bar thumb, half as wide while a finger holds it.
            val thumbWidth = THUMB_WIDTH.toPx() * if (pressed) 0.5f else 1f
            val thumbHeight = THUMB_HEIGHT.toPx()
            val thumbTop = (height - thumbHeight) / 2
            if (focused) {
                // Keyboard focus: the control highlight around the thumb.
                val ring = FOCUS_RING.toPx()
                rect.set(center - ring, (height - ring * 2) / 2, center + ring, (height + ring * 2) / 2)
                paint.color = focusHighlight
                canvas.drawRoundRect(rect, ring, ring, paint)
            }
            rect.set(center - thumbWidth / 2, thumbTop, center + thumbWidth / 2, thumbTop + thumbHeight)
            paint.color = thumb.getColorForState(state, 0)
            canvas.drawRoundRect(rect, thumbWidth / 2, thumbWidth / 2, paint)
        }
    }

    private fun track(
        canvas: android.graphics.Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        leftRadius: Float,
        rightRadius: Float,
        color: Int,
    ) {
        path.reset()
        rect.set(left, top, right, bottom)
        path.addRoundRect(
            rect,
            floatArrayOf(leftRadius, leftRadius, rightRadius, rightRadius, rightRadius, rightRadius, leftRadius, leftRadius),
            Path.Direction.CW,
        )
        paint.color = color
        canvas.drawPath(path, paint)
    }
}

private val SIDE_PADDING = 22.dp
private val TRACK_HEIGHT = 16.dp
private val INSIDE_CORNER = 2.dp
private val THUMB_GAP = 6.dp
private val THUMB_WIDTH = 4.dp
private val THUMB_HEIGHT = 44.dp
private val STOP = 4.dp
private val FOCUS_RING = 20.dp
