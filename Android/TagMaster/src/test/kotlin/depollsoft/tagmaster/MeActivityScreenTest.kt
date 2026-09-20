package depollsoft.tagmaster

import android.app.Application
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.textfield.TextInputEditText
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bindroid.trackable.TrackableCollection
import depollsoft.tagmaster.ScreenTestSupport.assertDisplayed
import depollsoft.tagmaster.ScreenTestSupport.idle
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

/**
 * The Home screen, migrated from the instrumented `MeActivityTest` and `FavoritesFlowTest`.
 *
 * Eleven of `FavoritesFlowTest`'s twenty-two cases were
 * `try { check(matches(anyOf(isDisplayed(), not(isDisplayed())))) } catch (e: Exception) {}` — a
 * tautology inside a swallowed catch, which no regression could fail. They are migrated as the
 * definite assertions they were reaching for rather than reproduced as no-ops.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28], qualifiers = "w411dp-h891dp")
class MeActivityScreenTest {
    private var controller: ActivityController<MeActivity>? = null
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

    private fun launch(): MeActivity {
        val created = ScreenTestSupport.build(MeActivity::class.java)
        controller = created
        created.setup()
        idle()
        ScreenTestSupport.dismissChangelog()
        return created.get()
    }

    private fun MeActivity.list(): RecyclerView = findViewById(R.id.homeList)

    /** Scroll a position into view and hand back the bound row, like Espresso's `scrollTo`. */
    private fun MeActivity.rowAt(position: Int): View {
        list().scrollToPosition(position)
        idle()
        val holder = list().findViewHolderForAdapterPosition(position)
        return requireNotNull(holder) { "row $position should be bound" }.itemView
    }

    /**
     * A favourite row loads its tag through `Tag.loadTagById` on a background thread, so wait for
     * that load rather than read whatever the row happens to be showing one `idle()` after bind.
     */
    private fun awaitFavourite(row: View): SavedTagItemView {
        assertTrue("the favourite row is a saved tag row", row is SavedTagItemView)
        val saved = row as SavedTagItemView
        ScreenTestSupport.await("the favourite row to resolve its tag") { !saved.isLoading }
        assertFalse("the favourite should resolve from the disk cache", saved.failedToLoad)
        return saved
    }

    private fun MeActivity.header(): View = rowAt(0)

    private fun MeActivity.footer(): View = rowAt(list().adapter!!.itemCount - 1)

    private fun MeActivity.chooseMenu(itemId: Int) {
        val menu = androidx.appcompat.widget.PopupMenu(this, list()).menu
        menuInflater.inflate(R.menu.memenu, menu)
        assertTrue("menu item should be handled", onOptionsItemSelected(menu.findItem(itemId)))
        idle()
    }

    private fun assertOpened(
        activity: MeActivity,
        target: Class<*>,
    ) {
        val started = requireNotNull(shadowOf(activity).nextStartedActivity) { "expected a screen to open" }
        assertEquals(target.name, started.component!!.className)
    }

    // ==================== Launch and layout ====================

    @Test
    fun activityLaunchesWithTheHomeTitle() {
        val activity = launch()
        assertEquals(
            activity.getString(R.string.home_title),
            activity.supportActionBar!!.title.toString(),
        )
    }

    @Test
    fun homeListIsDisplayedAndEnabled() {
        val activity = launch()
        assertDisplayed("homeList", activity.list())
        assertTrue(activity.list().isEnabled)
        assertNotNull("the home list has an adapter", activity.list().adapter)
    }

    @Test
    fun homeListIsAHeaderThenFavouritesThenAFooter() {
        val activity = launch()
        val concat = activity.list().adapter as ConcatAdapter
        assertEquals(3, concat.adapters.size)
        assertTrue("a static header", concat.adapters.first() is StaticViewAdapter)
        assertTrue("a static footer", concat.adapters.last() is StaticViewAdapter)
        assertEquals(activity.favoriteIds.size, activity.favoritesAdapter.itemCount)
        assertEquals(activity.favoriteIds.size + 2, concat.itemCount)
    }

    @Test
    fun headerViewIsDisplayed() {
        val activity = launch()
        assertDisplayed("meHeaderView1", activity.header().findViewById(R.id.meHeaderView1))
    }

    @Test
    fun searchActionIsDisplayedAndEnabled() {
        val activity = launch()
        val search = activity.findViewById<View>(R.id.searchButton)
        assertDisplayed("searchButton", search)
        assertTrue(search.isEnabled)
        assertTrue(search.isClickable)
    }

    @Test
    fun browseActionIsDisplayedAndEnabled() {
        val activity = launch()
        val browse = activity.header().findViewById<View>(R.id.browseButton)
        assertDisplayed("browseButton", browse)
        assertTrue(browse.isEnabled)
    }

    // ==================== A seeded favourite ====================

    @Test
    fun aFavouriteRendersItsTitleInTheHomeList() {
        FavoritesModel.favoriteIds = TrackableCollection(mutableListOf(fixture.id))
        val activity = launch()
        assertEquals(1, activity.favoritesAdapter.itemCount)
        val row = awaitFavourite(activity.rowAt(1))
        assertEquals(fixture.id, row.tagId)
        assertEquals(
            "the row resolves its tag from the disk cache",
            fixture.title,
            row.tag!!.title,
        )
        assertDisplayed("the favourite's title", row.findViewById(R.id.titleTextView))
        assertEquals(
            fixture.title,
            row.findViewById<TextView>(R.id.titleTextView).text.toString(),
        )
    }

    @Test
    fun favouriteRowsCarryTheirMarkersAndRating() {
        FavoritesModel.favoriteIds = TrackableCollection(mutableListOf(fixture.id))
        val activity = launch()
        val row = awaitFavourite(activity.rowAt(1))
        // The instrumented originals allowed either visibility for each of these, which nothing
        // could fail. Two of them — favoriteMarkerTextView and teachableMarkerTextView — live in
        // tagsummaryview, the detail screen's summary page, and are never part of a Home row at
        // all, so those cases could not have found their views even on a device. The markers are
        // covered where they actually appear, in the detail screen's tests.
        for (id in listOf(
            R.id.ratingTextView,
            R.id.idTextView,
            R.id.sheetMusicCheckBox,
            R.id.learningTracksCheckBox,
        )) {
            assertNotNull("view $id belongs to a saved row", row.findViewById<View>(id))
        }
        assertEquals("4.00", row.findViewById<TextView>(R.id.ratingTextView).text.toString())
        assertEquals(fixture.id.toString(), row.findViewById<TextView>(R.id.idTextView).text.toString())
    }

    // ==================== Scrolling ====================

    @Test
    fun theHomeListScrollsDownAndBackUp() {
        FavoritesModel.favoriteIds = TrackableCollection((1..12).map { fixture.id }.toMutableList())
        val activity = launch()
        val last = activity.list().adapter!!.itemCount - 1
        repeat(3) {
            activity.list().scrollToPosition(last)
            idle()
        }
        assertDisplayed("homeList after scrolling down", activity.list())
        repeat(3) {
            activity.list().scrollToPosition(0)
            idle()
        }
        assertDisplayed("homeList after scrolling back", activity.list())
        assertEquals("scrolling changes nothing about the list", last, activity.list().adapter!!.itemCount - 1)
    }

    // ==================== Navigation ====================

    @Test
    fun searchActionOpensSearch() {
        val activity = launch()
        activity.findViewById<View>(R.id.searchButton).performClick()
        idle()
        assertOpened(activity, TagSearchActivity::class.java)
    }

    @Test
    fun browseActionOpensBrowse() {
        val activity = launch()
        activity.header().findViewById<View>(R.id.browseButton).performClick()
        idle()
        assertOpened(activity, TagBrowserActivity::class.java)
    }

    @Test
    fun teachableActionOpensTheTeachableList() {
        val activity = launch()
        activity.header().findViewById<View>(R.id.teachableButton).performClick()
        idle()
        assertOpened(activity, TeachableTagsActivity::class.java)
    }

    @Test
    fun settingsMenuItemOpensSettings() {
        val activity = launch()
        activity.chooseMenu(R.id.settingsMenuItem)
        assertOpened(activity, SettingsActivity::class.java)
    }

    @Test
    fun theTeachableListShowsAnEmptyStateThatMatchesItsContents() {
        val teachable = ScreenTestSupport.launch(TeachableTagsActivity::class.java)
        try {
            val screen = teachable.get()
            assertNotNull(screen.findViewById<View>(R.id.teachableTagsItemsControl))
            assertEquals(
                "the empty state shows exactly when the list is empty",
                screen.teachableTags.isEmpty(),
                screen.findViewById<View>(R.id.teachableEmptyState).isShown,
            )
        } finally {
            teachable.close()
        }
    }

    // ==================== Home footer ====================

    @Test
    fun theFooterNamesTheApplicationAndItsVersion() {
        val activity = launch()
        val footer = activity.footer()
        for (id in listOf(R.id.appVersionTextView, R.id.appNameTextView, R.id.copyrightTextView)) {
            val text = footer.findViewById<TextView>(id)
            assertDisplayed("footer field $id", text)
            assertTrue("footer field $id should say something", text.text.isNotEmpty())
        }
        assertEquals(
            activity.getString(R.string.app_version),
            footer.findViewById<TextView>(R.id.appVersionTextView).text.toString(),
        )
    }

    // ==================== Settings fields reachable from Home ====================

    @Test
    fun settingsOffersTheFieldsHomeLinksTo() {
        val settings = ScreenTestSupport.launch(SettingsActivity::class.java)
        try {
            for (id in listOf(
                R.id.clearCacheButton,
                R.id.clearFavoritesButton,
                R.id.changelogButton,
                R.id.themeTitleTextView,
                R.id.sheetMusicWakeLockCheckBox,
                R.id.learningTracksSpinner,
            )) {
                val view = requireNotNull(settings.get().findViewById<View>(id)) { "settings field $id" }
                assertDisplayed("settings field $id", ScreenTestSupport.scrollTo(view))
            }
        } finally {
            settings.close()
        }
    }

    // ==================== Recreation ====================

    @Test
    fun favouritesSurviveRecreation() {
        FavoritesModel.favoriteIds = TrackableCollection(mutableListOf(fixture.id))
        val created = ScreenTestSupport.build(MeActivity::class.java)
        controller = created
        created.setup()
        idle()
        ScreenTestSupport.dismissChangelog()
        val before = created.get().favoriteIds.toList()

        created.recreate()
        idle()
        ScreenTestSupport.dismissChangelog()

        val restored = created.get()
        assertEquals(before, restored.favoriteIds.toList())
        assertEquals(restored.favoriteIds.size, restored.favoritesAdapter.itemCount)
        assertEquals(restored.favoriteIds.size + 2, restored.list().adapter!!.itemCount)
        assertDisplayed("searchButton after recreation", restored.findViewById(R.id.searchButton))
    }

    @Test
    fun searchNavigationStillWorksAfterRecreation() {
        val created = ScreenTestSupport.build(MeActivity::class.java)
        controller = created
        created.setup()
        idle()
        ScreenTestSupport.dismissChangelog()

        created.recreate()
        idle()
        ScreenTestSupport.dismissChangelog()

        created.get().findViewById<View>(R.id.searchButton).performClick()
        idle()
        assertOpened(created.get(), TagSearchActivity::class.java)
        assertFalse("Home stays open behind Search", created.get().isFinishing)
    }

    // ==================== The Lists group ====================

    private fun MeActivity.listsContainer(): ViewGroup = header().findViewById(R.id.listsContainer)

    private fun MeActivity.listRows(): List<View> =
        listsContainer().let { group -> (0 until group.childCount).map { group.getChildAt(it) } }

    private fun View.listName(): String = findViewById<TextView>(R.id.listRowName).text.toString()

    private fun View.listDetail(): String = findViewById<TextView>(R.id.listRowDetail).text.toString()

    @Test
    fun theListsGroupHeadsTheTeachableListAndTheUsersOwn() {
        val key = TagLists.create("Afterglow set")
        ListModel(key).add(fixture.id)
        val activity = launch()
        assertDisplayed("the Lists heading", activity.header().findViewById(R.id.listsHeading))
        assertEquals(
            activity.getString(R.string.lists_heading),
            activity.header().findViewById<TextView>(R.id.listsHeading).text.toString(),
        )
        assertDisplayed("the teachable row", activity.header().findViewById(R.id.teachableButton))
        assertDisplayed("the new-list row", activity.header().findViewById(R.id.newListButton))
        assertEquals(listOf("Afterglow set"), activity.listRows().map { it.listName() })
        assertEquals("1 tag", activity.listRows().single().listDetail())
    }

    @Test
    fun aListRowCountsItsTagsAsTheyChange() {
        val key = TagLists.create("Afterglow set")
        val activity = launch()
        assertEquals("0 tags", activity.listRows().single().listDetail())
        ListModel(key).add(fixture.id)
        ListModel(key).add(fixture.id + 1)
        idle()
        assertEquals("2 tags", activity.listRows().single().listDetail())
    }

    @Test
    fun theListsGroupFollowsCreationsRenamesAndDeletions() {
        val activity = launch()
        assertEquals(emptyList<String>(), activity.listRows().map { it.listName() })

        val first = TagLists.create("Afterglow set")
        val second = TagLists.create("Chorus warmups")
        idle()
        assertEquals(listOf("Afterglow set", "Chorus warmups"), activity.listRows().map { it.listName() })

        TagLists.rename(first, "Afterglow")
        idle()
        assertEquals(listOf("Afterglow", "Chorus warmups"), activity.listRows().map { it.listName() })

        TagLists.moveDown(first)
        idle()
        assertEquals(listOf("Chorus warmups", "Afterglow"), activity.listRows().map { it.listName() })

        TagLists.delete(second)
        idle()
        assertEquals(listOf("Afterglow"), activity.listRows().map { it.listName() })
    }

    @Test
    fun aListRowOpensThatList() {
        val key = TagLists.create("Afterglow set")
        val activity = launch()
        activity.listRows().single().performClick()
        idle()
        val started = requireNotNull(shadowOf(activity).nextStartedActivity)
        assertEquals(TagListActivity::class.java.name, started.component!!.className)
        assertEquals(key, started.getStringExtra(TagListActivity.EXTRA_LIST_KEY))
    }

    @Test
    fun theNewListRowNamesAndCreatesAList() {
        val activity = launch()
        activity.header().findViewById<View>(R.id.newListButton).performClick()
        idle()
        val dialog = requireNotNull(ShadowDialog.getLatestDialog() as? AlertDialog)
        requireNotNull(dialog.findViewById<TextInputEditText>(R.id.listNameInput)).setText("Afterglow set")
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        idle()
        assertEquals("Afterglow set", TagLists.name(TagLists.customKeys.single()))
        assertEquals(listOf("Afterglow set"), activity.listRows().map { it.listName() })
    }

    @Test
    fun everyRowActionIsAlsoAnAccessibilityAction() {
        TagLists.create("Afterglow set")
        TagLists.create("Chorus warmups")
        val activity = launch()
        val labels =
            activity.listRows().map { row ->
                row.createAccessibilityNodeInfo()!!.actionList.mapNotNull { it.label?.toString() }
            }
        assertTrue("the first list can move down but not up", labels[0].contains(activity.getString(R.string.MoveDown)))
        assertFalse(labels[0].contains(activity.getString(R.string.MoveUp)))
        assertTrue(labels[1].contains(activity.getString(R.string.MoveUp)))
        assertFalse("the last list cannot move down", labels[1].contains(activity.getString(R.string.MoveDown)))
        for (row in labels) {
            assertTrue(row.contains(activity.getString(R.string.list_row_rename)))
            assertTrue(row.contains(activity.getString(R.string.list_row_delete)))
        }
        assertTrue("and a long press opens the same menu", activity.listRows().first().isLongClickable)
    }

    @Test
    fun deletingAListFromHomeConfirmsWithItsTagCount() {
        val key = TagLists.create("Afterglow set")
        ListModel(key).add(fixture.id)
        val activity = launch()
        val delete =
            activity
                .listRows()
                .single()
                .createAccessibilityNodeInfo()!!
                .actionList
                .single { it.label == activity.getString(R.string.list_row_delete) }
        assertTrue(androidx.core.view.ViewCompat.performAccessibilityAction(activity.listRows().single(), delete.id, null))
        idle()
        val dialog = requireNotNull(ShadowDialog.getLatestDialog() as? AlertDialog)
        assertTrue(dialog.findViewById<TextView>(android.R.id.message)!!.text.toString().contains("1 tag"))
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        idle()
        assertTrue(TagLists.customKeys.isEmpty())
        assertEquals(emptyList<String>(), activity.listRows().map { it.listName() })
    }

    @Test
    fun homeIsAListNotADetailPaneOnAPhone() {
        val activity = launch()
        assertFalse("a phone has no detail pane", activity.hasDetailPane)
    }
}
