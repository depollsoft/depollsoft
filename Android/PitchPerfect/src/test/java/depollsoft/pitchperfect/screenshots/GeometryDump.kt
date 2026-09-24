package depollsoft.pitchperfect.screenshots

import android.app.Activity
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import depollsoft.lib.activity.RichApplication
import depollsoft.pitchperfect.AddSongActivity
import depollsoft.pitchperfect.AddSongsFromListActivity
import depollsoft.pitchperfect.ManageSetListsActivity
import depollsoft.pitchperfect.SettingsActivity
import depollsoft.pitchperfect.SongsModel
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.launch
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.launchMain
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.openDeleteSetListDialog
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.openNewSetListDialog
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.openSetListMenu
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.deleteSetListWithUndo
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.setEditingSongs
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.showLoginPrompt
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.showTab
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.toggleKeyMode
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Temporary: prints the View-era geometry of every screen for the port. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = RichApplication::class, qualifiers = ScreenshotSupport.PHONE)
class GeometryDump {
    private val out = File("/private/tmp/claude-501/-Users-depoll-Code-depollsoft/a7c990e6-36c3-411f-829f-f5ec0495a96b/scratchpad/pp/geometry").apply { mkdirs() }

    @Before
    fun setUp() = ScreenshotSupport.setUp()

    @After
    fun tearDown() = ScreenshotSupport.tearDown()

    private fun describe(
        view: View,
        depth: Int,
        sb: StringBuilder,
    ) {
        if (view.visibility == View.GONE) return
        val loc = IntArray(2)
        view.getLocationInWindow(loc)
        val id = runCatching { view.resources.getResourceEntryName(view.id) }.getOrNull() ?: ""
        sb.append("  ".repeat(depth))
        sb.append("${view.javaClass.simpleName} #$id [${loc[0]},${loc[1]} ${view.width}x${view.height}]")
        if (view.visibility != View.VISIBLE) sb.append(" INVISIBLE")
        if (view.paddingLeft or view.paddingTop or view.paddingRight or view.paddingBottom != 0) {
            sb.append(" pad(${view.paddingLeft},${view.paddingTop},${view.paddingRight},${view.paddingBottom})")
        }
        if (view.elevation != 0f) sb.append(" elev=${view.elevation}")
        if (view.alpha != 1f) sb.append(" alpha=${view.alpha}")
        val bg = view.background
        if (bg != null) sb.append(" bg=${bg.javaClass.simpleName}")
        if (view is TextView) {
            val p = view.paint
            sb.append(
                " text='${view.text.toString().replace("\n", "\\n").take(40)}' size=${view.textSize} " +
                    "tf=${p.typeface?.let { it.style.toString() + "/" + it.weight }} ls=${view.letterSpacing} " +
                    "color=#${Integer.toHexString(view.currentTextColor)} gravity=0x${Integer.toHexString(view.gravity)} " +
                    "baseline=${view.baseline} lineH=${view.lineHeight} lines=${view.lineCount} spacing=${view.lineSpacingExtra}/${view.lineSpacingMultiplier} " +
                    "fontPad=${view.includeFontPadding} caps=${view.transformationMethod?.javaClass?.simpleName}",
            )
        }
        if (view.contentDescription != null) sb.append(" cd='${view.contentDescription}'")
        sb.append("\n")
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) describe(view.getChildAt(i), depth + 1, sb)
        }
    }

    private fun dumpWindows(name: String) {
        ScreenshotSupport.settle()
        val sb = StringBuilder()
        // Every window root, top-most last.
        val type = Class.forName("android.view.WindowManagerGlobal")
        val global = type.getMethod("getInstance").invoke(null)
        val field = type.getDeclaredField("mViews").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val views = field.get(global) as List<View>
        views.forEach { root ->
            val lp = root.layoutParams
            sb.append("=== window ${root.javaClass.simpleName} ${root.width}x${root.height} lp=$lp\n")
            describe(root, 0, sb)
        }
        File(out, "$name.txt").writeText(sb.toString())
    }

    private fun intent(type: Class<*>) = Intent(ApplicationProvider.getApplicationContext(), type)

    @Test
    fun main() {
        ScreenshotSupport.seedMySongs()
        ScreenshotSupport.seedSetLists()
        val activity = launchMain()
        dumpWindows("main_pitch_pipe")
        activity.showTab(1)
        dumpWindows("main_notes")
        activity.showTab(2)
        dumpWindows("main_keys")
        activity.toggleKeyMode()
        dumpWindows("main_keys_minor")
        activity.showTab(3)
        dumpWindows("main_songs")
        activity.setEditingSongs(true)
        dumpWindows("main_songs_editing")
        activity.openSetListMenu(SongsModel.get().orderedLists[1].id)
        dumpWindows("menu_set_list")
    }

    @Test
    fun mainEmptySongs() {
        val activity = launchMain()
        activity.showTab(3)
        dumpWindows("main_songs_empty")
        activity.openNewSetListDialog()
        dumpWindows("dialog_new_set_list")
    }

    @Test
    fun deleteDialog() {
        val ids = ScreenshotSupport.seedSetLists()
        val activity = launchMain()
        activity.showTab(3)
        activity.openDeleteSetListDialog(ids[0])
        dumpWindows("dialog_delete_set_list")
    }

    @Test
    fun snackbar() {
        val ids = ScreenshotSupport.seedSetLists()
        val activity = launchMain()
        activity.showTab(3)
        activity.deleteSetListWithUndo(ids[0])
        dumpWindows("snackbar")
    }

    @Test
    fun adSlot() {
        depollsoft.pitchperfect.PurchaseService.areAdsRemoved = false
        launchMain()
        dumpWindows("main_ad_slot")
    }

    @Test
    @Config(qualifiers = ScreenshotSupport.TABLET)
    fun tablet() {
        ScreenshotSupport.seedMySongs()
        ScreenshotSupport.seedSetLists()
        depollsoft.pitchperfect.PurchaseService.areAdsRemoved = false
        val activity = launchMain()
        dumpWindows("tablet_main_pitch_pipe")
        activity.showTab(3)
        dumpWindows("tablet_main_songs")
        launch(SettingsActivity::class.java)
        dumpWindows("tablet_settings")
        launch(AddSongActivity::class.java, intent(AddSongActivity::class.java))
        dumpWindows("tablet_add_song")
    }

    @Test
    fun secondary() {
        ScreenshotSupport.seedMySongs()
        val ids = ScreenshotSupport.seedSetLists()
        val settings = launch(SettingsActivity::class.java)
        dumpWindows("settings")
        settings.showLoginPrompt()
        dumpWindows("dialog_login_prompt")
        launch(AddSongActivity::class.java, intent(AddSongActivity::class.java))
        dumpWindows("add_song")
        launch(
            AddSongsFromListActivity::class.java,
            intent(AddSongsFromListActivity::class.java).putExtra(AddSongsFromListActivity.LIST_EXTRA, ids[1]),
        )
        dumpWindows("add_from_list")
        launch(ManageSetListsActivity::class.java)
        dumpWindows("manage_set_lists")
    }
}
