package depollsoft.tagmaster

import android.app.Application
import android.content.Intent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** In a right-to-left language the custom-laid-out bars mirror, as the Toolbar and TabLayout did. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "ar-ldrtl-w411dp-h891dp-xxhdpi")
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

    @Test
    fun theRatingStarsCountFromTheRight() {
        val tag = ScreenTestSupport.fixtureTag()
        ScreenTestSupport.cacheOnDisk(tag)
        val activity =
            launch(TagDetailActivity::class.java, Intent(app, TagDetailActivity::class.java).putExtra(TagDetailActivity.TAG_ID_EXTRA, tag.id))
        ScreenTestSupport.awaitTagLoaded(activity)
        idle()
        click("rateButton")
        fun stars() = node("ratingPicker").fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current
        // The rightmost star is the first one.
        node("ratingPicker").performTouchInput { click(Offset(width - 4f, centerY)) }
        idle()
        assertEquals(1f, stars())
        // Left, toward the end, adds a star, as a mirrored SeekBar's D-pad does.
        node("ratingPicker").performSemanticsAction(SemanticsActions.RequestFocus)
        node("ratingPicker").performKeyInput { pressKey(Key.DirectionLeft) }
        idle()
        assertEquals(2f, stars())
    }

    @Test
    fun anOutlinedFieldsLabelNotchIsMirrored() {
        // A 300px field whose label notch opens 60px wide from 36px after the start.
        assertEquals(36f..96f, depollsoft.tagmaster.ui.outlineNotch(300f, 36f, 60f, androidx.compose.ui.unit.LayoutDirection.Ltr))
        assertEquals(204f..264f, depollsoft.tagmaster.ui.outlineNotch(300f, 36f, 60f, androidx.compose.ui.unit.LayoutDirection.Rtl))
    }
}
