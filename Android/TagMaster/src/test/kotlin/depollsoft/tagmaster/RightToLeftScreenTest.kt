package depollsoft.tagmaster

import android.app.Application
import android.content.Intent
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** In a right-to-left language the custom-laid-out bars mirror, as the Toolbar and TabLayout did. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35], qualifiers = "ar-ldrtl-w411dp-h891dp-xxhdpi")
class RightToLeftScreenTest : ComposeScreenTest() {
    private fun bounds(tag: String) = node(tag).fetchSemanticsNode().boundsInRoot

    @Test
    fun theToolbarPutsUpAtTheRightAndTheTabsRunRightToLeft() {
        val tag = ScreenTestSupport.fixtureTag()
        ScreenTestSupport.cacheOnDisk(tag)
        val activity =
            launch(TagDetailActivity::class.java, Intent(app, TagDetailActivity::class.java).putExtra(TagDetailActivity.TAG_ID_EXTRA, tag.id))
        ScreenTestSupport.awaitTagLoaded(activity)
        idle()
        val width = activity.window.decorView.width
        assertTrue("Up sits at the right: ${bounds("navigateUp")}", bounds("navigateUp").left > width / 2f)
        assertTrue("the title starts right of centre", bounds("toolbarTitle").right > width / 2f)
        assertTrue("the first tab is the rightmost", bounds("detailTab:0").left > bounds("detailTab:3").left)
    }
}
