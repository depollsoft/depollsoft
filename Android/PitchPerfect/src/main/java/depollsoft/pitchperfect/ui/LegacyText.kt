package depollsoft.pitchperfect.ui

import android.graphics.Typeface
import android.text.BoringLayout
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.layout
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit

/**
 * A paragraph laid out exactly as a TextView with `lineSpacingExtra` lays it out: the extra space
 * goes under every line but the last, and the first and last lines keep their font padding.
 * Compose's line height spreads space differently, so multi-line captions that must match their
 * View-era rendering use this. It also draws Android spans (typeface, relative size, subscript)
 * with the line metrics TextView gave them, which Compose's baseline shift does not reproduce.
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
                textSize = with(density) { size.toPx() }
                this.color = color.toArgb()
                this.typeface = typeface
                this.letterSpacing = letterSpacing
                this.density = density.density
            }
        }
    val spacing = with(density) { lineSpacingExtra.toPx() }
    val alignment =
        when (align) {
            TextAlign.Center -> Layout.Alignment.ALIGN_CENTER
            TextAlign.End, TextAlign.Right -> Layout.Alignment.ALIGN_OPPOSITE
            else -> Layout.Alignment.ALIGN_NORMAL
        }
    val built = remember { arrayOfNulls<Layout>(1) }
    androidx.compose.foundation.layout.Box(
        modifier
            .semantics { this.text = AnnotatedString(text.toString()) }
            .layout { measurable, constraints ->
                // TextView lays a single line out with BoringLayout, whose height comes from the
                // paint alone even when spans (a subscript, a bigger glyph) reach further.
                val boring = BoringLayout.isBoring(text, paint)
                // A wrap-content TextView is as wide as its text; one given a width (a weight) fills it.
                val width =
                    if (wrapWidth && !constraints.hasFixedWidth) {
                        (boring?.width ?: kotlin.math.ceil(Layout.getDesiredWidth(text, paint)).toInt())
                            .coerceIn(constraints.minWidth, constraints.maxWidth)
                    } else {
                        constraints.maxWidth
                    }
                val textLayout =
                    if (boring != null && boring.width <= width) {
                        BoringLayout.make(text, paint, width, alignment, 1f, spacing, boring, true)
                    } else {
                        StaticLayout.Builder
                            .obtain(text, 0, text.length, paint, width)
                            .setAlignment(alignment)
                            .setLineSpacing(spacing, 1f)
                            .setIncludePad(true)
                            // TextView's default since API 28: fallback fonts' taller lines count.
                            .setUseLineSpacingFromFallbacks(true)
                            .setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY)
                            .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
                            .build()
                    }
                built[0] = textLayout
                val height = textLayout.height
                val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width, minHeight = height, maxHeight = height))
                layout(width, height) { placeable.place(0, 0) }
            }.drawBehind { drawIntoCanvas { canvas -> built[0]?.draw(canvas.nativeCanvas) } },
    )
}
