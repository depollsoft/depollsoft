package depollsoft.pitchperfect.screenshots

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Looper
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import depollsoft.lib.privacy.PrivacyChoices
import depollsoft.pitchperfect.PitchPerfectActivity
import depollsoft.pitchperfect.PurchaseService
import depollsoft.pitchperfect.R
import depollsoft.pitchperfect.ScreenTestSupport
import depollsoft.pitchperfect.SongListFragment
import depollsoft.pitchperfect.SongsModel
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
 * this file: launching, switching tabs and entering edit mode.
 */
internal object ScreenshotSupport {
    const val PHONE = "w411dp-h891dp-xxhdpi"
    const val PHONE_NIGHT = "w411dp-h891dp-night-xxhdpi"
    const val TABLET = "w800dp-h1280dp-xhdpi"

    private val controllers = mutableListOf<ActivityController<*>>()

    /** A silent player: the goldens are about pixels, and Robolectric's audio is irrelevant. */
    private val silentPlayer =
        object : Note.NotePlayer {
            override fun play(n: Note) = Unit

            override fun stop(n: Note) = Unit
        }

    fun setUp() {
        ScreenTestSupport.startFromFirstLaunch()
        ScreenTestSupport.ensureFirebaseApp()
        PurchaseService.areAdsRemoved = true
        Note.setPlayer(silentPlayer)
        val context = ApplicationProvider.getApplicationContext<Context>()
        // A returning user who already answered the telemetry prompt, so no dialog covers a screen.
        PrivacyChoices(context).save(analytics = false, crashes = false)
        SongsModel.get().clearAll()
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

    fun settle() = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))

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
        val item =
            when (index) {
                0 -> R.id.pitchpipe_item
                1 -> R.id.notes_item
                2 -> R.id.keys_item
                else -> R.id.songs_item
            }
        findViewById<View>(item).performClick()
        settle()
    }

    fun PitchPerfectActivity.setEditingSongs(editing: Boolean) {
        val fragment = supportFragmentManager.fragments.filterIsInstance<SongListFragment>().single()
        if (fragment.isEditingSongs() != editing) fragment.toggleEditingSongs()
        settle()
    }

    fun PitchPerfectActivity.toggleKeyMode() {
        findViewById<View>(R.id.majorMinorFab).performClick()
        settle()
    }

    private val PitchPerfectActivity.songsFragment: SongListFragment
        get() = supportFragmentManager.fragments.filterIsInstance<SongListFragment>().single()

    fun PitchPerfectActivity.openNewSetListDialog() {
        songsFragment.promptNewSetList()
        settle()
    }

    fun PitchPerfectActivity.openRenameSetListDialog(listId: String) {
        songsFragment.promptRenameSetList(listId)
        settle()
    }

    /** Types [text] into the open set list name dialog and presses its positive button. */
    fun PitchPerfectActivity.submitSetListName(text: String) {
        val dialog =
            supportFragmentManager.findFragmentByTag(depollsoft.pitchperfect.SetListNameDialog.FRAGMENT_TAG)
                as androidx.fragment.app.DialogFragment
        val alert = dialog.dialog as androidx.appcompat.app.AlertDialog
        alert.findViewById<android.widget.EditText>(R.id.setListNameInput)!!.setText(text)
        alert.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick()
        settle()
    }

    fun PitchPerfectActivity.openDeleteSetListDialog(listId: String) {
        songsFragment.confirmDeleteList(listId)
        settle()
    }

    fun PitchPerfectActivity.openSetListMenu(listId: String) {
        val selector = findViewById<depollsoft.pitchperfect.SetListSelectorView>(R.id.setListSelector)
        songsFragment.showListMenu(selector.positionView(listId)!!, listId)
        settle()
    }

    fun PitchPerfectActivity.deleteSetListWithUndo(listId: String) {
        songsFragment.deleteList(listId)
        settle()
    }

    /** Opens the sign-in prompt from the settings screen's Log In button. */
    fun Activity.showLoginPrompt() {
        findViewById<View>(R.id.loginButton).performClick()
        settle()
    }

    fun Activity.scrollSettingsToEnd() {
        val scroll = findViewById<android.widget.ScrollView>(R.id.scrollView1)
        scroll.fullScroll(View.FOCUS_DOWN)
        settle()
    }

    fun Activity.pressSaveSong() {
        findViewById<View>(R.id.saveSongButton).performClick()
        settle()
    }

    fun Activity.chooseMinorKeys() {
        findViewById<View>(R.id.keyModeMinor).performClick()
        settle()
    }

    fun Activity.tickAddableSong(title: String) {
        val list = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.addableSongList)
        for (index in 0 until list.childCount) {
            val row = list.getChildAt(index)
            val label = row.findViewById<android.widget.TextView>(R.id.addableSongTitle) ?: continue
            if (label.text.toString() == title) row.performClick()
        }
        settle()
    }

    /**
     * Sounds [key] on the Keys tab. The View rows took their lit state from a Bindroid binding the
     * shared models no longer feed, so the row is lit here exactly as that binding would have.
     */
    fun PitchPerfectActivity.playKey(key: depollsoft.pitchperfect.lib.Key) {
        key.note.play()
        val list = findViewById<android.widget.ListView>(R.id.majorKeySignatureListView)
        for (index in 0 until list.childCount) {
            val row = list.getChildAt(index) as? depollsoft.pitchperfect.KeySignatureListItemView ?: continue
            if (row.key == key) row.isPressed = true
        }
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
