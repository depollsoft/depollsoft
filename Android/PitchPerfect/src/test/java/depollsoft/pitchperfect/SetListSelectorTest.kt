package depollsoft.pitchperfect

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import depollsoft.lib.activity.RichApplication
import depollsoft.pitchperfect.ComposeScreens.Companion.song
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** The Songs tab's set list selector and the list actions around it. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = RichApplication::class, qualifiers = "w411dp-h891dp")
class SetListSelectorTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val screens = ComposeScreens(compose)
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
        screens.finish()
    }

    private fun resetLists() {
        val default = SongList(SongsModel.DEFAULT_ID)
        default.name = "Default"
        model.songLists = mapOf(SongsModel.DEFAULT_ID to default)
        model.currentListId = SongsModel.DEFAULT_ID
    }

    private fun songs(): PitchPerfectActivity =
        screens.launchMain().also { activity ->
            with(screens) { activity.show(MainTab.SONGS) }
            assertEquals("Songs is page 3", MainTab.SONGS.ordinal, activity.pager.currentPage)
        }

    private fun position(listId: String) = TestTags.setListPosition(listId)

    private fun select(listId: String) = screens.click(position(listId))

    private fun rows(list: SongList): Int = list.songs.count { screens.exists(TestTags.songRow(it.id)) }

    // ==================== The part ====================

    @Test
    fun theSelectorShowsMySongsAndTheAddPosition() {
        songs()
        assertTrue(screens.exists(TestTags.SET_LIST_SELECTOR))
        assertEquals("MY SONGS", screens.text(position(SongsModel.DEFAULT_ID)))
        assertTrue("the current list's position reads as selected", screens.isSelected(position(SongsModel.DEFAULT_ID)))
        assertEquals("New set list", screens.description(TestTags.SET_LIST_ADD_POSITION))
    }

    @Test
    fun aPositionIsDescribedWithItsSongCount() {
        model.defaultSongList.addSong(song("Blue Skies"))
        songs()
        assertEquals("My Songs, 1 song", screens.description(position(SongsModel.DEFAULT_ID)))
    }

    @Test
    fun anEmptyListIsDescribedAsHavingNoSongs() {
        songs()
        assertEquals("My Songs, no songs", screens.description(position(SongsModel.DEFAULT_ID)))
    }

    // ==================== Creating ====================

    @Test
    fun creatingAListSwitchesToItAndShowsTheSetListEmptyState() {
        val activity = songs()
        activity.songs.toggleEditing()
        screens.click(TestTags.SET_LIST_ADD_POSITION)
        assertTrue("the + opens the name prompt", screens.exists(TestTags.NAME_DIALOG_FIELD))

        compose.onNodeWithTag(TestTags.NAME_DIALOG_FIELD).performTextReplacement("Saturday show")
        screens.click(TestTags.NAME_DIALOG_CONFIRM)

        val id = model.songLists.values.single { it.name == "Saturday show" }.id
        assertEquals(id, model.currentListId)
        assertTrue(screens.isSelected(position(id)))
        assertEquals(activity.getString(R.string.NoSongsInSetList), screens.text(TestTags.SONGS_EMPTY))
        assertFalse("creating leaves edit mode", activity.songs.editing)
        assertFalse("the prompt closes", screens.exists(TestTags.NAME_DIALOG_FIELD))
    }

    @Test
    fun aRejectedNameKeepsThePromptOpenWithTheReason() {
        model.createList("Saturday show")
        val activity = songs()
        screens.click(TestTags.SET_LIST_ADD_POSITION)
        compose.onNodeWithTag(TestTags.NAME_DIALOG_FIELD).performTextReplacement("saturday  SHOW")
        screens.click(TestTags.NAME_DIALOG_CONFIRM)
        assertTrue(screens.exists(TestTags.NAME_DIALOG_FIELD))
        compose.onNodeWithText(activity.getString(R.string.SetListNameErrorDuplicate)).assertExists()
        assertEquals(2, model.songLists.size)
    }

    // ==================== Switching ====================

    @Test
    fun tappingAPositionSwapsTheRows() {
        val other = model.createList("Saturday show")
        model.defaultSongList.addSong(song("Blue Skies"))
        model.songLists[other]!!.addSong(song("Shenandoah"))
        model.songLists[other]!!.addSong(song("Coney Island Baby"))
        songs()
        assertEquals(1, rows(model.defaultSongList))

        select(other)

        assertEquals(other, model.currentListId)
        assertEquals(2, rows(model.songLists[other]!!))
        assertEquals(0, rows(model.defaultSongList))
        assertTrue(screens.isSelected(position(other)))
        assertFalse(screens.isSelected(position(SongsModel.DEFAULT_ID)))
    }

    @Test
    fun theAddButtonTargetsTheCurrentList() {
        val other = model.createList("Saturday show")
        val activity = songs()
        select(other)

        screens.click(TestTags.ADD_SONG_FAB)
        val started = shadowOf(activity).nextStartedActivity
        assertNotNull("the FAB opens the song editor", started)
        assertEquals(AddSongActivity::class.java.name, started.component!!.className)
        assertEquals(other, started.getStringExtra(AddSongActivity.LIST_EXTRA))
    }

    // ==================== Edit mode actions ====================

    @Test
    fun theSetListActionsAppearOnlyWhileEditing() {
        songs()
        assertFalse(screens.exists(TestTags.OVERFLOW))
        assertFalse(screens.exists(TestTags.SORT_SONGS))

        screens.click(TestTags.EDIT_SONGS)
        assertTrue(screens.exists(TestTags.SORT_SONGS))
        screens.click(TestTags.OVERFLOW)
        assertTrue(screens.exists(TestTags.ADD_FROM_LIST))
        assertTrue(screens.exists(TestTags.MANAGE_LISTS))
    }

    @Test
    fun eachListKeepsItsOwnScrollPosition() {
        repeat(40) { model.defaultSongList.addSong(song("Song $it")) }
        val other = model.createList("Saturday show")
        repeat(40) { model.songLists[other]!!.addSong(song("Other $it")) }
        val activity = songs()
        fun top(listId: String) = activity.songs.scrollStateFor(listId).firstVisibleItemIndex

        compose.runOnIdle { kotlinx.coroutines.runBlocking { activity.songs.scrollStateFor(SongsModel.DEFAULT_ID).scrollToItem(30) } }
        screens.settle()
        assertTrue("My Songs is scrolled down", top(SongsModel.DEFAULT_ID) >= 20)

        select(other)
        assertEquals("a list shown for the first time starts at the top", 0, top(other))

        compose.runOnIdle { kotlinx.coroutines.runBlocking { activity.songs.scrollStateFor(other).scrollToItem(35) } }
        screens.settle()
        select(SongsModel.DEFAULT_ID)
        assertTrue("My Songs comes back where it was left", top(SongsModel.DEFAULT_ID) >= 20)
        assertTrue(screens.exists(TestTags.songRow(model.defaultSongList.songs[30].id)))

        select(other)
        assertTrue("and so does the other list", top(other) >= 25)
    }

    @Test
    fun switchingToAnEmptyListShowsItsEmptyState() {
        model.defaultSongList.addSong(song("Blue Skies"))
        val other = model.createList("Saturday show")
        val activity = songs()
        assertFalse(screens.exists(TestTags.SONGS_EMPTY))

        select(other)
        assertEquals(activity.getString(R.string.NoSongsInSetList), screens.text(TestTags.SONGS_EMPTY))

        select(SongsModel.DEFAULT_ID)
        assertFalse(screens.exists(TestTags.SONGS_EMPTY))
    }

    @Test
    fun addFromAnotherListIsDisabledWhenNothingIsAddable() {
        val other = model.createList("Saturday show")
        songs()
        select(other)
        screens.click(TestTags.EDIT_SONGS)
        screens.click(TestTags.OVERFLOW)
        compose.onNodeWithTag(TestTags.ADD_FROM_LIST).assertIsNotEnabled()
        compose.onNodeWithText("Nothing to add from other set lists").assertExists()

        model.defaultSongList.addSong(song("Blue Skies"))
        screens.settle()
        compose.onNodeWithTag(TestTags.ADD_FROM_LIST).assertIsEnabled()
    }

    @Test
    fun longPressingAPositionOffersTheListsOwnActions() {
        val activity = songs()
        val other = model.createList("Saturday show")
        screens.settle()

        // My Songs: everything but Delete.
        compose.onNodeWithTag(position(SongsModel.DEFAULT_ID)).performTouchInput { longClick() }
        screens.settle()
        compose.onNodeWithText("My Songs", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Rename set list…").assertExists()
        compose.onNodeWithText("Duplicate set list").assertExists()
        compose.onNodeWithText("Manage set lists…").assertExists()
        compose.onNodeWithText("Delete set list…").assertDoesNotExist()
        activity.songs.menuFor = null
        screens.settle()

        // A custom list offers Delete too, and deleting one not on screen leaves the tab on My Songs.
        compose.onNodeWithTag(position(other)).performTouchInput { longClick() }
        screens.settle()
        compose.onNodeWithText("Delete set list…").performClick()
        screens.settle()
        assertEquals(other, activity.songs.pendingDelete)
        compose.onNodeWithText("DELETE").performClick()
        screens.settle()
        assertFalse(screens.exists(position(other)))
        assertEquals(SongsModel.DEFAULT_ID, model.currentListId)
        assertTrue(screens.isSelected(position(SongsModel.DEFAULT_ID)))

        // The snackbar's Undo brings the list back under the same id.
        compose.onNodeWithText("Undo", ignoreCase = true).performClick()
        screens.settle()
        assertTrue(screens.exists(position(other)))
        assertFalse(model.songLists[other]!!.isDeleted)
    }

    @Test
    fun deletingTheCurrentListReturnsToMySongs() {
        val other = model.createList("Saturday show")
        val activity = songs()
        select(other)
        screens.click(TestTags.EDIT_SONGS)

        activity.songs.deleteList(other, { it }, "Undo")
        screens.settle()

        assertEquals(SongsModel.DEFAULT_ID, model.currentListId)
        assertFalse("the deleted position is gone", screens.exists(position(other)))
        assertTrue(screens.isSelected(position(SongsModel.DEFAULT_ID)))
        assertFalse("deleting leaves edit mode", activity.songs.editing)
        assertEquals(activity.getString(R.string.NoSongsInList), screens.text(TestTags.SONGS_EMPTY))
    }

    @Test
    fun duplicatingSwitchesToTheCopyAndStaysInEditMode() {
        val other = model.createList("Saturday show")
        model.songLists[other]!!.addSong(song("Blue Skies"))
        val activity = songs()
        select(other)
        screens.click(TestTags.EDIT_SONGS)

        compose.onNodeWithTag(position(other)).performTouchInput { longClick() }
        screens.settle()
        compose.onNodeWithText("Duplicate set list").performClick()
        screens.settle()

        val copy = model.songLists.values.single { model.displayName(it) == "Saturday show copy" }
        assertEquals(copy.id, model.currentListId)
        assertEquals(1, copy.songs.size)
        assertTrue("duplicating stays in edit mode", activity.songs.editing)
        compose.onNodeWithText("Duplicated as Saturday show copy").assertExists()
    }

    @Test
    fun theManageAndPickerActionsOpenTheirScreens() {
        model.defaultSongList.addSong(song("Blue Skies"))
        val other = model.createList("Saturday show")
        val activity = songs()
        select(other)
        screens.click(TestTags.EDIT_SONGS)

        screens.click(TestTags.OVERFLOW)
        screens.click(TestTags.MANAGE_LISTS)
        assertEquals(ManageSetListsActivity::class.java.name, shadowOf(activity).nextStartedActivity.component!!.className)

        screens.click(TestTags.OVERFLOW)
        screens.click(TestTags.ADD_FROM_LIST)
        val picker = shadowOf(activity).nextStartedActivity
        assertEquals(AddSongsFromListActivity::class.java.name, picker.component!!.className)
        assertEquals(other, picker.getStringExtra(AddSongsFromListActivity.LIST_EXTRA))
    }
}
