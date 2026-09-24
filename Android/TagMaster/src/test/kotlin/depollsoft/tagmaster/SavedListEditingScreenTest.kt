package depollsoft.tagmaster

import android.app.Application
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Editing a saved list (Teachable Tags here; every saved list shares the editor): Remove asks
 * first and removes exactly the tag it named, rows move with accessibility actions, rows do not
 * open tags while editing, and editing ends when the list empties.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class SavedListEditingScreenTest : ComposeScreenTest() {
    private val ids = listOf(2147483017, 2147483018, 2147483019)

    private fun populate(count: Int = 3): List<Int> {
        val chosen = ids.take(count)
        for ((index, id) in chosen.withIndex()) {
            ScreenTestSupport.cacheOnDisk(ScreenTestSupport.fixtureTag().apply { this.id = id; title = "Fixture ${index + 1}" })
            TeachableTagsModel.addTeachableTag(id)
        }
        return chosen
    }

    private fun teachable(): TeachableTagsActivity = launch(TeachableTagsActivity::class.java)

    @Test
    fun theEmptyListExplainsItselfAndOffersNoEdit() {
        teachable()
        assertTrue(compose.onAllNodes(androidx.compose.ui.test.hasText(string(R.string.NoTeachableTags))).fetchSemanticsNodes().isNotEmpty())
        node("editSavedList").assertIsNotEnabled()
    }

    @Test
    fun removeAsksFirstAndRemovesOnlyTheNamedTagAfterOutsideChanges() {
        val (first, second, third) = populate()
        val activity = teachable()
        click("editSavedList")
        click("remove:$second")
        assertTrue(exists("dialog"))
        // The list changes underneath the open confirmation.
        TeachableTagsModel.teachableTagIds = listOf(third, second, first)
        idle()
        click("dialogButton:${string(R.string.saved_list_remove)}")
        assertEquals(listOf(third, first), TeachableTagsModel.teachableTagIds.toList())
        assertNull(activity.listEditor.pendingRemoval)
    }

    @Test
    fun cancellingAConfirmationRemovesNothing() {
        val (_, second, _) = populate()
        teachable()
        click("editSavedList")
        click("remove:$second")
        click("dialogButton:${string(R.string.home_cancel)}")
        assertEquals(3, TeachableTagsModel.teachableTagIds.size)
        assertFalse(exists("dialog"))
    }

    @Test
    fun accessibilityMovesReorderAndOnlyOfferPossibleMoves() {
        val (first, second, third) = populate()
        teachable()
        click("editSavedList")
        assertFalse(customActions("savedTag:$first").contains(string(R.string.MoveUp)))
        assertFalse(customActions("savedTag:$third").contains(string(R.string.MoveDown)))
        customAction("savedTag:$first", string(R.string.MoveDown))
        assertEquals(listOf(second, first, third), TeachableTagsModel.teachableTagIds.toList())
        assertTrue(customActions("savedTag:$first").contains(string(R.string.MoveUp)))
    }

    @Test
    fun aSingleRowCannotBeReorderedButCanBeRemoved() {
        val (only) = populate(1)
        teachable()
        click("editSavedList")
        val actions = customActions("savedTag:$only")
        assertFalse(actions.contains(string(R.string.MoveUp)))
        assertFalse(actions.contains(string(R.string.MoveDown)))
        assertTrue(actions.contains(string(R.string.saved_list_remove)))
    }

    @Test
    fun rowsDoNotOpenTagsWhileEditing() {
        val (first) = populate()
        val activity = teachable()
        click("editSavedList")
        // While editing a row offers no click at all, so nothing can open it by accident.
        assertFalse(node("savedTag:$first").fetchSemanticsNode().config.contains(androidx.compose.ui.semantics.SemanticsActions.OnClick))
        assertNull(nextStarted(activity))
        click("editSavedList")
        click("savedTag:$first")
        assertEquals(TagDetailActivity::class.java.name, nextStarted(activity)?.component?.className)
    }

    @Test
    fun emptyingTheListEndsEditingAndDisablesTheAction() {
        val (only) = populate(1)
        val activity = teachable()
        click("editSavedList")
        click("remove:$only")
        click("dialogButton:${string(R.string.saved_list_remove)}")
        assertFalse(activity.listEditor.isEditing)
        node("editSavedList").assertIsNotEnabled()
    }

    @Test
    fun editModeSurvivesRecreation() {
        val (first) = populate()
        teachable()
        click("editSavedList")
        node("editSavedList").assertIsEnabled()
        val recreated = recreate<TeachableTagsActivity>()
        assertTrue(recreated.listEditor.isEditing)
        assertTrue(exists("remove:$first"))
        assertTrue(exists("drag:$first"))
    }
}
