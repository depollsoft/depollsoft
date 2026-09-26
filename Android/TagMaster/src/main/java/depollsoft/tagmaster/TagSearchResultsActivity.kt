package depollsoft.tagmaster

import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import depollsoft.compose.revealItem
import depollsoft.lib.json.JsonSerializer
import depollsoft.tagmaster.ui.BarAction
import depollsoft.tagmaster.ui.ListDetailScaffold
import depollsoft.tagmaster.ui.ListDialogsHost
import depollsoft.tagmaster.ui.QueryList
import depollsoft.tagmaster.ui.ShowAs
import depollsoft.tagmaster.ui.TagMasterTopBar
import depollsoft.tagmaster.ui.Watermark
import depollsoft.tagmaster.ui.navigateUpOrHome
import depollsoft.tagmaster.ui.rememberListDialogs
import depollsoft.tagmaster.ui.setTagMasterContent
import kotlinx.coroutines.launch

/**
 * The results of a search: the query the search form built (the [QUERY_MODEL] extra, as JSON),
 * titled with the search text.
 */
class TagSearchResultsActivity : TagPaneActivity() {
    lateinit var model: QueryModel
        private set

    private val retained: RetainedQueries by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // A rotated screen keeps the results it loaded.
        val fresh = !retained.isRetained
        model =
            retained.models {
                listOf(intent?.getStringExtra(QUERY_MODEL)?.let(::readModel) ?: QueryModel())
            }.single()
        val configuration = resources.configuration
        tagPane =
            TagPaneState(
                this,
                hasTwoPanes(configuration.screenWidthDp, configuration.screenHeightDp),
                listedIds = { model.tags.map { it.id } },
                hasMoreResults = { model.hasMoreResults },
                fetchMore = { model.fetchResults() },
            )
        setTagMasterContent { ResultsScreen() }
        tagPane.restore(savedInstanceState)
        if (fresh) model.refresh()
    }

    private fun readModel(json: String): QueryModel? =
        try {
            JsonSerializer.deserialize(json) as? QueryModel
        } catch (_: Exception) {
            null
        }

    companion object {
        const val QUERY_MODEL = "QueryModel"
    }

    @Composable
    private fun ResultsScreen() {
        val dialogs = rememberListDialogs()
        val pane = tagPane
        val listState = rememberLazyListState()
        val scope = rememberCoroutineScope()
        pane.reveal = { id ->
            val index = model.tags.indexOfFirst { it.id == id }
            if (index >= 0) scope.launch { listState.revealItem(index) }
        }
        val bar =
            @Composable {
                TagMasterTopBar(
                    title = model.query ?: "",
                    onNavigateUp = { navigateUpOrHome() },
                    paneTitle = if (pane.hasDetailPane) stringResource(R.string.tag_pane_list_title) else null,
                    actions =
                        listOf(
                            BarAction("refresh", stringResource(R.string.Refresh), R.drawable.ic_refresh, ShowAs.IfRoom) { model.refresh() },
                        ),
                )
            }
        ListDetailScaffold(pane, dialogs, Watermark.Content, bar) {
            QueryList(model, listState, pane.selectedTagId, pane::showTag)
        }
        ListDialogsHost(dialogs)
    }
}
