package depollsoft.pitchperfect.screenshots

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import depollsoft.lib.privacy.PrivacyChoices
import depollsoft.pitchperfect.MainTab
import depollsoft.pitchperfect.PitchPerfectActivity
import depollsoft.pitchperfect.PurchaseService
import depollsoft.pitchperfect.R
import depollsoft.pitchperfect.ScreenTestSupport
import depollsoft.pitchperfect.SettingsActivity
import depollsoft.pitchperfect.SettingsDialog
import depollsoft.pitchperfect.SongsModel
import depollsoft.pitchperfect.TestTags
import depollsoft.pitchperfect.lib.Key
import depollsoft.pitchperfect.lib.Note
import depollsoft.pitchperfect.lib.PitchedSong
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import java.time.Duration

/**
 * Shared setup for the screenshot tests.
 *
 * Every golden in `src/test/screenshots` was first recorded from the View implementation and the
 * Compose screens were diffed against it. The only calls that know how a screen is built are in
 * this file: launching, switching tabs, entering edit mode and opening prompts.
 */
internal object ScreenshotSupport {
    const val PHONE = "w411dp-h891dp-xxhdpi"

    /** A 420dpi phone: its density is not a whole number, so text and View-style sizes round differently. */
    const val PHONE_420 = "w411dp-h891dp-420dpi"
    const val PHONE_NIGHT = "w411dp-h891dp-night-xxhdpi"
    const val TABLET = "w800dp-h1280dp-xhdpi"

    private val controllers = mutableListOf<ActivityController<*>>()

    /** The test's compose rule, which finds nodes in whichever activity is showing. */
    lateinit var compose: ComposeTestRule

    /** A silent player: the goldens are about pixels, and Robolectric's audio is irrelevant. */
    private val silentPlayer =
        object : Note.NotePlayer {
            override fun play(n: Note) = Unit

            override fun stop(n: Note) = Unit
        }

    fun setUp(rule: ComposeTestRule) {
        compose = rule
        ScreenTestSupport.startFromFirstLaunch()
        ScreenTestSupport.ensureFirebaseApp()
        PurchaseService.areAdsRemoved = true
        Note.setPlayer(silentPlayer)
        val context = ApplicationProvider.getApplicationContext<Context>()
        // A returning user who already answered the telemetry prompt, so no dialog covers a screen.
        PrivacyChoices(context).save(analytics = false, crashes = false)
        SongsModel.get().clearAll()
        // The version the goldens show; release commits change the real one.
        shadowOf(context.packageManager).getInternalMutablePackageInfo(context.packageName).versionName = "5.1.1"
    }

    fun tearDown() {
        Note.getCommonNotes().forEach { it.stop() }
        Note.setPlayer(Note.DEFAULT_PLAYER)
        PurchaseService.areAdsRemoved = false
        val open = controllers.toList()
        controllers.clear()
        open.dropLast(1).forEach { runCatching { it.close() } }
        ScreenTestSupport.finishScreenTest(open.lastOrNull())
    }

    /** Lets posted work and animations finish: the main looper's, then Compose's own clock. */
    fun settle() {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
        if (::compose.isInitialized) compose.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()
    }

    fun <T : Activity> launch(
        type: Class<T>,
        intent: Intent? = null,
    ): T {
        val controller =
            if (intent == null) Robolectric.buildActivity(type) else Robolectric.buildActivity(type, intent)
        controllers += controller
        controller.setup()
        settle()
        return controller.get()
    }

    fun launchMain(): PitchPerfectActivity = launch(PitchPerfectActivity::class.java)

    /** Switches the main screen to tab [index]: 0 pitch pipe, 1 notes, 2 keys, 3 songs. */
    fun PitchPerfectActivity.showTab(index: Int) {
        compose.onNodeWithTag(MainTab.entries[index].testTag).performClick()
        settle()
    }

    fun PitchPerfectActivity.setEditingSongs(editing: Boolean) {
        if (songs.editing != editing) compose.onNodeWithTag(TestTags.EDIT_SONGS).performClick()
        settle()
    }

    fun PitchPerfectActivity.toggleKeyMode() {
        compose.onNodeWithTag(TestTags.MAJOR_MINOR_FAB).performClick()
        settle()
    }

    fun PitchPerfectActivity.openNewSetListDialog() {
        songs.promptNewList()
        settle()
    }

    fun PitchPerfectActivity.openRenameSetListDialog(listId: String) {
        songs.promptRename(listId)
        settle()
    }

    /** Types [text] into the open set list name dialog and presses its positive button. */
    fun PitchPerfectActivity.submitSetListName(text: String) {
        compose.onNodeWithTag(TestTags.NAME_DIALOG_FIELD).performTextReplacement(text)
        compose.onNodeWithTag(TestTags.NAME_DIALOG_CONFIRM).performClick()
        settle()
    }

    fun PitchPerfectActivity.openDeleteSetListDialog(listId: String) {
        songs.confirmDelete(listId)
        settle()
    }

    fun PitchPerfectActivity.openSetListMenu(listId: String) {
        songs.menuFor = listId
        settle()
    }

    /** Opens the sign-in prompt from the settings screen's Log In button. */
    fun Activity.showLoginPrompt() {
        compose.onNodeWithTag(TestTags.LOG_IN).performClick()
        settle()
        check((this as SettingsActivity).dialog == SettingsDialog.LOG_IN)
    }

    fun Activity.pressSaveSong() {
        compose.onNodeWithTag(TestTags.SAVE_SONG).performClick()
        settle()
    }

    fun Activity.chooseMinorKeys() {
        compose.onNodeWithContentDescription(getString(R.string.KeyModeMinor)).performClick()
        settle()
    }

    fun Activity.tickAddableSong(title: String) {
        // Every row with that title, as the View driver clicked each matching row.
        val rows = compose.onAllNodesWithText(title)
        repeat(rows.fetchSemanticsNodes().size) { rows[it].performClick() }
        settle()
    }

    /** Sounds [key] on the Keys tab, whose rows light while their key sounds. */
    fun PitchPerfectActivity.playKey(key: Key) {
        key.note.play()
        settle()
    }

    /** The activity window alone. */
    fun Activity.capture(name: String) {
        settle()
        window.decorView.captureRoboImage("src/test/screenshots/$name.png")
    }

    /** Every window, for dialogs and popups over an activity. */
    fun captureScreen(name: String) {
        settle()
        captureScreenRoboImage("src/test/screenshots/$name.png")
    }

    fun song(
        title: String,
        key: Key,
    ): PitchedSong =
        PitchedSong().apply {
            name = title
            this.key = key
        }

    val sampleTitles =
        listOf(
            "Blue Skies",
            "Down Our Way",
            "Heart of My Heart",
            "Shenandoah",
            "Sweet Adeline",
            "The Old Songs",
            "When You Were Sweet Sixteen",
            "You Are My Sunshine",
        )

    /** Fills My Songs with the store-capture library. */
    fun seedMySongs() {
        val list = SongsModel.get().defaultSongList
        list.resetSongs()
        sampleTitles.forEachIndexed { index, title ->
            list.addSong(song(title, Key.getMajorKeys()[index + 2]))
        }
    }

    /** Two custom set lists after My Songs; returns their ids. */
    fun seedSetLists(): List<String> {
        val model = SongsModel.get()
        val contest = model.createList("Contest Set")
        model.songLists[contest]!!.addSong(song("Sweet Adeline", Key.getMajorKeys()[4]))
        model.songLists[contest]!!.addSong(song("Shenandoah", Key.getMinorKeys()[6]))
        val chapter = model.createList("Chapter Show")
        return listOf(contest, chapter)
    }
}
