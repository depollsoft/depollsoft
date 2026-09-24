package depollsoft.tagmaster

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.junit4.v2.createComposeRule
import depollsoft.tagmaster.ui.detail.StopWhenNotCurrent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A page that plays sound stops when another tab becomes current, as a paused fragment did. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class StopWhenNotCurrentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun leavingThePageStopsItAndReturningDoesNot() {
        val current = mutableStateOf(true)
        var stops = 0
        compose.setContent { StopWhenNotCurrent(current.value) { stops++ } }
        compose.waitForIdle()
        assertEquals("showing the page stops nothing", 0, stops)

        current.value = false
        compose.waitForIdle()
        assertEquals("another tab became current", 1, stops)

        current.value = true
        compose.waitForIdle()
        assertEquals("coming back does not stop anything", 1, stops)
    }

    @Test
    fun aSwipeHeldPastHalfwayDoesNotStopTheTrackUntilThePagerSettles() {
        var stops = 0
        lateinit var pager: PagerState
        compose.setContent {
            pager = rememberPagerState(initialPage = 2) { 4 }
            HorizontalPager(pager, Modifier.fillMaxSize().testTag("pager")) { page ->
                // As the detail screen wires the Tracks page.
                if (page == 2) StopWhenNotCurrent(pager.settledPage == 2) { stops++ }
                Box(Modifier.fillMaxSize())
            }
        }
        compose.onNodeWithTag("pager").performTouchInput {
            down(center)
            moveBy(Offset(-width * 0.7f, 0f))
        }
        compose.waitForIdle()
        assertEquals("the pager is past halfway", 3, pager.currentPage)
        assertEquals("but the track plays on", 0, stops)
        // The swipe comes back to Tracks: nothing was stopped.
        compose.onNodeWithTag("pager").performTouchInput {
            repeat(7) { moveBy(Offset(width * 0.1f, 0f), delayMillis = 50) }
            // Still before lifting, so no fling carries it on.
            repeat(4) { moveBy(Offset.Zero, delayMillis = 100) }
            up()
        }
        compose.waitForIdle()
        assertEquals(2, pager.settledPage)
        assertEquals(0, stops)
        compose.onNodeWithTag("pager").performTouchInput { swipeLeft() }
        compose.waitForIdle()
        assertEquals("settled on Videos", 1, stops)
    }
}
