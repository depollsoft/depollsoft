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
