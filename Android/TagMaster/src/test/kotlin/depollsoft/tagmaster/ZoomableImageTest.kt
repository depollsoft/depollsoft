package depollsoft.tagmaster

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.layout.size
import org.robolectric.annotation.GraphicsMode
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import depollsoft.tagmaster.ui.ZoomState
import depollsoft.tagmaster.ui.ZoomableImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Sheet music zooms and pans as PhotoView did: stepped double-tap zoom in 200ms, a gliding pan, and double-tap-and-drag zoom. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ZoomableImageTest {
    @get:Rule
    val compose = createComposeRule()

    private val state = ZoomState()

    private fun show() {
        val image = Bitmap.createBitmap(300, 400, Bitmap.Config.ARGB_8888)
        compose.setContent { ZoomableImage(image, Modifier.size(300.dp, 400.dp).testTag("image"), state = state) }
        compose.waitForIdle()
    }

    @Test
    fun aDoubleTapStepsTheZoomIn200ms() {
        show()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("image").performTouchInput { doubleClick(center) }
        compose.mainClock.advanceTimeBy(100)
        compose.waitForIdle()
        assertTrue("on its way: ${state.scale}", state.scale > 1.05f && state.scale < 1.7f)
        compose.mainClock.advanceTimeBy(150)
        compose.waitForIdle()
        assertEquals(1.75f, state.scale, 0.001f)
    }

    @Test
    fun aPanKeepsGlidingAfterTheFingerLifts() {
        show()
        compose.onNodeWithTag("image").performTouchInput { doubleClick(center) }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("image").performTouchInput {
            down(center)
            repeat(3) { moveBy(Offset(-15f, 0f), delayMillis = 10) }
            up()
        }
        compose.waitForIdle()
        val released = state.offset.x
        compose.mainClock.advanceTimeBy(300)
        compose.waitForIdle()
        assertTrue("glides on: $released then ${state.offset.x}", state.offset.x < released - 1f)
    }

    @Test
    fun doubleTapAndDragDownZoomsIn() {
        show()
        compose.onNodeWithTag("image").performTouchInput {
            click(center)
            advanceEventTime(100)
            down(center)
            repeat(10) { moveBy(Offset(0f, 20f), delayMillis = 16) }
            up()
        }
        compose.waitForIdle()
        assertTrue("zoomed by the drag: ${state.scale}", state.scale > 1.2f)
        assertTrue("not the stepped zoom", state.scale != 1.75f)
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun aZoomedPageStaysInsideItsAreaAndOffTheToolbarAbove() {
        val image = Bitmap.createBitmap(300, 400, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.BLACK) }
        compose.setContent {
            Column {
                Box(Modifier.fillMaxWidth().height(40.dp).background(Color.White).testTag("above"))
                ZoomableImage(image, Modifier.size(300.dp, 400.dp).testTag("image"), state = state)
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("image").performTouchInput { doubleClick(center) }
        compose.onNodeWithTag("image").performTouchInput { doubleClick(center) }
        compose.waitForIdle()
        assertEquals(3f, state.scale, 0.001f)
        val above = compose.onNodeWithTag("above").captureToImage().toPixelMap()
        val dark = (0 until above.width).count { x -> (0 until above.height).any { y -> above[x, y].red < 0.5f } }
        assertEquals("columns of the bar above painted by the page", 0, dark)
    }
}
