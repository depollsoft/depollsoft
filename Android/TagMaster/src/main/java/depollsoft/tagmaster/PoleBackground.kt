package depollsoft.tagmaster

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat

/** One aspect-fit original pole behind the entire workspace, including both panes. */
class PoleBackground(
    context: Context,
) : Drawable() {
    private val pole = requireNotNull(AppCompatResources.getDrawable(context, R.drawable.ic_barberpole))
    private val canvasColor = ContextCompat.getColor(context, R.color.tm_background)

    override fun draw(canvas: Canvas) {
        canvas.drawColor(canvasColor)
        val scale =
            minOf(
                bounds.width().toFloat() / pole.intrinsicWidth,
                bounds.height().toFloat() / pole.intrinsicHeight,
            )
        val width = (pole.intrinsicWidth * scale).toInt()
        val height = (pole.intrinsicHeight * scale).toInt()
        val left = bounds.centerX() - width / 2
        val top = bounds.centerY() - height / 2
        pole.setBounds(left, top, left + width, top + height)
        pole.draw(canvas)
    }

    override fun setAlpha(alpha: Int) {
        pole.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        pole.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.OPAQUE
}
