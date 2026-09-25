package depollsoft.pitchperfect.ui

import depollsoft.compose.inWholePixels
import android.graphics.Typeface
import android.text.BoringLayout
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import kotlin.math.ceil

/**
 * Text laid out and drawn exactly as a TextView lays it out and draws it, by the same Android
 * text layouts: `lineSpacingExtra` goes under every line but the last, one line takes TextView's
 * BoringLayout (whose height comes from the paint alone, even when a span such as a subscript
 * reaches further), and Android spans (typeface, relative size, subscript) draw as they did.
 * Compose's own paragraph spreads line spacing and baseline shifts differently.
 *
 * @param wrapWidth whether the text is as wide as its content (a wrap-content TextView) rather
 *   than as wide as it is offered.
 */
@Composable
fun LegacyText(
    text: CharSequence,
    size: TextUnit,
    color: Color,
    typeface: Typeface,
    modifier: Modifier = Modifier,
    letterSpacing: Float = 0f,
    lineSpacingExtra: Dp = Dp(0f),
    align: TextAlign = TextAlign.Start,
    wrapWidth: Boolean = false,
) {
    val density = LocalDensity.current
    val paint =
        remember(size, color, typeface, letterSpacing, density) {
            TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                // Whole pixels, as a TextView reads an XML text size.
                textSize = with(density) { size.inWholePixels(density).toPx() }
                this.color = color.toArgb()
                this.typeface = typeface
                this.letterSpacing = letterSpacing
                this.density = density.density
            }
        }
    val alignment =
        when (align) {
            TextAlign.Center -> Layout.Alignment.ALIGN_CENTER
            TextAlign.End, TextAlign.Right -> Layout.Alignment.ALIGN_OPPOSITE
            else -> Layout.Alignment.ALIGN_NORMAL
        }
    val sizing =
        remember(text, paint, alignment, lineSpacingExtra, wrapWidth) {
            AndroidTextLayout(text, paint, alignment, with(density) { lineSpacingExtra.toPx() }, wrapWidth)
        }
    Box(
        modifier
            .semantics { this.text = AnnotatedString(text.toString()) }
            .then(sizing)
            .drawBehind { drawIntoCanvas { canvas -> sizing.laidOut?.draw(canvas.nativeCanvas) } },
    )
}

private class AndroidTextLayout(
    private val text: CharSequence,
    private val paint: TextPaint,
    private val alignment: Layout.Alignment,
    private val spacing: Float,
    private val wrapWidth: Boolean,
) : LayoutModifier {
    /** The layout from the last measure, which the draw pass paints. */
    var laidOut: Layout? = null
        private set

    private val boring: BoringLayout.Metrics? = BoringLayout.isBoring(text, paint)

    private val desiredWidth: Int
        get() = boring?.width ?: ceil(Layout.getDesiredWidth(text, paint)).toInt()

    private fun build(width: Int): Layout =
        if (boring != null && boring.width <= width) {
            BoringLayout.make(text, paint, width, alignment, 1f, spacing, boring, true)
        } else {
            StaticLayout.Builder
                .obtain(text, 0, text.length, paint, width.coerceAtLeast(0))
                .setAlignment(alignment)
                .setLineSpacing(spacing, 1f)
                .setIncludePad(true)
                // TextView's default since API 28: fallback fonts' taller lines count.
                .setUseLineSpacingFromFallbacks(true)
                .setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY)
                .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
                .build()
        }

    private fun widthFor(constraints: Constraints): Int =
        when {
            wrapWidth && !constraints.hasFixedWidth -> desiredWidth.coerceIn(constraints.minWidth, constraints.maxWidth)
            constraints.hasBoundedWidth -> constraints.maxWidth
            else -> desiredWidth.coerceAtLeast(constraints.minWidth)
        }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val width = widthFor(constraints)
        val textLayout = build(width)
        laidOut = textLayout
        val height = textLayout.height.coerceIn(constraints.minHeight, constraints.maxHeight)
        val placeable = measurable.measure(Constraints.fixed(width, height))
        return layout(width, height) { placeable.place(0, 0) }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int,
    ): Int = desiredWidth

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int,
    ): Int = if (wrapWidth) desiredWidth else 0

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int,
    ): Int = build(if (width == Constraints.Infinity) desiredWidth else width).height

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int,
    ): Int = build(if (width == Constraints.Infinity) desiredWidth else width).height
}
