package depollsoft.tagmaster.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import depollsoft.tagmaster.QueryModel
import depollsoft.tagmaster.R
import kotlin.math.abs

/**
 * The results of one catalog query: a status line when there is something to say, the tag rows,
 * the barber-pole loader while a page is on its way, and the next page fetched as the last rows
 * come into view.
 */
@Composable
fun QueryList(
    model: QueryModel,
    listState: LazyListState,
    selectedTagId: Int?,
    onOpen: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TagMasterTheme.colors
    val showing = stringResource(R.string.tag_row_showing)
    val nearEnd by remember(listState) {
        derivedStateOf {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            val first = listState.firstVisibleItemIndex
            val visible = info.visibleItemsInfo.size
            total > 0 && abs(total - (first + visible)) < 2
        }
    }
    LaunchedEffect(listState, model) {
        // The next page is fetched when the end comes into view. ListView asked again on every
        // scroll event, so after a failed page a nudge near the end retries; the same position
        // and rows never ask twice, so a failure does not retry in a loop.
        var asked: Any? = null
        snapshotFlow {
            if (nearEnd && !model.isLoading) {
                Triple(model.tags.size, listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
            } else {
                null
            }
        }.collect { position ->
            if (position != null && position != asked) {
                asked = position
                model.fetchResults()
            }
        }
    }
    Column(modifier.fillMaxSize()) {
        val status = model.statusText
        if (status.isPresent()) {
            Text(
                status!!,
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("queryStatus"),
                style = TagMasterType.bodyMedium,
                color = colors.text,
            )
        }
        val tags = model.tags.toList()
        ReadingWidth(Modifier.weight(1f).fillMaxWidth()) { inset ->
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .listViewScrollbar(listState, top = 16.dp, bottom = 88.dp, divider = 1.dp)
                    .testTag("queryResults"),
                state = listState,
                contentPadding = PaddingValues(start = inset, top = 16.dp, end = inset, bottom = 88.dp),
            ) {
                itemsIndexed(tags, key = { index, tag -> "${tag.id}:$index" }) { index, tag ->
                    Column(Modifier.listItemMotion(this)) {
                        if (index > 0) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(colors.outlineVariant),
                            )
                        }
                        val selected = selectedTagId == tag.id
                        val highlight by animateColorAsState(
                            if (selected) colors.secondaryContainer else colors.secondaryContainer.copy(alpha = 0f),
                            tween(ListMotion.CHANGE_MILLIS),
                            label = "selected",
                        )
                        TagRowContent(
                            tag,
                            Modifier
                                .drawBehind { if (highlight.alpha > 0f) drawRect(highlight) }
                                .clickable { onOpen(tag.id) }
                                .semantics { if (selected) stateDescription = showing }
                                .testTag("queryTag:${tag.id}"),
                        )
                    }
                }
            }
        }
        if (model.isLoading) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = ViewAlign.Center,
            ) {
                BarberPoleLoader(true, stringResource(R.string.query_loading), Modifier.testTag("queryLoading"))
            }
        }
    }
}
