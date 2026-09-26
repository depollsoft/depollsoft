package depollsoft.pitchperfect.screenshots

import android.content.Intent
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ApplicationProvider
import depollsoft.lib.activity.RichApplication
import depollsoft.lib.testing.SemanticsSnapshot
import depollsoft.pitchperfect.AddSongActivity
import depollsoft.pitchperfect.AddSongsFromListActivity
import depollsoft.pitchperfect.ManageSetListsActivity
import depollsoft.pitchperfect.SettingsActivity
import depollsoft.pitchperfect.SongsModel
import depollsoft.pitchperfect.lib.Key
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.launch
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.launchMain
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.openDeleteSetListDialog
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.openNewSetListDialog
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.openSetListMenu
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.setEditingSongs
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.settle
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.showLoginPrompt
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.showTab
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.tickAddableSong
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * What a screen reader hears and can do on each screen, pinned like the pixel goldens
 * (`src/test/semantics`). A lost label, role, state, custom action or live region fails here even
 * when the pixels are unchanged.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = RichApplication::class, qualifiers = ScreenshotSupport.PHONE)
class SemanticsSnapshotTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Before
    fun setUp() = ScreenshotSupport.setUp(compose)

    @After
    fun tearDown() = ScreenshotSupport.tearDown()

    private fun verify(name: String) {
        settle()
        SemanticsSnapshot.verify(compose, name)
    }

    private fun intent(type: Class<*>) = Intent(ApplicationProvider.getApplicationContext(), type)

    @Test
    fun pitchPipe() {
        launchMain()
        verify("main_pitch_pipe")
    }

    @Test
    fun notes() {
        launchMain().showTab(1)
        verify("main_notes")
    }

    @Test
    fun keys() {
        launchMain().showTab(2)
        verify("main_keys")
    }

    @Test
    fun songsWithSetLists() {
        ScreenshotSupport.seedMySongs()
        ScreenshotSupport.seedSetLists()
        launchMain().showTab(3)
        verify("main_songs")
    }

    @Test
    fun songsEditing() {
        ScreenshotSupport.seedMySongs()
        ScreenshotSupport.seedSetLists()
        val activity = launchMain()
        activity.showTab(3)
        activity.setEditingSongs(true)
        verify("main_songs_editing")
    }

    @Test
    fun songsEmpty() {
        launchMain().showTab(3)
        verify("main_songs_empty")
    }

    @Test
    fun newSetListDialog() {
        val activity = launchMain()
        activity.showTab(3)
        activity.openNewSetListDialog()
        verify("dialog_new_set_list")
    }

    @Test
    fun deleteSetListDialog() {
        val ids = ScreenshotSupport.seedSetLists()
        val activity = launchMain()
        activity.showTab(3)
        activity.openDeleteSetListDialog(ids[0])
        verify("dialog_delete_set_list")
    }

    @Test
    fun setListMenu() {
        val ids = ScreenshotSupport.seedSetLists()
        val activity = launchMain()
        activity.showTab(3)
        activity.openSetListMenu(ids[0])
        verify("menu_set_list")
    }

    @Test
    fun manageSetLists() {
        ScreenshotSupport.seedMySongs()
        ScreenshotSupport.seedSetLists()
        launch(ManageSetListsActivity::class.java)
        verify("manage_set_lists")
    }

    @Test
    fun editSong() {
        val song = ScreenshotSupport.song("Shenandoah", Key.getMinorKeys()[3])
        SongsModel.get().defaultSongList.addSong(song)
        launch(
            AddSongActivity::class.java,
            intent(AddSongActivity::class.java)
                .putExtra(AddSongActivity.LIST_EXTRA, SongsModel.DEFAULT_ID)
                .putExtra(AddSongActivity.ID_EXTRA, song.id),
        )
        verify("edit_song")
    }

    @Test
    fun addFromList() {
        ScreenshotSupport.seedMySongs()
        val ids = ScreenshotSupport.seedSetLists()
        val activity =
            launch(
                AddSongsFromListActivity::class.java,
                intent(AddSongsFromListActivity::class.java).putExtra(AddSongsFromListActivity.LIST_EXTRA, ids[1]),
            )
        activity.tickAddableSong("Heart of My Heart")
        verify("add_from_list")
    }

    @Test
    fun settings() {
        launch(SettingsActivity::class.java)
        verify("settings")
    }

    @Test
    fun loginPrompt() {
        launch(SettingsActivity::class.java).showLoginPrompt()
        verify("dialog_login_prompt")
    }
}
