package depollsoft.pitchperfect

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.Lifecycle
import depollsoft.lib.activity.RichApplication
import depollsoft.pitchperfect.ComposeScreens.Companion.song
import depollsoft.pitchperfect.lib.Key
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The main screen — its tabs, its action bar and the Songs tab — and the song editor it opens.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = RichApplication::class, qualifiers = "w411dp-h891dp")
class MainScreenTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val screens = ComposeScreens(compose)
    private val songs get() = SongsModel.get().defaultSongList.songs

    @Before
    fun setUp() {
        ScreenTestSupport.startFromFirstLaunch()
        ScreenTestSupport.ensureFirebaseApp()
        PurchaseService.areAdsRemoved = true
        SongsModel.get().clearAll()
    }

    @After
    fun tearDown() {
        SongsModel.get().clearAll()
        PurchaseService.areAdsRemoved = false
        screens.finish()
    }

    private fun PitchPerfectActivity.tap(tab: MainTab) = with(screens) { show(tab) }

    private fun PitchPerfectActivity.assertOn(tab: MainTab) {
        assertEquals("${tab.name} is page ${tab.ordinal}", tab.ordinal, pager.currentPage)
        assertTrue("its navigation item reads as selected", screens.isSelected(tab.testTag))
        MainTab.entries.filter { it != tab }.forEach { assertFalse(screens.isSelected(it.testTag)) }
    }

    /** Opens the editor the way the Songs tab's add button does, from the intent it started. */
    private fun openEditor(activity: PitchPerfectActivity): AddSongActivity {
        activity.tap(MainTab.SONGS)
        screens.click(TestTags.ADD_SONG_FAB)
        val started = shadowOf(activity).nextStartedActivity
        assertNotNull("the add button opens the song editor", started)
        assertEquals(AddSongActivity::class.java.name, started.component!!.className)
        return screens.launch(AddSongActivity::class.java, started).get()
    }

    /** A key row in the editor's picker; the Keys tab behind it has rows described alike. */
    private fun editorKey(description: String) =
        compose.onNode(hasContentDescription(description) and hasAnyAncestor(hasTestTag(TestTags.SONG_KEY_LIST)))

    // ==================== Launch and navigation ====================

    @Test
    fun theActivityLaunchesOnThePitchPipe() {
        val activity = screens.launchMain()
        assertEquals(Lifecycle.State.RESUMED, activity.lifecycle.currentState)
        compose.onNodeWithTag(TestTags.PAGER).assertIsDisplayed()
        compose.onNodeWithTag(TestTags.PITCH_INSTRUMENT).assertIsDisplayed()
        activity.assertOn(MainTab.PITCH_PIPE)
    }

    @Test
    fun everyTabIsReachableFromEveryOther() {
        val activity = screens.launchMain()
        for (tab in listOf(MainTab.NOTES, MainTab.KEYS, MainTab.SONGS, MainTab.PITCH_PIPE, MainTab.SONGS, MainTab.KEYS)) {
            activity.tap(tab)
            activity.assertOn(tab)
        }
    }

    @Test
    fun theActivitySurvivesRecreation() {
        val controller = screens.launch(PitchPerfectActivity::class.java)
        controller.get().tap(MainTab.KEYS)
        controller.recreate()
        screens.settle()
        compose.onNodeWithTag(TestTags.PAGER).assertIsDisplayed()
        compose.onNodeWithTag(TestTags.TAB_SONGS).assertIsDisplayed()
    }

    @Test
    fun theAdSlotShowsOnlyWhileAdsAreOn() {
        screens.launchMain()
        assertFalse(screens.exists(TestTags.REMOVE_ADS))
        screens.finish()

        PurchaseService.areAdsRemoved = false
        screens.launchMain()
        assertTrue("the remove-ads link sits above the banner", screens.exists(TestTags.REMOVE_ADS))
        assertTrue(screens.exists(TestTags.AD_CONTAINER))
    }

    // ==================== Action bar ====================

    @Test
    fun theSongActionsFollowThePageAndEditMode() {
        val activity = screens.launchMain()
        assertTrue(screens.exists(TestTags.SETTINGS))
        assertFalse("song actions stay hidden off the Songs page", screens.exists(TestTags.EDIT_SONGS))

        activity.tap(MainTab.SONGS)
        assertEquals(activity.getString(R.string.EditSongList), screens.description(TestTags.EDIT_SONGS))
        assertFalse(screens.exists(TestTags.SORT_SONGS))
        assertFalse("the set list actions live in edit mode", screens.exists(TestTags.OVERFLOW))

        screens.click(TestTags.EDIT_SONGS)
        assertEquals(activity.getString(R.string.StopEditing), screens.description(TestTags.EDIT_SONGS))
        assertTrue(screens.exists(TestTags.SORT_SONGS))
        assertTrue(screens.exists(TestTags.OVERFLOW))

        screens.click(TestTags.EDIT_SONGS)
        activity.tap(MainTab.NOTES)
        assertFalse(screens.exists(TestTags.EDIT_SONGS))
        assertFalse(screens.exists(TestTags.OVERFLOW))
    }

    @Test
    fun settingsOpensTheSettingsScreen() {
        val activity = screens.launchMain()
        screens.click(TestTags.SETTINGS)
        assertEquals(SettingsActivity::class.java.name, shadowOf(activity).nextStartedActivity.component!!.className)
    }

    @Test
    fun sortingOrdersTheCurrentListByTitle() {
        listOf("Shenandoah", "blue skies", "Coney Island Baby").forEach { songs.add(song(it)) }
        val activity = screens.launchMain()
        activity.tap(MainTab.SONGS)
        screens.click(TestTags.EDIT_SONGS)
        screens.click(TestTags.SORT_SONGS)
        assertEquals(listOf("blue skies", "Coney Island Baby", "Shenandoah"), songs.map { it.name })
    }

    // ==================== Songs tab ====================

    @Test
    fun theSongsTabShowsTheSelectorTheRowsAndTheAddButton() {
        songs.add(song("Blue Skies"))
        val activity = screens.launchMain()
        activity.tap(MainTab.SONGS)
        compose.onNodeWithTag(TestTags.SET_LIST_SELECTOR).assertIsDisplayed()
        compose.onNodeWithTag(TestTags.SONG_LIST).assertIsDisplayed()
        compose.onNodeWithTag(TestTags.ADD_SONG_FAB).assertIsDisplayed()
        compose.onNodeWithText("Blue Skies").assertIsDisplayed()
    }

    @Test
    fun aSongAddedWhileTheTabShowsAppearsAtOnce() {
        val activity = screens.launchMain()
        activity.tap(MainTab.SONGS)
        assertTrue(screens.exists(TestTags.SONGS_EMPTY))

        // A song arriving from Firestore goes straight into the list.
        songs.add(song("Synced Song"))
        screens.settle()

        compose.onNodeWithText("Synced Song").assertIsDisplayed()
        assertFalse(screens.exists(TestTags.SONGS_EMPTY))
    }

    @Test
    fun editModeOffersEachSongsEditButtonAndHandle() {
        songs.add(song("Blue Skies"))
        val activity = screens.launchMain()
        activity.tap(MainTab.SONGS)
        assertFalse(screens.exists(TestTags.EDIT_SONG_BUTTON))
        assertFalse(screens.exists(TestTags.DRAG_HANDLE))

        screens.click(TestTags.EDIT_SONGS)
        assertTrue(screens.exists(TestTags.EDIT_SONG_BUTTON))
        assertTrue(screens.exists(TestTags.DRAG_HANDLE))

        screens.click(TestTags.EDIT_SONG_BUTTON)
        val started = shadowOf(activity).nextStartedActivity
        assertEquals(AddSongActivity::class.java.name, started.component!!.className)
        assertEquals(songs[0].id, started.getStringExtra(AddSongActivity.ID_EXTRA))
        assertEquals(SongsModel.DEFAULT_ID, started.getStringExtra(AddSongActivity.LIST_EXTRA))
    }

    @Test
    fun aSongSoundsWhileHeldAndStopsTheOthers() {
        songs.add(song("Blue Skies", Key.getMajorKeys()[2]))
        songs.add(song("Shenandoah", Key.getMajorKeys()[5]))
        val activity = screens.launchMain()
        activity.tap(MainTab.SONGS)
        val first = songs[0]
        val second = songs[1]

        compose.onNodeWithText("Blue Skies").performTouchDown()
        screens.settle()
        assertTrue(first.isPlaying)
        compose.onNodeWithText("Blue Skies").performTouchUp()
        screens.settle()
        assertFalse("a held song falls silent on release", first.isPlaying)

        SettingsModel.toggleNotes = true
        compose.onNodeWithText("Blue Skies").performTap()
        screens.settle()
        assertTrue("in toggle mode a tap latches the song", first.isPlaying)
        compose.onNodeWithText("Shenandoah").performTap()
        screens.settle()
        assertTrue(second.isPlaying)
        assertFalse("sounding one song stops the others", first.isPlaying)
        compose.onNodeWithText("Shenandoah").performTap()
        screens.settle()
        assertFalse("a second tap stops it", second.isPlaying)
    }

    @Test
    fun leavingTheSongsTabSilencesIt() {
        songs.add(song("Blue Skies"))
        SettingsModel.toggleNotes = true
        val activity = screens.launchMain()
        activity.tap(MainTab.SONGS)
        compose.onNodeWithText("Blue Skies").performTap()
        screens.settle()
        assertTrue(songs[0].isPlaying)
        activity.tap(MainTab.KEYS)
        assertFalse(songs[0].isPlaying)
    }

    // ==================== Song editor ====================

    @Test
    fun theEditorOpensForTheCurrentListWithTitleAndKeys() {
        val editor = openEditor(screens.launchMain())
        assertEquals(SongsModel.DEFAULT_ID, editor.intent.getStringExtra(AddSongActivity.LIST_EXTRA))
        compose.onNodeWithTag(TestTags.SONG_TITLE).assertIsDisplayed()
        compose.onNodeWithTag(TestTags.SONG_KEY_LIST).assertIsDisplayed()
        editorKey("C major, no sharps or flats").assertExists()
    }

    @Test
    fun savingAddsTheSongWithItsTitleAndKey() {
        val editor = openEditor(screens.launchMain())
        compose.onNodeWithTag(TestTags.SONG_TITLE).performTextReplacement("  Test Song Title  ")
        editorKey("G major, 1 sharp").performTap()
        screens.settle()
        assertEquals(Lifecycle.State.RESUMED, editor.lifecycle.currentState)
        screens.click(TestTags.SAVE_SONG)

        assertTrue(editor.isFinishing)
        val saved = songs.single()
        assertEquals("the title is trimmed", "Test Song Title", saved.name)
        assertEquals("G", saved.key.friendlyName)
    }

    @Test
    fun savingWithoutATitleKeepsTheEditorOpenAndSaysWhy() {
        val editor = openEditor(screens.launchMain())
        screens.click(TestTags.SAVE_SONG)
        assertFalse(editor.isFinishing)
        compose.onNodeWithText(editor.getString(R.string.SongTitleRequired)).assertExists()
        assertTrue(songs.isEmpty())

        compose.onNodeWithTag(TestTags.SONG_TITLE).performTextReplacement("Blue Skies")
        screens.settle()
        compose.onNodeWithText(editor.getString(R.string.SongTitleRequired)).assertDoesNotExist()
    }

    @Test
    fun editingChangesTheSongOnlyWhenSaved() {
        songs.add(song("Blue Skies", Key.getMajorKeys()[6]))
        val original = songs.single()
        val intent =
            android.content.Intent(androidx.test.core.app.ApplicationProvider.getApplicationContext(), AddSongActivity::class.java)
                .putExtra(AddSongActivity.ID_EXTRA, original.id)
                .putExtra(AddSongActivity.LIST_EXTRA, SongsModel.DEFAULT_ID)
        val editor = screens.launch(AddSongActivity::class.java, intent).get()
        compose.onNodeWithText("Blue Skies").assertExists()

        compose.onNodeWithTag(TestTags.SONG_TITLE).performTextReplacement("Blue Skies (tag)")
        compose.onNodeWithContentDescriptionCompat(editor.getString(R.string.KeyModeMinor)).performTap()
        screens.settle()
        assertEquals("nothing changes before Save", "Blue Skies", original.name)

        screens.click(TestTags.OK_SONG)
        assertEquals("Blue Skies (tag)", original.name)
        assertEquals("flipping the mode keeps the signature", "a", original.key.friendlyName)
        assertEquals(original.id, songs.single().id)
    }

    @Test
    fun removeDeletesTheSongBeingEdited() {
        songs.add(song("Blue Skies"))
        val intent =
            android.content.Intent(androidx.test.core.app.ApplicationProvider.getApplicationContext(), AddSongActivity::class.java)
                .putExtra(AddSongActivity.ID_EXTRA, songs.single().id)
        val editor = screens.launch(AddSongActivity::class.java, intent).get()
        screens.click(TestTags.REMOVE_SONG)
        assertTrue(editor.isFinishing)
        assertTrue(songs.isEmpty())
    }

    @Test
    fun cancelLeavesTheListAlone() {
        val editor = openEditor(screens.launchMain())
        compose.onNodeWithTag(TestTags.SONG_TITLE).performTextReplacement("Blue Skies")
        screens.click(TestTags.CANCEL_SONG)
        assertTrue(editor.isFinishing)
        assertTrue(songs.isEmpty())
        assertNotEquals(android.app.Activity.RESULT_OK, shadowOf(editor).resultCode)
    }
}
