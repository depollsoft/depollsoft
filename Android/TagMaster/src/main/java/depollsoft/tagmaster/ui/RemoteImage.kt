package depollsoft.tagmaster.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.URL

private const val MAX_DOWNLOAD_BYTES = 8 * 1024 * 1024

/** Decoded thumbnails, keyed by URL and target size, shared by every image on screen. */
private val imageCache =
    object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 16).toInt().coerceAtLeast(1024 * 1024),
    ) {
        override fun sizeOf(
            key: String,
            value: Bitmap,
        ) = value.byteCount
    }

/**
 * An image downloaded from [url] and drawn fit-centered (a video thumbnail). A missing, invalid or
 * offline image leaves the space empty.
 */
@Composable
fun RemoteImage(
    url: String?,
    modifier: Modifier = Modifier,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    var bitmap by remember(url) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(url, size) {
        if (url.isNullOrEmpty() || size == IntSize.Zero) return@LaunchedEffect
        val key = "$url@${size.width}x${size.height}"
        bitmap = imageCache.get(key) ?: withContext(Dispatchers.IO) { download(url, size) }?.also { imageCache.put(key, it) }
    }
    Box(
        modifier
            .onSizeChanged { size = it }
            .drawBehind {
                val image = bitmap ?: return@drawBehind
                // ImageView's FIT_CENTER (Matrix.setRectToRect with CENTER).
                val scale = minOf(this.size.width / image.width, this.size.height / image.height)
                val width = image.width * scale
                val height = image.height * scale
                val left = (this.size.width - width) / 2f
                val top = (this.size.height - height) / 2f
                drawIntoCanvas {
                    it.nativeCanvas.drawBitmap(
                        image,
                        null,
                        android.graphics.RectF(left, top, left + width, top + height),
                        android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG),
                    )
                }
            },
    )
}

private fun download(
    url: String,
    target: IntSize,
): Bitmap? =
    try {
        val connection = URL(url).openConnection()
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        val data =
            connection.getInputStream().use { input ->
                ByteArrayOutputStream().use { output ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        val count = input.read(buffer)
                        if (count == -1) break
                        if (output.size() + count > MAX_DOWNLOAD_BYTES) return null
                        output.write(buffer, 0, count)
                    }
                    output.toByteArray()
                }
            }
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(data, 0, data.size, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) {
            null
        } else {
            options.inSampleSize = 1
            while (options.outWidth / options.inSampleSize > target.width * 2L ||
                options.outHeight / options.inSampleSize > target.height * 2L
            ) {
                options.inSampleSize *= 2
            }
            options.inJustDecodeBounds = false
            BitmapFactory.decodeByteArray(data, 0, data.size, options)
        }
    } catch (_: Exception) {
        null
    }
