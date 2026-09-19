package depollsoft.tagmaster

import android.app.Application
import android.content.Intent
import android.view.View
import android.widget.EditText
import android.widget.TextView
import bolts.Task
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import depollsoft.tagmaster.ScreenTestSupport.assertDisplayed
import depollsoft.tagmaster.ScreenTestSupport.chooseDropdown
import depollsoft.tagmaster.ScreenTestSupport.idle
import depollsoft.tagmaster.barbershop.TagCollection
import depollsoft.tagmaster.barbershop.TagSortOptions
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

/**
 * The search form and the results it produces, migrated from the instrumented
 * `TagSearchActivityTest`.
 *
 * On a device the form started the results screen and Espresso followed it. Robolectric does not
 * launch a started activity, so each navigation case asserts the intent the form produced and then
 * launches the results screen from that exact intent — the assertion still spans both screens,
 * just in two steps.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28], qualifiers = "w411dp-h891dp")
class TagSearchActivityScreenTest {
    private var controller: ActivityController<TagSearchActivity>? = null
    private var results: ActivityController<TagSearchResultsActivity>? = null
    private val fixture = ScreenTestSupport.fixtureTag()

    @Before
    fun setUp() {
        ScreenTestSupport.startClean()
        ScreenTestSupport.clearTagCaches()
        ScreenTestSupport.cacheOnDisk(fixture)
    }

    @After
    fun tearDown() {
        results?.close()
        ScreenTestSupport.finish(controller)
        ScreenTestSupport.clearTagCaches()
    }

    private fun launch(): TagSearchActivity {
        val created = ScreenTestSupport.build(TagSearchActivity::class.java)
        controller = created
        created.setup()
        idle()
        return created.get()
    }

    private fun TagSearchActivity.searchBox(): EditText = findViewById(R.id.searchTextBox)

    /** Submit the form and follow the intent it produced onto the results screen. */
    private fun submit(
        activity: TagSearchActivity,
        query: String = "navigation fixture",
    ): TagSearchResultsActivity {
        activity.searchBox().setText(query)
        idle()
        activity.findViewById<View>(R.id.searchButton).performClick()
        idle()
        return followToResults(activity, query)
    }

    private fun followToResults(
        activity: TagSearchActivity,
        query: String,
    ): TagSearchResultsActivity {
        val started = shadowOf(activity).nextStartedActivity
        assertNotNull("the form should open the results screen", started)
        assertEquals(
            TagSearchResultsActivity::class.java.name,
            started.component!!.className,
        )
        val created =
            ScreenTestSupport.withLocalData({ _, _ -> Task.forResult(fixture) }) {
                ScreenTestSupport.build(TagSearchResultsActivity::class.java, started).also {
                    it.setup()
                    idle()
                }
            }
        results = created
        val screen = created.get()
        val fragment = screen.supportFragmentManager.findFragmentById(R.id.tagQueryFragment) as TagQueryFragment
        assertEquals("the query reaches the results screen", query, fragment.model!!.query)
        assertEquals("and titles the results screen", query, screen.supportActionBar!!.title)
        return screen
    }

    /** Replace only the query data, as the instrumented fixture did; the fragment stays real. */
    private fun populate(screen: TagSearchResultsActivity): View {
        val fragment = screen.supportFragmentManager.findFragmentById(R.id.tagQueryFragment) as TagQueryFragment
        val query = fragment.model!!.query
        fragment.model =
            QueryModel().apply {
                this.query = query
                hasMoreResults = false
                tags.add(fixture)
            }
        idle()
        val list = screen.findViewById<android.widget.ListView>(R.id.queryResultListView)
        list.setSelection(0)
        idle()
        return requireNotNull(list.getChildAt(0)) { "the single result should be laid out" }
    }

    // ==================== Launch and form ====================

    @Test
    fun activityLaunchesSortedByTitle() {
        assertEquals(TagSortOptions.Title, launch().model.sortBy)
    }

    @Test
    fun searchFormIsDisplayedAndEnabled() {
        val activity = launch()
        assertDisplayed("searchTextBox", activity.searchBox())
        assertTrue("searchTextBox should be enabled", activity.searchBox().isEnabled)
        assertDisplayed("searchButton", activity.findViewById(R.id.searchButton))
        assertTrue(activity.findViewById<View>(R.id.searchButton).isEnabled)
    }

    @Test
    fun tagCollectionSpinnerIsDisplayedAndClickable() {
        val activity = launch()
        val spinner = activity.findViewById<View>(R.id.tagCollectionSpinner)
        assertDisplayed("tagCollectionSpinner", spinner)
        assertTrue("tagCollectionSpinner should be clickable", spinner.isClickable)
    }

    // ==================== Submitting ====================

    @Test
    fun searchButtonSubmitsTheQuery() {
        submit(launch())
    }

    @Test
    fun imeSearchActionSubmitsTheQuery() {
        val activity = launch()
        activity.searchBox().setText("keyboard query")
        idle()
        activity.searchBox().onEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH)
        idle()
        followToResults(activity, "keyboard query")
    }

    @Test
    fun resultsScreenShowsTheQueryFragmentAndItsList() {
        val screen = submit(launch())
        assertDisplayed("tagQueryFragment", screen.findViewById(R.id.tagQueryFragment))
        assertDisplayed("queryResultListView", screen.findViewById(R.id.queryResultListView))
    }

    // ==================== A bound result row ====================

    @Test
    fun resultRowShowsTitleIdAndRating() {
        val row = populate(submit(launch()))
        assertEquals(fixture.title, row.findViewById<TextView>(R.id.titleTextView).text.toString())
        assertEquals(fixture.id.toString(), row.findViewById<TextView>(R.id.idTextView).text.toString())
        assertEquals("4.00", row.findViewById<TextView>(R.id.ratingTextView).text.toString())
        assertDisplayed("ratingContainer", row.findViewById(R.id.ratingContainer))
    }

    @Test
    fun resultRowShowsAvailabilityIndicatorsForTheTagsContents() {
        val row = populate(submit(launch()))
        val sheet = row.findViewById<StatusIndicatorView>(R.id.sheetMusicCheckBox)
        val tracks = row.findViewById<StatusIndicatorView>(R.id.learningTracksCheckBox)
        assertDisplayed("sheetMusicCheckBox", sheet)
        assertDisplayed("learningTracksCheckBox", tracks)
        assertEquals("the fixture has no sheet music", false, sheet.getIsAvailable())
        assertEquals("the fixture has learning tracks", true, tracks.getIsAvailable())
    }

    @Test
    fun resultRowOpensTheMatchingDetailScreen() {
        val screen = submit(launch())
        val row = populate(screen)
        // The row itself carries the click handler; on a device Espresso's tap on the title
        // bubbled up to it.
        row.performClick()
        idle()
        val opened =
            requireNotNull(shadowOf(screen).nextStartedActivity) { "the row should open a tag" }
        assertEquals(TagDetailActivity::class.java.name, opened.component!!.className)
        assertEquals(fixture.id, opened.getIntExtra(TagDetailActivity.TAG_ID_EXTRA, -1))

        // The detail screen resolves that id from the same disk cache the fixture wrote.
        val detail = ScreenTestSupport.launch(TagDetailActivity::class.java, opened)
        try {
            val loaded = ScreenTestSupport.awaitTagLoaded(detail.get())
            assertEquals(fixture.id, detail.get().tagId)
            assertEquals(fixture.title, loaded.title)
            assertEquals(
                fixture.id.toString(),
                detail.get().findViewById<TextView>(R.id.tagIdTextView).text.toString(),
            )
        } finally {
            detail.close()
        }
    }

    // ==================== Filters ====================

    @Test
    fun choosingACollectionUpdatesTheModelAndTheField() {
        val activity = launch()
        chooseDropdown(activity, R.id.tagCollectionSpinner, R.array.TagCollectionChoices, "Classic Tags")
        assertEquals(TagCollection.ClassicTags, activity.model.collection)
        assertEquals(
            "Classic Tags",
            activity.findViewById<MaterialAutoCompleteTextView>(R.id.tagCollectionSpinner).text.toString(),
        )
    }

    @Test
    fun choosingASortUpdatesTheModel() {
        val activity = launch()
        chooseDropdown(activity, R.id.sortBySpinner, R.array.SortByChoices, "Rating")
        assertEquals(TagSortOptions.Rating, activity.model.sortBy)
    }

    @Test
    fun everyFilterReachesTheResultsScreen() {
        val activity = launch()
        chooseDropdown(activity, R.id.partsSpinner, R.array.PartsChoices, "4")
        chooseDropdown(activity, R.id.learningTracksSpinner, R.array.LearningTracksChoices, "Yes")
        chooseDropdown(activity, R.id.sheetMusicSpinner, R.array.SheetMusicChoices, "No")
        chooseDropdown(activity, R.id.tagCollectionSpinner, R.array.TagCollectionChoices, "Easy Tags")
        chooseDropdown(activity, R.id.sortBySpinner, R.array.SortByChoices, "Downloads")

        val screen = submit(activity)
        val model =
            (screen.supportFragmentManager.findFragmentById(R.id.tagQueryFragment) as TagQueryFragment).model!!
        assertEquals(4, model.parts)
        assertEquals(true, model.hasLearningTracks)
        assertEquals(false, model.hasSheetMusic)
        assertEquals(TagCollection.EasyTags, model.collection)
        assertEquals(TagSortOptions.Downloaded, model.sortBy)
    }

    @Test
    fun repeatedCollectionSelectionsUseTheLatestValue() {
        val activity = launch()
        repeat(3) {
            chooseDropdown(activity, R.id.tagCollectionSpinner, R.array.TagCollectionChoices, "Classic Tags")
            chooseDropdown(activity, R.id.tagCollectionSpinner, R.array.TagCollectionChoices, "Any")
        }
        assertNull("'Any' clears the collection filter", activity.model.collection)
        assertEquals(
            "Any",
            activity.findViewById<MaterialAutoCompleteTextView>(R.id.tagCollectionSpinner).text.toString(),
        )
    }

    // ==================== State retention ====================

    @Test
    fun queryIsPreservedAfterRecreation() {
        val created = ScreenTestSupport.build(TagSearchActivity::class.java)
        controller = created
        created.setup()
        idle()
        created.get().searchBox().setText("retained query")
        idle()

        created.recreate()
        idle()

        assertEquals("retained query", created.get().model.query)
        assertEquals("retained query", created.get().searchBox().text.toString())
    }

    @Test
    fun collectionIsPreservedAfterRecreation() {
        val created = ScreenTestSupport.build(TagSearchActivity::class.java)
        controller = created
        created.setup()
        idle()
        chooseDropdown(created.get(), R.id.tagCollectionSpinner, R.array.TagCollectionChoices, "Easy Tags")

        created.recreate()
        idle()

        assertEquals(TagCollection.EasyTags, created.get().model.collection)
        assertEquals(
            "Easy Tags",
            created.get().findViewById<MaterialAutoCompleteTextView>(R.id.tagCollectionSpinner).text.toString(),
        )
    }

    @Test
    fun returningFromResultsRetainsTheQuery() {
        val activity = launch()
        submit(activity)
        results?.close()
        results = null
        // Back returns to the form, which still holds the submitted query.
        controller!!.pause().resume()
        idle()
        assertEquals("navigation fixture", activity.model.query)
        assertEquals("navigation fixture", activity.searchBox().text.toString())
    }
}
