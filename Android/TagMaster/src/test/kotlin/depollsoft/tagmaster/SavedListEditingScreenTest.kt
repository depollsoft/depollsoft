package depollsoft.tagmaster

import android.app.Application
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
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
@Config(application = Application::class, qualifiers = "w411dp-h891dp-xxhdpi")
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

    /** Alt+Down and Alt+Up on a focused drag handle move its row, as on the View screens. */
    @OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
    @Test
    fun altArrowsOnAHandleMoveItsRow() {
        val (first, second, third) = populate()
        teachable()
        click("editSavedList")
        node("drag:$first").requestFocus()
        node("drag:$first").performKeyInput { withKeyDown(Key.AltLeft) { pressKey(Key.DirectionDown) } }
        idle()
        assertEquals(listOf(second, first, third), TeachableTagsModel.teachableTagIds.toList())
        node("drag:$first").performKeyInput { withKeyDown(Key.AltLeft) { pressKey(Key.DirectionUp) } }
        idle()
        assertEquals(listOf(first, second, third), TeachableTagsModel.teachableTagIds.toList())
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
        compose
            .onAllNodes(
                androidx.compose.ui.test.hasAnyAncestor(androidx.compose.ui.test.hasTestTag("savedTag:$first")) and
                    androidx.compose.ui.test.hasClickAction(),
                useUnmergedTree = true,
            ).onFirst()
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        idle()
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

    private fun populateMany(count: Int): List<Int> =
        (0 until count).map { index ->
            val id = 2147482000 + index
            ScreenTestSupport.cacheOnDisk(ScreenTestSupport.fixtureTag().apply { this.id = id; title = "Fixture %02d".format(index) })
            TeachableTagsModel.addTeachableTag(id)
            id
        }

    @Test
    fun aRowHeldAtTheBottomEdgeScrollsTheListAndTravelsPastTheVisibleRows() {
        val all = populateMany(30)
        teachable()
        click("editSavedList")
        val listBottom = node("teachableList").fetchSemanticsNode().boundsInRoot.bottom
        val handle = node("drag:${all[0]}").fetchSemanticsNode().boundsInRoot
        val travel = listBottom - handle.center.y - 20f
        node("drag:${all[0]}").performTouchInput {
            down(center)
            repeat(20) { moveBy(androidx.compose.ui.geometry.Offset(0f, travel / 20f)) }
        }
        idle()
        node("drag:${all[0]}").performTouchInput { up() }
        idle()
        val index = TeachableTagsModel.teachableTagIds.indexOf(all[0])
        assertTrue("held at the edge, the row rides the scroll to the end, got index $index", index >= 25)
        assertEquals(30, TeachableTagsModel.teachableTagIds.size)
    }

    @Test
    fun draggingTheTopVisibleRowDownKeepsTheListWhereItIs() {
        val all = populateMany(30)
        teachable()
        click("editSavedList")
        val listTop = node("teachableList").fetchSemanticsNode().boundsInRoot.top
        val firstTop = node("savedTag:${all[0]}").fetchSemanticsNode().boundsInRoot.top
        val rowHeight = node("savedTag:${all[0]}").fetchSemanticsNode().size.height.toFloat()
        node("drag:${all[0]}").performTouchInput {
            down(center)
            repeat(6) { moveBy(androidx.compose.ui.geometry.Offset(0f, rowHeight / 4f)) }
        }
        idle()
        // The row it passed now sits where the dragged row started: the list did not scroll after it.
        val passedTop = node("savedTag:${all[1]}").fetchSemanticsNode().boundsInRoot.top
        assertEquals(firstTop, passedTop, 1f)
        assertTrue(passedTop >= listTop)
        node("drag:${all[0]}").performTouchInput { up() }
        idle()
        assertEquals(listOf(all[1], all[0]), TeachableTagsModel.teachableTagIds.take(2))
    }

    @Test
    fun movingTheTopVisibleRowDownKeepsTheListWhereItIs() {
        val all = populateMany(30)
        teachable()
        click("editSavedList")
        node("teachableList").performScrollToIndex(10)
        idle()
        val top = node("savedTag:${all[10]}").fetchSemanticsNode().boundsInRoot.top
        customAction("savedTag:${all[10]}", string(R.string.MoveDown))
        // Without holding by index, the list would scroll after the moved row and bury the one
        // that took its place.
        assertEquals(top, node("savedTag:${all[11]}").fetchSemanticsNode().boundsInRoot.top, 1f)
        assertTrue(node("savedTag:${all[10]}").fetchSemanticsNode().boundsInRoot.top > top)
    }

    @Test
    fun leavingTheScreenDropsTheRemoveConfirmation() {
        val (_, second) = populate()
        val activity = teachable()
        click("editSavedList")
        click("remove:$second")
        assertTrue(exists("dialog"))
        controller!!.pause()
        idle()
        controller!!.resume()
        idle()
        assertNull(activity.listEditor.pendingRemoval)
        assertFalse(exists("dialog"))
        assertEquals(3, TeachableTagsModel.teachableTagIds.size)
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
