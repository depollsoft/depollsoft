package depollsoft.pitchperfect

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

/** Full-bleed, low-contrast score engraving behind every app surface. */
class HeritageScoreView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
    ) : View(context, attrs) {
        private val artwork =
            runCatching { BitmapFactory.decodeResource(resources, R.drawable.panobackground) }.getOrNull()
        private val engravingColor =
            runCatching { ContextCompat.getColor(context, R.color.plate_ink_secondary) }
                .getOrElse { Color.GRAY }
        private val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                alpha = 28
                colorFilter = PorterDuffColorFilter(engravingColor, PorterDuff.Mode.SRC_IN)
            }

        init {
            setWillNotDraw(false)
            isClickable = false
            isFocusable = false
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val bitmap = artwork ?: return
            val source = Rect(0, 0, bitmap.width, bitmap.height)
            val tileHeight = width * (bitmap.height.toFloat() / bitmap.width.toFloat())
            if (tileHeight <= 0f) return

            var tileTop = 0f
            while (tileTop < height) {
                canvas.drawBitmap(
                    bitmap,
                    source,
                    RectF(0f, tileTop, width.toFloat(), tileTop + tileHeight),
                    paint,
                )
                tileTop += tileHeight
            }
        }
    }
