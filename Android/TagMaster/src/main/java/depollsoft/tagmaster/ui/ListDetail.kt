package depollsoft.tagmaster.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import depollsoft.tagmaster.R
import depollsoft.tagmaster.TagPaneState
import depollsoft.tagmaster.ui.detail.TagDetailContent
import depollsoft.tagmaster.ui.detail.tagActions

/** Where a screen's barber-pole watermark is centered. */
enum class Watermark {
    /** Behind the whole window, toolbar included (Home and the saved lists). */
    Window,

    /** Behind the content below the toolbar (and above any bottom tabs). */
    Content,
}

/**
 * A list screen: on a phone, its toolbar over its list; on a wide window, the list in a 360dp
 * pane beside the detail pane showing the tag chosen from it, one watermark behind both.
 *
 * [content] is the list area (FAB included); [bottom] is anything under it, such as Browse's tabs.
 */
@Composable
fun ListDetailScaffold(
    pane: TagPaneState,
    dialogs: ListDialogs,
    watermark: Watermark,
    topBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    bottom: @Composable () -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    if (!pane.hasDetailPane) {
        Box(modifier.fillMaxSize()) {
            if (watermark == Watermark.Window) BarberPoleWatermark()
            Column(Modifier.fillMaxSize()) {
                topBar()
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    if (watermark == Watermark.Content) BarberPoleWatermark()
                    content()
                }
                bottom()
            }
        }
        return
    }
    Box(modifier.fillMaxSize()) {
        // One watermark behind both panes, below the shared bar, as the iPad split does.
        BarberPoleWatermark(Modifier.padding(top = 64.dp))
        Row(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .width(360.dp)
                    .fillMaxHeight()
                    .testTag("listPane"),
            ) {
                topBar()
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) { content() }
                bottom()
            }
            TagDetailPane(
                pane,
                dialogs,
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
    }
}

/**
 * The detail side of a wide window: its own charcoal bar (previous/next and the tag actions),
 * then the "Pick a tag" invitation or the chosen tag, a hairline down its starting edge.
 */
@Composable
fun TagDetailPane(
    pane: TagPaneState,
    dialogs: ListDialogs,
    modifier: Modifier = Modifier,
) {
    val colors = TagMasterTheme.colors
    val detail = pane.detail
    val stepping = pane.selectedTagId != null
    val disabledAlpha = 97 / 255f
    val canPrevious = pane.canStep(-1)
    val canNext = pane.canStep(1)
    val actions =
        buildList {
            if (stepping) {
                add(
                    BarAction(
                        "previousTag",
                        stringResource(R.string.previous_tag),
                        R.drawable.ic_chevron_up,
                        ShowAs.Always,
                        enabled = canPrevious,
                        iconAlpha = if (canPrevious) 1f else disabledAlpha,
                    ) { pane.step(-1) },
                )
                add(
                    BarAction(
                        "nextTag",
                        stringResource(R.string.next_tag),
                        R.drawable.ic_chevron_down,
                        ShowAs.Always,
                        enabled = canNext,
                        iconAlpha = if (canNext) 1f else disabledAlpha,
                    ) { pane.step(1) },
                )
            }
            if (detail != null) addAll(tagActions(detail, dialogs))
        }
    Column(modifier) {
        TagMasterTopBar(
            title = detail?.tag?.title ?: "",
            actions = actions,
            titleIsHeading = true,
            paneTitle = stringResource(R.string.tag_pane_detail_title),
            modifier = Modifier.testTag("detailPaneBar"),
        )
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .drawBehind { drawRect(colors.outlineVariant, Offset.Zero, Size(1.dp.roundToPx().toFloat(), size.height)) },
        ) {
            if (detail == null) {
                EmptyPane()
            } else {
                TagDetailContent(detail, dialogs, inPane = true)
            }
        }
    }
}

/** The detail pane before a tag is chosen: the quartet at rest and an invitation. */
@Composable
private fun EmptyPane() {
    val colors = TagMasterTheme.colors
    Box(Modifier.fillMaxSize(), contentAlignment = ViewAlign.Center) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(32.dp)
                .testTag("tagPaneEmpty"),
            horizontalAlignment = ViewAlign.CenterHorizontally,
        ) {
            QuartetIllustration(loading = false)
            Text(
                stringResource(R.string.tag_pane_empty_title),
                Modifier
                    .widthIn(max = 480.dp)
                    .padding(top = 12.dp)
                    .semantics { heading() },
                style = TagMasterType.titleLarge,
                color = colors.text,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(R.string.tag_pane_empty_body),
                Modifier
                    .widthIn(max = 480.dp)
                    .padding(top = 8.dp),
                style = TagMasterType.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
