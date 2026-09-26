package depollsoft.tagmaster

import android.app.Application
import android.content.Intent
import android.view.ContextThemeWrapper
import android.view.KeyEvent
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.performSemanticsAction
import depollsoft.tagmaster.ui.asRipple
import depollsoft.tagmaster.ui.tagMasterColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The toolbar and tab strip behave as AppCompat's and TabLayout's did: icons name themselves in a
 * tooltip on a long press, the Menu key opens the overflow, a tapped tab is selected at once with
 * no tab in between lit on the way, and presses ripple in a color that shows on the charcoal chrome.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w411dp-h891dp-xxhdpi")
class ChromeInteractionScreenTest : ComposeScreenTest() {
    private fun selected(tag: String) = node(tag).fetchSemanticsNode().config.getOrNull(SemanticsProperties.Selected) == true

    @Test
    fun aToolbarIconNamesItselfOnALongPressAndTheNameGoesAway() {
        launch(MeActivity::class.java)
        assertTrue("the action is laid out", node("settings").fetchSemanticsNode().size.width > 0)
        longClick("settings")
        assertTrue(exists("tooltip"))
        assertEquals(string(R.string.Settings), text("tooltip"))
        compose.mainClock.advanceTimeBy(2600)
        idle()
        assertFalse(exists("tooltip"))
    }

    @Test
    fun theUpAndOverflowButtonsNameThemselvesToo() {
        val key = TagLists.create("Afterglow set")
        launch(TagListActivity::class.java, TagListActivity.intent(app, key))
        assertTrue("Up is laid out", node("navigateUp").fetchSemanticsNode().size.width > 0)
        assertTrue("the overflow is laid out", node("overflowMenu").fetchSemanticsNode().size.width > 0)
        longClick("navigateUp")
        assertEquals(app.getString(androidx.appcompat.R.string.abc_action_bar_up_description), text("tooltip"))
        compose.mainClock.advanceTimeBy(2600)
        idle()
        longClick("overflowMenu")
        assertEquals(app.getString(androidx.appcompat.R.string.abc_action_menu_overflow_description), text("tooltip"))
    }

    @Test
    fun theRateButtonNamesItselfOnALongPress() {
        val tag = ScreenTestSupport.fixtureTag()
        ScreenTestSupport.cacheOnDisk(tag)
        val activity =
            launch(TagDetailActivity::class.java, Intent(app, TagDetailActivity::class.java).putExtra(TagDetailActivity.TAG_ID_EXTRA, tag.id))
        ScreenTestSupport.awaitTagLoaded(activity)
        idle()
        longClick("rateButton")
        assertEquals(string(R.string.Rate), text("tooltip"))
    }

    @Test
    fun theMenuKeyOpensTheOverflow() {
        val key = TagLists.create("Afterglow set")
        val activity = launch(TagListActivity::class.java, TagListActivity.intent(app, key))
        assertFalse(exists("renameList"))
        val callback = activity.window.callback
        callback.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MENU))
        callback.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MENU))
        idle()
        assertTrue(exists("renameList"))
        assertTrue(exists("deleteList"))
    }

    @Test
    fun aTappedTabIsSelectedAtOnceAndTheTabsBetweenNeverLight() {
        val tag = ScreenTestSupport.fixtureTag()
        ScreenTestSupport.cacheOnDisk(tag)
        val activity =
            launch(TagDetailActivity::class.java, Intent(app, TagDetailActivity::class.java).putExtra(TagDetailActivity.TAG_ID_EXTRA, tag.id))
        ScreenTestSupport.awaitTagLoaded(activity)
        idle()
        compose.mainClock.autoAdvance = false
        node("detailTab:3").performSemanticsAction(SemanticsActions.OnClick)
        var sawMidway = false
        repeat(40) {
            compose.mainClock.advanceTimeByFrame()
            compose.waitForIdle()
            assertTrue("the tapped tab is selected throughout", selected("detailTab:3"))
            for (between in 0..2) assertFalse("tab $between lit on the way", selected("detailTab:$between"))
            val page = activity.detail.page
            if (page == 0) sawMidway = true
        }
        compose.mainClock.autoAdvance = true
        idle()
        assertTrue("the pager was still on its way while the tab showed selected", sawMidway)
        assertTrue(selected("detailTab:3"))
    }

    @Test
    fun ripplesUseTheThemesHighlightAndALightOneOnTheChrome() {
        val colors = tagMasterColors(ContextThemeWrapper(app, R.style.AppTheme))
        val body = colors.controlHighlight.asRipple()
        assertEquals(colors.controlHighlight.copy(alpha = 1f), body.color)
        assertEquals(colors.controlHighlight.alpha, body.rippleAlpha!!.pressedAlpha, 0.001f)
        assertTrue("the chrome's ripple is light", colors.chromeHighlight.copy(alpha = 1f).luminance() > 0.5f)
    }
}
