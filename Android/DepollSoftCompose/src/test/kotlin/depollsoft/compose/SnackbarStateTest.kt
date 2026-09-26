package depollsoft.compose

import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Snackbars last as long as MDC's once they have slid in, one at a time, heard by screen readers. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class SnackbarStateTest {
    @get:Rule
    val compose = createComposeRule()

    private val accessibility =
        RuntimeEnvironment.getApplication().getSystemService(AccessibilityManager::class.java)
    private val state = SnackbarState(accessibility)
    private lateinit var scope: CoroutineScope
    private val results = mutableListOf<Boolean>()

    private fun start() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            scope = rememberCoroutineScope()
            Box(Modifier.fillMaxSize()) {
                SlidingSnackbarHost(state, Modifier.align(Alignment.BottomCenter)) { BasicText(it.message) }
            }
        }
        compose.mainClock.advanceTimeByFrame()
    }

    private fun show(
        message: String,
        action: String? = null,
        millis: Long? = SnackbarTiming.LONG_MILLIS,
    ) {
        compose.runOnIdle { scope.launch { results += state.show(message, action, millis) } }
        compose.mainClock.advanceTimeByFrame()
    }

    private fun after(millis: Long): ShownSnackbar? {
        compose.mainClock.advanceTimeBy(millis)
        return state.current
    }

    @Test
    fun aShortMessageStaysAsLongAsALongOneOnAndroid10AndLater() {
        // MDC handed LENGTH_SHORT (-1) to the accessibility manager, which returns 0 when no
        // timeout is set, and SnackbarManager shows 0 for LENGTH_LONG's 2750ms.
        start()
        show("Cache cleared", millis = SnackbarTiming.SHORT_MILLIS)
        assertEquals("Cache cleared", after(SnackbarTiming.SLIDE_MILLIS + 2650L)?.message)
        assertNull(after(200))
        assertEquals(listOf(false), results)
    }

    @Test
    @Config(sdk = [28])
    fun aShortMessageStaysOneAndAHalfSecondsBeforeAndroid10() {
        start()
        show("Cache cleared", millis = SnackbarTiming.SHORT_MILLIS)
        // The slide takes 250ms; then 1500ms.
        assertEquals("Cache cleared", after(SnackbarTiming.SLIDE_MILLIS + 1400L)?.message)
        assertNull(after(200))
        assertEquals(listOf(false), results)
    }

    @Test
    fun aLongMessageStaysTwoPointSevenFiveSeconds() {
        start()
        show("Could not play the track", "Retry")
        assertEquals("Could not play the track", after(SnackbarTiming.SLIDE_MILLIS + 2650L)?.message)
        assertNull(after(200))
    }

    @Test
    fun anIndefiniteMessageStaysUntilDismissed() {
        start()
        show("Could not refresh", "Retry", millis = null)
        assertEquals("Could not refresh", after(20_000)?.message)
        compose.runOnIdle { state.dismiss() }
        compose.mainClock.advanceTimeByFrame()
        assertNull(state.current)
    }

    @Test
    fun theActionEndsItAndIsReported() {
        start()
        show("List deleted", "Undo")
        compose.runOnIdle { state.current!!.performAction() }
        compose.mainClock.advanceTimeBy(100)
        assertEquals(listOf(true), results)
    }

    @Test
    fun aNewMessageWaitsForTheOneItReplacesToSlideAway() {
        start()
        show("First")
        compose.mainClock.advanceTimeBy(1000)
        show("Second")
        compose.mainClock.advanceTimeBy(SnackbarTiming.SLIDE_MILLIS / 2L)
        compose.onNodeWithText("First").assertExists()
        compose.onNodeWithText("Second").assertDoesNotExist()
        compose.mainClock.advanceTimeBy(SnackbarTiming.SLIDE_MILLIS * 2L)
        compose.onNodeWithText("First").assertDoesNotExist()
        compose.onNodeWithText("Second").assertExists()
        assertEquals(listOf(false), results)
    }

    @Test
    fun screenReadersHearItAndCanDismissIt() {
        start()
        show("List deleted", "Undo")
        compose.mainClock.advanceTimeBy(SnackbarTiming.SLIDE_MILLIS.toLong())
        compose
            .onNode(
                SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite) and hasAnyDescendant(hasText("List deleted")),
                useUnmergedTree = true,
            ).assertExists()
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss)).performSemanticsAction(SemanticsActions.Dismiss)
        compose.mainClock.advanceTimeByFrame()
        assertNull(state.current)
    }

    @Test
    fun anActionWaitsAsLongAsTheAccessibilityTimeoutAsks() {
        assertEquals(SnackbarTiming.LONG_MILLIS, SnackbarTiming.shownFor(accessibility, SnackbarTiming.LONG_MILLIS, hasAction = true))
        shadowOf(accessibility).setInteractiveUiTimeout(20_000)
        assertEquals(20_000L, SnackbarTiming.shownFor(accessibility, SnackbarTiming.LONG_MILLIS, hasAction = true))
        assertEquals(SnackbarTiming.SHORT_MILLIS, SnackbarTiming.shownFor(null, SnackbarTiming.SHORT_MILLIS, hasAction = false))
    }

    @Test
    fun aShortMessageStretchesToTheReadingTimeoutAsked() {
        assertEquals(SnackbarTiming.LONG_MILLIS, SnackbarTiming.shownFor(accessibility, SnackbarTiming.SHORT_MILLIS, hasAction = false))
        shadowOf(accessibility).setNonInteractiveUiTimeout(5_000)
        assertEquals(5_000L, SnackbarTiming.shownFor(accessibility, SnackbarTiming.SHORT_MILLIS, hasAction = false))
    }
}
