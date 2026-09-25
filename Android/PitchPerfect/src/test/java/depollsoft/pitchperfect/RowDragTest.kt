package depollsoft.pitchperfect

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import depollsoft.lib.activity.RichApplication
import depollsoft.pitchperfect.ui.RowDrag
import depollsoft.pitchperfect.ui.holdScrollPosition
import depollsoft.pitchperfect.ui.reorderableRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** How a dragged row and its neighbours move: lift, slide aside, settle, and the haptics on the way. */
@RunWith(RobolectricTestRunner::class)
@Config(application = RichApplication::class, qualifiers = "w411dp-h891dp")
class RowDragTest {
    @get:Rule
    val compose = createComposeRule()

    private val rows = mutableStateListOf("A", "B", "C", "D")
    private val haptics = RecordingHaptics()
    private val listState = LazyListState()
    private var drops = 0
    private lateinit var drag: RowDrag

    private fun show() {
        drag =
            RowDrag(
                listState,
                indexOf = { key -> rows.indexOf(key) },
                canMove = { _, _ -> true },
                move = { from, to -> rows.add(to, rows.removeAt(from)) },
                onDrop = { drops++ },
                haptics = haptics,
            )
        compose.setContent {
            CompositionLocalProvider(LocalHapticFeedback provides haptics) {
                LazyColumn(state = listState, modifier = Modifier.height(400.dp)) {
                    items(rows, key = { it }) { row ->
                        Row(reorderableRow(drag, row, Color.White).fillMaxWidth().height(ROW_DP.dp).testTag("row:$row")) {
                            Text(row, Modifier.weight(1f))
                            Box(Modifier.size(ROW_DP.dp).testTag("handle:$row").pointerInput(row) { drag.track(this, row) })
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun top(row: String) = compose.onNodeWithTag("row:$row").fetchSemanticsNode().boundsInRoot.top

    private val rowPx get() = compose.onNodeWithTag("row:A").fetchSemanticsNode().size.height.toFloat()

    @Test
    fun theRowLiftsAsTheFingerLandsAndTicksAsItStartsPassesAndEnds() {
        show()
        compose.onNodeWithTag("handle:A").performTouchInput { down(center) }
        compose.waitForIdle()
        assertEquals("A", drag.key)
        assertEquals("lifted clear of the list", 1f, drag.lift, 0.001f)
        compose.onNodeWithTag("handle:A").performTouchInput { repeat(8) { moveBy(Offset(0f, rowPx / 4f)) } }
        compose.onNodeWithTag("handle:A").performTouchInput { up() }
        compose.waitForIdle()
        assertEquals(listOf("B", "A", "C", "D"), rows.toList())
        assertEquals(
            listOf(HapticFeedbackType.GestureThresholdActivate, HapticFeedbackType.SegmentTick, HapticFeedbackType.GestureEnd),
            haptics.performed,
        )
        assertEquals("stored once", 1, drops)
    }

    @Test
    fun aPassedNeighbourSlidesIntoItsNewPlaceInsteadOfJumping() {
        show()
        val bBefore = top("B")
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("handle:A").performTouchInput {
            down(center)
            repeat(5) { moveBy(Offset(0f, rowPx / 4f)) }
        }
        compose.mainClock.advanceTimeByFrame()
        assertEquals("B now sits first", listOf("B", "A", "C", "D"), rows.toList())
        compose.mainClock.advanceTimeBy(ListMotionHalfway)
        val bMidway = top("B")
        assertTrue("B is on its way up, got $bMidway from $bBefore", bMidway < bBefore && bMidway > bBefore - rowPx)
        compose.mainClock.advanceTimeBy(400)
        assertEquals("and arrives in the first slot", bBefore - rowPx, top("B"), 1f)
        compose.onNodeWithTag("handle:A").performTouchInput { up() }
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
    }

    @Test
    fun aDroppedRowGlidesIntoItsSlotAndSetsDown() {
        show()
        val slotOfB = top("B")
        compose.onNodeWithTag("handle:A").performTouchInput {
            down(center)
            repeat(6) { moveBy(Offset(0f, rowPx / 4f)) }
        }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("handle:A").performTouchInput { up() }
        compose.mainClock.advanceTimeByFrame()
        assertEquals("still drawn as the dragged row while it settles", "A", drag.key)
        assertNotEquals("still where the finger left it", slotOfB, top("A"), 1f)
        compose.mainClock.advanceTimeBy(RowDrag.SETTLE_MS + 100L)
        assertEquals("in its slot", slotOfB, top("A"), 1f)
        assertNull("set down", drag.key)
        assertEquals(0f, drag.lift, 0.001f)
        compose.mainClock.autoAdvance = true
    }

    @Test
    fun aNewDragWhileTheLastRowSettlesFinishesThatSettleFirst() {
        show()
        compose.onNodeWithTag("handle:A").performTouchInput {
            down(center)
            repeat(6) { moveBy(Offset(0f, rowPx / 4f)) }
        }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("handle:A").performTouchInput { up() }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("handle:C").performTouchInput { down(center) }
        compose.mainClock.advanceTimeByFrame()
        assertEquals("C", drag.key)
        compose.mainClock.autoAdvance = true
        compose.onNodeWithTag("handle:C").performTouchInput { up() }
        compose.waitForIdle()
        assertNull(drag.key)
        assertEquals(listOf("B", "A", "C", "D"), rows.toList())
    }

    @Test
    fun holdingTheScrollPositionKeepsTheListInPlaceWhenItsTopRowMoves() {
        rows.clear()
        rows.addAll((0 until 30).map { "R$it" })
        show()
        compose.runOnIdle {
            listState.holdScrollPosition()
            // The top row moves to the end, as a sort might move it.
            rows.add(rows.removeAt(0))
        }
        compose.waitForIdle()
        assertEquals(0, listState.firstVisibleItemIndex)
        assertEquals("R1", listState.layoutInfo.visibleItemsInfo.first().key)
    }

    private class RecordingHaptics : HapticFeedback {
        val performed = mutableListOf<HapticFeedbackType>()

        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            performed += hapticFeedbackType
        }
    }

    private companion object {
        const val ROW_DP = 56
        const val ListMotionHalfway = depollsoft.pitchperfect.ui.ListMotion.MOVE_MS / 2L
    }
}
