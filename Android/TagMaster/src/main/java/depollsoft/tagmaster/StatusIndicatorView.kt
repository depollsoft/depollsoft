package depollsoft.tagmaster

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.content.ContextCompat
import com.google.android.material.color.MaterialColors

/** Read-only availability metadata, deliberately not a checkable control. */
class StatusIndicatorView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = android.R.attr.textViewStyle,
    ) : AppCompatTextView(context, attrs, defStyleAttr) {
        private var available = false

        init {
            isClickable = false
            isFocusable = false
            compoundDrawablePadding = (4 * resources.displayMetrics.density).toInt()
            setIsAvailable(false)
        }

        fun getIsAvailable(): Boolean = available

        fun setIsAvailable(value: Boolean) {
            available = value
            val icon = AppCompatResources.getDrawable(context, if (value) R.drawable.ic_check else R.drawable.ic_clear)?.mutate()
            val tint = if (value) {
                ContextCompat.getColor(context, R.color.status_available)
            } else {
                MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant)
            }
            icon?.let { DrawableCompat.setTint(it, tint) }
            setTextColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface))
            setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, null, null)
            contentDescription =
                context.getString(
                    if (value) R.string.status_available else R.string.status_unavailable,
                    text,
                )
        }
    }
