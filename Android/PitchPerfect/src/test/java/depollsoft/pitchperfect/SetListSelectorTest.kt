package depollsoft.pitchperfect

import android.os.Looper
import android.view.View
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import depollsoft.lib.activity.RichApplication
import depollsoft.pitchperfect.ScreenTestSupport.assertDisplayed
import depollsoft.pitchperfect.ScreenTestSupport.idle
import depollsoft.pitchperfect.lib.Key
import depollsoft.pitchperfect.lib.PitchedSong
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * The set list selector on the Songs tab, and the edit-mode actions that manage the current list.
 *
 * The positions are child views tagged with their list id, so a test finds one the same way a
 * person does: by the name it carries.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = RichApplication::class, qualifiers = "w411dp-h891dp")
class SetListSelectorTest {
    private var controller: ActivityController<PitchPerfectActivity>? = null

    private val model get() = SongsModel.get()

    @Before
    fun setUp() {
        ScreenTestSupport.startFromFirstLaunch()
        ScreenTestSupport.ensureFirebaseApp()
        PurchaseService.areAdsRemoved = true
        resetLists()
    }

    @After
    fun tearDown() {
        resetLists()
        PurchaseService.areAdsRemoved = false
        ScreenTestSupport.finishScreenTest(controller)
    }

    private fun resetLists() {
        val default = SongList(SongsModel.DEFAULT_ID)
        default.name = "Default"
        model.songLists = mapOf(SongsModel.DEFAULT_ID to default)
        model.currentListId = SongsModel.DEFAULT_ID
    }

    private fun launch(): PitchPerfectActivity {
        val created = Robolectric.buildActivity(PitchPerfectActivity::class.java)
        controller = created
        created.setup()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
        return created.get()
    }

    private fun PitchPerfectActivity.goToSongs(): SongListFragment {
        findViewById<View>(R.id.songs_item).performClick()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
        assertEquals("Songs is page 3", 3, findViewById<ViewPager2>(R.id.viewPager).currentItem)
        return supportFragmentManager.fragments.filterIsInstance<SongListFragment>().single()
    }

    private fun SongListFragment.selector(): SetListSelectorView =
        requireView().findViewById(R.id.setListSelector)

    private fun song(title: String): PitchedSong =
        PitchedSong().apply {
            name = title
            key = Key.getMajorKeys()[0]
        }

    private fun songsMenu(activity: PitchPerfectActivity): android.view.Menu {
        val menu = PopupMenu(activity, activity.findViewById<ViewPager2>(R.id.viewPager)).menu
        activity.onCreateOptionsMenu(menu)
        activity.syncSongMenuItems(menu)
        return menu
    }

    // ==================== The part ====================

    @Test
    fun theSelectorShowsMySongsAndTheAddPosition() {
        val activity = launch()
        val fragment = activity.goToSongs()
        val part = fragment.selector()
        assertDisplayed("setListSelector", part)

        val mySongs = part.positionView(SongsModel.DEFAULT_ID)
        assertNotNull("My Songs has a position", mySongs)
        assertEquals("MY SONGS", (mySongs as TextView).text.toString())
        assertTrue("the current list's position reads as selected", mySongs.isSelected)

        val add = part.addPositionView
        assertNotNull("the trailing + is always there", add)
        assertEquals("New set list", add!!.contentDescription)
    }

    @Test
    fun aPositionIsDescribedWithItsSongCount() {
        model.defaultSongList.addSong(song("Blue Skies"))
        val fragment = launch().goToSongs()
        assertEquals(
            "My Songs, 1 song",
            fragment.selector().positionView(SongsModel.DEFAULT_ID)!!.contentDescription,
        )
    }

    @Test
    fun anEmptyListIsDescribedAsHavingNoSongs() {
        val fragment = launch().goToSongs()
        assertEquals(
            "My Songs, no songs",
            fragment.selector().positionView(SongsModel.DEFAULT_ID)!!.contentDescription,
        )
    }

    // ==================== Creating ====================

    @Test
    fun creatingAListSwitchesToItAndShowsTheSetListEmptyState() {
        val activity = launch()
        val fragment = activity.goToSongs()
        fragment.selector().addPositionView!!.performClick()
        idle()

        val dialog =
            activity.supportFragmentManager.findFragmentByTag(SetListNameDialog.FRAGMENT_TAG)
        assertNotNull("the + opens the name prompt", dialog)

        // Naming is the model's job; the prompt's result is what the tab reacts to.
        val id = model.createList("Saturday show")
        activity.supportFragmentManager.setFragmentResult(
            "depollsoft.pitchperfect.songs.setListName",
            android.os.Bundle().apply {
                putString(SetListNameDialog.RESULT_LIST_ID, id)
                putBoolean(SetListNameDialog.RESULT_CREATED, true)
            },
        )
        idle()

        assertEquals(id, model.currentListId)
        assertNotNull(fragment.selector().positionView(id))
        assertTrue(fragment.selector().positionView(id)!!.isSelected)
        assertEquals(
            activity.getString(R.string.NoSongsInSetList),
            fragment.requireView().findViewById<TextView>(R.id.sorryText).text.toString(),
        )
        assertFalse("creating leaves edit mode", fragment.isEditingSongs())
    }

    // ==================== Switching ====================

    @Test
    fun tappingAPositionSwapsTheRows() {
        val other = model.createList("Saturday show")
        model.defaultSongList.addSong(song("Blue Skies"))
        model.songLists[other]!!.addSong(song("Shenandoah"))
        model.songLists[other]!!.addSong(song("Coney Island Baby"))

        val fragment = launch().goToSongs()
        val recycler = fragment.requireView().findViewById<RecyclerView>(R.id.songListView)
        assertEquals(1, recycler.adapter!!.itemCount)

        fragment.selector().positionView(other)!!.performClick()
        idle()

        assertEquals(other, model.currentListId)
        assertEquals(2, recycler.adapter!!.itemCount)
        assertTrue(fragment.selector().positionView(other)!!.isSelected)
        assertFalse(fragment.selector().positionView(SongsModel.DEFAULT_ID)!!.isSelected)
    }

    @Test
    fun theAddButtonTargetsTheCurrentList() {
        val other = model.createList("Saturday show")
        val activity = launch()
        val fragment = activity.goToSongs()
        fragment.selector().positionView(other)!!.performClick()
        idle()

        activity.findViewById<FloatingActionButton>(R.id.addSongButton).performClick()
        idle()
        val started = shadowOf(activity).nextStartedActivityForResult
        assertNotNull("the FAB opens the song editor", started)
        assertEquals(
            AddSongActivity::class.java.name,
            started.intent.component!!.className,
        )
        assertEquals(
            other,
            started.intent.getStringExtra(AddSongActivity.LIST_EXTRA),
        )
    }

    // ==================== Edit mode actions ====================

    @Test
    fun theSetListActionsAppearOnlyWhileEditing() {
        val activity = launch()
        val fragment = activity.goToSongs()
        val menu = songsMenu(activity)

        listOf(
            R.id.addFromListMenuItem,
            R.id.manageListsMenuItem,
        ).forEach { assertFalse(menu.findItem(it).isVisible) }

        fragment.toggleEditingSongs()
        activity.syncSongMenuItems(menu)

        listOf(
            R.id.addFromListMenuItem,
            R.id.manageListsMenuItem,
        ).forEach { assertTrue("$it should be visible in edit mode", menu.findItem(it).isVisible) }
        // The list itself is managed from its selector position and the Set Lists screen.
        assertNull(menu.findItem(R.id.renameSetListMenuItem))
        assertNull(menu.findItem(R.id.deleteSetListMenuItem))
    }

    @Test
    fun switchingToAnEmptyListShowsItsEmptyState() {
        model.defaultSongList.addSong(song("Blue Skies"))
        val other = model.createList("Saturday show")
        val activity = launch()
        val fragment = activity.goToSongs()
        val empty = fragment.requireView().findViewById<TextView>(R.id.sorryText)
        assertEquals(View.GONE, empty.visibility)

        fragment.selector().positionView(other)!!.performClick()
        idle()
        assertEquals("an empty list must say so after a switch", View.VISIBLE, empty.visibility)
        assertEquals(activity.getString(R.string.NoSongsInSetList), empty.text.toString())

        fragment.selector().positionView(SongsModel.DEFAULT_ID)!!.performClick()
        idle()
        assertEquals(View.GONE, empty.visibility)
    }

    @Test
    fun addFromAnotherListIsDisabledWhenNothingIsAddable() {
        val other = model.createList("Saturday show")
        val activity = launch()
        val fragment = activity.goToSongs()
        fragment.selector().positionView(other)!!.performClick()
        fragment.toggleEditingSongs()
        val menu = songsMenu(activity)
        activity.syncSongMenuItems(menu)
        assertFalse(
            "nothing to copy from an empty My Songs",
            menu.findItem(R.id.addFromListMenuItem).isEnabled,
        )

        model.defaultSongList.addSong(song("Blue Skies"))
        activity.syncSongMenuItems(menu)
        assertTrue(menu.findItem(R.id.addFromListMenuItem).isEnabled)
    }

    @Test
    fun longPressingAPositionOffersTheListsOwnActions() {
        val activity = launch()
        val fragment = activity.goToSongs()
        val other = SongsModel.get().createList("Saturday show")
        ScreenTestSupport.idle()

        // My Songs: everything but Delete.
        assertTrue(fragment.selector().positionView(SongsModel.DEFAULT_ID)!!.performLongClick())
        ScreenTestSupport.idle()
        var popup = org.robolectric.shadows.ShadowPopupMenu.getLatestPopupMenu()
        assertNotNull("a long press shows the list's actions", popup)
        var menu = popup.menu
        assertEquals("the menu names the list it acts on", "My Songs", menu.getItem(0).title.toString())
        assertFalse(menu.getItem(0).isEnabled)
        assertTrue(menu.findItem(R.id.renameSetListMenuItem).isVisible)
        assertTrue(menu.findItem(R.id.duplicateSetListMenuItem).isVisible)
        assertFalse("My Songs cannot be deleted", menu.findItem(R.id.deleteSetListMenuItem).isVisible)
        assertTrue(menu.findItem(R.id.manageSetListsMenuItem).isVisible)
        popup.dismiss()

        // A custom list offers Delete too, and deleting one that is not on screen leaves the
        // tab on My Songs.
        assertTrue(fragment.selector().positionView(other)!!.performLongClick())
        ScreenTestSupport.idle()
        popup = org.robolectric.shadows.ShadowPopupMenu.getLatestPopupMenu()
        menu = popup.menu
        assertEquals("Saturday show", menu.getItem(0).title.toString())
        assertTrue(menu.findItem(R.id.deleteSetListMenuItem).isVisible)
        popup.dismiss()
        val deleted = SongsModel.get().songLists[other]!!
        fragment.deleteList(other)
        ScreenTestSupport.idle()
        assertNull(fragment.selector().positionView(other))
        assertEquals(SongsModel.DEFAULT_ID, SongsModel.get().currentListId)
        assertTrue(fragment.selector().positionView(SongsModel.DEFAULT_ID)!!.isSelected)

        // The Snackbar's Undo brings the list back under the same id.
        SongsModel.get().restoreList(deleted)
        ScreenTestSupport.idle()
        assertNotNull(fragment.selector().positionView(other))
        assertFalse(deleted.isDeleted)
    }

    @Test
    fun deletingTheCurrentListReturnsToMySongs() {
        val other = model.createList("Saturday show")
        val activity = launch()
        val fragment = activity.goToSongs()
        fragment.selector().positionView(other)!!.performClick()
        fragment.toggleEditingSongs()
        idle()

        fragment.deleteCurrentList()
        idle()

        assertEquals(SongsModel.DEFAULT_ID, model.currentListId)
        assertNull("the deleted position is gone", fragment.selector().positionView(other))
        assertTrue(fragment.selector().positionView(SongsModel.DEFAULT_ID)!!.isSelected)
        assertFalse("deleting leaves edit mode", fragment.isEditingSongs())
        assertEquals(
            activity.getString(R.string.NoSongsInList),
            fragment.requireView().findViewById<TextView>(R.id.sorryText).text.toString(),
        )
    }

    @Test
    fun duplicatingSwitchesToTheCopyAndStaysInEditMode() {
        val other = model.createList("Saturday show")
        model.songLists[other]!!.addSong(song("Blue Skies"))
        val activity = launch()
        val fragment = activity.goToSongs()
        fragment.selector().positionView(other)!!.performClick()
        fragment.toggleEditingSongs()
        idle()

        fragment.duplicateCurrentList()
        idle()

        val copy = model.songLists.values.single { model.displayName(it) == "Saturday show copy" }
        assertEquals(copy.id, model.currentListId)
        assertEquals(1, copy.songs.size)
        assertTrue("duplicating stays in edit mode", fragment.isEditingSongs())
    }

    @Test
    fun theManageAndPickerActionsOpenTheirScreens() {
        model.defaultSongList.addSong(song("Blue Skies"))
        val other = model.createList("Saturday show")
        val activity = launch()
        val fragment = activity.goToSongs()
        fragment.selector().positionView(other)!!.performClick()
        idle()

        fragment.openManageSetLists()
        idle()
        assertEquals(
            ManageSetListsActivity::class.java.name,
            shadowOf(activity).nextStartedActivity.component!!.className,
        )

        fragment.openAddSongsFromList()
        idle()
        val picker = shadowOf(activity).nextStartedActivity
        assertEquals(AddSongsFromListActivity::class.java.name, picker.component!!.className)
        assertEquals(other, picker.getStringExtra(AddSongsFromListActivity.LIST_EXTRA))
    }
}
