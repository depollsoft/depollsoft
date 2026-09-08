package depollsoft.tagmaster

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import com.bindroid.converters.ToStringConverter
import depollsoft.lib.ui.CustomTypefaceSpan
import depollsoft.lib.ui.SpannableUtilities
import java.text.SimpleDateFormat
import java.util.*

/** Parses feed, millisecond, ISO, or US dates; missing or unparseable dates stay absent. */
fun parseDate(dateString: String?): Date? {
    if (dateString.isNullOrBlank()) return null
    for (format in listOf("EEE, d MMM yyyy", "yyyy-MM-dd", "MMM d, yyyy")) {
        // Keep numeric timestamps, including pre-epoch values, unchanged.
        if (format == "yyyy-MM-dd") dateString.toLongOrNull()?.let { return Date(it) }
        try {
            SimpleDateFormat(format, Locale.US).parse(dateString)?.let { return it }
        } catch (_: java.text.ParseException) {
            // Try the next supported feed format.
        }
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
    val span = CustomTypefaceSpan("Wickhop Handwriting", TitleTypeface.get(ctx))
    val title = SpannableString(this)
    SpannableUtilities.applyToAll(title, span)
    return title
}

fun <TElement> List<*>?.asList(): List<TElement>? {
    @Suppress("UNCHECKED_CAST")
    return this as? List<TElement>
}
