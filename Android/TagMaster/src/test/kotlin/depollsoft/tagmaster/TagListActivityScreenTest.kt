package depollsoft.tagmaster

import android.app.Application
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** One custom list: its title and empty state, renaming and deleting it, and closing when it goes away. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w411dp-h891dp-xxhdpi")
class TagListActivityScreenTest : ComposeScreenTest() {
    private val fixture = ScreenTestSupport.fixtureTag()

    private fun open(key: String): TagListActivity = launch(TagListActivity::class.java, TagListActivity.intent(app, key))

    private fun overflow(item: String) {
        click("overflowMenu")
        click(item)
    }

    @Test
    fun theScreenIsTitledWithTheListsOwnName() {
        val key = TagLists.create("Afterglow set")
        open(key)
        assertEquals("Afterglow set", text("toolbarTitle"))
    }

    @Test
    fun anEmptyListExplainsItselfAndOffersTheCatalog() {
        val key = TagLists.create("Easy tags")
        val activity = open(key)
        assertTrue(text("tagList").isEmpty() || !exists("savedTag:${fixture.id}"))
        assertTrue(compose.onAllNodes(androidx.compose.ui.test.hasText(string(R.string.list_empty_title, "Easy tags"))).fetchSemanticsNodes().isNotEmpty())
        click("tagListBrowseButton")
        assertEquals(TagBrowserActivity::class.java.name, nextStarted(activity)?.component?.className)
    }

    @Test
    fun aTagInTheListIsShownAndHidesTheEmptyState() {
        ScreenTestSupport.cacheOnDisk(fixture)
        val key = TagLists.create("Afterglow set")
        ListModel(key).add(fixture.id)
        open(key)
        assertTrue(text("savedTag:${fixture.id}").contains(fixture.title!!))
        assertFalse(exists("tagListBrowseButton"))
    }

    @Test
    fun aRenameAnywhereRetitlesTheScreen() {
        val key = TagLists.create("Afterglow set")
        open(key)
        TagLists.rename(key, "Pole cats")
        idle()
        assertEquals("Pole cats", text("toolbarTitle"))
    }

    @Test
    fun theRenameDialogRenamesAndRefusesABuiltInNameWithoutClosing() {
        val key = TagLists.create("Afterglow set")
        open(key)
        overflow("renameList")
        node("listNameInput").performTextClearance()
        node("listNameInput").performTextInput(string(R.string.Favorites))
        click("listNameConfirm")
        assertTrue("the dialog stays open on a rejected name", exists("dialog"))
        assertEquals("Afterglow set", TagLists.name(key))
        node("listNameInput").performTextClearance()
        node("listNameInput").performTextInput("Pole cats")
        click("listNameConfirm")
        assertFalse(exists("dialog"))
        assertEquals("Pole cats", TagLists.name(key))
    }

    @Test
    fun deletingSaysHowManyTagsGoAndClosesTheScreen() {
        val key = TagLists.create("Afterglow set")
        ListModel(key).add(1)
        ListModel(key).add(2)
        val activity = open(key)
        overflow("deleteList")
        assertTrue(text("dialog").contains(app.resources.getQuantityString(R.plurals.list_delete_message, 2, 2)))
        click("listDeleteConfirm")
        assertFalse(TagLists.customKeys.contains(key))
        assertTrue(activity.isFinishing)
    }

    @Test
    fun cancellingTheConfirmationKeepsTheList() {
        val key = TagLists.create("Afterglow set")
        val activity = open(key)
        overflow("deleteList")
        click("dialogButton:${string(R.string.home_cancel)}")
        assertTrue(TagLists.customKeys.contains(key))
        assertFalse(activity.isFinishing)
    }

    @Test
    fun aListDeletedFromAnotherDeviceClosesTheScreen() {
        val key = TagLists.create("Afterglow set")
        val activity = open(key)
        TagLists.delete(key)
        idle()
        assertTrue(activity.isFinishing)
    }

    @Test
    fun anUnknownKeyNeverOpensAScreen() {
        val activity = open("no-such-list")
        assertTrue(activity.isFinishing)
    }

    @Test
    fun editModeAndTheListSurviveRecreation() {
        ScreenTestSupport.cacheOnDisk(fixture)
        val key = TagLists.create("Afterglow set")
        ListModel(key).add(fixture.id)
        open(key).listEditor.toggle()
        idle()
        val recreated = recreate<TagListActivity>()
        assertTrue(recreated.listEditor.isEditing)
        assertTrue(exists("remove:${fixture.id}"))
    }

    @Test
    fun theScreenIsAListNotADetailPaneOnAPhone() {
        val key = TagLists.create("Afterglow set")
        assertFalse(open(key).hasDetailPane)
    }
}
