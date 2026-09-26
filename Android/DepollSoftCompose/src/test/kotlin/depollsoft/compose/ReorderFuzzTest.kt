package depollsoft.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Random

/**
 * Random gestures on a scrolling reorderable list: drags of any length in either direction, held at
 * the edges long enough to auto-scroll, ending in a drop, a system cancel, or a sync that changes
 * the list mid-drag. Whatever happens:
 * - the list always holds exactly its rows, once each;
 * - a drop that changed nothing stores nothing, and one that did stores a reordering of the rows
 *   the drag started from;
 * - a cancelled drag, or one whose list changed underneath, stores nothing;
 * - every gesture ends with nothing dragged or settling.
 * Each run is seeded, and a failure names its seed and gesture.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class ReorderFuzzTest {
    @get:Rule
    val compose = createComposeRule()

    private val rows = mutableStateListOf<String>()
    private val listState = LazyListState()
    private val commits = mutableListOf<Pair<List<String>, List<String>>>()
    private lateinit var reorder: ReorderState<String>

    private fun show() {
        compose.setContent {
            reorder =
                rememberReorderState(listState, keyOf = { it }) { baseline, order ->
                    commits += baseline to order
                    rows.clear()
                    rows.addAll(order)
                }
            val source = rows.toList()
            LaunchedEffect(source) { reorder.sourceChanged(source) }
            val shown = reorder.shownOrder(source, listState)
            LazyColumn(state = listState, modifier = Modifier.height((ROW_DP * 6).dp)) {
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
        compose.waitForIdle()
    }

    private val rowPx get() = with(compose.density) { ROW_DP.dp.toPx() }

    private fun visible(): List<String> =
        compose.runOnIdle {
            listState.layoutInfo.visibleItemsInfo
                .filter { it.offset >= 0 && it.offset + it.size <= listState.layoutInfo.viewportEndOffset }
                .map { it.key as String }
        }

    @Test
    fun randomGesturesKeepTheListWholeAndStoreOnlyRealReorders() {
        val all = (0 until 14).map { "R$it" }
        rows.addAll(all)
        show()
        var gesture = 0
        repeat(SEEDS) { seed ->
            val random = Random(seed.toLong())
            repeat(GESTURES) {
                gesture++
                val where = "seed $seed gesture $gesture"
                val before = rows.toList()
                val committed = commits.size
                if (random.nextInt(4) == 0) {
                    val target = random.nextInt(before.size)
                    compose.runOnIdle { listState.requestScrollToItem(target) }
                    compose.waitForIdle()
                }
                val candidates = visible()
                if (candidates.isEmpty()) return@repeat
                val row = candidates[random.nextInt(candidates.size)]
                val ending = random.nextInt(3)
                val moves = List(1 + random.nextInt(4)) { (random.nextFloat() * 3.6f - 1.8f) * rowPx }
                compose.onNodeWithTag("handle:$row").performTouchInput {
                    down(center)
                    for (dy in moves) {
                        repeat(4) { moveBy(Offset(0f, dy / 4), delayMillis = 16) }
                        // Sometimes hold still for a while, which at an edge scrolls the list.
                        if (random.nextInt(3) == 0) moveBy(Offset.Zero, delayMillis = 200)
                    }
                }
                compose.waitForIdle()
                // The finger lifts (or the system takes the gesture) wherever the row has got to,
                // so the event goes to the root rather than a handle that may have scrolled away.
                when (ending) {
                    0 -> compose.onRoot().performTouchInput { up() }
                    1 -> compose.onRoot().performTouchInput { cancel() }
                    else -> {
                        // A sync moves another row while the finger is down, then the finger lifts.
                        compose.runOnIdle {
                            val other = rows.indexOfFirst { it != row }
                            rows.add(rows.removeAt(other))
                        }
                        compose.waitForIdle()
                        compose.onRoot().performTouchInput { up() }
                    }
                }
                compose.waitForIdle()
                assertNull("$where: nothing is left dragged", reorder.dragging)
                assertNull("$where: nothing is left settling", reorder.settling)
                assertEquals("$where: the list holds each row once", all.toSet(), rows.toSet())
                assertEquals("$where: no row is doubled", all.size, rows.size)
                val stored = commits.drop(committed)
                when (ending) {
                    0 -> {
                        assertTrue("$where: one drop stores at most once", stored.size <= 1)
                        stored.firstOrNull()?.let { (baseline, order) ->
                            assertEquals("$where: stored against the rows it started from", before, baseline)
                            assertTrue("$where: a drop stores only a real change", order != baseline)
                            assertEquals("$where: a stored order holds the same rows", baseline.toSet(), order.toSet())
                        }
                        if (stored.isEmpty()) assertEquals("$where: an unchanged drop leaves the list alone", before, rows.toList())
                    }
                    1 -> {
                        assertTrue("$where: a cancelled drag stores nothing", stored.isEmpty())
                        assertEquals("$where: and leaves the list as it was", before, rows.toList())
                    }
                    else -> assertTrue("$where: a drag whose list changed underneath stores nothing", stored.isEmpty())
                }
            }
        }
        // The run is only worth something if drags really reordered the list.
        assertTrue("real reorders were stored: ${commits.size}", commits.size >= 10)
    }

    private companion object {
        const val ROW_DP = 56
        const val SEEDS = 12
        const val GESTURES = 10
    }
}
