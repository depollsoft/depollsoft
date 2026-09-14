package depollsoft.tagmaster

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import depollsoft.tagmaster.NavigationTestFixture.Companion.onResumed
import depollsoft.tagmaster.barbershop.TagCollection
import depollsoft.tagmaster.barbershop.TagSortOptions
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/** Real Search form -> results fragment -> bound row -> cached Detail. */
@RunWith(AndroidJUnit4::class)
@LargeTest
class TagSearchActivityTest {
    private val fixture = NavigationTestFixture()
    private val activityRule = ActivityScenarioRule(TagSearchActivity::class.java)
    @get:Rule val rules: RuleChain = RuleChain.outerRule(fixture).around(activityRule)

    @Test fun testActivityLaunches() { onResumed<TagSearchActivity> { assertEquals(TagSortOptions.Title, it.model.sortBy) } }
    @Test fun testSearchFormIsDisplayed() { onView(withId(R.id.searchTextBox)).check(matches(isDisplayed())) }
    @Test fun testSearchActionIsDisplayed() { onView(withId(R.id.searchButton)).check(matches(isDisplayed())) }
    @Test fun testTagQueryFragmentExists() {
        submit()
        onView(withId(R.id.tagQueryFragment)).check(matches(isDisplayed()))
        onView(withId(R.id.queryResultListView)).check(matches(isDisplayed()))
    }
    @Test fun testTagCollectionSpinnerExists() { onView(withId(R.id.tagCollectionSpinner)).check(matches(isDisplayed())) }
    @Test fun testTagCollectionSpinnerIsClickable() { onView(withId(R.id.tagCollectionSpinner)).check(matches(isClickable())) }
    @Test fun testTagCollectionSpinnerCanOpen() {
        choose(R.id.tagCollectionSpinner, "Classic Tags")
        activityRule.scenario.onActivity { assertEquals(TagCollection.ClassicTags, it.model.collection) }
        onView(withId(R.id.tagCollectionSpinner)).check(matches(withText("Classic Tags")))
    }
    @Test fun testTagItemTitleTextViewExists() { results(); onView(withId(R.id.titleTextView)).check(matches(withText(fixture.tag.title))) }
    @Test fun testTagIdTextViewExists() { results(); onView(withId(R.id.idTextView)).check(matches(withText(fixture.tag.id.toString()))) }
    @Test fun testResultRowOpensMatchingDetail() {
        results()
        onView(withId(R.id.titleTextView)).perform(click())
        EspressoTestUtils.waitForView(withId(R.id.tabLayout))
        onResumed<TagDetailActivity> { assertEquals(fixture.tag.id, it.tagId); assertEquals(fixture.tag.title, it.tag!!.title) }
        onView(withId(R.id.tagIdTextView)).check(matches(withText(fixture.tag.id.toString())))
    }
    @Test fun testSheetMusicAvailabilityIndicatorExists() {
        results()
        onView(withId(R.id.sheetMusicCheckBox)).check(matches(isDisplayed()))
        onResumed<TagSearchResultsActivity> {
            assertFalse(it.findViewById<StatusIndicatorView>(R.id.sheetMusicCheckBox).getIsAvailable())
        }
    }
    @Test fun testLearningTrackAvailabilityIndicatorExists() {
        results()
        onView(withId(R.id.learningTracksCheckBox)).check(matches(isDisplayed()))
        onResumed<TagSearchResultsActivity> {
            assertTrue(it.findViewById<StatusIndicatorView>(R.id.learningTracksCheckBox).getIsAvailable())
        }
    }
    @Test fun testRatingContainerExists() { results(); onView(withId(R.id.ratingContainer)).check(matches(isDisplayed())) }
    @Test fun testRatingTextViewExists() { results(); onView(withId(R.id.ratingTextView)).check(matches(withText("4.00"))) }
    @Test fun testSearchButtonSubmitsQuery() { submit(); assertQuery("navigation fixture") }
    @Test fun testImeSearchSubmitsQuery() {
        onView(withId(R.id.searchTextBox)).perform(replaceText("keyboard query"), pressImeActionButton())
        // IME dispatch can return while the Search activity is still RESUMED.
        EspressoTestUtils.waitForView(withId(R.id.tagQueryFragment))
        assertQuery("keyboard query")
    }
    @Test fun testBackFromResultsRetainsQuery() {
        submit()
        pressBack()
        onResumed<TagSearchActivity> { assertEquals("navigation fixture", it.model.query) }
        onView(withId(R.id.searchTextBox)).check(matches(withText("navigation fixture")))
    }
    @Test fun testSortSelectionIsApplied() {
        choose(R.id.sortBySpinner, "Rating")
        activityRule.scenario.onActivity { assertEquals(TagSortOptions.Rating, it.model.sortBy) }
    }
    @Test fun testFiltersReachResults() {
        choose(R.id.partsSpinner, "4")
        choose(R.id.learningTracksSpinner, "Yes")
        choose(R.id.sheetMusicSpinner, "No")
        choose(R.id.tagCollectionSpinner, "Easy Tags")
        choose(R.id.sortBySpinner, "Downloads")
        submit()
        onResumed<TagSearchResultsActivity> {
            val model = (it.supportFragmentManager.findFragmentById(R.id.tagQueryFragment) as TagQueryFragment).model!!
            assertEquals(4, model.parts)
            assertEquals(true, model.hasLearningTracks)
            assertEquals(false, model.hasSheetMusic)
            assertEquals(TagCollection.EasyTags, model.collection)
            assertEquals(TagSortOptions.Downloaded, model.sortBy)
        }
    }
    @Test fun testQueryPreservedAfterRecreation() {
        onView(withId(R.id.searchTextBox)).perform(replaceText("retained query"), closeSoftKeyboard())
        activityRule.scenario.recreate()
        onView(withId(R.id.searchTextBox)).check(matches(withText("retained query")))
        activityRule.scenario.onActivity { assertEquals("retained query", it.model.query) }
    }
    @Test fun testCollectionPreservedAfterRecreation() {
        choose(R.id.tagCollectionSpinner, "Easy Tags")
        activityRule.scenario.recreate()
        onView(withId(R.id.tagCollectionSpinner)).check(matches(withText("Easy Tags")))
        activityRule.scenario.onActivity { assertEquals(TagCollection.EasyTags, it.model.collection) }
    }
    @Test fun testQueryFieldIsEnabled() { onView(withId(R.id.searchTextBox)).check(matches(isEnabled())) }
    @Test fun testSearchActionIsEnabled() { onView(withId(R.id.searchButton)).check(matches(isEnabled())) }
    @Test fun testRepeatedCollectionSelectionsUseLatestValue() {
        repeat(3) { choose(R.id.tagCollectionSpinner, "Classic Tags"); choose(R.id.tagCollectionSpinner, "Any") }
        activityRule.scenario.onActivity { assertNull(it.model.collection) }
        onView(withId(R.id.tagCollectionSpinner)).check(matches(withText("Any")))
    }

    private fun choose(id: Int, text: String) {
        onView(withId(id)).perform(scrollTo(), click())
        onView(withText(text)).inRoot(isPlatformPopup()).perform(click())
    }
    private fun submit() {
        onView(withId(R.id.searchTextBox)).perform(replaceText("navigation fixture"), closeSoftKeyboard())
        onView(withId(R.id.searchButton)).perform(click())
        assertQuery("navigation fixture")
    }
    private fun assertQuery(query: String) {
        onResumed<TagSearchResultsActivity> {
            val fragment = it.supportFragmentManager.findFragmentById(R.id.tagQueryFragment) as TagQueryFragment
            assertEquals(query, fragment.model!!.query)
            assertEquals(query, it.supportActionBar!!.title)
        }
    }
    private fun results() { submit(); fixture.populateResults() }
}
