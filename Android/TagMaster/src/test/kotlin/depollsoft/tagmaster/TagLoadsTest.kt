package depollsoft.tagmaster

import android.app.Application
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import depollsoft.tagmaster.barbershop.Tag
import depollsoft.tagmaster.ui.TagLoads
import depollsoft.tagmaster.ui.rememberTagLoad
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A saved-list row scrolled back into view shows the tag it already loaded from its first frame.
 * Starting empty, it grew a frame later and pushed every row after it along mid-scroll.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class TagLoadsTest {
    @get:Rule
    val compose = createComposeRule()

    private val fixture = ScreenTestSupport.fixtureTag()
    private var shown by mutableStateOf(true)

    /** The tag the row held when each of its compositions began. */
    private val firstSeen = mutableListOf<Tag?>()

    @Before
    fun setUp() {
        ScreenTestSupport.startClean()
        ScreenTestSupport.clearTagCaches()
        ScreenTestSupport.cacheOnDisk(fixture)
    }

    @After
    fun tearDown() = ScreenTestSupport.clearTagCaches()

    private fun row(loads: TagLoads?) {
        compose.setContent {
            if (shown) {
                val load = rememberTagLoad(fixture.id, loads)
                val first = remember { load.tag }
                val entry = remember { firstSeen.size }
                SideEffect { if (firstSeen.size == entry) firstSeen += first }
            }
        }
        compose.waitForIdle()
    }

    private fun leaveAndComeBack() {
        compose.runOnIdle { shown = false }
        compose.waitForIdle()
        compose.runOnIdle { shown = true }
        compose.waitForIdle()
    }

    @Test
    fun aRowKeptByTheScreensLoadsComesBackWithItsTag() {
        val loads = TagLoads()
        row(loads)
        // The disk cache is read on a background thread.
        compose.waitUntil(5_000) { loads.of(fixture.id).tag != null }
        leaveAndComeBack()
        assertEquals(2, firstSeen.size)
        assertNull("the first time, the row starts empty", firstSeen[0])
        assertNotNull("coming back, it already has its tag", firstSeen[1])
        assertEquals(fixture.id, firstSeen[1]!!.id)
    }

    @Test
    fun withoutTheScreensLoadsARowComesBackEmpty() {
        row(null)
        leaveAndComeBack()
        assertEquals(2, firstSeen.size)
        assertNull(firstSeen[1])
    }
}
