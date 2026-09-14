package depollsoft.tagmaster

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.google.android.material.tabs.TabLayout
import kotlin.math.max

/** Fixed, equal safe-width slots. Custom native labels wrap rather than shrink at large fonts. */
class FullWidthTabLayout
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
    ) : TabLayout(context, attrs) {
        override fun onMeasure(
            widthMeasureSpec: Int,
            heightMeasureSpec: Int,
        ) {
            val density = resources.displayMetrics.density
            val slot = (View.MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight) / max(1, tabCount)
            var height = (72 * density).toInt()
            for (index in 0 until tabCount) {
                getTabAt(index)?.customView?.let { child ->
                    child.measure(
                        View.MeasureSpec.makeMeasureSpec(max(0, slot - (16 * density).toInt()), View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    )
                    height = max(height, child.measuredHeight + (16 * density).toInt())
                }
            }
            super.onMeasure(
                widthMeasureSpec,
                View.MeasureSpec.makeMeasureSpec(
                    View.resolveSize(height + paddingTop + paddingBottom, heightMeasureSpec),
                    View.MeasureSpec.EXACTLY,
                ),
            )
        }
    }
