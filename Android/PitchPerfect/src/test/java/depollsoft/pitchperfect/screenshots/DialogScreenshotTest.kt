package depollsoft.pitchperfect.screenshots

import depollsoft.lib.activity.RichApplication
import depollsoft.pitchperfect.SettingsActivity
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.captureScreen
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.launch
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.launchMain
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.openDeleteSetListDialog
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.openNewSetListDialog
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.openRenameSetListDialog
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.openSetListMenu
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.showLoginPrompt
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.showTab
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.submitSetListName
import org.junit.After
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Dialogs, menus and snackbars, captured with every window on screen. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = RichApplication::class, qualifiers = ScreenshotSupport.PHONE)
class DialogScreenshotTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Before
    fun setUp() = ScreenshotSupport.setUp(compose)

    @After
    fun tearDown() = ScreenshotSupport.tearDown()

    @Test
    fun newSetList() {
        val activity = launchMain()
        activity.showTab(3)
        activity.openNewSetListDialog()
        captureScreen("dialog_new_set_list")
    }

    @Test
    fun newSetListRejectedName() {
        ScreenshotSupport.seedSetLists()
        val activity = launchMain()
        activity.showTab(3)
        activity.openNewSetListDialog()
        activity.submitSetListName("contest set")
        captureScreen("dialog_new_set_list_duplicate")
    }

    @Test
    fun renameSetList() {
        val ids = ScreenshotSupport.seedSetLists()
        val activity = launchMain()
        activity.showTab(3)
        activity.openRenameSetListDialog(ids[0])
        captureScreen("dialog_rename_set_list")
    }

    @Test
    fun deleteSetList() {
        val ids = ScreenshotSupport.seedSetLists()
        val activity = launchMain()
        activity.showTab(3)
        activity.openDeleteSetListDialog(ids[0])
        captureScreen("dialog_delete_set_list")
    }

    @Test
    fun deleteSetListWithALongName() {
        val ids = ScreenshotSupport.seedSetLists()
        // Too long for the dialog window's first measuring pass: the title takes AppCompat's
        // two-line size, however wide the dialog then opens.
        depollsoft.pitchperfect.SongsModel.get().renameList(ids[0], "Saturday chapter show at the lake house")
        val activity = launchMain()
        activity.showTab(3)
        activity.openDeleteSetListDialog(ids[0])
        captureScreen("dialog_delete_set_list_long_name")
    }

    @Test
    fun setListMenu() {
        val ids = ScreenshotSupport.seedSetLists()
        val activity = launchMain()
        activity.showTab(3)
        activity.openSetListMenu(ids[0])
        captureScreen("menu_set_list")
    }

    @Test
    fun loginPrompt() {
        launch(SettingsActivity::class.java).showLoginPrompt()
        captureScreen("dialog_login_prompt")
    }

    @Test
    @Config(qualifiers = ScreenshotSupport.PHONE_NIGHT)
    fun newSetListNight() {
        val activity = launchMain()
        activity.showTab(3)
        activity.openNewSetListDialog()
        captureScreen("night_dialog_new_set_list")
    }
}
