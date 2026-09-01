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

/** Sparse heritage marks over the clean staff: one treble and one bass clef. */
class HeritageScoreView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
    ) : View(context, attrs) {
        private val artwork =
            runCatching { BitmapFactory.decodeResource(resources, R.drawable.panobackground) }.getOrNull()
        private val hairline =
            runCatching { ContextCompat.getColor(context, R.color.plate_ink_secondary) }
                .getOrElse { Color.GRAY }
        private val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                alpha = 42
                colorFilter = PorterDuffColorFilter(hairline, PorterDuff.Mode.SRC_IN)
            }

        init {
            setWillNotDraw(false)
            isClickable = false
            isFocusable = false
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val bitmap = artwork ?: return
            val density = resources.displayMetrics.density
            val markWidth = minOf(width * 0.43f, 220f * density)
            val markHeight = markWidth * 0.58f
            val margin = 14f * density

            val trebleSource = Rect(0, 0, bitmap.width / 3, bitmap.height / 2)
            val bassSource = Rect(0, bitmap.height / 2, bitmap.width / 3, bitmap.height)
            val trebleTop = height * 0.08f
            val bassTop = height * 0.66f

            canvas.drawBitmap(
                bitmap,
                trebleSource,
                RectF(margin, trebleTop, margin + markWidth, trebleTop + markHeight),
                paint,
            )
            canvas.drawBitmap(
                bitmap,
                bassSource,
                RectF(
                    width - margin - markWidth,
                    bassTop,
                    width - margin,
                    bassTop + markHeight,
                ),
                paint,
            )
        }
    }
