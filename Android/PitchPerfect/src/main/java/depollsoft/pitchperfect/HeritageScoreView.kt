package depollsoft.pitchperfect

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.RectF
import android.content.res.Resources
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

internal object HeritageArtwork {
    @Volatile
    private var bitmap: Bitmap? = null

    fun get(resources: Resources): Bitmap? =
        bitmap ?: synchronized(this) {
            bitmap ?: runCatching {
                BitmapFactory.decodeResource(resources, R.drawable.panobackground)
            }.getOrNull()?.also { bitmap = it }
        }
}

/** Full-bleed, low-contrast score engraving behind every app surface. */
class HeritageScoreView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
    ) : View(context, attrs) {
        private val artwork = HeritageArtwork.get(resources)
        private val engravingColor =
            runCatching { ContextCompat.getColor(context, R.color.plate_ink_secondary) }
                .getOrElse { Color.GRAY }
        private val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                alpha = 28
                colorFilter = PorterDuffColorFilter(engravingColor, PorterDuff.Mode.SRC_IN)
            }
        private var renderedBackground: Bitmap? = null

        init {
            setWillNotDraw(false)
            isClickable = false
            isFocusable = false
        }

        override fun onSizeChanged(
            width: Int,
            height: Int,
            oldWidth: Int,
            oldHeight: Int,
        ) {
            super.onSizeChanged(width, height, oldWidth, oldHeight)
            renderedBackground?.recycle()
            renderedBackground = null
            val sourceBitmap = artwork ?: return
            if (width <= 0 || height <= 0) return

            val tileHeight = width * (sourceBitmap.height.toFloat() / sourceBitmap.width.toFloat())
            if (tileHeight <= 0f) return
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val backgroundCanvas = Canvas(bitmap)
            val source = Rect(0, 0, sourceBitmap.width, sourceBitmap.height)
            var tileTop = 0f
            while (tileTop < height) {
                backgroundCanvas.drawBitmap(
                    sourceBitmap,
                    source,
                    RectF(0f, tileTop, width.toFloat(), tileTop + tileHeight),
                    paint,
                )
                tileTop += tileHeight
            }
            renderedBackground = bitmap
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            renderedBackground?.let { canvas.drawBitmap(it, 0f, 0f, null) }
        }
    }
