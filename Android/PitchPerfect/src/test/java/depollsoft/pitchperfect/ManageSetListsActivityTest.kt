package depollsoft.pitchperfect

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import depollsoft.lib.activity.RichApplication
import depollsoft.pitchperfect.ScreenTestSupport.idle
import depollsoft.pitchperfect.lib.Key
import depollsoft.pitchperfect.lib.PitchedSong
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

/** The "Set Lists" screen: its rows, its ordering, and the prompts its row actions raise. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = RichApplication::class, qualifiers = "w411dp-h891dp")
class ManageSetListsActivityTest {
    private var controller: ActivityController<ManageSetListsActivity>? = null

    private val model get() = SongsModel.get()

    @Before
    fun setUp() {
        ScreenTestSupport.startFromFirstLaunch()
        ScreenTestSupport.ensureFirebaseApp()
        resetLists()
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

    private fun launch(): ManageSetListsActivity {
        val created = Robolectric.buildActivity(ManageSetListsActivity::class.java)
        controller = created
        created.setup()
        idle()
        return created.get()
    }

    private fun ManageSetListsActivity.rows(): RecyclerView =
        findViewById<RecyclerView>(R.id.setListManageList).also {
            it.measure(0, 0)
            it.layout(0, 0, 1000, 2000)
            idle()
        }

    private fun RecyclerView.rowView(position: Int): View {
        scrollToPosition(position)
        idle()
        return requireNotNull(findViewHolderForAdapterPosition(position)) {
            "row $position should be bound"
        }.itemView
    }

    private fun song(title: String): PitchedSong =
        PitchedSong().apply {
            name = title
            key = Key.getMajorKeys()[0]
        }

    // ==================== Rows ====================

    @Test
    fun mySongsIsTheFirstRowAndCarriesItsCount() {
        model.defaultSongList.addSong(song("Blue Skies"))
        model.createList("Saturday show")
        val activity = launch()
        val rows = activity.rows()

        assertEquals(2, rows.adapter!!.itemCount)
        val first = rows.rowView(0)
        assertEquals("My Songs", first.findViewById<TextView>(R.id.setListRowName).text.toString())
        assertEquals("1 song", first.findViewById<TextView>(R.id.setListRowCount).text.toString())
        assertEquals(
            "My Songs has no drag handle",
            View.INVISIBLE,
            first.findViewById<View>(R.id.setListRowDragHandle).visibility,
        )
    }

    @Test
    fun anEmptyCustomListReadsAsNoSongs() {
        val id = model.createList("Saturday show")
        val rows = launch().rows()
        val row = rows.rowView(1)
        assertEquals("Saturday show", row.findViewById<TextView>(R.id.setListRowName).text.toString())
        assertEquals("No songs", row.findViewById<TextView>(R.id.setListRowCount).text.toString())
        assertEquals(
            "custom rows are draggable",
            View.VISIBLE,
            row.findViewById<View>(R.id.setListRowDragHandle).visibility,
        )
        assertEquals(id, row.tag)
    }

    // ==================== Reordering ====================

    @Test
    fun aDropPersistsTheNewOrderOnce() {
        val a = model.createList("Alpha")
        val b = model.createList("Bravo")
        val c = model.createList("Charlie")
        val activity = launch()
        activity.rows()

        // The ItemTouchHelper's onMove/clearView pair, without a real gesture: move Charlie up to
        // the head of the custom rows and drop it there.
        activity.moveForTest(3, 1)
        activity.commitOrder()
        idle()

        assertEquals(0L, model.songLists[c]!!.order)
        assertEquals(1L, model.songLists[a]!!.order)
        assertEquals(2L, model.songLists[b]!!.order)
        assertEquals(
            listOf(SongsModel.DEFAULT_ID, c, a, b),
            model.orderedLists.map { it.id },
        )
    }

    // ==================== Row actions ====================

    @Test
    fun tappingARowOpensTheRenamePrompt() {
        model.createList("Saturday show")
        val activity = launch()
        activity.rows().rowView(1).performClick()
        idle()
        assertNotNull(
            "a row tap asks for a new name",
            activity.supportFragmentManager.findFragmentByTag(SetListNameDialog.FRAGMENT_TAG),
        )
    }

    @Test
    fun theRowOverflowCanDuplicateAndDelete() {
        val id = model.createList("Saturday show")
        model.songLists[id]!!.addSong(song("Blue Skies"))
        val activity = launch()
        val list = model.songLists[id]!!

        activity.duplicate(list)
        idle()
        val copy = model.songLists.values.single { model.displayName(it) == "Saturday show copy" }
        assertEquals(1, copy.songs.size)
        assertEquals(3, activity.rows().adapter!!.itemCount)

        activity.delete(list)
        idle()
        assertTrue(id !in model.songLists.keys)
        assertEquals(2, activity.rows().adapter!!.itemCount)
    }

    @Test
    fun deleteIsHiddenOnTheDefaultRow() {
        model.createList("Saturday show")
        val activity = launch()
        // `delete` refuses the default list outright, whatever the menu shows.
        activity.delete(model.defaultSongList)
        idle()
        assertTrue(model.songLists.containsKey(SongsModel.DEFAULT_ID))
        assertEquals(
            "the default row's overflow offers no Delete",
            false,
            activity.rowMenuOffersDeleteForTest(model.defaultSongList),
        )
        assertEquals(
            true,
            activity.rowMenuOffersDeleteForTest(model.orderedLists.last()),
        )
    }

    // ==================== Live updates ====================

    @Test
    fun aRemoteChangeReRendersTheRows() {
        val activity = launch()
        assertEquals(1, activity.rows().adapter!!.itemCount)

        model.createList("Arrived from elsewhere")
        idle()

        assertEquals(2, activity.rows().adapter!!.itemCount)
        assertEquals(
            "Arrived from elsewhere",
            activity.rows().rowView(1).findViewById<TextView>(R.id.setListRowName).text.toString(),
        )
    }
}
