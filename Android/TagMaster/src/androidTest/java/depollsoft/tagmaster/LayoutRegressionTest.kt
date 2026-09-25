package depollsoft.tagmaster

import android.graphics.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * What only a device can check: long titles in edit mode keep every control on screen, and the
 * list-name dialog keeps its field, error and buttons above the real on-screen keyboard.
 */
@RunWith(AndroidJUnit4::class)
class LayoutRegressionTest {
    @get:Rule(order = 0)
    val sandbox = SavedListsSandbox()

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    /** Clears this test's lists from memory; [sandbox] then restores the stored copy. */
    @After
    fun tearDown() {
        instrumentation.runOnMainSync {
            TeachableTagsModel.teachableTagIds = emptyList()
            TagLists.customKeys.toList().forEach(TagLists::delete)
        }
    }
    private val longTitle = "Sweet Adeline, the one I dream of when the harmonies ring all night long and the tags keep coming"

    private fun onScreen(
        tag: String,
        frame: Rect,
    ) {
        val bounds = compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInWindow
        assertTrue(
            "$tag $bounds inside $frame",
            bounds.left >= frame.left - 1 && bounds.right <= frame.right + 1 && bounds.top >= frame.top - 1 && bounds.bottom <= frame.bottom + 1,
        )
    }

    private fun visibleFrame(scenario: ActivityScenario<*>): Rect {
        val frame = Rect()
        scenario.onActivity { it.window.decorView.getWindowVisibleDisplayFrame(frame) }
        return frame
    }

    @Test
    fun longTitlesInEditModeKeepEveryControlOnScreen() {
        val ids = (0 until 6).map { 2147483300 + it }
        instrumentation.runOnMainSync {
            for (id in ids) {
                depollsoft.tagmaster.barbershop.Tag().apply {
                    this.id = id
                    title = "$longTitle $id"
                    parts = 4
                }.cache()
            }
            TeachableTagsModel.teachableTagIds = ids
        }
        ActivityScenario.launch(TeachableTagsActivity::class.java).use { scenario ->
            scenario.onActivity { it.listEditor.toggle() }
            compose.waitForIdle()
            val frame = visibleFrame(scenario)
            for (id in ids.take(3)) {
                compose.onNodeWithTag("savedTag:$id").performScrollTo()
                onScreen("drag:$id", frame)
                onScreen("remove:$id", frame)
            }
        }
    }

    @Test
    fun theListNameDialogValidatesAboveTheKeyboardAndTheLowestSettingsAreReachable() {
        ActivityScenario.launch(MeActivity::class.java).use { scenario ->
            compose.onNodeWithTag("homeList").performScrollToNode(androidx.compose.ui.test.hasTestTag("newListButton"))
            compose.onNodeWithTag("newListButton").performSemanticsAction(SemanticsActions.OnClick)
            compose.onNodeWithTag("listNameInput").performClick()
            compose.onNodeWithTag("listNameConfirm").performSemanticsAction(SemanticsActions.OnClick)
            compose.waitForIdle()
            Thread.sleep(500) // Let the keyboard finish arriving.
            val frame = visibleFrame(scenario)
            assertTrue(
                "the empty-name error shows",
                compose.onAllNodes(hasText(instrumentation.targetContext.getString(R.string.list_name_error_empty))).fetchSemanticsNodes().isNotEmpty(),
            )
            onScreen("listNameInput", frame)
            onScreen("listNameConfirm", frame)
        }
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            compose.onNodeWithTag("privacyChoicesButton").performScrollTo()
            onScreen("privacyChoicesButton", visibleFrame(scenario))
        }
    }
}
