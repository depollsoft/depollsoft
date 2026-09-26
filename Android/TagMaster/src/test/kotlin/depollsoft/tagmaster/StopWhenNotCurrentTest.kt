package depollsoft.tagmaster

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import depollsoft.tagmaster.ui.detail.StopWhenNotCurrent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A page that plays sound stops when another tab becomes current, as a paused fragment did. The
 * detail screen's wiring (the Tracks page is current once the pager settles on it) is covered
 * through the real pager in [DetailTracksAndVideosScreenTest].
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
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
}
