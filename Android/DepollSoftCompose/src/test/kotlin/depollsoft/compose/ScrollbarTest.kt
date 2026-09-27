package depollsoft.compose

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration

/** The scrollbar shows the way a View's did: 1.2 seconds when the screen shows, and again on coming back. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class ScrollbarTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val listState = LazyListState()

    private fun show() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            LazyColumn(
                state = listState,
                modifier =
                    Modifier
                        .testTag("list")
                        .height(300.dp)
                        .fillMaxWidth()
                        .background(Color.White)
                        .recyclerScrollbar(listState),
            ) {
                items(60) { androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(48.dp)) }
            }
        }
        compose.mainClock.advanceTimeByFrame()
    }

    /** Moves both clocks on: the fade's timer runs on the main looper, its animation on Compose's. */
    private fun advance(millis: Long) {
        var left = millis
        while (left > 0) {
            val step = minOf(left, 16L)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(step))
            compose.mainClock.advanceTimeBy(step)
            left -= step
        }
    }

    /** Whether anything but the white list shows in the trailing 12 pixels, where the thumb sits. */
    private fun thumbShowing(): Boolean {
        val pixels = compose.onNodeWithTag("list").captureToImage().toPixelMap()
        for (x in pixels.width - 12 until pixels.width) {
            for (y in 0 until pixels.height step 4) {
                if (pixels[x, y] != Color.White) return true
            }
        }
        return false
    }

    @Test
    fun theThumbShowsForOnePointTwoSecondsWhenTheListFirstShows() {
        show()
        advance(900)
        assertTrue("still showing at 0.9s", thumbShowing())
        advance(1200)
        assertFalse("faded after 1.2s and the fade", thumbShowing())
    }

    @Test
    fun theThumbShowsAgainWhenTheScreenComesBack() {
        show()
        advance(3000)
        assertFalse(thumbShowing())
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        advance(100)
        assertTrue("awake again on return", thumbShowing())
    }
}
