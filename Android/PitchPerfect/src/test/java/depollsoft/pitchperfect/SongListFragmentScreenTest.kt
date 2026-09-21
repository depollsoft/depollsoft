package depollsoft.pitchperfect

import android.content.Intent
import android.os.Looper
import android.view.View
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import depollsoft.lib.activity.RichApplication
import depollsoft.lib.util.Preferences
import depollsoft.pitchperfect.ScreenTestSupport.assertDisplayed
import depollsoft.pitchperfect.ScreenTestSupport.idle
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * The Songs tab and the song editor it opens, migrated from the instrumented
 * `SongListFragmentTest`.
 *
 * The instrumented original called the editor a "dialog"; it is actually [AddSongActivity]. Under
 * Robolectric the FAB's `startActivityForResult` is observed as the started intent, and the editor
 * is then launched from that same intent so its contents are still asserted end to end.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = RichApplication::class, qualifiers = "w411dp-h891dp")
class SongListFragmentScreenTest {
    private var controller: ActivityController<PitchPerfectActivity>? = null
    private var editor: ActivityController<AddSongActivity>? = null

    private val songs get() = SongsModel.get().defaultSongList.songs

    @Before
    fun setUp() {
        ScreenTestSupport.startFromFirstLaunch()
        ScreenTestSupport.ensureFirebaseApp()
        PurchaseService.areAdsRemoved = true
        songs.clear()
    }

    @After
    fun tearDown() {
        editor?.close()
        songs.clear()
        PurchaseService.areAdsRemoved = false
        ScreenTestSupport.finishScreenTest(controller)
    }

    private fun launch(): PitchPerfectActivity {
        val created = Robolectric.buildActivity(PitchPerfectActivity::class.java)
        controller = created
        created.setup()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
        return created.get()
    }

    /** The bottom-navigation route onto the Songs page, settled through the animated swap. */
    private fun PitchPerfectActivity.goToSongs(): SongListFragment {
        findViewById<View>(R.id.songs_item).performClick()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
        assertEquals("Songs is page 3", 3, findViewById<ViewPager2>(R.id.viewPager).currentItem)
        return supportFragmentManager.fragments.filterIsInstance<SongListFragment>().single()
    }

    /** Follow the FAB's `startActivityForResult` into the real editor. */
    private fun openEditor(activity: PitchPerfectActivity): AddSongActivity {
        activity.findViewById<FloatingActionButton>(R.id.addSongButton).performClick()
        idle()
        val started = shadowOf(activity).nextStartedActivityForResult
        assertNotNull("the add button should open the song editor", started)
        assertEquals(
            AddSongActivity::class.java.name,
            started.intent.component!!.className,
        )
        val created = Robolectric.buildActivity(AddSongActivity::class.java, started.intent)
        editor = created
        created.setup()
        idle()
        return created.get()
    }

    // ==================== Navigation ====================

    @Test
    fun navigateToSongsTabShowsTheAddButton() {
        val activity = launch()
        activity.goToSongs()
        assertDisplayed("addSongButton", activity.findViewById<FloatingActionButton>(R.id.addSongButton))
    }

    @Test
    fun bottomNavigationHasASongsItem() {
        val activity = launch()
        assertDisplayed("songs_item", activity.findViewById(R.id.songs_item))
    }

    // ==================== Layout ====================

    @Test
    fun songListViewIsDisplayed() {
        val activity = launch()
        val fragment = activity.goToSongs()
        val list = fragment.requireView().findViewById<RecyclerView>(R.id.songListView)
        assertDisplayed("songListView", list)
        assertNotNull("the list is backed by an adapter", list.adapter)
    }

    @Test
    fun addSongFabIsDisplayed() {
        val activity = launch()
        activity.goToSongs()
        assertDisplayed("addSongButton", activity.findViewById<FloatingActionButton>(R.id.addSongButton))
    }

    @Test
    fun theSetListSelectorSitsAboveTheRows() {
        val activity = launch()
        val fragment = activity.goToSongs()
        val part = fragment.requireView().findViewById<SetListSelectorView>(R.id.setListSelector)
        assertDisplayed("setListSelector", part)
        assertNotNull("My Songs is always a position", part.positionView(SongsModel.DEFAULT_ID))
        assertNotNull("the + is how the feature is found", part.addPositionView)
    }

    @Test
    fun theEditorIsOpenedForTheCurrentList() {
        val activity = launch()
        activity.goToSongs()
        activity.findViewById<FloatingActionButton>(R.id.addSongButton).performClick()
        idle()
        assertEquals(
            SongsModel.DEFAULT_ID,
            shadowOf(activity).nextStartedActivityForResult.intent
                .getStringExtra(AddSongActivity.LIST_EXTRA),
        )
    }

    // ==================== Song editor ====================

    @Test
    fun fabOpensTheSongEditor() {
        val activity = launch()
        activity.goToSongs()
        val editorActivity = openEditor(activity)
        assertDisplayed(
            "songTitleEditText",
            editorActivity.findViewById<EditText>(R.id.songTitleEditText),
        )
    }

    @Test
    fun songEditorHasAKeyList() {
        val activity = launch()
        activity.goToSongs()
        val editorActivity = openEditor(activity)
        val keys = editorActivity.findViewById<RecyclerView>(R.id.songKeyList)
        assertDisplayed("songKeyList", keys)
        assertTrue("the key list offers keys to choose", keys.adapter!!.itemCount > 0)
    }

    @Test
    fun songTitleCanBeEntered() {
        val activity = launch()
        activity.goToSongs()
        val editorActivity = openEditor(activity)
        val title = editorActivity.findViewById<EditText>(R.id.songTitleEditText)
        title.setText("Test Song Title")
        idle()
        assertEquals("Test Song Title", title.text.toString())
    }

    @Test
    fun tappingTheKeyListSelectsARowWithoutLeavingTheEditor() {
        val activity = launch()
        activity.goToSongs()
        val editorActivity = openEditor(activity)
        val keys = editorActivity.findViewById<RecyclerView>(R.id.songKeyList)
        keys.scrollToPosition(1)
        idle()
        val row = requireNotNull(keys.findViewHolderForAdapterPosition(1)) { "row 1 should be bound" }
        row.itemView.performClick()
        idle()
        assertDisplayed("songKeyList after a tap", keys)
        assertEquals(
            "a row tap stays in the editor",
            androidx.lifecycle.Lifecycle.State.RESUMED,
            editorActivity.lifecycle.currentState,
        )
    }

    // ==================== Round trip ====================

    @Test
    fun canNavigateBetweenTabsAndBackToSongs() {
        val activity = launch()
        activity.goToSongs()
        assertDisplayed("addSongButton", activity.findViewById<FloatingActionButton>(R.id.addSongButton))

        activity.findViewById<View>(R.id.pitchpipe_item).performClick()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
        assertEquals(0, activity.findViewById<ViewPager2>(R.id.viewPager).currentItem)

        activity.goToSongs()
        assertDisplayed(
            "addSongButton after returning",
            activity.findViewById<FloatingActionButton>(R.id.addSongButton),
        )
    }
}
