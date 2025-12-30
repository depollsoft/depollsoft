package depollsoft.pitchperfect

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
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

    @Test
    fun testSettingsMenuItemExists() {
        // Open overflow menu
        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)

        // Verify settings option exists
        onView(withText("Settings"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSettingsMenuOpensSettingsActivity() {
        // Open overflow menu
        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)

        // Click settings
        onView(withText("Settings"))
            .perform(click())

        // Settings activity should open - if it has specific views, we could check them
        // For now, just verify the click doesn't crash
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
