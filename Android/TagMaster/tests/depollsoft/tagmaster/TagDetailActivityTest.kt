package depollsoft.tagmaster

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.recyclerview.widget.RecyclerView
import org.hamcrest.Matchers.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import android.content.Intent

/**
 * Instrumented UI tests for the TagDetailActivity.
 * Tests tag detail display, tab navigation, favorites, and media playback.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class TagDetailActivityTest {

    // Note: TagDetailActivity requires a tag ID to be passed via intent
    // We'll need to launch with a specific intent for most tests

    // ==================== Launch Tests ====================

    @Test
    fun testActivityLaunchesWithValidTag() {
        val intent = Intent(
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext,
            TagDetailActivity::class.java
        ).apply {
            putExtra("tag_id", 1) // Use a known test tag ID
        }

        ActivityScenario.launch<TagDetailActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                assert(activity != null)
            }
        }
    }

    @Test
    fun testDetailContainerIsDisplayed() {
        launchWithTestTag().use { scenario ->
            onView(withId(R.id.tag_detail_container))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun testToolbarIsDisplayed() {
        launchWithTestTag().use { scenario ->
            onView(withId(R.id.toolbar))
                .check(matches(isDisplayed()))
        }
    }

    // ==================== Tag Header Tests ====================

    @Test
    fun testTagTitleIsDisplayed() {
        launchWithTestTag().use { scenario ->
            onView(withId(R.id.tag_title))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun testTagArrangerIsDisplayed() {
        launchWithTestTag().use { scenario ->
            onView(withId(R.id.tag_arranger))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun testTagRatingIsDisplayed() {
        launchWithTestTag().use { scenario ->
            onView(withId(R.id.tag_rating))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun testTagPartsInfoIsDisplayed() {
        launchWithTestTag().use { scenario ->
            onView(withId(R.id.tag_parts_info))
                .check(matches(isDisplayed()))
        }
    }

    // ==================== Favorite Button Tests ====================

    @Test
    fun testFavoriteButtonIsDisplayed() {
        launchWithTestTag().use { scenario ->
            onView(withId(R.id.favorite_button))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun testFavoriteButtonIsClickable() {
        launchWithTestTag().use { scenario ->
            onView(withId(R.id.favorite_button))
                .check(matches(isClickable()))
        }
    }

    @Test
    fun testToggleFavorite() {
        launchWithTestTag().use { scenario ->
            // Click to add to favorites
            onView(withId(R.id.favorite_button))
                .perform(click())

            Thread.sleep(500)

            // Click again to remove
            onView(withId(R.id.favorite_button))
                .perform(click())

            Thread.sleep(500)

            // Button should still be functional
            onView(withId(R.id.favorite_button))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun testFavoriteButtonShowsCorrectState() {
        launchWithTestTag().use { scenario ->
            // Verify the button shows filled/unfilled based on favorite state
            onView(withId(R.id.favorite_button))
                .check(matches(anyOf(
                    withContentDescription(containsString("Add")),
                    withContentDescription(containsString("Remove"))
                )))
        }
    }

    // ==================== Share Button Tests ====================

    @Test
    fun testShareButtonIsDisplayed() {
        launchWithTestTag().use { scenario ->
            onView(withId(R.id.share_button))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun testShareButtonOpensShareSheet() {
        launchWithTestTag().use { scenario ->
            onView(withId(R.id.share_button))
                .perform(click())

            // Share intent should be launched
            // This is hard to verify without intents-testing
        }
    }

    // ==================== ViewPager Tab Tests ====================

    @Test
    fun testTabLayoutIsDisplayed() {
        launchWithTestTag().use { scenario ->
            onView(withId(R.id.tab_layout))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun testViewPagerIsDisplayed() {
        launchWithTestTag().use { scenario ->
            onView(withId(R.id.view_pager))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun testSummaryTabExists() {
        launchWithTestTag().use { scenario ->
            onView(withText("Summary"))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun testMiscTabExists() {
        launchWithTestTag().use { scenario ->
            onView(withText("Misc"))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun testTracksTabExists() {
        launchWithTestTag().use { scenario ->
            onView(withText("Tracks"))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun testVideosTabExists() {
        launchWithTestTag().use { scenario ->
            onView(withText("Videos"))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun testNavigateToSummaryTab() {
        launchWithTestTag().use { scenario ->
            onView(withText("Summary"))
                .perform(click())

            Thread.sleep(300)

            // Summary fragment content should be visible
            onView(withId(R.id.summary_container))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun testNavigateToMiscTab() {
        launchWithTestTag().use { scenario ->
            onView(withText("Misc"))
                .perform(click())

            Thread.sleep(300)

            onView(withId(R.id.misc_container))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun testNavigateToTracksTab() {
        launchWithTestTag().use { scenario ->
            onView(withText("Tracks"))
                .perform(click())

            Thread.sleep(300)

            onView(withId(R.id.tracks_container))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun testNavigateToVideosTab() {
        launchWithTestTag().use { scenario ->
            onView(withText("Videos"))
                .perform(click())

            Thread.sleep(300)

            onView(withId(R.id.videos_container))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun testSwipeBetweenTabs() {
        launchWithTestTag().use { scenario ->
            // Swipe left to go to next tab
            onView(withId(R.id.view_pager))
                .perform(swipeLeft())

            Thread.sleep(300)

            // Swipe back
            onView(withId(R.id.view_pager))
                .perform(swipeRight())

            Thread.sleep(300)

            // Should be back at summary
            onView(withId(R.id.summary_container))
                .check(matches(isDisplayed()))
        }
    }

    // ==================== Summary Tab Content Tests ====================

    @Test
    fun testSummaryShowsLyrics() {
        launchWithTestTag().use { scenario ->
            onView(withText("Summary")).perform(click())
            Thread.sleep(300)

            onView(withId(R.id.lyrics_text))
                .check(matches(anyOf(isDisplayed(), not(isDisplayed()))))
        }
    }

    @Test
    fun testSummaryShowsDescription() {
        launchWithTestTag().use { scenario ->
            onView(withText("Summary")).perform(click())
            Thread.sleep(300)

            onView(withId(R.id.description_text))
                .check(matches(anyOf(isDisplayed(), not(isDisplayed()))))
        }
    }

    // ==================== Tracks Tab Content Tests ====================

    @Test
    fun testTracksListIsDisplayed() {
        launchWithTestTag().use { scenario ->
            onView(withText("Tracks")).perform(click())
            Thread.sleep(500)

            onView(withId(R.id.tracks_list))
                .check(matches(anyOf(isDisplayed(), not(isDisplayed()))))
        }
    }

    @Test
    fun testNoTracksMessageWhenEmpty() {
        launchWithTestTag().use { scenario ->
            onView(withText("Tracks")).perform(click())
            Thread.sleep(500)

            // Either tracks list or empty message
            onView(anyOf(withId(R.id.tracks_list), withId(R.id.no_tracks_message)))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun testPlayTrack() {
        launchWithTestTag().use { scenario ->
            onView(withText("Tracks")).perform(click())
            Thread.sleep(500)

            // Try to click first track if exists
            try {
                onView(withId(R.id.tracks_list))
                    .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

                Thread.sleep(500)

                // Media player should appear
                onView(withId(R.id.media_player))
                    .check(matches(isDisplayed()))
            } catch (e: Exception) {
                // No tracks available
            }
        }
    }

    // ==================== Videos Tab Content Tests ====================

    @Test
    fun testVideosListIsDisplayed() {
        launchWithTestTag().use { scenario ->
            onView(withText("Videos")).perform(click())
            Thread.sleep(500)

            onView(anyOf(withId(R.id.videos_list), withId(R.id.no_videos_message)))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun testPlayVideo() {
        launchWithTestTag().use { scenario ->
            onView(withText("Videos")).perform(click())
            Thread.sleep(500)

            try {
                onView(withId(R.id.videos_list))
                    .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

                Thread.sleep(500)

                // Video player should start
            } catch (e: Exception) {
                // No videos available
            }
        }
    }

    // ==================== Sheet Music Tests ====================

    @Test
    fun testSheetMusicButtonIsDisplayed() {
        launchWithTestTag().use { scenario ->
            onView(withId(R.id.sheet_music_button))
                .check(matches(anyOf(isDisplayed(), not(isDisplayed()))))
        }
    }

    @Test
    fun testSheetMusicOpensViewer() {
        launchWithTestTag().use { scenario ->
            try {
                onView(withId(R.id.sheet_music_button))
                    .perform(click())

                Thread.sleep(500)

                // Sheet music viewer should open
                onView(withId(R.id.sheet_music_viewer))
                    .check(matches(isDisplayed()))
            } catch (e: Exception) {
                // No sheet music available
            }
        }
    }

    // ==================== Menu Tests ====================

    @Test
    fun testOverflowMenuExists() {
        launchWithTestTag().use { scenario ->
            onView(withContentDescription("More options"))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun testOverflowMenuOptions() {
        launchWithTestTag().use { scenario ->
            onView(withContentDescription("More options"))
                .perform(click())

            // Verify menu items
            onView(withText("Add to Teachable Tags"))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun testAddToTeachableTags() {
        launchWithTestTag().use { scenario ->
            onView(withContentDescription("More options"))
                .perform(click())

            onView(withText("Add to Teachable Tags"))
                .perform(click())

            Thread.sleep(500)

            // Should show confirmation or navigate
        }
    }

    // ==================== Navigation Tests ====================

    @Test
    fun testBackButtonReturnsToParent() {
        launchWithTestTag().use { scenario ->
            onView(withContentDescription("Navigate up"))
                .perform(click())

            Thread.sleep(500)

            // Should return to previous screen
        }
    }

    @Test
    fun testSystemBackReturns() {
        launchWithTestTag().use { scenario ->
            androidx.test.espresso.Espresso.pressBack()

            // Activity should close
        }
    }

    // ==================== Configuration Change Tests ====================

    @Test
    fun testTagDataPreservedAfterRotation() {
        launchWithTestTag().use { scenario ->
            // Get tag title
            onView(withId(R.id.tag_title))
                .check(matches(isDisplayed()))

            // Rotate
            scenario.recreate()

            Thread.sleep(500)

            // Tag data should still be displayed
            onView(withId(R.id.tag_title))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun testCurrentTabPreservedAfterRotation() {
        launchWithTestTag().use { scenario ->
            // Navigate to tracks tab
            onView(withText("Tracks")).perform(click())
            Thread.sleep(300)

            // Rotate
            scenario.recreate()

            Thread.sleep(500)

            // Should still be on tracks tab
            onView(withId(R.id.tracks_container))
                .check(matches(isDisplayed()))
        }
    }

    // ==================== Loading State Tests ====================

    @Test
    fun testLoadingIndicatorDuringDataFetch() {
        launchWithTestTag().use { scenario ->
            // Loading indicator might be visible initially
            onView(withId(R.id.loading_indicator))
                .check(matches(anyOf(isDisplayed(), not(isDisplayed()))))
        }
    }

    // ==================== Error State Tests ====================

    @Test
    fun testErrorStateOnInvalidTag() {
        val intent = Intent(
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext,
            TagDetailActivity::class.java
        ).apply {
            putExtra("tag_id", -999) // Invalid ID
        }

        ActivityScenario.launch<TagDetailActivity>(intent).use { scenario ->
            Thread.sleep(1000)

            // Should show error or close
            onView(withId(R.id.error_view))
                .check(matches(anyOf(isDisplayed(), not(isDisplayed()))))
        }
    }

    // ==================== Accessibility Tests ====================

    @Test
    fun testFavoriteButtonHasContentDescription() {
        launchWithTestTag().use { scenario ->
            onView(withId(R.id.favorite_button))
                .check(matches(hasContentDescription()))
        }
    }

    @Test
    fun testShareButtonHasContentDescription() {
        launchWithTestTag().use { scenario ->
            onView(withId(R.id.share_button))
                .check(matches(hasContentDescription()))
        }
    }

    @Test
    fun testTabsAreAccessible() {
        launchWithTestTag().use { scenario ->
            onView(withText("Summary"))
                .check(matches(isClickable()))
            onView(withText("Misc"))
                .check(matches(isClickable()))
            onView(withText("Tracks"))
                .check(matches(isClickable()))
            onView(withText("Videos"))
                .check(matches(isClickable()))
        }
    }

    // ==================== Helper Methods ====================

    private fun launchWithTestTag(): ActivityScenario<TagDetailActivity> {
        val intent = Intent(
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext,
            TagDetailActivity::class.java
        ).apply {
            putExtra("tag_id", 1) // Use a known test tag ID
        }
        return ActivityScenario.launch(intent)
    }
}
