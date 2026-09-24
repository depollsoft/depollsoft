package depollsoft.tagmaster.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalDensity
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

    private val headlineSmallBase = style(24f, 32f, 0f, FontWeight.Normal)
    private val titleLargeBase = style(22f, 28f, 0f, FontWeight.Normal)
    private val titleMediumBase = style(16f, 24f, 0.15f, FontWeight.Medium)
    private val bodyLargeBase = style(16f, 24f, 0.5f, FontWeight.Normal)
    private val bodyMediumBase = style(14f, 20f, 0.25f, FontWeight.Normal)
    private val bodySmallBase = style(12f, 16f, 0.4f, FontWeight.Normal)
    private val labelLargeBase = style(14f, 20f, 0.1f, FontWeight.Medium)
    private val labelMediumBase = style(12f, 16f, 0.5f, FontWeight.Medium)

    val headlineSmall: TextStyle
        @Composable @ReadOnlyComposable get() = headlineSmallBase.inWholePixels()
    val titleLarge: TextStyle
        @Composable @ReadOnlyComposable get() = titleLargeBase.inWholePixels()
    val titleMedium: TextStyle
        @Composable @ReadOnlyComposable get() = titleMediumBase.inWholePixels()
    val bodyLarge: TextStyle
        @Composable @ReadOnlyComposable get() = bodyLargeBase.inWholePixels()
    val bodyMedium: TextStyle
        @Composable @ReadOnlyComposable get() = bodyMediumBase.inWholePixels()
    val bodySmall: TextStyle
        @Composable @ReadOnlyComposable get() = bodySmallBase.inWholePixels()
    val labelLarge: TextStyle
        @Composable @ReadOnlyComposable get() = labelLargeBase.inWholePixels()
    val labelMedium: TextStyle
        @Composable @ReadOnlyComposable get() = labelMediumBase.inWholePixels()

    /**
     * The same appearances on widgets that ignore a text appearance's line height (buttons,
     * chips, toolbar titles, AppCompat text views): one line is exactly as tall as the font.
     */
    fun TextStyle.withoutLineHeight() = copy(lineHeight = TextUnit.Unspecified, lineHeightStyle = null)
}

/**
 * A TextView reads a text appearance's size and line height as whole pixels
 * (`getDimensionPixelSize` rounds 14sp at 2.625x, 36.75px, to 37px), where Compose would keep the
 * fraction. On a device whose density is not a whole number (420, 440 or 560dpi, most phones)
 * the fraction makes every line shorter than the View's; round them the same way.
 */
@Composable
@ReadOnlyComposable
fun TextStyle.inWholePixels(): TextStyle {
    val density = LocalDensity.current
    fun TextUnit.rounded(): TextUnit {
        if (!isSp) return this
        val px = with(density) { toPx() }
        val whole = if (px == 0f) 0f else maxOf(1f, (px + 0.5f).toInt().toFloat())
        return with(density) { (whole / fontScale / this@with.density).sp }
    }
    return copy(fontSize = fontSize.rounded(), lineHeight = lineHeight.rounded())
}
