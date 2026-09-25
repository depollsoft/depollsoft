package depollsoft.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** How a dragged row and its neighbours move: lift, trade places, settle, commit, and the haptics. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class ReorderStateTest {
    @get:Rule
    val compose = createComposeRule()

    private val rows = mutableStateListOf("A", "B", "C", "D")
    private val haptics = RecordingHaptics()
    private val listState = LazyListState()
    private val commits = mutableListOf<List<String>>()
    private var refuse = false
    private lateinit var reorder: ReorderState<String>

    /** Shows [rows] in a list that follows its source mid-drag when [followSource]. */
    private fun show(
        heightDp: Int = 400,
        bottomPaddingDp: Int = 0,
        followSource: Boolean = false,
    ) {
        compose.setContent {
            CompositionLocalProvider(LocalHapticFeedback provides haptics) {
                reorder =
                    rememberReorderState(listState, keyOf = { it }) { _, order ->
                        if (refuse) return@rememberReorderState false
                        commits += order
                        rows.clear()
                        rows.addAll(order)
                        true
                    }
                val source = rows.toList()
                if (followSource) LaunchedEffect(source) { reorder.sourceChanged(source) }
                val shown = reorder.shownOrder(rows.toList(), listState)
                LazyColumn(
                    state = listState,
                    modifier = Modifier.height(heightDp.dp),
                    contentPadding = PaddingValues(bottom = bottomPaddingDp.dp),
                ) {
                    items(shown, key = { it }) { row ->
                        Row(
                            Modifier
                                .listItemMotion(this, animatePlacement = !reorder.isMoving(row))
                                .reorderRow(reorder, row, Color.White)
                                .fillMaxWidth()
                                .height(ROW_DP.dp)
                                .testTag("row:$row"),
                        ) {
                            BasicText(row, Modifier.weight(1f))
                            Box(Modifier.size(ROW_DP.dp).testTag("handle:$row").reorderHandle(reorder, row, { rows.toList() }, enabled = true))
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun top(row: String) = compose.onNodeWithTag("row:$row").fetchSemanticsNode().boundsInRoot.top

    private val rowPx get() = with(compose.density) { ROW_DP.dp.toPx() }

    private fun drag(
        row: String,
        by: Float,
        steps: Int = 8,
    ) {
        compose.onNodeWithTag("handle:$row").performTouchInput {
            down(center)
            repeat(steps) { moveBy(Offset(0f, by / steps)) }
        }
    }

    private fun release(row: String) = compose.onNodeWithTag("handle:$row").performTouchInput { up() }

    @Test
    fun theRowLiftsAsTheFingerLandsAndTicksAsItStartsPassesAndEnds() {
        show()
        compose.onNodeWithTag("handle:A").performTouchInput { down(center) }
        compose.waitForIdle()
        assertEquals("A", reorder.dragging)
        assertEquals("lifted clear of the list", 1f, reorder.lift, 0.001f)
        compose.onNodeWithTag("handle:A").performTouchInput { repeat(8) { moveBy(Offset(0f, rowPx * 1.5f / 8)) } }
        release("A")
        compose.waitForIdle()
        assertEquals(listOf("B", "A", "C", "D"), rows.toList())
        assertEquals(
            listOf(HapticFeedbackType.GestureThresholdActivate, HapticFeedbackType.SegmentTick, HapticFeedbackType.GestureEnd),
            haptics.performed,
        )
        assertEquals("stored once", 1, commits.size)
    }

    @Test
    @Config(sdk = [33])
    fun beforeAndroid14ADragStartsWithTheLongPressBuzzAndNothingElse() {
        show()
        drag("A", rowPx * 1.5f)
        release("A")
        compose.waitForIdle()
        assertEquals(listOf("B", "A", "C", "D"), rows.toList())
        assertEquals(listOf(HapticFeedbackType.LongPress), haptics.performed)
    }

    @Test
    fun aRowTradesPlacesOnlyOnceItHasPassedTheWholeNeighbour() {
        show()
        drag("A", rowPx * 0.9f)
        compose.waitForIdle()
        assertEquals("most of the way past B is not past it", listOf("A", "B", "C", "D"), reorder.order(rows))
        compose.onNodeWithTag("handle:A").performTouchInput { moveBy(Offset(0f, rowPx * 0.2f)) }
        compose.waitForIdle()
        assertEquals(listOf("B", "A", "C", "D"), reorder.order(rows))
        release("A")
        compose.waitForIdle()
    }

    @Test
    fun aTouchThatMovesNothingStoresNothing() {
        show()
        compose.onNodeWithTag("handle:B").performTouchInput {
            down(center)
            up()
        }
        compose.waitForIdle()
        assertTrue("nothing stored", commits.isEmpty())
        assertNull(reorder.dragging)
    }

    @Test
    fun aPassedNeighbourSlidesIntoItsNewPlaceInsteadOfJumping() {
        show()
        val bBefore = top("B")
        compose.mainClock.autoAdvance = false
        drag("A", rowPx * 1.5f)
        compose.mainClock.advanceTimeByFrame()
        assertEquals("B now sits first", listOf("B", "A", "C", "D"), reorder.order(rows))
        compose.mainClock.advanceTimeBy(ListMotion.MOVE_MILLIS / 2L)
        val bMidway = top("B")
        assertTrue("B is on its way up, got $bMidway from $bBefore", bMidway < bBefore && bMidway > bBefore - rowPx)
        compose.mainClock.advanceTimeBy(400)
        assertEquals("and arrives in the first slot", bBefore - rowPx, top("B"), 1f)
        release("A")
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
    }

    @Test
    fun aDroppedRowGlidesIntoItsSlotAndSetsDown() {
        show()
        val slotOfB = top("B")
        drag("A", rowPx * 1.5f)
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        release("A")
        compose.mainClock.advanceTimeByFrame()
        assertEquals("still drawn as the moving row while it settles", "A", reorder.settling)
        assertNotEquals("still near where the finger left it", slotOfB, top("A"), 1f)
        compose.mainClock.advanceTimeBy(ListMotion.SETTLE_MILLIS + 100L)
        assertEquals("in its slot", slotOfB, top("A"), 1f)
        assertNull("set down", reorder.settling)
        assertEquals(0f, reorder.lift, 0.001f)
        compose.mainClock.autoAdvance = true
    }

    @Test
    fun aRefusedDropGlidesBackToTheRowsOldSlot() {
        show()
        val slotOfA = top("A")
        refuse = true
        drag("A", rowPx * 1.5f)
        compose.waitForIdle()
        val heldAt = top("A")
        compose.mainClock.autoAdvance = false
        release("A")
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeByFrame()
        val midway = top("A")
        assertTrue("on its way back, got $midway between $slotOfA and $heldAt", midway > slotOfA + 1 && midway < heldAt)
        compose.mainClock.advanceTimeBy(ListMotion.SETTLE_MILLIS + 100L)
        assertEquals("back in its slot", slotOfA, top("A"), 1f)
        assertEquals(listOf("A", "B", "C", "D"), rows.toList())
        compose.mainClock.autoAdvance = true
    }

    @Test
    fun aSourceChangeMidDragCancelsItAndTheRowGlidesToItsNewSlot() {
        show(followSource = true)
        drag("A", rowPx * 0.5f)
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { rows.add(0, "Z") }
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        compose.mainClock.advanceTimeByFrame()
        assertNull("the drag is abandoned", reorder.dragging)
        assertEquals("A", reorder.settling)
        compose.mainClock.advanceTimeBy(ListMotion.SETTLE_MILLIS + 100L)
        assertEquals("in its new slot, under Z", top("Z") + rowPx, top("A"), 1f)
        compose.mainClock.autoAdvance = true
        release("A")
        compose.waitForIdle()
        assertTrue("nothing stored", commits.isEmpty())
    }

    @Test
    fun withoutSourceChangedTheDragHoldsItsPreviewAndCommitsOverTheSource() {
        show()
        drag("A", rowPx * 1.5f)
        compose.waitForIdle()
        compose.runOnIdle { rows.add("E") }
        compose.waitForIdle()
        assertEquals("A", reorder.dragging)
        assertEquals(listOf("B", "A", "C", "D"), reorder.order(rows))
        release("A")
        compose.waitForIdle()
        assertEquals(listOf("B", "A", "C", "D"), commits.single())
    }

    @Test
    fun aNewDragWhileTheLastRowSettlesFinishesThatSettleFirst() {
        show()
        drag("A", rowPx * 1.5f)
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        release("A")
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("handle:C").performTouchInput { down(center) }
        compose.mainClock.advanceTimeByFrame()
        assertEquals("C", reorder.dragging)
        assertNull(reorder.settling)
        compose.mainClock.autoAdvance = true
        release("C")
        compose.waitForIdle()
        assertNull(reorder.dragging)
        assertEquals(listOf("B", "A", "C", "D"), rows.toList())
    }

    @Test
    fun holdingARowJustAboveTheListsBottomPaddingScrollsTheList() {
        rows.clear()
        rows.addAll((0 until 30).map { "R$it" })
        // 5 rows' worth of list, the last 1.5 of them padding.
        show(heightDp = ROW_DP * 5, bottomPaddingDp = ROW_DP * 3 / 2)
        assertFalse(listState.canScrollBackward)
        // R2's bottom lands inside the 48dp zone above the padding, not the screen's edge.
        drag("R1", rowPx * 1.2f, steps = 4)
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        assertTrue("the list scrolled", listState.canScrollBackward)
        release("R1")
        compose.waitForIdle()
    }

    @Test
    fun holdingTheScrollPositionKeepsTheListInPlaceWhenItsTopRowMoves() {
        rows.clear()
        rows.addAll((0 until 30).map { "R$it" })
        show()
        compose.runOnIdle {
            // The top row moves to the end, as a sort might move it; the shown order holds the list.
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
    }
}
