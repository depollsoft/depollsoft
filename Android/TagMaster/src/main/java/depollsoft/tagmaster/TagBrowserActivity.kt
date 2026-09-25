package depollsoft.tagmaster

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import depollsoft.tagmaster.barbershop.TagCollection
import depollsoft.tagmaster.barbershop.TagSortOptions
import depollsoft.tagmaster.ui.BarAction
import depollsoft.tagmaster.ui.BottomTabs
import depollsoft.tagmaster.ui.ListDetailScaffold
import depollsoft.tagmaster.ui.ListDialogsHost
import depollsoft.tagmaster.ui.PagingTouchSlop
import depollsoft.tagmaster.ui.QueryList
import depollsoft.tagmaster.ui.SearchFab
import depollsoft.tagmaster.ui.ShowAs
import depollsoft.tagmaster.ui.TabItem
import depollsoft.tagmaster.ui.TagMasterTopBar
import depollsoft.tagmaster.ui.UsualTouchSlop
import depollsoft.tagmaster.ui.Watermark
import depollsoft.tagmaster.ui.navigateUpOrHome
import depollsoft.tagmaster.ui.rememberListDialogs
import depollsoft.tagmaster.ui.rememberPagerTabs
import depollsoft.tagmaster.ui.revealItem
import depollsoft.tagmaster.ui.setTagMasterContent
import kotlinx.coroutines.launch

/** Browse: the catalog four ways — latest, highest rated, most downloaded, and the classics. */
class TagBrowserActivity :
    TagPaneActivity() {
    private val retained: RetainedQueries by viewModels()

    /** One query per tab, in tab order; each fetches its first page when its tab is first shown. */
    val models: List<QueryModel> by lazy { retained.models(::browseQueries) }

    private fun browseQueries(): List<QueryModel> =
        listOf(
            QueryModel().apply {
                maxResults = Integer.MAX_VALUE
                sortBy = TagSortOptions.Posted
            },
            QueryModel().apply {
                maxResults = Integer.MAX_VALUE
                sortBy = TagSortOptions.Rating
            },
            QueryModel().apply {
                maxResults = Integer.MAX_VALUE
                sortBy = TagSortOptions.Downloaded
            },
            QueryModel().apply {
                sortBy = TagSortOptions.Classic
                collection = TagCollection.ClassicTags
                maxResults = 400
            },
        )

    /** The tab on screen. */
    var currentPage by mutableIntStateOf(0)
        internal set

    val currentModel: QueryModel
        get() = models[currentPage]

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentPage = savedInstanceState?.getInt(STATE_PAGE, 0) ?: 0
        val configuration = resources.configuration
        tagPane =
            TagPaneState(
                this,
                hasTwoPanes(configuration.screenWidthDp, configuration.screenHeightDp),
                listedIds = { currentModel.tags.map { it.id } },
                hasMoreResults = { currentModel.hasMoreResults },
                fetchMore = { currentModel.fetchResults() },
            )
        setTagMasterContent { BrowseScreen(this) }
        tagPane.restore(savedInstanceState)
    }

    /** Starts [page]'s query the first time it is shown. */
    internal fun startPage(page: Int) {
        if (!retained.started.add(page)) return
        models[page].refresh()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_PAGE, currentPage)
        super.onSaveInstanceState(outState)
    }

    private companion object {
        const val STATE_PAGE = "depollsoft.tagmaster.browse.page"
    }
}

private val browseTabs =
    listOf(
        R.string.latest to R.drawable.ic_latest,
        R.string.Rating to R.drawable.ic_rating,
        R.string.Downloads to R.drawable.ic_downloads,
        R.string.classic to R.drawable.ic_classic,
    )

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BrowseScreen(activity: TagBrowserActivity) {
    val dialogs = rememberListDialogs()
    val pane = activity.tagPane
    val pager = rememberPagerState(initialPage = activity.currentPage) { browseTabs.size }
    val scope = rememberCoroutineScope()
    val listStates = browseTabs.indices.map { rememberLazyListState() }
    val pagerTabs = rememberPagerTabs(pager)
    // The page Refresh acts on is the selected tab's, as onPageSelected set it.
    LaunchedEffect(pager) { snapshotFlow { pagerTabs.selected }.collect { activity.currentPage = it } }
    pane.reveal = { id ->
        val index = activity.currentModel.tags.indexOfFirst { it.id == id }
        if (index >= 0) scope.launch { listStates[activity.currentPage].revealItem(index) }
    }
    val bar =
        @Composable {
            TagMasterTopBar(
                title = stringResource(R.string.detail_brand_title),
                brandTitle = true,
                onNavigateUp = { activity.navigateUpOrHome() },
                paneTitle = if (pane.hasDetailPane) stringResource(R.string.tag_pane_list_title) else null,
                actions =
                    listOf(
                        BarAction("refresh", stringResource(R.string.Refresh), R.drawable.ic_refresh, ShowAs.IfRoom) {
                            activity.currentModel.refresh()
                        },
                    ),
            )
        }
    val tabs =
        @Composable {
            BottomTabs(
                tabs = browseTabs.mapIndexed { index, (label, icon) -> TabItem(stringResource(label), icon, "browseTab:$index") },
                selected = pagerTabs.selected,
                position = pager.currentPage + pager.currentPageOffsetFraction,
                onSelect = pagerTabs::select,
                modifier = Modifier.testTag("browseTabs"),
            )
        }
    ListDetailScaffold(pane, dialogs, Watermark.Content, bar, bottom = tabs) {
        PagingTouchSlop {
            HorizontalPager(pager, Modifier.fillMaxSize().testTag("browsePager"), key = { it }) { page ->
                UsualTouchSlop {
                    LaunchedEffect(page) { activity.startPage(page) }
                    QueryList(activity.models[page], listStates[page], pane.selectedTagId, pane::showTag)
                }
            }
        }
        SearchFab { activity.startActivity(Intent(activity, TagSearchActivity::class.java)) }
    }
    ListDialogsHost(dialogs)
}
