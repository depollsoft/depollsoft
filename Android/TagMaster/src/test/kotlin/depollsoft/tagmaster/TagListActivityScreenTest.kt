package depollsoft.tagmaster

import android.app.Application
import android.content.Intent
import android.view.Menu
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import com.bindroid.trackable.TrackableCollection
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import depollsoft.tagmaster.ScreenTestSupport.assertDisplayed
import depollsoft.tagmaster.ScreenTestSupport.idle
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

/**
 * One of the user's own lists: [TagListActivity] over an arbitrary [ListModel] key, its title, its
 * empty state, and the two actions a built-in list does not have — rename and delete.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28], qualifiers = "w411dp-h891dp")
class TagListActivityScreenTest {
    private var controller: ActivityController<TagListActivity>? = null
    private val fixture = ScreenTestSupport.fixtureTag()

    @Before
    fun setUp() {
        ScreenTestSupport.startClean()
        ScreenTestSupport.clearTagCaches()
        ScreenTestSupport.cacheOnDisk(fixture)
    }

    @After
    fun tearDown() {
        ScreenTestSupport.finish(controller)
        ScreenTestSupport.clearTagCaches()
    }

    private fun newList(name: String = "Afterglow set"): String = TagLists.create(name)

    private fun launch(key: String): TagListActivity {
        val intent =
            Intent(RuntimeEnvironment.getApplication(), TagListActivity::class.java)
                .putExtra(TagListActivity.EXTRA_LIST_KEY, key)
        val created = ScreenTestSupport.build(TagListActivity::class.java, intent)
        controller = created
        created.setup()
        idle()
        return created.get()
    }

    private fun TagListActivity.menu(): Menu {
        val menu = PopupMenu(this, findViewById(android.R.id.content)).menu
        menuInflater.inflate(R.menu.savedlistmenu, menu)
        menuInflater.inflate(R.menu.taglistmenu, menu)
        return menu
    }

    private fun TagListActivity.choose(itemId: Int) {
        assertTrue("menu item should be handled", onOptionsItemSelected(menu().findItem(itemId)))
        idle()
    }

    private fun latestDialog(): AlertDialog =
        requireNotNull(ShadowDialog.getLatestDialog() as? AlertDialog) { "a dialog should be showing" }

    // ==================== Title and contents ====================

    @Test
    fun theScreenIsTitledWithTheListsOwnName() {
        val key = newList()
        val activity = launch(key)
        assertEquals("Afterglow set", activity.supportActionBar!!.title.toString())
        assertEquals("Afterglow set", activity.listName)
    }

    @Test
    fun anEmptyListExplainsItselfAndOffersTheCatalog() {
        val key = newList()
        val activity = launch(key)
        assertDisplayed("the empty state", activity.findViewById(R.id.tagListEmptyState))
        assertEquals(
            activity.getString(R.string.list_empty_title, "Afterglow set"),
            activity.findViewById<TextView>(R.id.tagListEmptyText).text.toString(),
        )
        activity.findViewById<View>(R.id.tagListBrowseButton).performClick()
        idle()
        assertEquals(
            TagBrowserActivity::class.java.name,
            requireNotNull(shadowOf(activity).nextStartedActivity).component!!.className,
        )
    }

    @Test
    fun aTagInTheListIsShownAndHidesTheEmptyState() {
        val key = newList()
        ListModel(key).add(fixture.id)
        val activity = launch(key)
        assertEquals(1, activity.listAdapter.itemCount)
        assertFalse("the empty state stands down", activity.findViewById<View>(R.id.tagListEmptyState).isShown)

        val list = activity.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.tagListItemsControl)
        list.scrollToPosition(0)
        idle()
        val row = (list.findViewHolderForAdapterPosition(0) as SavedTagListAdapter.Holder).row
        ScreenTestSupport.await("the row to resolve its tag") { !row.isLoading }
        assertEquals(fixture.id, row.tagId)
        assertEquals(fixture.title, row.findViewById<TextView>(R.id.titleTextView).text.toString())
    }

    // ==================== Rename ====================

    @Test
    fun aRenameAnywhereRetitlesTheScreenAndItsEmptyState() {
        val key = newList()
        val activity = launch(key)
        TagLists.rename(key, "Chorus warmups")
        idle()
        assertEquals("Chorus warmups", activity.supportActionBar!!.title.toString())
        assertEquals(
            activity.getString(R.string.list_empty_title, "Chorus warmups"),
            activity.findViewById<TextView>(R.id.tagListEmptyText).text.toString(),
        )
        assertEquals("the spoken list name follows too", "Chorus warmups", activity.listEditor.listLabel)
    }

    @Test
    fun theRenameDialogRefusesABuiltInNameWithoutClosing() {
        val key = newList()
        val activity = launch(key)
        activity.choose(R.id.renameListMenuItem)
        val dialog = latestDialog()
        val input = requireNotNull(dialog.findViewById<TextInputEditText>(R.id.listNameInput))
        assertEquals("the dialog opens on the current name", "Afterglow set", input.text.toString())

        input.setText("Favorites")
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        idle()
        assertTrue("a rejected name keeps the dialog open", dialog.isShowing)
        assertEquals(
            activity.getString(R.string.list_name_error_reserved),
            dialog.findViewById<TextInputLayout>(R.id.listNameLayout)!!.error.toString(),
        )
        assertEquals("nothing was renamed", "Afterglow set", TagLists.name(key))

        input.setText("  Afterglow   Set  ")
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        idle()
        assertFalse("an accepted name closes the dialog", dialog.isShowing)
        assertEquals("the name is trimmed and collapsed", "Afterglow Set", TagLists.name(key))
        assertEquals("Afterglow Set", activity.supportActionBar!!.title.toString())
    }

    // ==================== Delete ====================

    @Test
    fun deletingSaysHowManyTagsGoWithTheListAndClosesTheScreen() {
        val key = newList()
        ListModel(key).ids = TrackableCollection(mutableListOf(fixture.id, fixture.id + 1))
        val activity = launch(key)
        activity.choose(R.id.deleteListMenuItem)
        val dialog = latestDialog()
        assertTrue("the confirmation is open", dialog.isShowing)
        val message = dialog.findViewById<TextView>(android.R.id.message)!!.text.toString()
        assertTrue("the confirmation counts the tags: $message", message.contains("2 tags"))

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        idle()
        assertTrue("the list is gone", TagLists.customKeys.isEmpty())
        assertTrue("and so is the screen onto it", activity.isFinishing)
    }

    @Test
    fun anEmptyListSaysSoInsteadOfCountingNothing() {
        val key = newList()
        val activity = launch(key)
        activity.choose(R.id.deleteListMenuItem)
        assertEquals(
            activity.getString(R.string.list_delete_message_empty),
            latestDialog().findViewById<TextView>(android.R.id.message)!!.text.toString(),
        )
    }

    @Test
    fun cancellingTheConfirmationKeepsTheList() {
        val key = newList()
        val activity = launch(key)
        activity.choose(R.id.deleteListMenuItem)
        latestDialog().getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
        idle()
        assertEquals(listOf(key), TagLists.customKeys.toList())
        assertFalse(activity.isFinishing)
    }

    // ==================== Lists that stop existing ====================

    @Test
    fun aListDeletedFromAnotherDeviceClosesTheScreen() {
        val key = newList()
        val activity = launch(key)
        assertFalse(activity.isFinishing)
        TagLists.delete(key)
        idle()
        assertTrue("the screen closes rather than showing a list that is gone", activity.isFinishing)
    }

    @Test
    fun anUnknownKeyNeverOpensAScreen() {
        val activity = launch("no-such-list-abcd")
        assertTrue(activity.isFinishing)
    }

    // ==================== Editing and recreation ====================

    @Test
    fun editModeAndTheListSurviveRecreation() {
        val key = newList()
        ListModel(key).add(fixture.id)
        val activity = launch(key)
        val menu = activity.menu()
        activity.listEditor.selectMenu(menu.findItem(R.id.editSavedList))
        idle()
        assertTrue(activity.listEditor.isEditing)

        val created = requireNotNull(controller)
        created.recreate()
        idle()
        val restored = created.get()
        assertTrue("editing survives recreation", restored.listEditor.isEditing)
        assertEquals("Afterglow set", restored.supportActionBar!!.title.toString())
        assertEquals(listOf(fixture.id), ListModel(key).ids.toList())
    }

    @Test
    fun theScreenIsAListNotADetailPaneOnAPhone() {
        val activity = launch(newList())
        assertFalse(activity.hasDetailPane)
        assertNotNull(activity.findViewById<View>(R.id.tagListItemsControl))
    }
}
