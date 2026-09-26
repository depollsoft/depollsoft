package depollsoft.tagmaster.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import depollsoft.compose.PlatformIcon
import depollsoft.compose.ViewAlign
import depollsoft.tagmaster.R
import depollsoft.tagmaster.barbershop.Tag
import java.util.Date

/** The visibility rule the View bindings used: absent, `false` and integer zero hide a field. */
fun Any?.isPresent(): Boolean =
    when (this) {
        null -> false
        is Boolean -> this
        is Int -> this != 0
        else -> true
    }

/** A date as the lists and details print it, or blank for a missing date. */
fun formatDate(
    format: String,
    date: Date?,
): String = if (date == null) "" else String.format(format, date)

/**
 * Read-only availability of a tag's sheet music or learning tracks: a check or a cross and a
 * label, spoken as "Sheet music available" / "No sheet music". Not a control.
 *
 * It was a TextView with the mark as a compound drawable: the mark is always centred on the
 * row's height, and the label follows the TextView's gravity. Tag rows centred it in a 36dp row;
 * a video row left the default, so its label sits at the top of the mark.
 */
@Composable
fun StatusIndicator(
    label: String,
    available: Boolean,
    modifier: Modifier = Modifier,
    textColor: Color = TagMasterTheme.colors.onSurface,
    minHeight: Dp = 36.dp,
    labelAlignment: Alignment.Vertical = ViewAlign.CenterVertically,
) {
    val colors = TagMasterTheme.colors
    val description =
        stringResource(if (available) R.string.status_available else R.string.status_unavailable, label)
    Row(
        modifier
            .heightIn(min = minHeight)
            .clearAndSetSemantics { contentDescription = description },
        verticalAlignment = labelAlignment,
    ) {
        PlatformIcon(
            if (available) R.drawable.ic_check else R.drawable.ic_clear,
            tint = if (available) colors.statusAvailable else colors.onSurfaceVariant,
            size = 32.dp,
            modifier = Modifier.align(ViewAlign.CenterVertically),
        )
        val style = with(TagMasterType) { bodyMedium.withoutLineHeight() }
        // The View set its drawable padding as (int) (4 * density), truncating where dp rounds.
        val drawablePadding = with(LocalDensity.current) { (4 * density).toInt().toDp() }
        Text(
            label,
            modifier =
                Modifier
                    .padding(start = drawablePadding)
                    .widthPx(rememberTextViewWidth(label, style)),
            style = style,
            color = textColor,
        )
    }
}

/**
 * One tag in a list: its title, alternate title, id, rating, date, downloads and whether it has
 * sheet music and learning tracks — the row every list in the app shows.
 *
 * [wrapWhenNarrow] lets each metadata line stack its label/value groups when the row has lost
 * width to the editing controls, instead of clipping them.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagRowContent(
    tag: Tag?,
    modifier: Modifier = Modifier,
    wrapWhenNarrow: Boolean = false,
) {
    val colors = TagMasterTheme.colors
    val body = TagMasterType.bodyMedium
    Column(
        modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(tag?.title ?: "", modifier = Modifier.fillMaxWidth(), style = TagMasterType.titleMedium, color = colors.text)
        val alternative = tag?.alternativeTitle
        if (alternative.isPresent()) {
            Text(
                String.format(stringResource(R.string.alternative_title_format), alternative),
                modifier = Modifier.fillMaxWidth(),
                style = body,
                color = colors.text,
            )
        }
        MetadataLine(wrapWhenNarrow) {
            LabelledValue(stringResource(R.string.IdColon), "" + tag?.id, Modifier.padding(end = 16.dp))
            if (tag?.rating.isPresent()) {
                LabelledValue(stringResource(R.string.RatingColon), String.format("%3.2f", tag?.rating))
            }
        }
        MetadataLine(wrapWhenNarrow) {
            if (tag?.posted.isPresent()) {
                LabelledValue(stringResource(R.string.PostedColon), formatDate(" %tD", tag?.posted), Modifier.padding(end = 16.dp))
            }
            if (tag?.downloadCount.isPresent()) {
                LabelledValue(stringResource(R.string.DLs), String.format("%d", tag?.downloadCount))
            }
        }
        FlowRow(
            Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            StatusIndicator(stringResource(R.string.SheetMusicCheck), tag?.sheetMusicUri.isPresent())
            StatusIndicator(stringResource(R.string.LearningTracks), !tag?.tracks.isNullOrEmpty())
        }
    }
}

@Composable
private fun LabelledValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val colors = TagMasterTheme.colors
    val style = TagMasterType.bodyMedium
    Row(modifier) {
        Text(label, Modifier.widthPx(rememberTextViewWidth(label, style)), style = style, color = colors.text, maxLines = 1)
        Text(value, Modifier.widthPx(rememberTextViewWidth(value, style)), style = style, color = colors.text, maxLines = 1)
    }
}

/**
 * A row of label/value groups. Normally they sit side by side; with [wrap] set, a line whose groups
 * do not fit stacks them one above the other rather than clipping.
 */
@Composable
private fun MetadataLine(
    wrap: Boolean,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = Modifier.fillMaxWidth()) { measurables, constraints ->
        val loose = Constraints(maxWidth = constraints.maxWidth)
        val natural = measurables.sumOf { it.maxIntrinsicWidth(Constraints.Infinity) }
        val stack = wrap && natural > constraints.maxWidth
        val placeables = measurables.map { it.measure(loose) }
        val width = constraints.maxWidth
        val height = if (stack) placeables.sumOf { it.height } else placeables.maxOfOrNull { it.height } ?: 0
        layout(width, height) {
            var x = 0
            var y = 0
            for (placeable in placeables) {
                placeable.placeRelative(x, y)
                if (stack) y += placeable.height else x += placeable.width
            }
        }
    }
}
