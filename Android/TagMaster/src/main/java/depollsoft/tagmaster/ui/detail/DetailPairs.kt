package depollsoft.tagmaster.ui.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import depollsoft.compose.viewPx
import depollsoft.tagmaster.ui.TagMasterTheme
import depollsoft.tagmaster.ui.TagMasterType
import depollsoft.tagmaster.ui.rememberTextViewPaint
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * What a detail page's caption/value rows need to lay out as the View pages did: whether this is
 * the Summary's compact fact list (28dp rows; 48dp for Rating) and each row's value width budget.
 */
class DetailPair(
    val caption: String,
    /** How wide the value wants to be, in pixels, before a stacked layout is considered. */
    val valueBudget: (Density) -> Int,
    val isRating: Boolean = false,
    /** Filled in by the value's text as it lays out; a wrapped value gets 4dp above and below. */
    val lines: LineCount = LineCount(),
    val value: @Composable (LineCount) -> Unit,
)

/** The number of lines a value's text laid out in, reported from its onTextLayout. */
class LineCount {
    var lines = 1
}

private data class PairRole(
    val caption: Boolean,
) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?) = this@PairRole
}

/**
 * Caption/value rows with one caption column for the whole group: every caption is as wide as
 * the widest, values start 8dp after it, and when a caption and a value would not fit side by side
 * every row stacks the value under its caption instead. Baselines line up across a row.
 */
@Composable
fun DetailPairs(
    pairs: List<DetailPair>,
    summary: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = TagMasterTheme.colors
    val captionStyle = TagMasterType.labelLarge
    val captionPaint = rememberTextViewPaint(captionStyle)
    // DetailMetadataLayout: the caption column is the widest caption's ceil(measureText).
    val captionWidth = pairs.maxOfOrNull { ceil(captionPaint.measureText(it.caption)).toInt() } ?: 0
    Layout(
        modifier = modifier.fillMaxWidth(),
        content = {
            for (pair in pairs) {
                Text(pair.caption, Modifier.then(PairRole(true)), style = captionStyle, color = colors.text)
                Box(Modifier.then(PairRole(false))) { pair.value(pair.lines) }
            }
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val captions = measurables.filterIndexed { index, _ -> index % 2 == 0 }
        val values = measurables.filterIndexed { index, _ -> index % 2 == 1 }
        val budget =
            pairs.maxOfOrNull { max(viewPx(96), it.valueBudget(this)) } ?: 0
        val stacked = captionWidth + viewPx(8) + budget > width
        val gap = viewPx(if (stacked) 4 else 8)
        val rows = mutableListOf<Row>()
        var y = 0
        pairs.forEachIndexed { index, pair ->
            val topMargin =
                viewPx(
                    when {
                        index > 0 && summary && pair.isRating -> 8
                        index > 0 && stacked -> 4
                        else -> 0
                    },
                )
            y += topMargin
            val row =
                measurePair(captions[index], values[index], pair.lines, width, if (stacked) -1 else captionWidth, gap, if (summary && !pair.isRating) 28 else 48)
            row.y = y
            rows += row
            y += row.height
        }
        layout(width, y) {
            for (row in rows) {
                row.caption.placeRelative(0, row.y + row.captionTop)
                row.value.placeRelative(row.valueLeft, row.y + row.valueTop)
            }
        }
    }
}

private class Row(
    val caption: Placeable,
    val value: Placeable,
    val captionTop: Int,
    val valueTop: Int,
    val valueLeft: Int,
    val height: Int,
) {
    var y = 0
}

/** DetailPairLayout's measure: baselines aligned, centered in a row at least [rowHeightDp] tall. */
private fun MeasureScope.measurePair(
    captionMeasurable: Measurable,
    valueMeasurable: Measurable,
    lines: LineCount,
    width: Int,
    captionWidth: Int,
    gap: Int,
    rowHeightDp: Int,
): Row {
    val caption = captionMeasurable.measure(Constraints.fixedWidth(if (captionWidth < 0) width else captionWidth))
    val valueLeft = if (captionWidth < 0) 0 else captionWidth + gap
    val value = valueMeasurable.measure(Constraints.fixedWidth(max(0, width - valueLeft)))
    var captionTop = 0
    var valueTop = if (captionWidth < 0) caption.height + gap else 0
    val captionBaseline = caption[FirstBaseline]
    val valueBaseline = value[FirstBaseline]
    if (captionWidth >= 0 && captionBaseline != AlignmentLine.Unspecified &&
        valueBaseline != AlignmentLine.Unspecified
    ) {
        captionTop = max(0, valueBaseline - captionBaseline)
        valueTop = max(0, captionBaseline - valueBaseline)
    }
    val natural = max(captionTop + caption.height, valueTop + value.height)
    val multiline = lines.lines > 1 || captionWidth < 0
    val inset = if (multiline) viewPx(4) else 0
    val height = max(viewPx(rowHeightDp), natural + inset * 2)
    val offset = (height - natural) / 2
    return Row(caption, value, captionTop + offset, valueTop + offset, valueLeft, height)
}

/** The width a text value asks for: its natural width, capped at eight of its line heights. */
fun textBudget(
    widthPx: Int,
    textSizePx: Float,
): Int = min(widthPx, (textSizePx * 8).toInt())
