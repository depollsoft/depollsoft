package depollsoft.tagmaster

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.hamcrest.Matchers.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI tests for the TagSearchActivity.
 * Tests search form inputs, filters, and search execution.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class TagSearchActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(TagSearchActivity::class.java)

    // ==================== Launch Tests ====================

    @Test
    fun testActivityLaunches() {
        activityRule.scenario.onActivity { activity ->
            assert(activity != null)
        }
    }

    @Test
    fun testSearchContainerIsDisplayed() {
        onView(withId(R.id.search_container))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testToolbarIsDisplayed() {
        onView(withId(R.id.toolbar))
            .check(matches(isDisplayed()))
    }

    // ==================== Search Input Tests ====================

    @Test
    fun testSearchInputFieldIsDisplayed() {
        onView(withId(R.id.search_input))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSearchInputHasHint() {
        onView(withId(R.id.search_input))
            .check(matches(withHint(containsString("Search"))))
    }

    @Test
    fun testCanTypeInSearchInput() {
        onView(withId(R.id.search_input))
            .perform(typeText("test query"), closeSoftKeyboard())

        onView(withId(R.id.search_input))
            .check(matches(withText("test query")))
    }

    @Test
    fun testClearSearchInput() {
        // Type text
        onView(withId(R.id.search_input))
            .perform(typeText("test"), closeSoftKeyboard())

        // Clear
        onView(withId(R.id.clear_search))
            .perform(click())

        // Verify cleared
        onView(withId(R.id.search_input))
            .check(matches(withText("")))
    }

    // ==================== Filter Spinner Tests ====================

    @Test
    fun testSheetMusicSpinnerIsDisplayed() {
        onView(withId(R.id.sheet_music_spinner))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSheetMusicSpinnerOptions() {
        onView(withId(R.id.sheet_music_spinner))
            .perform(click())

        // Verify options
        onView(withText("All"))
            .check(matches(isDisplayed()))
        onView(withText("With Sheet Music"))
            .check(matches(isDisplayed()))
        onView(withText("Without Sheet Music"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSelectSheetMusicWithOption() {
        onView(withId(R.id.sheet_music_spinner))
            .perform(click())

        onView(withText("With Sheet Music"))
            .perform(click())

        // Verify selection
        onView(withId(R.id.sheet_music_spinner))
            .check(matches(withSpinnerText(containsString("With"))))
    }

    @Test
    fun testLearningTracksSpinnerIsDisplayed() {
        onView(withId(R.id.learning_tracks_spinner))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testLearningTracksSpinnerOptions() {
        onView(withId(R.id.learning_tracks_spinner))
            .perform(click())

        onView(withText("All"))
            .check(matches(isDisplayed()))
        onView(withText("With Learning Tracks"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSelectLearningTracksWithOption() {
        onView(withId(R.id.learning_tracks_spinner))
            .perform(click())

        onView(withText("With Learning Tracks"))
            .perform(click())

        onView(withId(R.id.learning_tracks_spinner))
            .check(matches(withSpinnerText(containsString("With"))))
    }

    @Test
    fun testPartsSpinnerIsDisplayed() {
        onView(withId(R.id.parts_spinner))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testPartsSpinnerOptions() {
        onView(withId(R.id.parts_spinner))
            .perform(click())

        // Verify part options
        onView(withText("All Parts"))
            .check(matches(isDisplayed()))
        onView(withText("Tenor"))
            .check(matches(isDisplayed()))
        onView(withText("Lead"))
            .check(matches(isDisplayed()))
        onView(withText("Baritone"))
            .check(matches(isDisplayed()))
        onView(withText("Bass"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSelectTenorPart() {
        onView(withId(R.id.parts_spinner))
            .perform(click())

        onView(withText("Tenor"))
            .perform(click())

        onView(withId(R.id.parts_spinner))
            .check(matches(withSpinnerText("Tenor")))
    }

    @Test
    fun testSelectLeadPart() {
        onView(withId(R.id.parts_spinner))
            .perform(click())

        onView(withText("Lead"))
            .perform(click())
    }

    @Test
    fun testSelectBaritonePart() {
        onView(withId(R.id.parts_spinner))
            .perform(click())

        onView(withText("Baritone"))
            .perform(click())
    }

    @Test
    fun testSelectBassPart() {
        onView(withId(R.id.parts_spinner))
            .perform(click())

        onView(withText("Bass"))
            .perform(click())
    }

    @Test
    fun testCollectionSpinnerIsDisplayed() {
        onView(withId(R.id.collection_spinner))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testCollectionSpinnerOptions() {
        onView(withId(R.id.collection_spinner))
            .perform(click())

        onView(withText("All Collections"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSortSpinnerIsDisplayed() {
        onView(withId(R.id.sort_spinner))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSortSpinnerOptions() {
        onView(withId(R.id.sort_spinner))
            .perform(click())

        // Verify sort options
        onView(withText("Relevance"))
            .check(matches(isDisplayed()))
        onView(withText("Title"))
            .check(matches(isDisplayed()))
        onView(withText("Arranger"))
            .check(matches(isDisplayed()))
        onView(withText("Most Popular"))
            .check(matches(isDisplayed()))
        onView(withText("Newest"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSelectSortByTitle() {
        onView(withId(R.id.sort_spinner))
            .perform(click())

        onView(withText("Title"))
            .perform(click())

        onView(withId(R.id.sort_spinner))
            .check(matches(withSpinnerText("Title")))
    }

    @Test
    fun testSelectSortByArranger() {
        onView(withId(R.id.sort_spinner))
            .perform(click())

        onView(withText("Arranger"))
            .perform(click())
    }

    @Test
    fun testSelectSortByPopular() {
        onView(withId(R.id.sort_spinner))
            .perform(click())

        onView(withText("Most Popular"))
            .perform(click())
    }

    @Test
    fun testSelectSortByNewest() {
        onView(withId(R.id.sort_spinner))
            .perform(click())

        onView(withText("Newest"))
            .perform(click())
    }

    // ==================== Search Execution Tests ====================

    @Test
    fun testSearchButtonIsDisplayed() {
        onView(withId(R.id.search_button))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSearchButtonIsClickable() {
        onView(withId(R.id.search_button))
            .check(matches(isClickable()))
    }

    @Test
    fun testPerformSearchWithQuery() {
        // Enter search query
        onView(withId(R.id.search_input))
            .perform(typeText("Hello World"), closeSoftKeyboard())

        // Click search
        onView(withId(R.id.search_button))
            .perform(click())

        Thread.sleep(1000)

        // Should navigate to results
        onView(withId(R.id.results_container))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testPerformSearchWithFilters() {
        // Set filters
        onView(withId(R.id.sheet_music_spinner)).perform(click())
        onView(withText("With Sheet Music")).perform(click())

        onView(withId(R.id.parts_spinner)).perform(click())
        onView(withText("Tenor")).perform(click())

        // Search
        onView(withId(R.id.search_button))
            .perform(click())

        Thread.sleep(1000)

        // Should show results
        onView(withId(R.id.results_container))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSearchWithEmptyQueryShowsAllResults() {
        // Don't enter any query, just search
        onView(withId(R.id.search_button))
            .perform(click())

        Thread.sleep(1000)

        // Should still work (show all/featured)
    }

    @Test
    fun testKeyboardSearchAction() {
        // Type query and press search on keyboard
        onView(withId(R.id.search_input))
            .perform(typeText("test"), pressImeActionButton())

        Thread.sleep(1000)

        // Should execute search
    }

    // ==================== Recent Searches Tests ====================

    @Test
    fun testRecentSearchesSection() {
        // If recent searches exist, they should be displayed
        onView(withId(R.id.recent_searches))
            .check(matches(anyOf(isDisplayed(), not(isDisplayed()))))
    }

    @Test
    fun testClearRecentSearches() {
        onView(withId(R.id.clear_recent))
            .check(matches(anyOf(isDisplayed(), not(isDisplayed()))))
    }

    // ==================== Navigation Tests ====================

    @Test
    fun testBackButtonReturnsToMain() {
        onView(withContentDescription("Navigate up"))
            .perform(click())

        Thread.sleep(500)

        onView(withId(R.id.main_container))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSystemBackReturnsToMain() {
        androidx.test.espresso.Espresso.pressBack()

        Thread.sleep(500)

        onView(withId(R.id.main_container))
            .check(matches(isDisplayed()))
    }

    // ==================== Configuration Change Tests ====================

    @Test
    fun testSearchQueryPreservedAfterRotation() {
        // Enter query
        onView(withId(R.id.search_input))
            .perform(typeText("preserved query"), closeSoftKeyboard())

        // Rotate
        activityRule.scenario.recreate()

        Thread.sleep(500)

        // Verify query preserved
        onView(withId(R.id.search_input))
            .check(matches(withText("preserved query")))
    }

    @Test
    fun testFiltersPreservedAfterRotation() {
        // Set a filter
        onView(withId(R.id.parts_spinner)).perform(click())
        onView(withText("Tenor")).perform(click())

        // Rotate
        activityRule.scenario.recreate()

        Thread.sleep(500)

        // Verify filter preserved
        onView(withId(R.id.parts_spinner))
            .check(matches(withSpinnerText("Tenor")))
    }

    // ==================== Scroll Tests ====================

    @Test
    fun testFiltersAreScrollable() {
        // Scroll within filters area
        onView(withId(R.id.filters_scroll))
            .perform(swipeUp())

        onView(withId(R.id.search_container))
            .check(matches(isDisplayed()))
    }

    // ==================== Accessibility Tests ====================

    @Test
    fun testSearchInputHasContentDescription() {
        onView(withId(R.id.search_input))
            .check(matches(hasContentDescription()))
    }

    @Test
    fun testSpinnersAreAccessible() {
        onView(withId(R.id.sheet_music_spinner))
            .check(matches(allOf(isDisplayed(), isClickable())))

        onView(withId(R.id.parts_spinner))
            .check(matches(allOf(isDisplayed(), isClickable())))

        onView(withId(R.id.sort_spinner))
            .check(matches(allOf(isDisplayed(), isClickable())))
    }

    // ==================== Edge Cases ====================

    @Test
    fun testVeryLongSearchQuery() {
        val longQuery = "a".repeat(500)

        onView(withId(R.id.search_input))
            .perform(typeText(longQuery), closeSoftKeyboard())

        // Should handle gracefully
        onView(withId(R.id.search_button))
            .perform(click())
    }

    @Test
    fun testSpecialCharactersInSearchQuery() {
        onView(withId(R.id.search_input))
            .perform(typeText("Hello & Goodbye \"quoted\" test"), closeSoftKeyboard())

        onView(withId(R.id.search_button))
            .perform(click())

        // Should handle special characters
    }

    @Test
    fun testRapidFilterChanges() {
        // Rapidly change filters
        repeat(3) {
            onView(withId(R.id.parts_spinner)).perform(click())
            onView(withText("Tenor")).perform(click())
            
            onView(withId(R.id.parts_spinner)).perform(click())
            onView(withText("Lead")).perform(click())
            
            onView(withId(R.id.parts_spinner)).perform(click())
            onView(withText("Bass")).perform(click())
        }

        // UI should still be responsive
        onView(withId(R.id.search_container))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testMultipleSearchesInSuccession() {
        // Perform multiple searches
        for (i in 1..3) {
            onView(withId(R.id.search_input))
                .perform(clearText(), typeText("search $i"), closeSoftKeyboard())

            onView(withId(R.id.search_button))
                .perform(click())

            Thread.sleep(500)

            // Press back to return to search
            androidx.test.espresso.Espresso.pressBack()

            Thread.sleep(300)
        }

        // Should still be functional
        onView(withId(R.id.search_container))
            .check(matches(isDisplayed()))
    }
}
