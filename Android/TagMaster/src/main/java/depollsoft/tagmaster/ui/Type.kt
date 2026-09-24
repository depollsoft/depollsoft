package depollsoft.tagmaster.ui

import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Material 3's type scale, laid out the way a themed TextView lays it out.
 *
 * A TextView keeps the font's top and bottom padding (`includeFontPadding`) and applies a text
 * appearance's line height as spacing between lines only, so a single line is exactly as tall as
 * the font and every further line adds the line height. Compose reproduces that with font padding
 * on and the extra height placed above each line, trimmed from the first and last.
 */
object TagMasterType {
    private val viewLineHeight = LineHeightStyle(LineHeightStyle.Alignment.Bottom, LineHeightStyle.Trim.Both)
    private val viewPlatform = PlatformTextStyle(includeFontPadding = true)

    private fun style(
        size: Float,
        lineHeight: Float?,
        letterSpacing: Float,
        weight: FontWeight,
    ) = TextStyle(
        fontSize = size.sp,
        lineHeight = lineHeight?.sp ?: TextUnit.Unspecified,
        letterSpacing = (letterSpacing / size).em,
        fontWeight = weight,
        platformStyle = viewPlatform,
        lineHeightStyle = if (lineHeight != null) viewLineHeight else null,
    )

    val headlineSmall = style(24f, 32f, 0f, FontWeight.Normal)
    val titleLarge = style(22f, 28f, 0f, FontWeight.Normal)
    val titleMedium = style(16f, 24f, 0.15f, FontWeight.Medium)
    val bodyLarge = style(16f, 24f, 0.5f, FontWeight.Normal)
    val bodyMedium = style(14f, 20f, 0.25f, FontWeight.Normal)
    val bodySmall = style(12f, 16f, 0.4f, FontWeight.Normal)
    val labelLarge = style(14f, 20f, 0.1f, FontWeight.Medium)
    val labelMedium = style(12f, 16f, 0.5f, FontWeight.Medium)

    /**
     * The same appearances on widgets that ignore a text appearance's line height (buttons,
     * chips, toolbar titles, AppCompat text views): one line is exactly as tall as the font.
     */
    fun TextStyle.withoutLineHeight() = copy(lineHeight = TextUnit.Unspecified, lineHeightStyle = null)
}
