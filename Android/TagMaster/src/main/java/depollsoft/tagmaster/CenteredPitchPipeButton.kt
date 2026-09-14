package depollsoft.tagmaster

import android.content.Context
import android.text.Layout
import android.util.AttributeSet
import android.view.Gravity
import android.view.View.MeasureSpec
import depollsoft.pitchperfect.lib.ui.PitchPipeButton
import kotlin.math.ceil
import kotlin.math.max

/** Center the note icon and text as one group without changing shared pitch behavior. */
class CenteredPitchPipeButton
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
    ) : PitchPipeButton(context, attrs) {
        private var ready = false
        private val minimumStart = paddingStart
        private val minimumEnd = paddingEnd

        init {
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            ready = true
        }

        override fun onTextChanged(
            text: CharSequence?,
            start: Int,
            lengthBefore: Int,
            lengthAfter: Int,
        ) {
            super.onTextChanged(text, start, lengthBefore, lengthAfter)
            // Fixed-size TextViews may only invalidate when the text changes.
            if (ready) requestLayout()
        }

        override fun onMeasure(
            widthMeasureSpec: Int,
            heightMeasureSpec: Int,
        ) {
            // TextView resolves its final text metrics during measurement.
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY) {
                val icons = compoundDrawablesRelative
                val iconWidth =
                    listOfNotNull(icons[0], icons[2]).sumOf {
                        max(0, it.intrinsicWidth) + compoundDrawablePadding
                    }
                val textWidth = ceil(Layout.getDesiredWidth(text, paint).toDouble()).toInt()
                val centered = (MeasureSpec.getSize(widthMeasureSpec) - iconWidth - textWidth) / 2
                val start = if (centered >= max(minimumStart, minimumEnd)) centered else minimumStart
                val end = if (centered >= max(minimumStart, minimumEnd)) centered else minimumEnd
                if (paddingStart != start || paddingEnd != end) {
                    setPaddingRelative(start, paddingTop, end, paddingBottom)
                    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
                }
            }
        }
    }
