package depollsoft.tagmaster

import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import depollsoft.lib.state.SnapshotNotifications
import depollsoft.lib.state.watchState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Reordering a saved list with a real pointer on a device: a drag previews the new order and
 * commits it once when the finger lifts, and anything that ends editing or changes the list
 * mid-drag discards the preview instead of writing a stale order back.
 */
@RunWith(AndroidJUnit4::class)
class SavedListEditingTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val ids = listOf(2147483017, 2147483018, 2147483019, 2147483020)
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private var commits = 0
    private var watch: depollsoft.lib.state.StateWatch? = null

    @Before
    fun setUp() {
        instrumentation.runOnMainSync {
            TeachableTagsModel.teachableTagIds = ids
            watch = watchState(read = { TeachableTagsModel.teachableTagIds.toList() }) { commits++ }
        }
    }

    @After
    fun tearDown() {
        instrumentation.runOnMainSync {
            watch?.stop()
            TeachableTagsModel.teachableTagIds = emptyList()
        }
    }

    private fun order(): List<Int> {
        var current = emptyList<Int>()
        instrumentation.runOnMainSync {
            SnapshotNotifications.flush()
            current = TeachableTagsModel.teachableTagIds.toList()
        }
        return current
    }

    private fun commits(): Int {
        instrumentation.runOnMainSync { SnapshotNotifications.flush() }
        return commits
    }

    private fun rowHeight() = compose.onNodeWithTag("savedTag:${ids[0]}").fetchSemanticsNode().size.height.toFloat()

    private fun startEditing(scenario: ActivityScenario<TeachableTagsActivity>) {
        scenario.onActivity { if (!it.listEditor.isEditing) it.listEditor.toggle() }
        compose.waitForIdle()
    }

    @Test
    fun aPointerDragFromFirstToLastAndBackCommitsOncePerDrop() {
        ActivityScenario.launch(TeachableTagsActivity::class.java).use { scenario ->
            startEditing(scenario)
            val step = rowHeight()
            compose.onNodeWithTag("drag:${ids[0]}").performTouchInput {
                down(center)
                repeat(12) { moveBy(androidx.compose.ui.geometry.Offset(0f, step * 3 / 12)) }
                up()
            }
            compose.waitForIdle()
            assertEquals(listOf(ids[1], ids[2], ids[3], ids[0]), order())
            assertEquals(1, commits())
            compose.onNodeWithTag("drag:${ids[0]}").performTouchInput {
                down(center)
                repeat(12) { moveBy(androidx.compose.ui.geometry.Offset(0f, -step * 3 / 12)) }
                up()
            }
            compose.waitForIdle()
            assertEquals(ids, order())
            assertEquals(2, commits())
        }
    }

    @Test
    fun doneAndPauseDiscardAPreviewInFlight() {
        ActivityScenario.launch(TeachableTagsActivity::class.java).use { scenario ->
            startEditing(scenario)
            val step = rowHeight()
            compose.onNodeWithTag("drag:${ids[0]}").performTouchInput {
                down(center)
                moveBy(androidx.compose.ui.geometry.Offset(0f, step * 2))
            }
            compose.waitForIdle()
            scenario.onActivity { it.listEditor.toggle() }
            compose.waitForIdle()
            assertEquals(ids, order())
            assertEquals(0, commits())

            startEditing(scenario)
            compose.onNodeWithTag("drag:${ids[1]}").performTouchInput {
                down(center)
                moveBy(androidx.compose.ui.geometry.Offset(0f, step))
            }
            scenario.moveToState(Lifecycle.State.STARTED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            compose.waitForIdle()
            assertEquals(ids, order())
            assertEquals(0, commits())
        }
    }

    @Test
    fun anOutsideChangeDuringADragCancelsItWithoutAStaleOverwrite() {
        ActivityScenario.launch(TeachableTagsActivity::class.java).use { scenario ->
            startEditing(scenario)
            val step = rowHeight()
            compose.onNodeWithTag("drag:${ids[0]}").performTouchInput {
                down(center)
                moveBy(androidx.compose.ui.geometry.Offset(0f, step * 2))
            }
            compose.waitForIdle()
            val external = listOf(ids[3], ids[2], ids[1], ids[0])
            instrumentation.runOnMainSync { TeachableTagsModel.teachableTagIds = external }
            compose.waitForIdle()
            compose.onNodeWithTag("drag:${ids[0]}").performTouchInput { up() }
            compose.waitForIdle()
            assertEquals(external, order())
        }
    }
}
