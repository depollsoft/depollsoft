package depollsoft.tagmaster

import android.app.Application
import android.view.accessibility.AccessibilityManager
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.junit4.v2.createComposeRule
import depollsoft.tagmaster.ui.Snackbars
import kotlinx.coroutines.CoroutineScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Snackbars stay up as long as MDC's did: 1500ms short, 2750ms long, until dismissed when indefinite. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class SnackbarTimingTest {
    @get:Rule
    val compose = createComposeRule()

    private val host = SnackbarHostState()
    private lateinit var snackbars: Snackbars

    private fun start() {
        compose.mainClock.autoAdvance = false
        lateinit var scope: CoroutineScope
        compose.setContent { scope = rememberCoroutineScope() }
        compose.waitForIdle()
        val accessibility = RuntimeEnvironment.getApplication().getSystemService(AccessibilityManager::class.java)
        snackbars = Snackbars(host, scope, accessibility)
    }

    private fun shownAfter(millis: Long): Boolean {
        compose.mainClock.advanceTimeBy(millis)
        compose.waitForIdle()
        return host.currentSnackbarData != null
    }

    @Test
    fun aShortMessageStaysOneAndAHalfSeconds() {
        start()
        compose.runOnIdle { snackbars.show("Cache cleared", long = false) }
        assertEquals(true, shownAfter(1400))
        assertEquals(false, shownAfter(200))
    }

    @Test
    fun aLongMessageWithAnActionStaysTwoPointSevenFiveSeconds() {
        start()
        compose.runOnIdle { snackbars.show("Could not play the track", "Retry") }
        assertEquals(true, shownAfter(2650))
        assertEquals(false, shownAfter(200))
    }

    @Test
    fun anIndefiniteMessageStaysUntilDismissed() {
        start()
        compose.runOnIdle { snackbars.show("Could not refresh", "Retry", indefinite = true) }
        assertEquals(true, shownAfter(20_000))
        assertNotNull(host.currentSnackbarData)
        compose.runOnIdle { snackbars.dismiss() }
        compose.waitForIdle()
        assertNull(host.currentSnackbarData)
    }
}
