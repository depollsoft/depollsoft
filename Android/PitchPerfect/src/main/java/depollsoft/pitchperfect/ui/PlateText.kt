package depollsoft.pitchperfect.ui

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import kotlin.math.ceil

/**
 * Text sized the way a wrap-content TextView sizes itself.
 *
 * Compose pads a letter-spaced paragraph's intrinsic width by half a pixel so rounding can never
 * wrap it, which makes a one-line label a pixel wider than its TextView was — and anything centred
 * on it or aligned to its end a pixel off. Text that fits on one line here is laid out at exactly
 * the width of its line, rounded up, as the TextView was; [align] then places the line within that
 * width as the TextView's gravity did.
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
        modifier.then(
            Modifier.layout { measurable, constraints ->
                if (constraints.hasFixedWidth) {
                    val placeable = measurable.measure(constraints)
                    return@layout layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                }
                val probe =
                    measurer.measure(
                        text,
                        // Start-aligned: a centred line's bounds depend on the width it is centred in.
                        style = styled.copy(textAlign = TextAlign.Start),
                        overflow = overflow,
                        softWrap = softWrap,
                        maxLines = maxLines,
                        constraints = Constraints(maxWidth = constraints.maxWidth),
                    )
                val placeable =
                    if (probe.lineCount == 1) {
                        val width =
                            ceil(probe.getLineRight(0) - probe.getLineLeft(0)).toInt()
                                .coerceIn(constraints.minWidth, constraints.maxWidth)
                        measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
                    } else {
                        measurable.measure(constraints)
                    }
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }.drawWithContent {
                // TextView clips glyphs that overhang its left edge and its top and bottom, but
                // not its right edge; a glyph's negative bearing is cut exactly as it was.
                clipRect(left = 0f, top = 0f, right = Float.MAX_VALUE, bottom = size.height) {
                    this@drawWithContent.drawContent()
                }
            },
        ),
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
