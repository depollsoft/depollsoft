package depollsoft.tagmaster

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.TextPaint
import com.bindroid.converters.ToStringConverter
import depollsoft.lib.ui.CustomTypefaceSpan
import depollsoft.lib.ui.SpannableUtilities
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.*

/** Parses feed, millisecond, ISO, or US dates; missing or unparseable dates stay absent. */
fun parseDate(dateString: String?): Date? {
    if (dateString.isNullOrBlank()) return null
    // Keep numeric timestamps, including pre-epoch values, unchanged.
    dateString.toLongOrNull()?.let { return Date(it) }
    for (format in listOf("EEE, d MMM yyyy", "yyyy-MM-dd", "MMM d, yyyy")) {
        val parser = SimpleDateFormat(format, Locale.US).apply { isLenient = false }
        val position = ParsePosition(0)
        val parsed = parser.parse(dateString, position)
        if (parsed != null && position.index == dateString.length) return parsed
    }
    return null
}

/** Date formats otherwise render null as the literal text "null". */
class NullableDateConverter(
    format: String,
) : ToStringConverter(format) {
    override fun convertToTarget(
        sourceValue: Any?,
        targetType: Class<*>?,
    ): Any = if (sourceValue == null) "" else super.convertToTarget(sourceValue, targetType)
}

private object TitleTypeface {
    private var cached: Typeface? = null

    @Synchronized
    fun get(context: Context): Typeface =
        cached ?: Typeface
            .createFromAsset(
                context.applicationContext.assets,
                "fonts/wickhop-handwriting.ttf",
            ).also { cached = it }
}

fun CharSequence.makeTitleString(ctx: Context): CharSequence {
    // Keep Wickhop's full font bounds inside the toolbar, including at large font scales.
    // Body text continues to scale normally; only this display face has a height ceiling.
    val maxHeight = 40f * ctx.resources.displayMetrics.density
    val span = object : CustomTypefaceSpan("Wickhop Handwriting", TitleTypeface.get(ctx)) {
        private fun fit(paint: TextPaint) {
            val metrics = paint.fontMetrics
            val height = metrics.bottom - metrics.top
            if (height > maxHeight) paint.textSize *= maxHeight / height
        }

        override fun updateMeasureState(paint: TextPaint) {
            super.updateMeasureState(paint)
            fit(paint)
        }

        override fun updateDrawState(paint: TextPaint) {
            super.updateDrawState(paint)
            fit(paint)
        }
    }
    val title = SpannableString(this)
    SpannableUtilities.applyToAll(title, span)
    return title
}

fun <TElement> List<*>?.asList(): List<TElement>? {
    @Suppress("UNCHECKED_CAST")
    return this as? List<TElement>
}
