package depollsoft.tagmaster

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.textview.MaterialTextView
import java.util.Calendar
import java.util.GregorianCalendar

/** Keeps the footer year current, including when returning after New Year. */
class CopyrightTextView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = android.R.attr.textViewStyle,
    ) : MaterialTextView(context, attrs, defStyleAttr) {
        init {
            updateYear()
        }

        override fun onWindowVisibilityChanged(visibility: Int) {
            super.onWindowVisibilityChanged(visibility)
            if (visibility == VISIBLE) updateYear()
        }

        private fun updateYear() {
            text = context.getString(R.string.Copyright, GregorianCalendar().get(Calendar.YEAR))
        }
    }
