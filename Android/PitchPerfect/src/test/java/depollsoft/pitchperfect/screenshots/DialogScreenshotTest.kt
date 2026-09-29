package depollsoft.pitchperfect.screenshots

import org.robolectric.RuntimeEnvironment
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import depollsoft.pitchperfect.R
import androidx.core.content.ContextCompat
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.asAndroidBitmap
import android.graphics.Bitmap
import depollsoft.lib.activity.RichApplication
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import depollsoft.pitchperfect.SettingsActivity
import depollsoft.pitchperfect.TestTags
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
    @Config(qualifiers = ScreenshotSupport.PHONE_NIGHT)
    fun setListMenuNight() {
        val ids = ScreenshotSupport.seedSetLists()
        val activity = launchMain()
        activity.showTab(3)
        activity.openSetListMenu(ids[0])
        captureScreen("night_menu_set_list")
    }

    @Test
    fun loginPrompt() {
        launch(SettingsActivity::class.java).showLoginPrompt()
        captureScreen("dialog_login_prompt")
    }

    @Test
    fun tuning() {
        launch(SettingsActivity::class.java)
        ScreenshotSupport.compose.onNodeWithTag(TestTags.TUNING).performClick()
        ScreenshotSupport.settle()
        captureScreen("dialog_tuning")
    }

    @Test
    fun sound() {
        launch(SettingsActivity::class.java)
        ScreenshotSupport.compose.onNodeWithTag(TestTags.SOUND).performClick()
        ScreenshotSupport.settle()
        captureScreen("dialog_sound")
        // More below: the bottom hairline and the scrollbar show; nothing above yet.
        val top = listImage(TestTags.SOUND_LIST)
        assertTrue("bottom hairline", top.hairlineAt(top.height - 1))
        assertFalse("no top hairline at the top", top.hairlineAt(0))
        assertTrue("scrollbar thumb", top.thumbShowing())

        ScreenshotSupport.compose.onNodeWithTag(TestTags.SOUND_CHOICE + "harp").performScrollTo()
        ScreenshotSupport.settle()
        captureScreen("dialog_sound_bottom")
        val bottom = listImage(TestTags.SOUND_LIST)
        assertTrue("top hairline once scrolled", bottom.hairlineAt(0))
        assertFalse("no bottom hairline at the end", bottom.hairlineAt(bottom.height - 1))
        assertTrue("scrollbar thumb", bottom.thumbShowing())
    }

    @Test
    fun aTuningListThatFitsShowsNoScrollCues() {
        launch(SettingsActivity::class.java)
        ScreenshotSupport.compose.onNodeWithTag(TestTags.TUNING).performClick()
        ScreenshotSupport.settle()
        val list = listImage(TestTags.TUNING_LIST)
        assertFalse(list.hairlineAt(0))
        assertFalse(list.hairlineAt(list.height - 1))
        assertFalse(list.thumbShowing())
    }

    private fun listImage(tag: String) = ScreenshotSupport.compose.onNodeWithTag(tag).captureToImage().asAndroidBitmap()

    /** Whether row [y] is the plate's hairline most of the way across (a divider, not a row's ink). */
    private fun Bitmap.hairlineAt(y: Int): Boolean {
        val hairline = ContextCompat.getColor(RuntimeEnvironment.getApplication(), R.color.plate_hairline)
        val xs = (width / 10 until width * 9 / 10)
        return xs.count { getPixel(it, y) == hairline } > xs.count() * 9 / 10
    }

    /** Whether the scrollbar's thumb is drawn along the trailing edge: a column that isn't the background. */
    private fun Bitmap.thumbShowing(): Boolean {
        // The trailing padding beside the rows, clear of the thumb.
        val background = getPixel(width - 40, height / 2)
        return (0 until height).count { getPixel(width - 3, it) != background } > height / 10
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
