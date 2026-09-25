package depollsoft.pitchperfect

import android.view.KeyEvent
import android.view.accessibility.AccessibilityManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import depollsoft.lib.activity.RichApplication
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** The main screen's chrome as a person handles it: tooltips, tabs, the overflow and the snackbar. */
@RunWith(RobolectricTestRunner::class)
@Config(application = RichApplication::class, qualifiers = "w411dp-h891dp")
class ChromeInteractionTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val screens = ComposeScreens(compose)
    private val model get() = SongsModel.get()

    @Before
    fun setUp() {
        ScreenTestSupport.startFromFirstLaunch()
        ScreenTestSupport.ensureFirebaseApp()
        PurchaseService.areAdsRemoved = true
        model.clearAll()
    }

    @After
    fun tearDown() {
        model.clearAll()
        PurchaseService.areAdsRemoved = false
        screens.finish()
    }

    private fun PitchPerfectActivity.tap(tab: MainTab) = with(screens) { show(tab) }

    private fun count(text: String) = compose.onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().size

    // ==================== Tooltips ====================

    @Test
    fun longPressingAnActionIconNamesItForTwoAndAHalfSeconds() {
        val activity = screens.launchMain()
        val settings = activity.getString(R.string.Settings)
        assertEquals(0, count(settings))
        compose.onNodeWithTag(TestTags.SETTINGS).performTouchInput { longClick() }
        compose.waitForIdle()
        assertEquals("the tooltip reads the action's name", 1, count(settings))
        compose.mainClock.advanceTimeBy(3_000)
        compose.waitForIdle()
        assertEquals(0, count(settings))
    }

    @Test
    fun longPressingATabNamesIt() {
        val activity = screens.launchMain()
        val keys = activity.getString(R.string.keys)
        val before = count(keys)
        compose.onNodeWithTag(TestTags.TAB_KEYS).performTouchInput { longClick() }
        compose.waitForIdle()
        assertEquals(before + 1, count(keys))
        assertEquals("a long press does not switch tabs", MainTab.PITCH_PIPE.ordinal, activity.pager.currentPage)
    }

    // ==================== Tabs ====================

    @Test
    fun theTabsReadAsACollectionAndTheCurrentOneOffersNoClick() {
        screens.launchMain()
        val current = compose.onNodeWithTag(TestTags.TAB_PITCH_PIPE).fetchSemanticsNode()
        val other = compose.onNodeWithTag(TestTags.TAB_NOTES).fetchSemanticsNode()
        assertFalse("the current tab has nothing to activate", current.config.contains(SemanticsActions.OnClick))
        assertTrue(other.config.contains(SemanticsActions.OnClick))
        assertEquals(1, other.config[SemanticsProperties.CollectionItemInfo].columnIndex)
        val bar = current.parent!!
        assertEquals(4, bar.config[SemanticsProperties.CollectionInfo].columnCount)
    }

    @Test
    fun aTappedTabStaysLitWhileThePagerScrollsPastTheOthers() {
        val activity = screens.launchMain()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag(TestTags.TAB_SONGS).performTouchInput { click(center) }
        repeat(20) {
            compose.mainClock.advanceTimeByFrame()
            assertTrue("Songs is lit from the tap on", screens.isSelected(TestTags.TAB_SONGS))
            assertFalse("the tabs passed over never light", screens.isSelected(TestTags.TAB_NOTES) || screens.isSelected(TestTags.TAB_KEYS))
        }
        compose.mainClock.autoAdvance = true
        screens.settle()
        assertEquals(MainTab.SONGS.ordinal, activity.pager.currentPage)
        assertTrue(screens.isSelected(TestTags.TAB_SONGS))
    }

    @Test
    fun aSlightlySidewaysPressOnANoteDoesNotStartAPageSwipe() {
        val activity = screens.launchMain()
        activity.tap(MainTab.NOTES)
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val slop = android.view.ViewConfiguration.get(context).scaledTouchSlop.toFloat()
        compose.onNodeWithTag(TestTags.NOTE_LIST).performTouchInput {
            down(center)
            moveBy(Offset(-slop * 1.5f, 0f))
        }
        compose.waitForIdle()
        assertEquals("past the usual slop but inside the paging slop", 0f, activity.pager.currentPageOffsetFraction, 0f)
        compose.onNodeWithTag(TestTags.NOTE_LIST).performTouchInput { up() }
        screens.settle()
    }

    // ==================== Overflow ====================

    @Test
    fun theMenuKeyOpensTheEditModeOverflow() {
        val activity = screens.launchMain()
        activity.tap(MainTab.SONGS)
        screens.click(TestTags.EDIT_SONGS)
        assertFalse(screens.exists(TestTags.ADD_FROM_LIST))
        compose.runOnIdle { activity.onKeyUp(KeyEvent.KEYCODE_MENU, KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MENU)) }
        screens.settle()
        assertTrue(screens.exists(TestTags.ADD_FROM_LIST))
    }

    @Test
    fun theOverflowFadesAwayRatherThanVanishing() {
        val activity = screens.launchMain()
        activity.tap(MainTab.SONGS)
        screens.click(TestTags.EDIT_SONGS)
        screens.click(TestTags.OVERFLOW)
        assertTrue(screens.exists(TestTags.MANAGE_LISTS))
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag(TestTags.MANAGE_LISTS).assertExists()
        compose.mainClock.advanceTimeBy(300)
        compose.mainClock.autoAdvance = true
        screens.settle()
    }

    // ==================== Snackbar ====================

    @Test
    fun theUndoSnackbarSitsOverTheNavigationAndOutlastsATabSwitch() {
        val other = model.createList("Saturday show")
        val activity = screens.launchMain()
        activity.tap(MainTab.SONGS)
        compose.runOnIdle { activity.songs.deleteList(other, { "Deleted $it" }, "Undo") }
        compose.waitForIdle()
        val snackbar = compose.onNodeWithText("Deleted Saturday show").fetchSemanticsNode().boundsInRoot
        val fab = compose.onNodeWithTag(TestTags.ADD_SONG_FAB).fetchSemanticsNode().boundsInRoot
        val nav = compose.onNodeWithTag(TestTags.TAB_SONGS).fetchSemanticsNode().boundsInRoot
        assertTrue("clear of the add button", snackbar.top >= fab.bottom)
        assertTrue("at the bottom of the window, over the navigation", snackbar.bottom > nav.top)
        activity.tap(MainTab.KEYS)
        compose.onNodeWithText("Deleted Saturday show").assertExists()
    }

    @Test
    fun aSnackbarWithAnActionWaitsAsLongAsTheAccessibilityTimeoutAsks() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val accessibility = context.getSystemService(AccessibilityManager::class.java)
        assertEquals(SnackbarTiming.LONG_MS, SnackbarTiming.timeoutMillis(context, SnackbarTiming.LONG_MS, hasAction = true))
        shadowOf(accessibility).setInteractiveUiTimeout(20_000)
        assertEquals(20_000L, SnackbarTiming.timeoutMillis(context, SnackbarTiming.LONG_MS, hasAction = true))
        assertNotNull(SnackbarTiming.timeoutMillis(context, SnackbarTiming.SHORT_MS, hasAction = false))
    }

    @Test
    fun theSnackbarSlidesUpFromBelow() {
        val other = model.createList("Saturday show")
        val activity = screens.launchMain()
        activity.tap(MainTab.SONGS)
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { activity.songs.deleteList(other, { "Deleted $it" }, "Undo") }
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeBy(SnackbarTiming.SLIDE_MS / 2L)
        val midway = compose.onNodeWithText("Deleted Saturday show").fetchSemanticsNode().boundsInRoot.top
        compose.mainClock.advanceTimeBy(SnackbarTiming.SLIDE_MS.toLong())
        val settled = compose.onNodeWithText("Deleted Saturday show").fetchSemanticsNode().boundsInRoot.top
        assertTrue("on its way up ($midway, then $settled)", midway > settled)
        compose.mainClock.autoAdvance = true
    }
}
