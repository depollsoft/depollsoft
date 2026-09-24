package depollsoft.pitchperfect.ui

import android.graphics.Typeface
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
 * View-era rendering use this.
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
    val built = remember { arrayOfNulls<StaticLayout>(1) }
    androidx.compose.foundation.layout.Box(
        modifier
            .semantics { this.text = AnnotatedString(text.toString()) }
            .layout { measurable, constraints ->
                val width = constraints.maxWidth
                val staticLayout =
                    StaticLayout.Builder
                        .obtain(text, 0, text.length, paint, width)
                        .setAlignment(alignment)
                        .setLineSpacing(spacing, 1f)
                        .setIncludePad(true)
                        .setBreakStrategy(android.text.Layout.BREAK_STRATEGY_HIGH_QUALITY)
                        .setHyphenationFrequency(android.text.Layout.HYPHENATION_FREQUENCY_NONE)
                        .build()
                built[0] = staticLayout
                val placeable = measurable.measure(constraints.copy(minHeight = staticLayout.height, maxHeight = staticLayout.height))
                layout(width, staticLayout.height) { placeable.place(0, 0) }
            }.drawBehind { drawIntoCanvas { canvas -> built[0]?.draw(canvas.nativeCanvas) } },
    )
}
