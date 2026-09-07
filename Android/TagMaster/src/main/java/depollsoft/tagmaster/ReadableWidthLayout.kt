package depollsoft.tagmaster

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout

/**
 * Centres its content and stops it growing past a readable measure.
 *
 * Lyrics, notes and settings rows are text, and text that runs the full width of
 * a 10-inch tablet is hard to track back to the next line. The cap comes from
 * `tm_content_max_width`, which itself varies by screen width.
 */
class ReadableWidthLayout
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : FrameLayout(context, attrs, defStyleAttr) {
        private val maxContentWidth: Int
            get() = resources.getDimensionPixelSize(R.dimen.tm_content_max_width)

        override fun onMeasure(
            widthMeasureSpec: Int,
            heightMeasureSpec: Int,
        ) {
            val available = MeasureSpec.getSize(widthMeasureSpec)
            val mode = MeasureSpec.getMode(widthMeasureSpec)
            val cap = maxContentWidth
            val spec =
                if (mode != MeasureSpec.UNSPECIFIED && available > cap) {
                    MeasureSpec.makeMeasureSpec(cap, mode)
                } else {
                    widthMeasureSpec
                }
            super.onMeasure(spec, heightMeasureSpec)
            if (mode == MeasureSpec.EXACTLY) {
                setMeasuredDimension(available, measuredHeight)
            }
        }

        override fun onLayout(
            changed: Boolean,
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
        ) {
            val width = right - left
            val cap = minOf(maxContentWidth, width)
            val offset = (width - cap) / 2
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                if (child.visibility == View.GONE) continue
                child.layout(offset, 0, offset + cap, child.measuredHeight)
            }
        }
    }
