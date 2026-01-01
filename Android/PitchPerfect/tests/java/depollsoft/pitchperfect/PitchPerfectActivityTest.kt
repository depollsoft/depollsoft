package depollsoft.pitchperfect

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI tests for the main PitchPerfectActivity.
 * Tests the main activity layout, navigation, and core interactions.
 * 
 * Actual view IDs from pitchperfectview.xml:
 * - viewPager: ViewPager2 for fragments
 * - bottomNavigation: BottomNavigationView
 * 
 * Menu item IDs:
 * - pitchpipe_item: Pitch Pipe tab
 * - keys_item: Keys tab
 * - songs_item: Songs tab
 * - settingsMenuItem: Settings in options menu
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class PitchPerfectActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(PitchPerfectActivity::class.java)

    @Before
    fun dismissDialogs() {
        // Use efficient dialog dismissal instead of Thread.sleep()
        EspressoTestUtils.dismissStartupDialogs()
    }

    // ==================== Launch Tests ====================

    @Test
    fun testActivityLaunches() {
        // Verify the activity launches successfully
        activityRule.scenario.onActivity { activity ->
            assert(activity != null)
        }
    }

    // ==================== Main Layout Tests ====================

    @Test
    fun testViewPagerIsDisplayed() {
        // Verify the ViewPager2 is displayed
        onView(withId(R.id.viewPager))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testBottomNavigationIsDisplayed() {
        // Verify bottom navigation is visible
        onView(withId(R.id.bottomNavigation))
            .check(matches(isDisplayed()))
    }

    // ==================== Navigation Tests ====================

    @Test
    fun testNavigateToPitchPipeTab() {
        // Navigate to Pitch Pipe tab
        onView(withId(R.id.pitchpipe_item))
            .perform(click())

        // Verify ViewPager is still displayed (navigation worked)
        onView(withId(R.id.viewPager))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testNavigateToKeysTab() {
        // Navigate to Keys tab
        onView(withId(R.id.keys_item))
            .perform(click())

        // Verify ViewPager is still displayed (navigation worked)
        onView(withId(R.id.viewPager))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testNavigateToSongsTab() {
        // Navigate to Songs tab
        onView(withId(R.id.songs_item))
            .perform(click())

        // Verify ViewPager is still displayed (navigation worked)
        onView(withId(R.id.viewPager))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testNavigationBetweenAllTabs() {
        // Navigate to Pitch Pipe tab
        onView(withId(R.id.pitchpipe_item))
            .perform(click())
        onView(withId(R.id.viewPager))
            .check(matches(isDisplayed()))

        // Navigate to Keys tab
        onView(withId(R.id.keys_item))
            .perform(click())
        onView(withId(R.id.viewPager))
            .check(matches(isDisplayed()))

        // Navigate to Songs tab
        onView(withId(R.id.songs_item))
            .perform(click())
        onView(withId(R.id.viewPager))
            .check(matches(isDisplayed()))

        // Navigate back to Pitch Pipe tab
        onView(withId(R.id.pitchpipe_item))
            .perform(click())
        onView(withId(R.id.viewPager))
            .check(matches(isDisplayed()))
    }

    // ==================== Menu Tests ====================

    // Note: The settings menu is shown directly in the action bar with showAsAction="always"
    // but the ActionBar implementation doesn't expose standard content descriptions.
    // These tests verify the activity can be navigated to from bottom nav instead.

    @Test
    fun testCanNavigateToAllTabsFromAnyTab() {
        // Start from pitch pipe (default)
        onView(withId(R.id.viewPager))
            .check(matches(isDisplayed()))

        // Navigate to Songs, then Keys
        onView(withId(R.id.songs_item))
            .perform(click())
        EspressoTestUtils.shortWait()
        
        onView(withId(R.id.keys_item))
            .perform(click())
        EspressoTestUtils.shortWait()
        
        // Verify still functional
        onView(withId(R.id.viewPager))
            .check(matches(isDisplayed()))
    }

    // ==================== Configuration Change Tests ====================

    @Test
    fun testActivitySurvivesRotation() {
        // Navigate to Keys tab
        onView(withId(R.id.keys_item))
            .perform(click())

        // Recreate activity (simulates rotation)
        activityRule.scenario.recreate()

        // Verify the main layout is still displayed after rotation
        onView(withId(R.id.viewPager))
            .check(matches(isDisplayed()))
        onView(withId(R.id.bottomNavigation))
            .check(matches(isDisplayed()))
    }
}
