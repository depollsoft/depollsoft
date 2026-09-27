package depollsoft.pitchperfect.screenshots

import android.content.Intent
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ApplicationProvider
import depollsoft.lib.activity.RichApplication
import depollsoft.pitchperfect.AddSongActivity
import depollsoft.pitchperfect.ManageSetListsActivity
import depollsoft.pitchperfect.SettingsActivity
import depollsoft.pitchperfect.SongsModel
import depollsoft.pitchperfect.lib.Key
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.capture
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.captureScreen
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.launch
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.launchMain
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.openDeleteSetListDialog
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.openNewSetListDialog
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.setEditingSongs
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.showTab
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The key screens in the configurations the other goldens don't cover: right to left, 200% font,
 * landscape and tablet. These were recorded from the Compose screens (there is no View rendering
 * to diff against) and pin how each lays out, so a mirroring, wrapping or sizing regression shows.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = RichApplication::class, qualifiers = ScreenshotSupport.PHONE)
class MatrixScreenshotTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Before
    fun setUp() = ScreenshotSupport.setUp(compose)

    @After
    fun tearDown() = ScreenshotSupport.tearDown()

    private fun editSong(name: String) {
        val song = ScreenshotSupport.song("Shenandoah", Key.getMinorKeys()[3])
        SongsModel.get().defaultSongList.addSong(song)
        val intent =
            Intent(ApplicationProvider.getApplicationContext(), AddSongActivity::class.java)
                .putExtra(AddSongActivity.LIST_EXTRA, SongsModel.DEFAULT_ID)
                .putExtra(AddSongActivity.ID_EXTRA, song.id)
        launch(AddSongActivity::class.java, intent).capture(name)
    }

    private fun songsWithSetLists(
        name: String,
        editing: Boolean = false,
    ) {
        ScreenshotSupport.seedMySongs()
        ScreenshotSupport.seedSetLists()
        val activity = launchMain()
        activity.showTab(3)
        if (editing) activity.setEditingSongs(true)
        activity.capture(name)
    }

    private fun manageSetLists(name: String) {
        ScreenshotSupport.seedMySongs()
        ScreenshotSupport.seedSetLists()
        launch(ManageSetListsActivity::class.java).capture(name)
    }

    private fun newSetListDialog(name: String) {
        val activity = launchMain()
        activity.showTab(3)
        activity.openNewSetListDialog()
        captureScreen(name)
    }

    // ==================== Right to left ====================

    @Test
    @Config(qualifiers = RTL)
    fun pitchPipeRtl() = launchMain().capture("rtl_main_pitch_pipe")

    @Test
    @Config(qualifiers = RTL)
    fun keysRtl() {
        val activity = launchMain()
        activity.showTab(2)
        activity.capture("rtl_main_keys_major")
    }

    @Test
    @Config(qualifiers = RTL)
    fun songsEditingRtl() = songsWithSetLists("rtl_main_songs_set_lists_editing", editing = true)

    @Test
    @Config(qualifiers = RTL)
    fun manageSetListsRtl() = manageSetLists("rtl_manage_set_lists")

    @Test
    @Config(qualifiers = RTL)
    fun editSongRtl() = editSong("rtl_edit_song")

    @Test
    @Config(qualifiers = RTL)
    fun settingsRtl() = launch(SettingsActivity::class.java).capture("rtl_settings")

    @Test
    @Config(qualifiers = RTL)
    fun newSetListDialogRtl() = newSetListDialog("rtl_dialog_new_set_list")

    // ==================== 200% font ====================

    @Test
    @Config(fontScale = 2f)
    fun pitchPipeLargeFont() = launchMain().capture("font200_main_pitch_pipe")

    @Test
    @Config(fontScale = 2f)
    fun songsLargeFont() = songsWithSetLists("font200_main_songs_set_lists")

    @Test
    @Config(fontScale = 2f)
    fun editSongLargeFont() = editSong("font200_edit_song")

    @Test
    @Config(fontScale = 2f)
    fun settingsLargeFont() = launch(SettingsActivity::class.java).capture("font200_settings")

    @Test
    @Config(fontScale = 2f)
    fun deleteSetListDialogLargeFont() {
        val ids = ScreenshotSupport.seedSetLists()
        val activity = launchMain()
        activity.showTab(3)
        activity.openDeleteSetListDialog(ids[0])
        captureScreen("font200_dialog_delete_set_list")
    }

    // ==================== Landscape phone ====================

    @Test
    @Config(qualifiers = LANDSCAPE)
    fun pitchPipeLandscape() = launchMain().capture("land_main_pitch_pipe")

    @Test
    @Config(qualifiers = LANDSCAPE)
    fun songsLandscape() = songsWithSetLists("land_main_songs_set_lists")

    @Test
    @Config(qualifiers = LANDSCAPE)
    fun editSongLandscape() = editSong("land_edit_song")

    @Test
    @Config(qualifiers = LANDSCAPE)
    fun settingsLandscape() = launch(SettingsActivity::class.java).capture("land_settings")

    @Test
    @Config(qualifiers = LANDSCAPE)
    fun newSetListDialogLandscape() = newSetListDialog("land_dialog_new_set_list")

    // ==================== Tablet ====================

    @Test
    @Config(qualifiers = ScreenshotSupport.TABLET)
    fun manageSetListsTablet() = manageSetLists("tablet_manage_set_lists")

    @Test
    @Config(qualifiers = ScreenshotSupport.TABLET)
    fun newSetListDialogTablet() = newSetListDialog("tablet_dialog_new_set_list")

    private companion object {
        // Hebrew: right to left with Western digits, so the goldens show layout rather than numerals.
        const val RTL = "iw-ldrtl-w411dp-h891dp-xxhdpi"
        const val LANDSCAPE = "w891dp-h411dp-land-xxhdpi"
    }
}
