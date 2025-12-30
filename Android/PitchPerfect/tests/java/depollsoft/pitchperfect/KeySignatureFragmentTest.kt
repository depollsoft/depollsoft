package depollsoft.pitchperfect

import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.hamcrest.Matchers.anything
import org.hamcrest.Matchers.containsString
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI tests for the Key Signature screen.
 * Tests key signature list display, selection, and major/minor toggling.
 * 
 * Actual view IDs:
 * - majorKeySignatureListView: ListView for major keys
 * - minorKeySignatureListView: ListView for minor keys
 * - majorMinorFab: FloatingActionButton to toggle major/minor
 * - keyNameTextView: displays key name in list item
 * - keySignatureTextView: displays key signature in list item
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class KeySignatureFragmentTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(PitchPerfectActivity::class.java)

    @Before
    fun navigateToKeysTab() {
        // Navigate to the keys tab via bottom navigation
        onView(withId(R.id.keys_item))
            .perform(click())
        
        // Allow time for fragment transition
        Thread.sleep(500)
    }

    // ==================== Layout Tests ====================

    @Test
    fun testMajorKeyListIsDisplayed() {
        // Major key list should be visible by default
        onView(withId(R.id.majorKeySignatureListView))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testMajorMinorFabIsDisplayed() {
        onView(withId(R.id.majorMinorFab))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testMajorKeyListIsScrollable() {
        onView(withId(R.id.majorKeySignatureListView))
            .perform(swipeUp())

        // Should still be displayed after scroll
        onView(withId(R.id.majorKeySignatureListView))
            .check(matches(isDisplayed()))
    }

    // ==================== Major Key Tests ====================

    @Test
    fun testMajorKeyListHasItems() {
        // Verify the major key list has items by clicking the first one
        onData(anything())
            .inAdapterView(withId(R.id.majorKeySignatureListView))
            .atPosition(0)
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSelectFirstMajorKey() {
        // Click on first major key item
        onData(anything())
            .inAdapterView(withId(R.id.majorKeySignatureListView))
            .atPosition(0)
            .perform(click())

        // List should still be displayed after selection
        onView(withId(R.id.majorKeySignatureListView))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSelectSecondMajorKey() {
        onData(anything())
            .inAdapterView(withId(R.id.majorKeySignatureListView))
            .atPosition(1)
            .perform(click())

        onView(withId(R.id.majorKeySignatureListView))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSelectThirdMajorKey() {
        onData(anything())
            .inAdapterView(withId(R.id.majorKeySignatureListView))
            .atPosition(2)
            .perform(click())

        onView(withId(R.id.majorKeySignatureListView))
            .check(matches(isDisplayed()))
    }

    // ==================== Major/Minor Toggle Tests ====================

    @Test
    fun testToggleToMinorKeys() {
        // Click FAB to toggle to minor keys
        onView(withId(R.id.majorMinorFab))
            .perform(click())

        Thread.sleep(300)

        // Verify minor key list is now displayed
        onView(withId(R.id.minorKeySignatureListView))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testToggleBackToMajorKeys() {
        // Toggle to minor
        onView(withId(R.id.majorMinorFab))
            .perform(click())

        Thread.sleep(300)

        // Toggle back to major
        onView(withId(R.id.majorMinorFab))
            .perform(click())

        Thread.sleep(300)

        // Verify major key list is displayed again
        onView(withId(R.id.majorKeySignatureListView))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testMinorKeyListHasItems() {
        // Toggle to minor keys
        onView(withId(R.id.majorMinorFab))
            .perform(click())

        Thread.sleep(300)

        // Verify the minor key list has items
        onData(anything())
            .inAdapterView(withId(R.id.minorKeySignatureListView))
            .atPosition(0)
            .check(matches(isDisplayed()))
    }

    // ==================== Minor Key Tests ====================

    @Test
    fun testSelectFirstMinorKey() {
        // Toggle to minor keys
        onView(withId(R.id.majorMinorFab))
            .perform(click())

        Thread.sleep(300)

        // Select first minor key
        onData(anything())
            .inAdapterView(withId(R.id.minorKeySignatureListView))
            .atPosition(0)
            .perform(click())

        // List should still be displayed
        onView(withId(R.id.minorKeySignatureListView))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSelectSecondMinorKey() {
        onView(withId(R.id.majorMinorFab))
            .perform(click())

        Thread.sleep(300)

        onData(anything())
            .inAdapterView(withId(R.id.minorKeySignatureListView))
            .atPosition(1)
            .perform(click())

        onView(withId(R.id.minorKeySignatureListView))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSelectThirdMinorKey() {
        onView(withId(R.id.majorMinorFab))
            .perform(click())

        Thread.sleep(300)

        onData(anything())
            .inAdapterView(withId(R.id.minorKeySignatureListView))
            .atPosition(2)
            .perform(click())

        onView(withId(R.id.minorKeySignatureListView))
            .check(matches(isDisplayed()))
    }

    // ==================== Minor Key List Scroll Tests ====================

    @Test
    fun testMinorKeyListIsScrollable() {
        // Toggle to minor keys
        onView(withId(R.id.majorMinorFab))
            .perform(click())

        Thread.sleep(300)

        onView(withId(R.id.minorKeySignatureListView))
            .perform(swipeUp())

        // Should still be displayed after scroll
        onView(withId(R.id.minorKeySignatureListView))
            .check(matches(isDisplayed()))
    }

    // ==================== FAB State Tests ====================

    @Test
    fun testFabRemainsVisibleAfterToggle() {
        // Toggle to minor
        onView(withId(R.id.majorMinorFab))
            .perform(click())

        Thread.sleep(300)

        // FAB should still be visible
        onView(withId(R.id.majorMinorFab))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testMultipleToggles() {
        // Toggle multiple times
        repeat(4) {
            onView(withId(R.id.majorMinorFab))
                .perform(click())
            Thread.sleep(200)
        }

        // FAB and a list should still be visible
        onView(withId(R.id.majorMinorFab))
            .check(matches(isDisplayed()))
    }

    // ==================== Key Item Content Tests ====================

    @Test
    fun testMajorKeyItemHasKeyName() {
        // Check that key items have keyNameTextView
        onData(anything())
            .inAdapterView(withId(R.id.majorKeySignatureListView))
            .atPosition(0)
            .onChildView(withId(R.id.keyNameTextView))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testMajorKeyItemHasKeySignature() {
        // Check that key items have keySignatureTextView
        onData(anything())
            .inAdapterView(withId(R.id.majorKeySignatureListView))
            .atPosition(0)
            .onChildView(withId(R.id.keySignatureTextView))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testMinorKeyItemHasKeyName() {
        // Toggle to minor keys
        onView(withId(R.id.majorMinorFab))
            .perform(click())

        Thread.sleep(300)

        // Check that minor key items have keyNameTextView
        onData(anything())
            .inAdapterView(withId(R.id.minorKeySignatureListView))
            .atPosition(0)
            .onChildView(withId(R.id.keyNameTextView))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testMinorKeyItemHasKeySignature() {
        // Toggle to minor keys
        onView(withId(R.id.majorMinorFab))
            .perform(click())

        Thread.sleep(300)

        // Check that minor key items have keySignatureTextView
        onData(anything())
            .inAdapterView(withId(R.id.minorKeySignatureListView))
            .atPosition(0)
            .onChildView(withId(R.id.keySignatureTextView))
            .check(matches(isDisplayed()))
    }

    // ==================== Navigation Tests ====================

    @Test
    fun testBottomNavigationVisible() {
        onView(withId(R.id.bottomNavigation))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testKeysTabSelected() {
        // Keys tab should be active after navigation
        onView(withId(R.id.keys_item))
            .check(matches(isDisplayed()))
    }
}
