package depollsoft.tagmaster.ui.detail

import depollsoft.compose.listViewScrollbar
import depollsoft.compose.scrollViewScrollbar
import depollsoft.compose.ViewAlign
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import depollsoft.tagmaster.R
import depollsoft.tagmaster.barbershop.Tag
import depollsoft.tagmaster.barbershop.Video
import depollsoft.tagmaster.ui.RemoteImage
import depollsoft.tagmaster.ui.StatusIndicator
import depollsoft.tagmaster.ui.TagMasterTheme
import depollsoft.tagmaster.ui.TagMasterType
import depollsoft.tagmaster.ui.TagMasterType.withoutLineHeight
import depollsoft.tagmaster.ui.formatDate
import depollsoft.tagmaster.ui.isPresent
import depollsoft.tagmaster.ui.rememberTextViewPaint
import kotlin.math.ceil

/**
 * A detail page's scrolling column: 16dp above and below, 16dp margins, and on wide windows a
 * centered reading measure of at most [maxWidth] dp.
 */
@Composable
fun DetailScroll(
    modifier: Modifier = Modifier,
    maxWidth: Int = 640,
    top: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val extra =
            with(density) {
                val room = constraints.maxWidth - maxWidth.dp.roundToPx()
                if (room > 0) (room / 2).toDp() else 0.dp
            }
        val scroll = rememberScrollState()
        val topPadding = if (top) 16.dp else 0.dp
        Column(
            Modifier
                .fillMaxSize()
                .scrollViewScrollbar(scroll, top = topPadding, bottom = 16.dp)
                .verticalScroll(scroll)
                .padding(top = topPadding, bottom = 16.dp, start = extra + 16.dp, end = extra + 16.dp),
        ) {
            content()
        }
    }
}

/** A text value of a caption/value row, reporting how many lines it took. */
@Composable
private fun PairValue(
    text: String,
    lines: LineCount,
) {
    Text(
        text,
        Modifier.fillMaxWidth(),
        style = TagMasterType.bodyMedium,
        color = TagMasterTheme.colors.text,
        onTextLayout = { lines.lines = it.lineCount },
    )
}

/** A link value: underlined when it goes somewhere, in a 48dp row. */
@Composable
private fun PairLink(
    text: String,
    uri: String?,
    lines: LineCount,
    fill: Boolean,
) {
    val context = LocalContext.current
    Box(
        Modifier
            .then(if (fill) Modifier.fillMaxWidth() else Modifier)
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button) {
                if (uri != null) context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
            },
        contentAlignment = ViewAlign.CenterStart,
    ) {
        Text(
            text,
            style = TagMasterType.bodyMedium.withoutLineHeight().copy(textDecoration = if (uri != null) TextDecoration.Underline else null),
            color = TagMasterTheme.colors.text,
            onTextLayout = { lines.lines = it.lineCount },
        )
    }
}

@Composable
private fun rememberBudget(style: TextStyle): (String) -> (Density) -> Int {
    val paint = rememberTextViewPaint(style)
    return { text -> { _ -> textBudget(ceil(paint.measureText(text)).toInt(), paint.textSize) } }
}

/** The Details page: where the tag comes from and who arranged and sang it. */
@Composable
fun DetailsPage(
    tag: Tag,
    modifier: Modifier = Modifier,
) {
    val colors = TagMasterTheme.colors
    val budget = rememberBudget(TagMasterType.bodyMedium)
    val pairs = mutableListOf<DetailPair>()

    fun text(
        caption: String,
        value: String,
    ) = DetailPair(caption, budget(value)) { PairValue(value, it) }

    fun link(
        caption: String,
        value: String,
        uri: String?,
        fill: Boolean = true,
    ) = DetailPair(caption, budget(value)) { PairLink(value, uri, it, fill) }

    pairs += text(stringResource(R.string.LastRefreshed), String.format("%1\$tD %1\$tr", tag.lastRefreshed))
    pairs += text(stringResource(R.string.Downloads), "${tag.downloadCount}")
    pairs += link(stringResource(R.string.Link), stringResource(R.string.BarbershopTagsCom), tag.tagUri, fill = false)
    if (tag.provider.isPresent()) pairs += link(stringResource(R.string.PostedBy), tag.provider!!, tag.providerWebsite)
    if (tag.posted.isPresent()) pairs += text(stringResource(R.string.Posted), formatDate("%1\$tA, %1\$tB %1\$te, %1\$tY", tag.posted))
    if (tag.arranger.isPresent()) pairs += link(stringResource(R.string.ArrangedBy), tag.arranger!!, tag.arrangerWebsite)
    if (tag.yearArranged.isPresent()) pairs += text(stringResource(R.string.YearArranged), tag.yearArranged!!)
    if (tag.sungBy.isPresent()) pairs += link(stringResource(R.string.SungBy), tag.sungBy!!, tag.sungByWebsite)
    if (tag.sungYear.isPresent()) pairs += text(stringResource(R.string.YearSung), tag.sungYear!!)

    DetailScroll(modifier) {
        Text(
            "${tag.title}",
            Modifier.padding(bottom = 16.dp),
            style = TagMasterType.headlineSmall,
            color = colors.text,
        )
        DetailPairs(pairs, summary = false)
    }
}

/** The Videos page: the teaching video, then everyone else's performances. */
@Composable
fun VideosPage(
    tag: Tag,
    modifier: Modifier = Modifier,
) {
    val colors = TagMasterTheme.colors
    val videos = tag.videos.orEmpty()
    // The teaching video and the heading stay put; only the performances scroll, as the
    // ListView below them did.
    Column(modifier.fillMaxSize()) {
        val teaching = tag.teachingVideo
        if (teaching.isPresent()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp),
            ) {
                Text(
                    stringResource(R.string.TeachingVideo),
                    Modifier.semantics { heading() },
                    style = TagMasterType.titleLarge,
                    color = colors.text,
                )
                VideoRow(
                    thumbnail = String.format("https://img.youtube.com/vi/%s/2.jpg", teaching),
                    watch = String.format("https://www.youtube.com/watch?v=%s", teaching),
                    rows = listOf(stringResource(R.string.Teacher) to (tag.teacher ?: "null")),
                    padding = 0.dp,
                    modifier = Modifier.testTag("teachingVideo"),
                )
            }
        }
        Text(
            stringResource(R.string.UserSubmissions),
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                .semantics { heading() },
            style = TagMasterType.titleLarge,
            color = colors.text,
        )
        if (videos.isEmpty()) {
            Text(
                stringResource(R.string.NoVideos),
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                style = TagMasterType.bodyLarge,
                color = colors.text,
            )
        }
        val listState = rememberLazyListState()
        LazyColumn(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .listViewScrollbar(listState, top = 16.dp, bottom = 16.dp, divider = 1.dp)
                .testTag("videoList"),
            state = listState,
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            // A LazyColumn refuses a repeated key, and the catalog may list a video twice.
            itemsIndexed(videos, key = { index, video -> "${video.id}:$index" }) { index, video ->
                Column {
                    // ListView's divider: 1dp between rows, taking its own space.
                    if (index > 0) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(colors.outlineVariant),
                        )
                    }
                    UserVideoRow(video)
                }
            }
        }
    }
}

@Composable
private fun UserVideoRow(video: Video) {
    val rows =
        buildList {
            if (video.sungBy.isPresent()) add(stringResource(R.string.SungBy) to video.sungBy!!)
            if (video.sungKey.isPresent()) add(stringResource(R.string.Key) to video.sungKey!!)
            if (video.posted.isPresent()) add(stringResource(R.string.Posted) to formatDate("%1\$tA, %1\$tB %1\$te, %1\$tY", video.posted))
        }
    VideoRow(
        thumbnail = String.format("https://img.youtube.com/vi/%s/2.jpg", video.youTubeCode),
        watch = String.format("https://www.youtube.com/watch?v=%s", video.youTubeCode),
        rows = rows,
        padding = 16.dp,
        multitrack = video.isMultitrack,
        modifier = Modifier.testTag("video:${video.id}"),
    )
}

/**
 * A video: its thumbnail and a small table of facts, opening the video on YouTube when tapped.
 */
@Composable
private fun VideoRow(
    thumbnail: String,
    watch: String,
    rows: List<Pair<String, String>>,
    padding: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    multitrack: Boolean? = null,
) {
    val context = LocalContext.current
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(watch))) }
            .padding(horizontal = padding),
        verticalAlignment = ViewAlign.CenterVertically,
    ) {
        RemoteImage(
            thumbnail,
            Modifier
                .padding(top = 4.dp, bottom = 4.dp, end = 16.dp)
                .size(80.dp, 60.dp),
        )
        Column(Modifier.weight(1f)) {
            FactTable(rows)
            if (multitrack != null) {
                Box(Modifier.fillMaxWidth(), contentAlignment = ViewAlign.Center) {
                    StatusIndicator(
                        stringResource(R.string.Multitrack),
                        multitrack,
                        minHeight = 0.dp,
                        labelAlignment = androidx.compose.ui.Alignment.Top,
                    )
                }
            }
        }
    }
}

/** A two-column table: labels as wide as the widest (plus 5dp), values filling the rest. */
@Composable
private fun FactTable(rows: List<Pair<String, String>>) {
    val colors = TagMasterTheme.colors
    Layout(
        modifier = Modifier.fillMaxWidth(),
        content = {
            for ((label, value) in rows) {
                Text(label, Modifier.padding(end = 5.dp), style = TagMasterType.labelLarge, color = colors.text)
                Text(value, style = TagMasterType.bodyMedium, color = colors.text)
            }
        },
    ) { measurables, constraints ->
        val labels = measurables.filterIndexed { index, _ -> index % 2 == 0 }
        val values = measurables.filterIndexed { index, _ -> index % 2 == 1 }
        val labelWidth = labels.maxOfOrNull { it.maxIntrinsicWidth(Constraints.Infinity) } ?: 0
        val valueWidth = (constraints.maxWidth - labelWidth).coerceAtLeast(0)
        val placed =
            labels.indices.map { index ->
                labels[index].measure(Constraints.fixedWidth(labelWidth)) to values[index].measure(Constraints.fixedWidth(valueWidth))
            }
        val height = placed.sumOf { (label, value) -> maxOf(label.height, value.height) }
        layout(constraints.maxWidth, height) {
            var y = 0
            for ((label, value) in placed) {
                label.placeRelative(0, y)
                value.placeRelative(labelWidth, y)
                y += maxOf(label.height, value.height)
            }
        }
    }
}
