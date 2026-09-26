package depollsoft.compose

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A label tooltip shows as AppCompat's TooltipCompat did: 2.5s after a long press, while a mouse rests. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LabelTooltipStateTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var tooltip: LabelTooltipState
    private val haptics = mutableListOf<HapticFeedbackType>()

    private fun show() {
        compose.setContent {
            tooltip = rememberLabelTooltipState()
            val recording =
                object : HapticFeedback {
                    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                        haptics += hapticFeedbackType
                    }
                }
            CompositionLocalProvider(LocalHapticFeedback provides recording) {
                Box(
                    Modifier
                        .size(48.dp)
                        .testTag("anchor")
                        .tooltipOnHover(tooltip)
                        .combinedClickable(onLongClick = tooltip::longPressed) {},
                )
            }
        }
    }

    @Test
    fun aLongPressShowsItForTwoAndAHalfSecondsWithOneBuzz() {
        show()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("anchor").performTouchInput { longClick() }
        compose.mainClock.advanceTimeByFrame()
        assertTrue(tooltip.visible)
        assertEquals("combinedClickable's buzz, and no other", listOf(HapticFeedbackType.LongPress), haptics)
        compose.mainClock.advanceTimeBy(LabelTooltipState.LONG_PRESS_SHOW_MILLIS - 300)
        assertTrue(tooltip.visible)
        compose.mainClock.advanceTimeBy(400)
        assertFalse(tooltip.visible)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun aRestingMouseShowsItAfterTheLongPressTimeoutUntilItLeaves() {
        show()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("anchor").performMouseInput { enter(center) }
        compose.mainClock.advanceTimeBy(100)
        assertFalse("not yet", tooltip.visible)
        compose.mainClock.advanceTimeBy(600)
        assertTrue(tooltip.visible)
        compose.onNodeWithTag("anchor").performMouseInput { exit(Offset(-10f, -10f)) }
        compose.mainClock.advanceTimeByFrame()
        assertFalse(tooltip.visible)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun aMouseSweepingAcrossShowsNothingUntilItRests() {
        show()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("anchor").performMouseInput { enter(Offset(2f, 24f)) }
        // Past the hover slop every 300ms, for longer than the 500ms long-press timeout.
        for (step in 1..4) {
            compose.mainClock.advanceTimeBy(300)
            compose.onNodeWithTag("anchor").performMouseInput { moveTo(Offset(2f + step * 10f, 24f)) }
            assertFalse("still moving at step $step", tooltip.visible)
        }
        compose.mainClock.advanceTimeBy(700)
        assertTrue("shown once the mouse rests", tooltip.visible)
    }

    @Test
    fun aFingerDoesNotHover() {
        show()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("anchor").performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(100)
        compose.onNodeWithTag("anchor").performTouchInput { up() }
        compose.mainClock.advanceTimeBy(1000)
        assertFalse(tooltip.visible)
    }
}
