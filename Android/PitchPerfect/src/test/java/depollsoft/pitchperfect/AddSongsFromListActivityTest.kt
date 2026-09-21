package depollsoft.pitchperfect

import android.content.Intent
import android.view.View
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.button.MaterialButton
import depollsoft.lib.activity.RichApplication
import depollsoft.pitchperfect.ScreenTestSupport.idle
import depollsoft.pitchperfect.lib.Key
import depollsoft.pitchperfect.lib.PitchedSong
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

/** "Add songs": the sectioned checklist, what it leaves out, and what confirming copies. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = RichApplication::class, qualifiers = "w411dp-h891dp")
class AddSongsFromListActivityTest {
    private var controller: ActivityController<AddSongsFromListActivity>? = null

    private val model get() = SongsModel.get()
    private lateinit var targetId: String

    @Before
    fun setUp() {
        ScreenTestSupport.startFromFirstLaunch()
        ScreenTestSupport.ensureFirebaseApp()
        resetLists()
        targetId = model.createList("Saturday show")
        model.currentListId = targetId
    }

    @After
    fun tearDown() {
        resetLists()
        ScreenTestSupport.finishScreenTest(controller)
    }

    private fun resetLists() {
        val default = SongList(SongsModel.DEFAULT_ID)
        default.name = "Default"
        model.songLists = mapOf(SongsModel.DEFAULT_ID to default)
        model.currentListId = SongsModel.DEFAULT_ID
    }

    private fun launch(): AddSongsFromListActivity {
        val intent =
            Intent(
                ApplicationProvider.getApplicationContext(),
                AddSongsFromListActivity::class.java,
            ).putExtra(AddSongsFromListActivity.LIST_EXTRA, targetId)
        val created = Robolectric.buildActivity(AddSongsFromListActivity::class.java, intent)
        controller = created
        created.setup()
        idle()
        return created.get()
    }

    private fun AddSongsFromListActivity.rows(): RecyclerView =
        findViewById<RecyclerView>(R.id.addableSongList).also {
            it.measure(0, 0)
            it.layout(0, 0, 1000, 4000)
            idle()
        }

    private fun RecyclerView.rowView(position: Int): View {
        scrollToPosition(position)
        idle()
        return requireNotNull(findViewHolderForAdapterPosition(position)) {
            "row $position should be bound"
        }.itemView
    }

    private fun song(
        title: String,
        keyIndex: Int = 0,
    ): PitchedSong =
        PitchedSong().apply {
            name = title
            key = Key.getMajorKeys()[keyIndex]
        }

    // ==================== Sections ====================

    @Test
    fun oneSectionPerOtherListWithSongsToOffer() {
        model.defaultSongList.addSong(song("Blue Skies"))
        val afterglow = model.createList("Afterglow")
        model.songLists[afterglow]!!.addSong(song("Shenandoah"))
        model.createList("Empty one")

        val rows = launch().rows()

        // My Songs header, Blue Skies, Afterglow header, Shenandoah — the empty list has no header.
        assertEquals(4, rows.adapter!!.itemCount)
        assertEquals("My Songs", rows.rowView(0).findViewById<TextView>(R.id.sectionHeaderText).text.toString())
        assertEquals("Blue Skies", rows.rowView(1).findViewById<TextView>(R.id.addableSongTitle).text.toString())
        assertEquals("Afterglow", rows.rowView(2).findViewById<TextView>(R.id.sectionHeaderText).text.toString())
        assertEquals("Shenandoah", rows.rowView(3).findViewById<TextView>(R.id.addableSongTitle).text.toString())
    }

    @Test
    fun aSongTheTargetAlreadyHasIsOmitted() {
        model.defaultSongList.addSong(song("Blue Skies"))
        model.defaultSongList.addSong(song("Shenandoah", 3))
        // Same title in another case, same key.
        model.songLists[targetId]!!.addSong(song("BLUE SKIES"))

        val rows = launch().rows()
        assertEquals(2, rows.adapter!!.itemCount)
        assertEquals("Shenandoah", rows.rowView(1).findViewById<TextView>(R.id.addableSongTitle).text.toString())
    }

    @Test
    fun nothingToAddShowsTheEmptyStateInsteadOfTheList() {
        val activity = launch()
        assertEquals(View.GONE, activity.findViewById<View>(R.id.addableSongList).visibility)
        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.addableEmptyText).visibility)
    }

    // ==================== Selection ====================

    @Test
    fun theConfirmActionCountsTheSelection() {
        model.defaultSongList.addSong(song("Blue Skies"))
        model.defaultSongList.addSong(song("Shenandoah", 3))
        val activity = launch()
        val button = activity.findViewById<MaterialButton>(R.id.addSongsConfirmButton)
        val menu = PopupMenu(activity, button).menu
        activity.onCreateOptionsMenu(menu)
        val confirmItem = menu.findItem(R.id.confirmAddSongsMenuItem)

        assertEquals("Add", button.text.toString())
        assertFalse("nothing selected yet", button.isEnabled)
        assertFalse(confirmItem.isEnabled)

        val rows = activity.rows()
        rows.rowView(1).performClick()
        idle()
        assertEquals("Add 1 song", button.text.toString())
        assertTrue(button.isEnabled)
        assertTrue(confirmItem.isEnabled)

        rows.rowView(2).performClick()
        idle()
        assertEquals("Add 2 songs", button.text.toString())

        // Tapping again un-ticks.
        rows.rowView(2).performClick()
        idle()
        assertEquals("Add 1 song", button.text.toString())
        assertEquals(1, activity.selectedCount)
    }

    // ==================== Confirming ====================

    @Test
    fun confirmingAppendsDeepCopiesWithFreshIdsInSourceOrder() {
        model.defaultSongList.addSong(song("Blue Skies"))
        model.defaultSongList.addSong(song("Shenandoah", 3))
        val activity = launch()
        val rows = activity.rows()

        // Tick the second song first: the copies must still land in source order.
        rows.rowView(2).performClick()
        rows.rowView(1).performClick()
        idle()
        activity.confirm()
        idle()

        val target = model.songLists[targetId]!!.songs
        assertEquals(listOf("Blue Skies", "Shenandoah"), target.map { it.name })
        val sources = model.defaultSongList.songs
        sources.zip(target).forEach { (left, right) ->
            assertNotEquals("a copy is its own song", left.id, right.id)
            assertEquals(left.key, right.key)
        }
        assertTrue("confirming closes the picker", activity.isFinishing)
    }

    @Test
    fun confirmingWithNothingSelectedDoesNothing() {
        model.defaultSongList.addSong(song("Blue Skies"))
        val activity = launch()
        activity.confirm()
        idle()
        assertEquals(0, model.songLists[targetId]!!.songs.size)
        assertFalse(activity.isFinishing)
    }
}
