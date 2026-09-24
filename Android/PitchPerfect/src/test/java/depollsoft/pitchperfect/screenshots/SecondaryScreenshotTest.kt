package depollsoft.pitchperfect.screenshots

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import depollsoft.lib.activity.RichApplication
import depollsoft.pitchperfect.AddSongActivity
import depollsoft.pitchperfect.AddSongsFromListActivity
import depollsoft.pitchperfect.ManageSetListsActivity
import depollsoft.pitchperfect.SettingsActivity
import depollsoft.pitchperfect.SongsModel
import depollsoft.pitchperfect.lib.Key
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.capture
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.chooseMinorKeys
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.launch
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.pressSaveSong
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.scrollSettingsToEnd
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.tickAddableSong
import org.junit.After
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The screens the main screen opens: the song editor, add-from-list, set lists and settings. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = RichApplication::class, qualifiers = ScreenshotSupport.PHONE)
class SecondaryScreenshotTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Before
    fun setUp() = ScreenshotSupport.setUp(compose)

    @After
    fun tearDown() = ScreenshotSupport.tearDown()

    private fun intent(type: Class<*>) = Intent(ApplicationProvider.getApplicationContext(), type)

    private fun editor(songId: String? = null): AddSongActivity {
        val intent =
            intent(AddSongActivity::class.java)
                .putExtra(AddSongActivity.LIST_EXTRA, SongsModel.DEFAULT_ID)
        if (songId != null) intent.putExtra(AddSongActivity.ID_EXTRA, songId)
        return launch(AddSongActivity::class.java, intent)
    }

    @Test
    fun addSong() = editor().capture("add_song")

    @Test
    fun addSongMissingTitle() {
        val activity = editor()
        activity.pressSaveSong()
        activity.capture("add_song_missing_title")
    }

    @Test
    fun addSongMinorKeys() {
        val activity = editor()
        activity.chooseMinorKeys()
        activity.capture("add_song_minor")
    }

    @Test
    fun editSong() {
        val song = ScreenshotSupport.song("Shenandoah", Key.getMinorKeys()[3])
        SongsModel.get().defaultSongList.addSong(song)
        editor(song.id).capture("edit_song")
    }

    @Test
    @Config(qualifiers = ScreenshotSupport.PHONE_NIGHT)
    fun editSongNight() {
        val song = ScreenshotSupport.song("Sweet Adeline", Key.getMajorKeys()[9])
        SongsModel.get().defaultSongList.addSong(song)
        editor(song.id).capture("night_edit_song")
    }

    @Test
    @Config(qualifiers = ScreenshotSupport.TABLET)
    fun addSongTablet() = editor().capture("tablet_add_song")

    @Test
    fun addFromList() {
        ScreenshotSupport.seedMySongs()
        val ids = ScreenshotSupport.seedSetLists()
        val activity =
            launch(
                AddSongsFromListActivity::class.java,
                intent(AddSongsFromListActivity::class.java)
                    .putExtra(AddSongsFromListActivity.LIST_EXTRA, ids[1]),
            )
        activity.tickAddableSong("Heart of My Heart")
        activity.tickAddableSong("Shenandoah")
        activity.capture("add_from_list")
    }

    @Test
    fun addFromListNothingToAdd() {
        val activity =
            launch(
                AddSongsFromListActivity::class.java,
                intent(AddSongsFromListActivity::class.java)
                    .putExtra(AddSongsFromListActivity.LIST_EXTRA, SongsModel.DEFAULT_ID),
            )
        activity.capture("add_from_list_empty")
    }

    @Test
    @Config(qualifiers = ScreenshotSupport.PHONE_NIGHT)
    fun addFromListNight() {
        ScreenshotSupport.seedMySongs()
        val ids = ScreenshotSupport.seedSetLists()
        val activity =
            launch(
                AddSongsFromListActivity::class.java,
                intent(AddSongsFromListActivity::class.java)
                    .putExtra(AddSongsFromListActivity.LIST_EXTRA, ids[0]),
            )
        activity.tickAddableSong("Blue Skies")
        activity.capture("night_add_from_list")
    }

    @Test
    fun manageSetLists() {
        ScreenshotSupport.seedMySongs()
        val ids = ScreenshotSupport.seedSetLists()
        SongsModel.get().currentListId = ids[0]
        launch(ManageSetListsActivity::class.java).capture("manage_set_lists")
    }

    @Test
    fun manageSetListsOnlyMySongs() = launch(ManageSetListsActivity::class.java).capture("manage_set_lists_only_my_songs")

    @Test
    @Config(qualifiers = ScreenshotSupport.PHONE_NIGHT)
    fun manageSetListsNight() {
        ScreenshotSupport.seedMySongs()
        ScreenshotSupport.seedSetLists()
        launch(ManageSetListsActivity::class.java).capture("night_manage_set_lists")
    }

    @Test
    fun settings() = launch(SettingsActivity::class.java).capture("settings")

    @Test
    @Config(qualifiers = ScreenshotSupport.PHONE_420)
    fun settings420() = launch(SettingsActivity::class.java).capture("dpi420_settings")

    @Test
    fun settingsEnd() {
        val activity = launch(SettingsActivity::class.java)
        activity.scrollSettingsToEnd()
        activity.capture("settings_end")
    }

    @Test
    fun settingsWithOptionsOn() {
        depollsoft.pitchperfect.SettingsModel.toggleNotes = true
        depollsoft.pitchperfect.SettingsModel.wakeLock = true
        depollsoft.pitchperfect.PitchPerfectApplication.themeMode =
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        launch(SettingsActivity::class.java).capture("settings_options_on")
    }

    @Test
    @Config(qualifiers = ScreenshotSupport.PHONE_NIGHT)
    fun settingsNight() = launch(SettingsActivity::class.java).capture("night_settings")

    @Test
    @Config(qualifiers = ScreenshotSupport.TABLET)
    fun settingsTablet() = launch(SettingsActivity::class.java).capture("tablet_settings")
}
