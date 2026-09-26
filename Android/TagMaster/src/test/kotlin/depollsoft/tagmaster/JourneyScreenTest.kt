package depollsoft.tagmaster

import android.app.Application
import android.content.Intent
import androidx.compose.ui.test.performTextInput
import depollsoft.tagmaster.screenshots.ScreenshotFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A user's path through several screens, each started from the intent the screen before it sent:
 * search for a tag, open two results, add both to a list, reorder the list, and turn the phone.
 * Each screen has its own tests; this one catches a break in the hand-offs between them.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w411dp-h891dp-xxhdpi")
class JourneyScreenTest : ComposeScreenTest() {
    private val first = 2147483200
    private val second = 2147483201

    /** Starts the screen [from] asked for, as the system would. */
    private fun <A : android.app.Activity> follow(
        from: android.app.Activity,
        clazz: Class<A>,
    ): A {
        val started: Intent = nextStarted(from) ?: error("${from.javaClass.simpleName} started nothing")
        assertEquals(clazz.name, started.component?.className)
        return launch(clazz, started)
    }

    private fun addToList(
        results: TagSearchResultsActivity,
        id: Int,
        key: String,
    ) {
        click("queryTag:$id")
        val detail = follow(results, TagDetailActivity::class.java)
        ScreenTestSupport.awaitTagLoaded(detail)
        idle()
        assertEquals(id, detail.tag?.id)
        click("chip:add")
        click("pickerRow:$key")
        click("listPickerDone")
        assertTrue("tag $id is in the list", ListModel(key).contains(id))
        // Back to the results, as the back button would.
        controller!!.pause().stop().destroy()
        idle()
    }

    @Test
    fun searchOpenAddToAListReorderItAndRotate() =
        ScreenTestSupport.withTransport({ ScreenshotFixtures.catalogPage(it) }) {
            val key = TagLists.create("Afterglow set")
            for (id in listOf(first, second)) {
                ScreenTestSupport.cacheOnDisk(ScreenTestSupport.fixtureTag().apply { this.id = id; title = "Tag $id" })
            }

            val search = launch(TagSearchActivity::class.java)
            val searchController = controller!!
            node("searchTextBox").performTextInput("heart")
            click("searchButton")

            val results = follow(search, TagSearchResultsActivity::class.java)
            ScreenTestSupport.await("the results to load") { !results.model.isLoading && results.model.tags.isNotEmpty() }
            idle()
            assertEquals("the query reached the results screen", "heart", results.model.query)
            val resultsController = controller

            addToList(results, first, key)
            controller = resultsController
            addToList(results, second, key)
            controller = resultsController
            assertEquals(listOf(first, second), ListModel(key).ids.toList())

            // Leaving search: the results and the search form close behind.
            resultsController!!.pause().stop().destroy()
            searchController.pause().stop().destroy()
            launch(TagListActivity::class.java, TagListActivity.intent(app, key))
            assertTrue(exists("savedTag:$first") && exists("savedTag:$second"))
            click("editSavedList")
            customAction("savedTag:$second", string(R.string.MoveUp))
            assertEquals("the move is stored", listOf(second, first), ListModel(key).ids.toList())

            rotate<TagListActivity>()
            assertEquals("still in the list's order after turning", listOf(second, first), ListModel(key).ids.toList())
            assertTrue("and still editing", exists("drag:$second"))
        }
}
