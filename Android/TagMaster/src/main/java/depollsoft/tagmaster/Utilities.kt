package depollsoft.tagmaster

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

fun <TElement> List<*>?.asList(): List<TElement>? {
    @Suppress("UNCHECKED_CAST")
    return this as? List<TElement>
}
