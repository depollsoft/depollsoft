package depollsoft.tagmaster

import android.app.Application
import android.content.Intent
import androidx.compose.ui.test.performScrollToNode
import depollsoft.lib.json.JsonSerializer
import depollsoft.tagmaster.barbershop.TagSortOptions
import depollsoft.tagmaster.screenshots.ScreenshotFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.io.InputStream
import java.net.URL

/**
 * Browse and search results against canned catalog pages: each browse mode asks for its own sort,
 * empty and failed queries explain themselves, reaching the end asks for the next page, and
 * Refresh asks the catalog again.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class BrowseAndResultsScreenTest : ComposeScreenTest() {
    private val requested = mutableListOf<String>()
    private var respond: (URL) -> InputStream = { ScreenshotFixtures.catalogPage(it) }

    private fun <T> catalog(block: () -> T): T =
        ScreenTestSupport.withTransport({ url ->
            synchronized(requested) { requested += url.toString() }
            respond(url)
        }, block)

    private fun settle(model: QueryModel) = ScreenTestSupport.await("the query to settle") { !model.isLoading }.also { idle() }

    private fun results(query: String): TagSearchResultsActivity {
        val model =
            QueryModel().apply {
                this.query = query
                resultSetSize = 20
                maxResults = 50
            }
        val intent =
            Intent(app, TagSearchResultsActivity::class.java)
                .putExtra(TagSearchResultsActivity.QUERY_MODEL, JsonSerializer.serialize(model).toString())
        return launch(TagSearchResultsActivity::class.java, intent)
    }

    @Test
    fun everyBrowseModeSortsItsOwnPageAndRendersItsRows() =
        catalog {
            val activity = launch(TagBrowserActivity::class.java)
            val sorts = listOf(TagSortOptions.Posted, TagSortOptions.Rating, TagSortOptions.Downloaded, TagSortOptions.Classic)
            for ((page, sort) in sorts.withIndex()) {
                click("browseTab:$page")
                val model = activity.models[page]
                settle(model)
                assertEquals(page, activity.currentPage)
                assertEquals(sort, model.sortBy)
                assertTrue("page $page rows", model.tags.isNotEmpty())
                assertTrue(exists("queryTag:${model.tags.first().id}"))
            }
            assertEquals("each tab queries once, when first shown", 4, requested.size)
        }

    @Test
    fun anEmptyQueryExplainsItself() {
        respond = { ScreenshotFixtures.catalogPage(it, available = 0) }
        catalog {
            val activity = results("zzz")
            settle(activity.model)
            assertEquals("No tags could be found that matched your query.", text("queryStatus"))
        }
    }

    @Test
    fun aFailedQueryExplainsItselfAndRefreshRecovers() {
        respond = { throw IOException("The connection was interrupted.") }
        catalog {
            val activity = results("heart")
            settle(activity.model)
            assertTrue(text("queryStatus").contains("The connection was interrupted."))
            respond = { ScreenshotFixtures.catalogPage(it) }
            activity.model.refresh()
            settle(activity.model)
            assertFalse(exists("queryStatus"))
            assertTrue(activity.model.tags.isNotEmpty())
        }
    }

    @Test
    fun reachingTheEndOfTheListRequestsAndRendersTheNextPage() =
        catalog {
            val activity = results("heart")
            settle(activity.model)
            assertEquals(20, activity.model.tags.size)
            val scrollTo = androidx.compose.ui.test.hasTestTag("queryTag:${activity.model.tags.last().id}")
            node("queryResults").performScrollToNode(scrollTo)
            idle()
            settle(activity.model)
            assertEquals(26, activity.model.tags.size)
            assertFalse(activity.model.hasMoreResults)
            assertTrue(requested.any { it.contains("start=21") })
        }

    @Test
    fun theRefreshActionAsksTheCatalogAgain() =
        catalog {
            val activity = results("heart")
            settle(activity.model)
            val before = requested.size
            click("refresh")
            settle(activity.model)
            assertEquals(before + 1, requested.size)
            assertEquals(20, activity.model.tags.size)
        }

    @Test
    fun aResultRowOpensItsTag() =
        catalog {
            val activity = results("heart")
            settle(activity.model)
            val id = activity.model.tags.first().id
            click("queryTag:$id")
            val started = nextStarted(activity)
            assertEquals(TagDetailActivity::class.java.name, started?.component?.className)
            assertEquals(id, started!!.getIntExtra(TagDetailActivity.TAG_ID_EXTRA, -1))
        }
}
