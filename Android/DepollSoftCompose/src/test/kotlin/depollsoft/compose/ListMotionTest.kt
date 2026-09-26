package depollsoft.compose

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Rows glide when the list changes at rest, and snap while it scrolls: a row pushed along by a
 * neighbour that changed height as it scrolled into view (its content loading in) lagged behind a
 * fast scroll when it glided.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class ListMotionTest {
    @get:Rule
    val compose = createComposeRule()

    private val listState = LazyListState()
    private var items by mutableStateOf(listOf("a", "b", "c") + (1..30).map { "row$it" })
    private var firstHeight by mutableStateOf(50.dp)

    private fun show() {
        compose.setContent {
            LazyColumn(Modifier.testTag("list"), state = listState) {
                items(items, key = { it }) { item ->
                    Box(
                        Modifier
                            .listItemMotion(this, animatePlacement = !listState.isScrollInProgress)
                            .testTag(item)
                            .fillMaxWidth()
                            .height(if (item == "a") firstHeight else 50.dp),
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
    }

    private fun top(tag: String): Dp = compose.onNodeWithTag(tag).getUnclippedBoundsInRoot().top

    private fun frames(count: Int) = repeat(count) { compose.mainClock.advanceTimeByFrame() }

    /** Writes state on the UI thread and lets the paused clock's next frame see it. */
    private fun change(block: () -> Unit) {
        compose.runOnUiThread(block)
        compose.waitForIdle()
    }

    @Test
    fun whileTheListScrollsARowPushedByItsNeighboursNewHeightSnaps() {
        show()
        // A finger holding the list mid-scroll, with row a still the first one showing.
        compose.onNodeWithTag("list").performTouchInput {
            down(center)
            moveBy(Offset(0f, -15f))
            moveBy(Offset(0f, -15f))
        }
        frames(2)
        val before = top("b")
        change { firstHeight = 150.dp }
        frames(3)
        assertEquals(before + 100.dp, top("b"))
        compose.onNodeWithTag("list").performTouchInput { up() }
    }

    @Test
    fun atRestRowsGlideWhenTheListChanges() {
        show()
        // The first row stays put: a lazy list keeps its first visible row where it is.
        change { items = listOf("a", "c", "b") + items.drop(3) }
        frames(8)
        val midway = top("c")
        assertTrue("c glides up from 100dp: $midway", midway > 50.dp && midway < 100.dp)
        frames(60)
        assertEquals(50.dp, top("c"))
    }

    @Test
    fun afterAScrollEndsRowsGlideAgain() {
        show()
        compose.onNodeWithTag("list").performTouchInput {
            down(center)
            moveBy(Offset(0f, -40f))
            up()
        }
        frames(120)
        val cTop = top("c")
        change { items = listOf("a", "c", "b") + items.drop(3) }
        frames(8)
        val midway = top("c")
        assertTrue("c glides up 50dp from $cTop: $midway", midway < cTop && midway > cTop - 50.dp)
    }
}
