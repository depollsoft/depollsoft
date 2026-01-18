package depollsoft.tagmaster

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import depollsoft.lib.ui.CustomTypefaceSpan
import depollsoft.lib.ui.SpannableUtilities
import java.text.SimpleDateFormat
import java.util.*

/**
 * Parses a date string that could be in various formats:
 * - Milliseconds timestamp (as a string)
 * - ISO format (yyyy-MM-dd)
 * - US format (MMM d, yyyy)
 * Returns Date(0) if parsing fails.
 */
fun parseDate(dateString: String?): Date {
    if (dateString.isNullOrBlank()) return Date(0)
    return try {
        // Try parsing as milliseconds first (for date strings that are timestamps)
        Date(dateString.toLong())
    } catch (e: NumberFormatException) {
        try {
            // Try ISO date format
            SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateString) ?: Date(0)
        } catch (e2: Exception) {
            try {
                // Try US date format
                SimpleDateFormat("MMM d, yyyy", Locale.US).parse(dateString) ?: Date(0)
            } catch (e3: Exception) {
                Date(0)
            }
        }
    }
}

fun CharSequence.makeTitleString(ctx: Context): CharSequence {
    val typeface = Typeface.createFromAsset(ctx.assets, "fonts/wickhop-handwriting.ttf")
    val span = CustomTypefaceSpan("Wickhop Handwriting", typeface)
    val title = SpannableString(this)
    SpannableUtilities.applyToAll(title, span)
    return title
}

fun <TElement> List<*>?.asList(): List<TElement>? {
    @Suppress("UNCHECKED_CAST")
    return this as? List<TElement>
}