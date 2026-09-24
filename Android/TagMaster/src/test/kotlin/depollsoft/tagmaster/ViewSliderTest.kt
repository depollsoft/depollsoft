package depollsoft.tagmaster

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import depollsoft.tagmaster.ui.ViewSlider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The track sliders answer a keyboard or D-pad as MDC's slider did. */
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
}
