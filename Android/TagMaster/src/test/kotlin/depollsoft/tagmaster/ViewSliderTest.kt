package depollsoft.tagmaster

import android.app.Application
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import depollsoft.tagmaster.ui.ViewSlider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The track sliders answer touch, a keyboard or a D-pad as MDC's slider did inside a scrolling page. */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class ViewSliderTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theArrowKeysMoveTheValueOneUnitAPress() {
        var value by mutableFloatStateOf(500f)
        compose.setContent {
            ViewSlider(value, { value = it }, Modifier.testTag("slider"), valueRange = 0f..1000f)
        }
        val slider = compose.onNodeWithTag("slider")
        slider.performSemanticsAction(SemanticsActions.RequestFocus)
        slider.performKeyInput { pressKey(Key.DirectionRight) }
        assertEquals(501f, value)
        slider.performKeyInput {
            pressKey(Key.DirectionLeft)
            pressKey(Key.DirectionLeft)
        }
        assertEquals(499f, value)
        slider.performKeyInput { pressKey(Key.Plus) }
        assertEquals(500f, value)
    }

    @Test
    fun theValueStopsAtTheEndsOfItsRange() {
        var value by mutableFloatStateOf(0f)
        compose.setContent {
            ViewSlider(value, { value = it }, Modifier.testTag("slider"), valueRange = 0f..1000f)
        }
        val slider = compose.onNodeWithTag("slider")
        slider.performSemanticsAction(SemanticsActions.RequestFocus)
        slider.performKeyInput { pressKey(Key.DirectionLeft) }
        assertEquals(0f, value)
    }

    private var value by mutableFloatStateOf(500f)
    private val scroll = ScrollState(0)

    /** A slider on a page that scrolls, as the Tracks page is. */
    private fun onAScrollingPage(direction: LayoutDirection = LayoutDirection.Ltr) {
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                Column(
                    Modifier
                        .size(300.dp, 400.dp)
                        .verticalScroll(scroll)
                        .testTag("page"),
                ) {
                    ViewSlider(value, { value = it }, Modifier.testTag("slider"), valueRange = 0f..1000f)
                    Box(Modifier.height(2000.dp))
                }
            }
        }
    }

    @Test
    fun aVerticalDragOnTheSliderScrollsThePageAndLeavesTheValue() {
        onAScrollingPage()
        compose.onNodeWithTag("slider").performTouchInput {
            down(Offset(width * 0.8f, centerY))
            repeat(10) { moveBy(Offset(0f, -30f)) }
            up()
        }
        compose.waitForIdle()
        assertEquals(500f, value)
        assertTrue("the page scrolled", scroll.value > 0)
    }

    @Test
    fun aTapSetsTheValueWhereTheFingerLifts() {
        onAScrollingPage()
        val slider = compose.onNodeWithTag("slider")
        slider.performTouchInput { down(Offset(width * 0.75f, centerY)) }
        compose.waitForIdle()
        assertEquals("nothing happens on touch-down", 500f, value)
        slider.performTouchInput { up() }
        compose.waitForIdle()
        assertTrue("value near three quarters: $value", value in 700f..800f)
    }

    @Test
    fun aSidewaysDragMovesTheValueOncePastTheSlop() {
        onAScrollingPage()
        compose.onNodeWithTag("slider").performTouchInput {
            down(Offset(width * 0.5f, centerY))
            repeat(10) { moveBy(Offset(width * 0.03f, 0f)) }
            up()
        }
        compose.waitForIdle()
        assertTrue("dragged to about 0.8: $value", value in 720f..880f)
        assertEquals(0, scroll.value)
    }

    @Test
    fun rightToLeftTheSliderRunsFromTheRight() {
        onAScrollingPage(LayoutDirection.Rtl)
        compose.onNodeWithTag("slider").performTouchInput { click(Offset(width * 0.25f, centerY)) }
        compose.waitForIdle()
        assertTrue("a quarter from the left is three quarters in: $value", value in 700f..800f)
    }
}
