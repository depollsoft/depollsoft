package depollsoft.tagmaster

import android.app.Application
import android.app.Dialog
import android.content.Intent
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.viewpager2.widget.ViewPager2
import com.bindroid.trackable.TrackableCollection
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import depollsoft.tagmaster.ScreenTestSupport.assertDisplayed
import depollsoft.tagmaster.ScreenTestSupport.idle
import depollsoft.tagmaster.ScreenTestSupport.scrollTo
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

/**
 * The tag detail screen with a cached tag, migrated from the instrumented `TagDetailActivityTest`.
 *
 * The tag is written to the real disk cache, so `Tag.loadTagById` resolves it through the
 * production path with no network — exactly what the instrumented `NavigationTestFixture` did.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28], qualifiers = "w411dp-h891dp")
class TagDetailActivityScreenTest {
    private var controller: ActivityController<TagDetailActivity>? = null
    private val fixture = ScreenTestSupport.fixtureTag()

    @Before
    fun setUp() {
        ScreenTestSupport.startClean()
        ScreenTestSupport.clearTagCaches()
        ScreenTestSupport.cacheOnDisk(fixture)
    }

    @After
    fun tearDown() {
        ScreenTestSupport.finish(controller)
        ScreenTestSupport.clearTagCaches()
    }

    private fun intent() =
        Intent(RuntimeEnvironment.getApplication(), TagDetailActivity::class.java)
            .putExtra(TagDetailActivity.TAG_ID_EXTRA, fixture.id)

    private fun launch(): TagDetailActivity {
        val created = ScreenTestSupport.build(TagDetailActivity::class.java, intent())
        controller = created
        created.setup()
        assertCachedTagLoaded(created.get())
        return created.get()
    }

    /**
     * `Tag.loadTagById` reads the disk cache on Bolts' background executor, so the screen is not
     * loaded the instant `setup()` returns — the assertions below used to race that read. Waiting
     * for the load to actually settle, with the network refused, is the same claim as before: the
     * tag came out of the cache rather than off the wire.
     */
    private fun assertCachedTagLoaded(activity: TagDetailActivity) {
        val loaded = ScreenTestSupport.awaitTagLoaded(activity)
        assertDisplayed("tabLayout", activity.findViewById(R.id.tabLayout))
        assertEquals(fixture.id, activity.tagId)
        assertEquals(fixture.title, loaded.title)
        assertFalse("the settled screen is no longer loading", activity.isLoading)
    }

    private fun TagDetailActivity.pager(): ViewPager2 = findViewById(R.id.viewPager)

    private fun TagDetailActivity.tabs(): TabLayout = findViewById(R.id.tabLayout)

    /** Select a tab the way a tap does, then settle on the page it asked for. */
    private fun TagDetailActivity.selectTab(position: Int) {
        tabs().selectTab(tabs().getTabAt(position))
        idle()
        assertPage(position)
    }

    private fun TagDetailActivity.assertPage(position: Int) {
        assertEquals("the pager should settle on page $position", position, pager().currentItem)
        assertEquals(
            "and the tab strip should agree",
            position,
            tabs().selectedTabPosition,
        )
    }

    /** The view of the page fragment currently showing, once it has been created. */
    private fun TagDetailActivity.page(): View {
        idle()
        val fragments = detailFragment!!.childFragmentManager.fragments
        val current = fragments.firstOrNull { it.view != null && it.isResumed } ?: fragments.first { it.view != null }
        return current.requireView()
    }

    private fun TagDetailActivity.tracks(): View {
        selectTab(2)
        return page()
    }

    // ==================== Launch and chrome ====================

    @Test
    fun activityLaunchesTitledByItsTag() {
        assertEquals(fixture.title, launch().title.toString())
    }

    @Test
    fun viewPagerAndTabsAreDisplayedOnTheSummaryPage() {
        val activity = launch()
        assertDisplayed("viewPager", activity.pager())
        assertDisplayed("tabLayout", activity.tabs())
        assertTrue(activity.pager().isEnabled)
        assertTrue(activity.tabs().isEnabled)
        activity.assertPage(0)
    }

    // ==================== Summary page ====================

    @Test
    fun summaryShowsTheKeyButton() {
        val activity = launch()
        val key = scrollTo(activity.findViewById(R.id.playKeyNoteButton)) as TextView
        assertEquals("C", key.text.toString())
        assertTrue("the key button should be clickable", key.isClickable)
    }

    @Test
    fun summaryShowsTheTitleAndClassicTagNumber() {
        val activity = launch()
        assertEquals(
            fixture.title,
            (scrollTo(activity.page().findViewById(R.id.titleTextView)) as TextView).text.toString(),
        )
        assertEquals(
            "17",
            (scrollTo(activity.page().findViewById(R.id.classicTagTextView)) as TextView).text.toString(),
        )
    }

    // ==================== Details page ====================

    @Test
    fun detailsPageShowsArrangerAndYear() {
        val activity = launch()
        activity.selectTab(1)
        val details = activity.page()
        assertEquals(
            "Fixture arranger",
            (scrollTo(details.findViewById(R.id.arrangedByTextView)) as TextView).text.toString(),
        )
        assertEquals(
            "2026",
            (scrollTo(details.findViewById(R.id.yearArrangedTextView)) as TextView).text.toString(),
        )
    }

    // ==================== Tracks page ====================

    @Test
    fun tracksPageShowsTheBalanceControlAndRecordingNotes() {
        val activity = launch()
        val tracks = activity.tracks()
        assertDisplayed("balanceSeekBar", scrollTo(tracks.findViewById(R.id.balanceSeekBar)))
        assertEquals(
            fixture.recordingMethod,
            (scrollTo(tracks.findViewById(R.id.trackNotesTextView)) as TextView).text.toString(),
        )
    }

    @Test
    fun everyAvailablePartIsShownAndSelectsItsOwnTrack() {
        val activity = launch()
        val tracks = activity.tracks()
        val player = activity.findViewById<MediaPlayerView>(R.id.mediaPlayer)
        for ((id, expected) in listOf(
            R.id.allPartsButton to fixture.allPartsTrackUri!!.uri,
            R.id.tenorButton to fixture.tenorTrackUri!!.uri,
            R.id.leadButton to fixture.leadTrackUri!!.uri,
            R.id.bariButton to fixture.baritoneTrackUri!!.uri,
            R.id.bassButton to fixture.bassTrackUri!!.uri,
        )) {
            val button = scrollTo(tracks.findViewById(id)) as android.widget.RadioButton
            assertDisplayed("part button $id", button)
            button.performClick()
            idle()
            assertTrue("part button $id should end up checked", button.isChecked)
            assertEquals(
                "part button $id should route its own track to the player",
                expected,
                player.remoteLocation.uri,
            )
        }
    }

    // ==================== Videos page ====================

    @Test
    fun videosPageListsTheTagsVideos() {
        val activity = launch()
        activity.selectTab(3)
        val videos = activity.page()
        val list = videos.findViewById<ListView>(R.id.videoList)
        assertDisplayed("videoList", list)
        assertEquals(1, list.adapter.count)
        list.setSelection(0)
        idle()
        val row = requireNotNull(list.getChildAt(0)) { "the single video should be laid out" }
        assertDisplayed("videoPreview", row.findViewById(R.id.videoPreview))
        assertEquals(
            "Fixture quartet",
            row.findViewById<TextView>(R.id.sungByTextView).text.toString(),
        )
    }

    // ==================== Paging ====================

    @Test
    fun pagingForwardAndBackReachesEveryPage() {
        val activity = launch()
        for (page in 1..3) {
            activity.pager().setCurrentItem(page, false)
            idle()
            activity.assertPage(page)
        }
        for (page in 2 downTo 0) {
            activity.pager().setCurrentItem(page, false)
            idle()
            activity.assertPage(page)
        }
        assertEquals(
            fixture.id.toString(),
            activity.findViewById<TextView>(R.id.tagIdTextView).text.toString(),
        )
    }

    @Test
    fun pagingToTheDetailsPageShowsItsContent() {
        val activity = launch()
        activity.pager().setCurrentItem(1, false)
        idle()
        activity.assertPage(1)
        assertDisplayed("arrangedByTextView", activity.page().findViewById(R.id.arrangedByTextView))
    }

    @Test
    fun repeatedTabChangesLandOnTheRequestedPage() {
        val activity = launch()
        repeat(5) {
            activity.selectTab(3)
            activity.selectTab(0)
        }
        assertEquals(
            fixture.id.toString(),
            activity.findViewById<TextView>(R.id.tagIdTextView).text.toString(),
        )
    }

    // ==================== Recreation ====================

    @Test
    fun activitySurvivesRecreation() {
        val created = ScreenTestSupport.build(TagDetailActivity::class.java, intent())
        controller = created
        created.setup()
        idle()
        assertCachedTagLoaded(created.get())

        created.recreate()
        idle()

        assertCachedTagLoaded(created.get())
        assertEquals(
            fixture.id.toString(),
            created.get().findViewById<TextView>(R.id.tagIdTextView).text.toString(),
        )
    }

    @Test
    fun selectedTabIsPreservedAfterRecreation() {
        val created = ScreenTestSupport.build(TagDetailActivity::class.java, intent())
        controller = created
        created.setup()
        idle()
        created.get().selectTab(1)

        created.recreate()
        idle()

        assertCachedTagLoaded(created.get())
        created.get().assertPage(1)
        assertEquals(
            "Fixture arranger",
            (created.get().page().findViewById(R.id.arrangedByTextView) as TextView).text.toString(),
        )
    }

    // ==================== Lists: the chips row and the Add to list picker ====================

    private fun TagDetailActivity.menu(): Menu {
        val menu = PopupMenu(this, findViewById(android.R.id.content)).menu
        menuInflater.inflate(R.menu.tagdetailmenu, menu)
        return menu
    }

    private fun TagDetailActivity.chips(): ChipGroup = findViewById(R.id.savedStatusLayout)

    private fun ChipGroup.labels(): List<String> = (0 until childCount).map { (getChildAt(it) as Chip).text.toString() }

    private fun ChipGroup.chip(label: String): Chip =
        (0 until childCount)
            .map { getChildAt(it) as Chip }
            .single { it.text.toString() == label }

    private fun openPicker(activity: TagDetailActivity): AlertDialog {
        // Only refresh reports itself handled; every other tag action returns false and acts.
        activity.onOptionsItemSelected(activity.menu().findItem(R.id.addToListMenuItem))
        idle()
        return requireNotNull(ShadowDialog.getLatestDialog() as? AlertDialog) { "the picker should be showing" }
    }

    private fun rows(dialog: Dialog): List<View> {
        val found = mutableListOf<View>()

        fun walk(view: View) {
            if (view.id == R.id.listRow) found.add(view)
            if (view is ViewGroup) for (index in 0 until view.childCount) walk(view.getChildAt(index))
        }
        walk(dialog.window!!.decorView)
        return found
    }

    private fun View.rowName(): String = findViewById<TextView>(R.id.listRowName).text.toString()

    private fun View.rowChecked(): Boolean = findViewById<View>(R.id.listRowCheck).visibility == View.VISIBLE

    // ==================== The chips under the title ====================

    @Test
    fun aTagInNoListStillOffersAddToList() {
        val activity = launch()
        assertEquals(listOf(activity.getString(R.string.list_add_to_list)), activity.chips().labels())
    }

    @Test
    fun everyListTheTagIsInGetsAChipInDisplayOrder() {
        val custom = TagLists.create("Afterglow set")
        FavoritesModel.addFavorite(fixture.id)
        TeachableTagsModel.addTeachableTag(fixture.id)
        ListModel(custom).add(fixture.id)
        val activity = launch()
        assertEquals(
            listOf(
                activity.getString(R.string.Favorites),
                activity.getString(R.string.list_chip_teachable),
                "Afterglow set",
                activity.getString(R.string.list_add_to_list),
            ),
            activity.chips().labels(),
        )
    }

    @Test
    fun aChipAppearsAndDisappearsWithMembership() {
        val custom = TagLists.create("Afterglow set")
        val activity = launch()
        ListModel(custom).add(fixture.id)
        idle()
        assertTrue(activity.chips().labels().contains("Afterglow set"))
        ListModel(custom).remove(fixture.id)
        idle()
        assertFalse(activity.chips().labels().contains("Afterglow set"))
    }

    @Test
    fun aChipOpensTheListItNames() {
        val custom = TagLists.create("Afterglow set")
        ListModel(custom).add(fixture.id)
        val activity = launch()
        activity.chips().chip("Afterglow set").performClick()
        idle()
        val started = requireNotNull(shadowOf(activity).nextStartedActivity)
        assertEquals(TagListActivity::class.java.name, started.component!!.className)
        assertEquals(custom, started.getStringExtra(TagListActivity.EXTRA_LIST_KEY))
    }

    @Test
    fun theCloseIconRemovesTheTagAndUndoPutsItBackWhereItWas() {
        val custom = TagLists.create("Afterglow set")
        ListModel(custom).ids = TrackableCollection(mutableListOf(1, fixture.id, 2))
        val activity = launch()
        val chip = activity.chips().chip("Afterglow set")
        assertEquals(
            activity.getString(R.string.list_chip_remove, "Afterglow set"),
            chip.closeIconContentDescription?.toString(),
        )
        chip.performCloseIconClick()
        idle()
        assertEquals(listOf(1, 2), ListModel(custom).ids.toList())

        val undo =
            requireNotNull(
                activity.findViewById<android.widget.Button>(com.google.android.material.R.id.snackbar_action),
            ) { "an Undo action should be offered" }
        assertEquals(activity.getString(R.string.list_undo), undo.text.toString())
        undo.performClick()
        idle()
        assertEquals("undo restores the original position", listOf(1, fixture.id, 2), ListModel(custom).ids.toList())
    }

    // ==================== The picker ====================

    @Test
    fun thePickerNamesEveryListAndMarksTheOnesTheTagIsIn() {
        val custom = TagLists.create("Afterglow set")
        ListModel(custom).add(fixture.id)
        val activity = launch()
        val rows = rows(openPicker(activity))
        assertEquals(
            listOf(
                activity.getString(R.string.Favorites),
                activity.getString(R.string.TeachableTags),
                "Afterglow set",
                activity.getString(R.string.list_new_row),
            ),
            rows.map { it.rowName() },
        )
        assertEquals(listOf(false, false, true, false), rows.map { it.rowChecked() })
    }

    @Test
    fun aPickerRowTogglesMembershipStraightAway() {
        val custom = TagLists.create("Afterglow set")
        val activity = launch()
        val dialog = openPicker(activity)
        rows(dialog).single { it.rowName() == "Afterglow set" }.performClick()
        idle()
        assertEquals(listOf(fixture.id), ListModel(custom).ids.toList())
        assertTrue("the row marks itself", rows(dialog).single { it.rowName() == "Afterglow set" }.rowChecked())
        assertTrue("and the chips behind follow", activity.chips().labels().contains("Afterglow set"))

        rows(dialog).single { it.rowName() == "Afterglow set" }.performClick()
        idle()
        assertTrue(ListModel(custom).ids.isEmpty())
        assertFalse(rows(dialog).single { it.rowName() == "Afterglow set" }.rowChecked())
    }

    @Test
    fun theNewListRowMakesTheListAndPutsTheTagInIt() {
        val activity = launch()
        val picker = openPicker(activity)
        rows(picker).single { it.rowName() == activity.getString(R.string.list_new_row) }.performClick()
        idle()

        val name = requireNotNull(ShadowDialog.getLatestDialog() as? AlertDialog)
        requireNotNull(name.findViewById<TextInputEditText>(R.id.listNameInput)).setText("Afterglow set")
        name.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        idle()

        val created = TagLists.customKeys.single()
        assertEquals("Afterglow set", TagLists.name(created))
        assertEquals(listOf(fixture.id), ListModel(created).ids.toList())
        assertTrue(
            "the picker shows the new list, checked",
            rows(picker).single { it.rowName() == "Afterglow set" }.rowChecked(),
        )
    }

    @Test
    fun theFavoritesRowInThePickerIsTheSameFavoritesTheHeartToggles() {
        val activity = launch()
        val dialog = openPicker(activity)
        rows(dialog).single { it.rowName() == activity.getString(R.string.Favorites) }.performClick()
        idle()
        assertTrue(FavoritesModel.getIsFavorite(fixture.id))
        assertTrue(activity.chips().labels().contains(activity.getString(R.string.Favorites)))
    }
}
