package depollsoft.tagmaster.ui

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.launch

private const val MID_SCALE = 1.75f
private const val MAX_SCALE = 3f

/**
 * A pinch-and-pan image, as PhotoView showed sheet music: fitted and centered at first, zoomed
 * from 1x to 3x by pinching, and stepped 1x → 1.75x → 3x → 1x by double-tapping. Panning keeps
 * the image covering the view wherever it is larger than it.
 */
@Composable
fun ZoomableImage(
    image: Bitmap,
    modifier: Modifier = Modifier,
    keepScreenOn: Boolean = false,
) {
    val view = LocalView.current
    DisposableEffect(view, keepScreenOn) {
        val previous = view.keepScreenOn
        view.keepScreenOn = keepScreenOn
        onDispose { view.keepScreenOn = previous }
    }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var scale by remember(image) { mutableFloatStateOf(1f) }
    var offset by remember(image) { mutableStateOf(Offset.Zero) }
    val scope = rememberCoroutineScope()
    val paint = remember { Paint(Paint.FILTER_BITMAP_FLAG) }

    fun fitted(): RectF {
        // FIT_CENTER (Matrix.setRectToRect with CENTER): scaled to fit and centered.
        val fit = minOf(size.width.toFloat() / image.width, size.height.toFloat() / image.height)
        val width = image.width * fit
        val height = image.height * fit
        val left = (size.width - width) * 0.5f
        val top = (size.height - height) * 0.5f
        return RectF(left, top, left + width, top + height)
    }

    fun clamp(candidate: Offset, zoom: Float): Offset {
        val base = fitted()
        val width = base.width() * zoom
        val height = base.height() * zoom
        val maxX = ((width - size.width) / 2f).coerceAtLeast(0f)
        val maxY = ((height - size.height) / 2f).coerceAtLeast(0f)
        return Offset(candidate.x.coerceIn(-maxX, maxX), candidate.y.coerceIn(-maxY, maxY))
    }

    Box(
        modifier
            .onSizeChanged { size = it }
            .pointerInput(image) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val next = (scale * zoom).coerceIn(1f, MAX_SCALE)
                    val center = Offset(size.width / 2f, size.height / 2f)
                    // Zoom about the fingers: the point under them stays under them.
                    val focus = centroid - center - offset
                    val moved = offset + pan - focus * (next / scale - 1f)
                    scale = next
                    offset = clamp(moved, next)
                }
            }.pointerInput(image) {
                detectTapGestures(onDoubleTap = { tap ->
                    val target =
                        when {
                            scale < MID_SCALE - 0.01f -> MID_SCALE
                            scale < MAX_SCALE - 0.01f -> MAX_SCALE
                            else -> 1f
                        }
                    scope.launch {
                        val start = scale
                        val startOffset = offset
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val focus = tap - center - startOffset
                        val progress = Animatable(0f)
                        progress.animateTo(1f) {
                            val current = start + (target - start) * value
                            scale = current
                            offset = clamp(startOffset - focus * (current / start - 1f), current)
                        }
                    }
                })
            }.drawBehind {
                if (size == IntSize.Zero) return@drawBehind
                val base = fitted()
                val width = base.width() * scale
                val height = base.height() * scale
                val left = base.centerX() - width / 2f + offset.x
                val top = base.centerY() - height / 2f + offset.y
                drawIntoCanvas { it.nativeCanvas.drawBitmap(image, null, RectF(left, top, left + width, top + height), paint) }
            },
    )
}
