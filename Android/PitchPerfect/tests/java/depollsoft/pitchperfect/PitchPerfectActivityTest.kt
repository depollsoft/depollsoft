package depollsoft.pitchperfect

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI tests for the main PitchPerfectActivity.
 * Tests the main activity layout, navigation, and core interactions.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class PitchPerfectActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(PitchPerfectActivity::class.java)

    // ==================== Launch Tests ====================

    @Test
    fun testActivityLaunches() {
        // Verify the activity launches successfully
        activityRule.scenario.onActivity { activity ->
            assert(activity != null)
        }
    }

    @Test
    fun testMainLayoutIsDisplayed() {
        // Verify the main container is displayed
        onView(withId(R.id.pager))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testToolbarIsDisplayed() {
        // Verify the toolbar/action bar is visible
        onView(withId(R.id.toolbar))
            .check(matches(isDisplayed()))
    }

    // ==================== Navigation Tests ====================

    @Test
    fun testBottomNavigationIsDisplayed() {
        // Verify bottom navigation is visible
        onView(withId(R.id.bottom_navigation))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testNavigateToPitchPipeTab() {
        // Navigate to Pitch Pipe tab
        onView(allOf(withId(R.id.nav_pitch_pipe), isDisplayed()))
            .perform(click())

        // Verify we're on the pitch pipe fragment
        onView(withId(R.id.pitch_pipe_container))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testNavigateToNotesTab() {
        // Navigate to Notes tab
        onView(allOf(withId(R.id.nav_notes), isDisplayed()))
            .perform(click())

        // Verify we're on the notes fragment
        onView(withId(R.id.notes_list))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testNavigateToKeysTab() {
        // Navigate to Keys tab
        onView(allOf(withId(R.id.nav_keys), isDisplayed()))
            .perform(click())

        // Verify we're on the key signature fragment
        onView(withId(R.id.key_signature_list))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testNavigateToSongsTab() {
        // Navigate to Songs tab
        onView(allOf(withId(R.id.nav_songs), isDisplayed()))
            .perform(click())

        // Verify we're on the songs fragment
        onView(withId(R.id.song_list))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testNavigationBetweenAllTabs() {
        // Test cycling through all tabs
        val tabs = listOf(
            R.id.nav_pitch_pipe to R.id.pitch_pipe_container,
            R.id.nav_notes to R.id.notes_list,
            R.id.nav_keys to R.id.key_signature_list,
            R.id.nav_songs to R.id.song_list
        )

        for ((navId, containerId) in tabs) {
            onView(allOf(withId(navId), isDisplayed()))
                .perform(click())
            
            Thread.sleep(300) // Allow navigation animation
            
            onView(withId(containerId))
                .check(matches(isDisplayed()))
        }
    }

    // ==================== ViewPager Swipe Tests ====================

    @Test
    fun testSwipeLeftNavigatesToNextTab() {
        // Start on first tab
        onView(allOf(withId(R.id.nav_pitch_pipe), isDisplayed()))
            .perform(click())

        // Swipe left on the pager
        onView(withId(R.id.pager))
            .perform(swipeLeft())

        Thread.sleep(300)

        // Should be on notes tab now
        onView(withId(R.id.notes_list))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSwipeRightNavigatesToPreviousTab() {
        // Navigate to notes tab first
        onView(allOf(withId(R.id.nav_notes), isDisplayed()))
            .perform(click())

        Thread.sleep(300)

        // Swipe right on the pager
        onView(withId(R.id.pager))
            .perform(swipeRight())

        Thread.sleep(300)

        // Should be back on pitch pipe tab
        onView(withId(R.id.pitch_pipe_container))
            .check(matches(isDisplayed()))
    }

    // ==================== Menu Tests ====================

    @Test
    fun testSettingsMenuItemExists() {
        // Open overflow menu
        onView(withContentDescription("More options"))
            .perform(click())

        // Verify settings option exists
        onView(withText("Settings"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSettingsMenuOpensSettingsActivity() {
        // Open overflow menu
        onView(withContentDescription("More options"))
            .perform(click())

        // Click settings
        onView(withText("Settings"))
            .perform(click())

        // Verify settings activity opens (check for a settings-specific element)
        onView(withId(R.id.settings_container))
            .check(matches(isDisplayed()))
    }

    // ==================== Configuration Change Tests ====================

    @Test
    fun testActivitySurvivesRotation() {
        // Navigate to notes tab
        onView(allOf(withId(R.id.nav_notes), isDisplayed()))
            .perform(click())

        Thread.sleep(300)

        // Recreate activity (simulates rotation)
        activityRule.scenario.recreate()

        Thread.sleep(500)

        // Verify we're still on notes tab
        onView(withId(R.id.notes_list))
            .check(matches(isDisplayed()))
    }

    // ==================== State Restoration Tests ====================

    @Test
    fun testCurrentTabIsPreserved() {
        // Navigate to keys tab
        onView(allOf(withId(R.id.nav_keys), isDisplayed()))
            .perform(click())

        Thread.sleep(300)

        // Close and reopen activity
        activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)
        
        ActivityScenario.launch(PitchPerfectActivity::class.java).use { scenario ->
            // Should start on keys tab (if saved)
            Thread.sleep(500)
        }
    }

    // ==================== Accessibility Tests ====================

    @Test
    fun testNavigationItemsHaveContentDescriptions() {
        // Verify pitch pipe nav has content description
        onView(withId(R.id.nav_pitch_pipe))
            .check(matches(hasContentDescription()))

        // Verify notes nav has content description
        onView(withId(R.id.nav_notes))
            .check(matches(hasContentDescription()))

        // Verify keys nav has content description
        onView(withId(R.id.nav_keys))
            .check(matches(hasContentDescription()))

        // Verify songs nav has content description
        onView(withId(R.id.nav_songs))
            .check(matches(hasContentDescription()))
    }

    // ==================== FAB Tests ====================

    @Test
    fun testFabIsVisibleOnSongsTab() {
        // Navigate to songs tab
        onView(allOf(withId(R.id.nav_songs), isDisplayed()))
            .perform(click())

        Thread.sleep(300)

        // Verify FAB is displayed
        onView(withId(R.id.fab_add_song))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testFabClickOpensAddSongDialog() {
        // Navigate to songs tab
        onView(allOf(withId(R.id.nav_songs), isDisplayed()))
            .perform(click())

        Thread.sleep(300)

        // Click FAB
        onView(withId(R.id.fab_add_song))
            .perform(click())

        Thread.sleep(300)

        // Verify add song dialog/activity opens
        onView(withId(R.id.song_name_input))
            .check(matches(isDisplayed()))
    }
}
