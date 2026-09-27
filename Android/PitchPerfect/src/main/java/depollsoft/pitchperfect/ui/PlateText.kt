package depollsoft.pitchperfect.ui

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import kotlin.math.ceil

/**
 * Text sized the way a wrap-content TextView sizes itself.
 *
 * Compose pads a letter-spaced paragraph's intrinsic width by half a pixel so rounding can never
 * wrap it, and rounds a line's height from its float metrics; TextView rounds each font metric
 * outward. Either makes a label a pixel off, and anything centred on it too. Text that fits on one
 * line here is laid out at exactly TextView's size, for layout and for intrinsics alike; [align]
 * places the line within its width as the TextView's gravity did.
 */
@Composable
fun PlateText(
    text: AnnotatedString,
    style: TextStyle,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Visible,
    softWrap: Boolean = true,
    align: TextAlign = TextAlign.Unspecified,
) {
    val measurer = rememberTextMeasurer()
    val styled = if (align == TextAlign.Unspecified) style else style.copy(textAlign = align)
    Text(
        text,
        modifier
            .then(TextViewSizing(text, styled, overflow, softWrap, maxLines, measurer))
            .drawWithContent {
                // TextView clips glyphs that overhang its left edge and its top and bottom, but
                // not its right edge; a glyph's negative bearing is cut exactly as it was.
                clipRect(left = 0f, top = 0f, right = Float.MAX_VALUE, bottom = size.height) {
                    this@drawWithContent.drawContent()
                }
            },
        style = styled,
        maxLines = maxLines,
        overflow = overflow,
        softWrap = softWrap,
    )
}

@Composable
fun PlateText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Visible,
    softWrap: Boolean = true,
    align: TextAlign = TextAlign.Unspecified,
) = PlateText(AnnotatedString(text), style, modifier, maxLines, overflow, softWrap, align)

/** One-line text's TextView size, answered for layout and for intrinsic measurements. */
private class TextViewSizing(
    private val text: AnnotatedString,
    private val style: TextStyle,
    private val overflow: TextOverflow,
    private val softWrap: Boolean,
    private val maxLines: Int,
    private val measurer: TextMeasurer,
) : LayoutModifier {
    /** The line's width and height, or null when the text takes more than one line. */
    private fun lineSize(maxWidth: Int): IntSize? {
        val probe =
            measurer.measure(
                text,
                // Start-aligned: a centred line's bounds depend on the width it is centred in.
                style = style.copy(textAlign = TextAlign.Start),
                overflow = overflow,
                softWrap = softWrap,
                maxLines = maxLines,
                constraints = Constraints(maxWidth = maxWidth),
            )
        if (probe.lineCount != 1) return null
        val baseline = probe.getLineBaseline(0)
        return IntSize(
            ceil(probe.getLineRight(0) - probe.getLineLeft(0)).toInt(),
            (ceil(baseline) + ceil(probe.getLineBottom(0) - baseline)).toInt(),
        )
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val line = lineSize(constraints.maxWidth)
        if (line == null) {
            val placeable = measurable.measure(constraints)
            return layout(placeable.width, placeable.height) { placeable.place(0, 0) }
        }
        val width = line.width.coerceIn(constraints.minWidth, constraints.maxWidth)
        // Centred or end-aligned text is laid out at exactly that width so it lands where the
        // TextView's gravity put it. Start-aligned text keeps the room Compose wants — a hair more
        // than the line, which at exactly the line's width could wrap a multi-word label — and
        // simply reports the TextView's width.
        val exact = style.textAlign == TextAlign.Center || style.textAlign == TextAlign.End
        val placeable =
            measurable.measure(
                if (exact) {
                    constraints.copy(minWidth = width, maxWidth = width, minHeight = 0)
                } else {
                    constraints.copy(minWidth = width, minHeight = 0)
                },
            )
        val height = line.height.coerceIn(constraints.minHeight, constraints.maxHeight)
        // Given more height than its line (a button's), centred text is centred vertically too,
        // the odd pixel below, as TextView's `center` gravity placed it.
        val top = if (exact) (height - line.height) / 2 else 0
        return layout(width, height) { placeable.place(0, top) }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int,
    ): Int = lineSize(Constraints.Infinity)?.width ?: measurable.maxIntrinsicWidth(height)

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int,
    ): Int = lineSize(width)?.height ?: measurable.minIntrinsicHeight(width)

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int,
    ): Int = lineSize(width)?.height ?: measurable.maxIntrinsicHeight(width)
}
