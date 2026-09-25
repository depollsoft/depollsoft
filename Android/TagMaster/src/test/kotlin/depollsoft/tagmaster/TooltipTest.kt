package depollsoft.tagmaster

import android.app.Application
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import depollsoft.tagmaster.ui.WithTooltip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A long press shows the tooltip with the long-press haptic, and it goes after 2.5 seconds, as TooltipCompat's did. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class TooltipTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun aLongPressShowsTheNameWithTheHapticForTwoAndAHalfSeconds() {
        val felt = mutableListOf<HapticFeedbackType>()
        var clicks = 0
        compose.setContent {
            CompositionLocalProvider(
                LocalHapticFeedback provides
                    object : HapticFeedback {
                        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                            felt += hapticFeedbackType
                        }
                    },
            ) {
                MaterialTheme {
                    WithTooltip("Settings") { tooltip ->
                        Box(
                            Modifier
                                .size(48.dp)
                                .testTag("anchor")
                                .combinedClickable(onLongClick = tooltip::longPressed) { clicks++ },
                        )
                    }
                }
            }
        }
        compose.onNodeWithTag("anchor").performTouchInput { longClick() }
        compose.waitForIdle()
        assertEquals(listOf(HapticFeedbackType.LongPress), felt)
        assertEquals("a long press is not a click", 0, clicks)
        assertTrue(shown())
        compose.mainClock.advanceTimeBy(2200)
        assertTrue(shown())
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        assertFalse(shown())
    }

    private fun shown() = compose.onAllNodes(hasTestTag("tooltip"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
}
