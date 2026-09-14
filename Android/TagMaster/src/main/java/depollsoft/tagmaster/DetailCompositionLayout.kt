package depollsoft.tagmaster

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.children
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/** One measured caption axis and one fallback decision for the whole metadata group. */
class DetailMetadataLayout
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
    ) : LinearLayout(context, attrs) {
        init {
            orientation = VERTICAL
        }

        override fun onMeasure(
            widthMeasureSpec: Int,
            heightMeasureSpec: Int,
        ) {
            val rows = children.filterIsInstance<DetailPairLayout>().filter { it.visibility != GONE }.toList()
            val width = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
            val captionWidth =
                rows.maxOfOrNull {
                    ceil(
                        (it.getChildAt(0) as TextView).paint.measureText((it.getChildAt(0) as TextView).text.toString()),
                    ).toInt()
                }
                    ?: 0
            val valueBudget =
                rows.maxOfOrNull { row ->
                    fun budget(view: View): Int =
                        if (view is TextView) {
                            min(
                                ceil(view.paint.measureText(view.text.toString())).toInt(),
                                (
                                    view.textSize *
                                        8
                                ).toInt(),
                            )
                        } else if (view is DetailRatingLayout) {
                            view.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
                            view.measuredWidth
                        } else if (view is ViewGroup) {
                            view.children.sumOf { budget(it) }
                        } else {
                            view.minimumWidth
                        }
                    max((96 * resources.displayMetrics.density).toInt(), budget(row.getChildAt(1)))
                } ?: 0
            val stacked = captionWidth + (8 * resources.displayMetrics.density).toInt() + valueBudget > width
            var previousGroup: Any? = null
            rows.forEachIndexed { index, row ->
                row.captionWidth = if (stacked) -1 else captionWidth
                val group = row.tag
                val gap =
                    if (index == 0) {
                        0
                    } else if (stacked || group != previousGroup) {
                        16
                    } else {
                        4
                    }
                (row.layoutParams as LayoutParams).topMargin = (gap * resources.displayMetrics.density).toInt()
                previousGroup = group
            }
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }

/** Baselines refer to the visible text, not the top of a 48dp link hit region. */
class DetailPairLayout
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
    ) : ViewGroup(context, attrs) {
        var captionWidth = -1
        private var captionTop = 0
        private var valueTop = 0
        private var valueLeft = 0

        override fun onMeasure(
            widthMeasureSpec: Int,
            heightMeasureSpec: Int,
        ) {
            val width = MeasureSpec.getSize(widthMeasureSpec)
            val caption = getChildAt(0)
            val value = getChildAt(1)
            val gap = (resources.displayMetrics.density * if (captionWidth < 0) 4 else 8).toInt()

            fun measure(
                view: View,
                w: Int,
            ) = view.measure(
                MeasureSpec.makeMeasureSpec(max(0, w), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            )
            measure(caption, if (captionWidth < 0) width else captionWidth)
            valueLeft = if (captionWidth < 0) 0 else captionWidth + gap
            measure(value, width - valueLeft)
            captionTop = 0
            valueTop = if (captionWidth < 0) caption.measuredHeight + gap else 0
            if (captionWidth >= 0 && caption.baseline >= 0 && value.baseline >= 0) {
                captionTop = max(0, value.baseline - caption.baseline)
                valueTop = max(0, caption.baseline - value.baseline)
            }
            setMeasuredDimension(width, max(captionTop + caption.measuredHeight, valueTop + value.measuredHeight))
        }

        override fun onLayout(
            changed: Boolean,
            l: Int,
            t: Int,
            r: Int,
            b: Int,
        ) {
            val caption = getChildAt(0)
            val value = getChildAt(1)
            val rtl = layoutDirection == LAYOUT_DIRECTION_RTL
            val cx = if (rtl) width - caption.measuredWidth else 0
            val vx = if (rtl) width - valueLeft - value.measuredWidth else valueLeft
            caption.layout(cx, captionTop, cx + caption.measuredWidth, captionTop + caption.measuredHeight)
            value.layout(vx, valueTop, vx + value.measuredWidth, valueTop + value.measuredHeight)
        }

        override fun generateDefaultLayoutParams() = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        override fun generateLayoutParams(attrs: AttributeSet) = LayoutParams(context, attrs)
    }

class DetailRatingLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    init { gravity = android.view.Gravity.CENTER_VERTICAL }
    override fun getBaseline(): Int {
        val number = findViewById<TextView>(R.id.ratingTextView)
        return if (number != null && number.visibility != GONE && number.baseline >= 0) (measuredHeight - number.measuredHeight) / 2 + number.baseline else -1
    }
}
/** Split complete groups only when both have useful scaled reading width. */
class SummaryColumnsLayout
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
    ) : LinearLayout(context, attrs) {
        override fun onMeasure(
            widthMeasureSpec: Int,
            heightMeasureSpec: Int,
        ) {
            val prose = getChildAt(1) as ViewGroup
            prose.visibility = if (prose.children.any { it.visibility != GONE }) VISIBLE else GONE
            val gap = (16 * resources.displayMetrics.density).toInt()
            val minimum = 280 * resources.displayMetrics.density * resources.configuration.fontScale
            val wide = prose.visibility != GONE && MeasureSpec.getSize(widthMeasureSpec) >= minimum * 2 + gap
            orientation = if (wide) HORIZONTAL else VERTICAL
            children.forEachIndexed { index, child ->
                child.layoutParams =
                    (child.layoutParams as LayoutParams).apply {
                        width = if (wide) 0 else LayoutParams.MATCH_PARENT
                        weight = if (wide) 1f else 0f
                        marginStart = if (wide && index == 1) gap else 0
                        topMargin = if (!wide && index == 1 && prose.visibility != GONE) gap else 0
                    }
            }
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }

/** Section spacing belongs to visible siblings, never a hidden optional block. */
class DetailSectionsLayout
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
    ) : LinearLayout(context, attrs) {
        init {
            orientation = VERTICAL
        }

        override fun onMeasure(
            widthMeasureSpec: Int,
            heightMeasureSpec: Int,
        ) {
            children.filter { it.visibility != GONE }.forEachIndexed { index, child ->
                (child.layoutParams as LayoutParams).topMargin = if (index == 0) 0 else (16 * resources.displayMetrics.density).toInt()
            }
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }
