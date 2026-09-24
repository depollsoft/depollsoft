package depollsoft.tagmaster

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.CompositionLocalProvider
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
import depollsoft.tagmaster.ui.ListMotion
import depollsoft.tagmaster.ui.ReorderState
import depollsoft.tagmaster.ui.listItemMotion
import depollsoft.tagmaster.ui.rememberReorderState
import depollsoft.tagmaster.ui.reorderHandle
import depollsoft.tagmaster.ui.reorderRow
import depollsoft.tagmaster.ui.shownOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * How a saved list's rows move: a drag picks its row up on touch-down and lifts it, neighbours
 * slide aside rather than jump, the dropped row glides into its slot, each step is felt, and a
 * reorder that is not a drag keeps the list where it is. The screens (Home, Teachable, custom
 * lists) all build their rows from these pieces.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class ReorderMotionTest {
    @get:Rule
    val compose = createComposeRule()

    private val items = mutableStateListOf(*(0 until 30).toList().toTypedArray())
    private val felt = mutableListOf<HapticFeedbackType>()
    private lateinit var state: ReorderState<Int>
    private lateinit var listState: LazyListState

    private val rowHeight = 56.dp

    private fun show() {
        compose.setContent {
            CompositionLocalProvider(
                LocalHapticFeedback provides
                    object : HapticFeedback {
                        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                            felt += hapticFeedbackType
                        }
                    },
            ) {
                listState = rememberLazyListState()
                state =
                    rememberReorderState(listState, keyOf = { it }) { _, order ->
                        items.clear()
                        items.addAll(order)
                        true
                    }
                val shown = state.shownOrder(items.toList(), listState)
                LazyColumn(Modifier.testTag("list"), state = listState) {
                    itemsIndexed(shown, key = { _, item -> item }) { _, item ->
                        Row(
                            Modifier
                                .listItemMotion(this, animatePlacement = !state.isMoving(item))
                                .reorderRow(state, item, Color.White)
                                .fillMaxWidth()
                                .height(rowHeight)
                                .testTag("row:$item"),
                        ) {
                            Box(
                                Modifier
                                    .size(48.dp)
                                    .reorderHandle(state, item, { items.toList() }, enabled = true)
                                    .testTag("handle:$item"),
                            )
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun top(item: Int) = compose.onNodeWithTag("row:$item", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.top

    private fun rowPx() = with(compose.density) { rowHeight.toPx() }

    private fun advance(millis: Long) {
        compose.mainClock.advanceTimeBy(millis)
        compose.waitForIdle()
    }

    @Test
    fun touchingTheHandlePicksTheRowUpAtOnceAndLiftsIt() {
        show()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("handle:0").performTouchInput { down(center) }
        compose.waitForIdle()
        assertEquals("no slop: the touch itself starts the drag", 0, state.dragging)
        assertEquals(listOf(HapticFeedbackType.GestureThresholdActivate), felt)
        advance(ListMotion.LIFT_MILLIS.toLong() + 32)
        assertEquals(1f, state.lift, 0.001f)
        compose.onNodeWithTag("handle:0").performTouchInput { up() }
    }

    @Test
    fun neighboursSlideAsideAndTheDroppedRowGlidesIntoItsSlot() {
        show()
        val row = rowPx()
        val secondTop = top(1)
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("handle:0").performTouchInput {
            down(center)
            moveBy(Offset(0f, row * 0.8f))
        }
        compose.waitForIdle()
        assertEquals(listOf(1, 0), state.preview!!.take(2))
        assertTrue("a swap is felt", HapticFeedbackType.SegmentTick in felt)
        // Row 1 is on its way up to the slot row 0 left, not already there.
        advance(ListMotion.MOVE_MILLIS / 2L)
        val midway = top(1)
        assertTrue("row 1 slides: $midway between ${secondTop - row} and $secondTop", midway < secondTop - 1f && midway > secondTop - row + 1f)
        advance(ListMotion.MOVE_MILLIS.toLong())
        assertEquals(secondTop - row, top(1), 1f)

        // Let go short of the slot: the order is stored at once, and the row glides the rest.
        compose.onNodeWithTag("handle:0").performTouchInput { up() }
        compose.waitForIdle()
        assertEquals(listOf(1, 0), items.take(2))
        assertEquals(HapticFeedbackType.GestureEnd, felt.last())
        assertEquals(0, state.settling)
        val slot = secondTop
        val released = top(0)
        assertTrue("the row starts where the finger left it", released < slot - 1f)
        advance(ListMotion.SETTLE_MILLIS / 2L)
        val gliding = top(0)
        assertTrue("gliding: $gliding between $released and $slot", gliding > released + 0.5f && gliding < slot - 0.5f)
        advance(ListMotion.SETTLE_MILLIS.toLong())
        assertEquals(slot, top(0), 0.5f)
        assertEquals(null, state.settling)
        assertEquals(0f, state.lift, 0.001f)
    }

    @Test
    fun aReorderThatIsNotADragKeepsTheListWhereItIs() {
        show()
        compose.runOnIdle { listState.requestScrollToItem(10) }
        compose.waitForIdle()
        assertEquals(10, listState.firstVisibleItemIndex)
        // Move up / Move down, or a sync: row 10 trades places with row 11.
        compose.runOnIdle {
            items.removeAt(10)
            items.add(11, 10)
        }
        compose.waitForIdle()
        assertEquals("the list holds its place by index", 10, listState.firstVisibleItemIndex)
        assertEquals(0, listState.firstVisibleItemScrollOffset)
        assertEquals("the row that came up is the one at the top", 11, listState.layoutInfo.visibleItemsInfo.first().key)
    }

    @Test
    fun addedAndRemovedRowsFade() {
        show()
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { items.removeAt(2) }
        advance(ListMotion.FADE_MILLIS / 2L)
        assertTrue("the removed row is still fading out", compose.onAllNodesWithTagExists("row:2"))
        advance(ListMotion.FADE_MILLIS.toLong() + ListMotion.MOVE_MILLIS)
        assertFalse(compose.onAllNodesWithTagExists("row:2"))
    }

    @Test
    fun aNewDragDuringASettleFinishesTheSettleFirst() {
        show()
        val row = rowPx()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("handle:0").performTouchInput {
            down(center)
            moveBy(Offset(0f, row * 0.8f))
            up()
        }
        compose.waitForIdle()
        assertEquals(0, state.settling)
        compose.onNodeWithTag("handle:2").performTouchInput { down(center) }
        compose.waitForIdle()
        assertEquals(null, state.settling)
        assertEquals(2, state.dragging)
        assertEquals(0f, state.offset, 0f)
        compose.onNodeWithTag("handle:2").performTouchInput { up() }
    }

    private fun androidx.compose.ui.test.junit4.ComposeTestRule.onAllNodesWithTagExists(tag: String) =
        onAllNodes(androidx.compose.ui.test.hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
}
