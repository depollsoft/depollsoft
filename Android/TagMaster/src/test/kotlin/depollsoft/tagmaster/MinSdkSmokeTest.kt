package depollsoft.tagmaster

import android.app.Application
import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The main screens on the oldest Android the app supports (minSdk 24). Every other test runs on
 * SDK 35, where a call newer than minSdk can't fail; here a missing method throws.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24], application = Application::class, qualifiers = "w411dp-h891dp-xxhdpi")
class MinSdkSmokeTest : ComposeScreenTest() {
    @Test
    fun theHomeScreenOpens() {
        launch(MeActivity::class.java)
        assertTrue("the home screen is composed", compose.onAllNodes(androidx.compose.ui.test.isRoot()).fetchSemanticsNodes().isNotEmpty())
    }

    private fun composed() = compose.onAllNodes(androidx.compose.ui.test.isRoot()).fetchSemanticsNodes().isNotEmpty()

    private fun <A : android.app.Activity> openAndClose(
        clazz: Class<A>,
        intent: Intent? = null,
    ) {
        launch(clazz, intent)
        assertTrue("${clazz.simpleName} is composed", composed())
        controller!!.pause().stop().destroy()
        controller = null
    }

    @Test
    fun everyScreenOpens() =
        ScreenTestSupport.withTransport({ depollsoft.tagmaster.screenshots.ScreenshotFixtures.catalogPage(it) }) {
            val tag = ScreenTestSupport.fixtureTag()
            ScreenTestSupport.cacheOnDisk(tag)
            val key = TagLists.create("Afterglow set")
            ListModel(key).add(tag.id)
            TeachableTagsModel.addTeachableTag(tag.id)
            val results =
                depollsoft.lib.json.JsonSerializer
                    .serialize(QueryModel().apply { query = "heart" })
                    .toString()
            openAndClose(TagSearchActivity::class.java)
            openAndClose(TagBrowserActivity::class.java)
            openAndClose(TagSearchResultsActivity::class.java, Intent(app, TagSearchResultsActivity::class.java).putExtra(TagSearchResultsActivity.QUERY_MODEL, results))
            openAndClose(SettingsActivity::class.java)
            openAndClose(TeachableTagsActivity::class.java)
            openAndClose(TagListActivity::class.java, TagListActivity.intent(app, key))
            openAndClose(
                SheetMusicActivity::class.java,
                Intent(Intent.ACTION_VIEW)
                    .setClass(app, SheetMusicActivity::class.java)
                    .setDataAndType(android.net.Uri.fromFile(depollsoft.tagmaster.screenshots.ScreenshotFixtures.sheetImage()), "image/png")
                    .putExtra("tagId", tag.id),
            )
        }

    @Test
    fun everyDetailPageOpens() {
        val tag = ScreenTestSupport.fixtureTag()
        ScreenTestSupport.cacheOnDisk(tag)
        val activity =
            launch(TagDetailActivity::class.java, Intent(app, TagDetailActivity::class.java).putExtra(TagDetailActivity.TAG_ID_EXTRA, tag.id))
        ScreenTestSupport.awaitTagLoaded(activity)
        idle()
        for (page in 0 until 4) {
            click("detailTab:$page")
            assertTrue("detail page $page shows", composed())
        }
    }

    @Test
    fun switchingTagsRevealsTheNextOne() {
        // Moving to another tag fades it in, and checks whether the system allows animators.
        val first = ScreenTestSupport.fixtureTag()
        val second = ScreenTestSupport.fixtureTag().apply { id = first.id + 1 }
        ScreenTestSupport.cacheOnDisk(first)
        ScreenTestSupport.cacheOnDisk(second)
        val activity =
            launch(TagDetailActivity::class.java, Intent(app, TagDetailActivity::class.java).putExtra(TagDetailActivity.TAG_ID_EXTRA, first.id))
        ScreenTestSupport.awaitTagLoaded(activity)
        activity.detail.showTag(second.id)
        ScreenTestSupport.awaitTagLoaded(activity)
        idle()
        assertEquals(second.id, activity.tag?.id)
    }
}
