package depollsoft.pitchperfect

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI tests for the PitchPipeFragment within PitchPerfectActivity.
 * Tests pitch button interactions and range toggle modes.
 * 
 * Uses the correct resource IDs from the actual app:
 * - pitchButton0 through pitchButton11 for the 12 pitch buttons
 * - cToCButton, fToFButton for range radio buttons
 * - bottomNavigation for the BottomNavigationView
 * - viewPager for the ViewPager2
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class PitchPipeFragmentTest {

    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(PitchPerfectActivity::class.java)

    @Before
    fun dismissDialogs() {
        // Dismiss any dialogs that may appear on startup
        for (i in 1..3) {
            try {
                Thread.sleep(500)
                try {
                    onView(withText("Skip"))
                        .inRoot(isDialog())
                        .perform(click())
                } catch (e: Exception) {
                    try {
                        onView(withText("OK"))
                            .inRoot(isDialog())
                            .perform(click())
                    } catch (e2: Exception) {
                        break
                    }
                }
            } catch (e: Exception) {
                break
            }
        }
        Thread.sleep(300)
    }

    // ==================== Layout Tests ====================

    @Test
    fun testBottomNavigationIsDisplayed() {
        onView(withId(R.id.bottomNavigation))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testViewPagerIsDisplayed() {
        onView(withId(R.id.viewPager))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testAllTwelvePitchButtonsAreDisplayed() {
        // Test all 12 pitch buttons (pitchButton0 through pitchButton11)
        onView(withId(R.id.pitchButton0)).check(matches(isDisplayed()))
        onView(withId(R.id.pitchButton1)).check(matches(isDisplayed()))
        onView(withId(R.id.pitchButton2)).check(matches(isDisplayed()))
        onView(withId(R.id.pitchButton3)).check(matches(isDisplayed()))
        onView(withId(R.id.pitchButton4)).check(matches(isDisplayed()))
        onView(withId(R.id.pitchButton5)).check(matches(isDisplayed()))
        onView(withId(R.id.pitchButton6)).check(matches(isDisplayed()))
        onView(withId(R.id.pitchButton7)).check(matches(isDisplayed()))
        onView(withId(R.id.pitchButton8)).check(matches(isDisplayed()))
        onView(withId(R.id.pitchButton9)).check(matches(isDisplayed()))
        onView(withId(R.id.pitchButton10)).check(matches(isDisplayed()))
        onView(withId(R.id.pitchButton11)).check(matches(isDisplayed()))
    }

    @Test
    fun testAllPitchButtonsAreClickable() {
        onView(withId(R.id.pitchButton0)).check(matches(isClickable()))
        onView(withId(R.id.pitchButton1)).check(matches(isClickable()))
        onView(withId(R.id.pitchButton2)).check(matches(isClickable()))
        onView(withId(R.id.pitchButton3)).check(matches(isClickable()))
        onView(withId(R.id.pitchButton4)).check(matches(isClickable()))
        onView(withId(R.id.pitchButton5)).check(matches(isClickable()))
        onView(withId(R.id.pitchButton6)).check(matches(isClickable()))
        onView(withId(R.id.pitchButton7)).check(matches(isClickable()))
        onView(withId(R.id.pitchButton8)).check(matches(isClickable()))
        onView(withId(R.id.pitchButton9)).check(matches(isClickable()))
        onView(withId(R.id.pitchButton10)).check(matches(isClickable()))
        onView(withId(R.id.pitchButton11)).check(matches(isClickable()))
    }

    // ==================== Pitch Button Click Tests ====================

    @Test
    fun testClickPitchButton0() {
        onView(withId(R.id.pitchButton0))
            .check(matches(isDisplayed()))
            .perform(click())
    }

    @Test
    fun testClickPitchButton1() {
        onView(withId(R.id.pitchButton1))
            .check(matches(isDisplayed()))
            .perform(click())
    }

    @Test
    fun testClickPitchButton2() {
        onView(withId(R.id.pitchButton2))
            .check(matches(isDisplayed()))
            .perform(click())
    }

    @Test
    fun testClickPitchButton3() {
        onView(withId(R.id.pitchButton3))
            .check(matches(isDisplayed()))
            .perform(click())
    }

    @Test
    fun testClickPitchButton4() {
        onView(withId(R.id.pitchButton4))
            .check(matches(isDisplayed()))
            .perform(click())
    }

    @Test
    fun testClickPitchButton5() {
        onView(withId(R.id.pitchButton5))
            .check(matches(isDisplayed()))
            .perform(click())
    }

    @Test
    fun testClickPitchButton6() {
        onView(withId(R.id.pitchButton6))
            .check(matches(isDisplayed()))
            .perform(click())
    }

    @Test
    fun testClickPitchButton7() {
        onView(withId(R.id.pitchButton7))
            .check(matches(isDisplayed()))
            .perform(click())
    }

    @Test
    fun testClickPitchButton8() {
        onView(withId(R.id.pitchButton8))
            .check(matches(isDisplayed()))
            .perform(click())
    }

    @Test
    fun testClickPitchButton9() {
        onView(withId(R.id.pitchButton9))
            .check(matches(isDisplayed()))
            .perform(click())
    }

    @Test
    fun testClickPitchButton10() {
        onView(withId(R.id.pitchButton10))
            .check(matches(isDisplayed()))
            .perform(click())
    }

    @Test
    fun testClickPitchButton11() {
        onView(withId(R.id.pitchButton11))
            .check(matches(isDisplayed()))
            .perform(click())
    }

    @Test
    fun testClickAllPitchButtonsSequentially() {
        // Click all 12 pitch buttons in sequence
        onView(withId(R.id.pitchButton0)).perform(click())
        onView(withId(R.id.pitchButton1)).perform(click())
        onView(withId(R.id.pitchButton2)).perform(click())
        onView(withId(R.id.pitchButton3)).perform(click())
        onView(withId(R.id.pitchButton4)).perform(click())
        onView(withId(R.id.pitchButton5)).perform(click())
        onView(withId(R.id.pitchButton6)).perform(click())
        onView(withId(R.id.pitchButton7)).perform(click())
        onView(withId(R.id.pitchButton8)).perform(click())
        onView(withId(R.id.pitchButton9)).perform(click())
        onView(withId(R.id.pitchButton10)).perform(click())
        onView(withId(R.id.pitchButton11)).perform(click())
    }

    // ==================== Range Toggle Tests ====================

    @Test
    fun testCToCButtonIsDisplayed() {
        onView(withId(R.id.cToCButton))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testFToFButtonIsDisplayed() {
        onView(withId(R.id.fToFButton))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testCToCButtonIsClickable() {
        onView(withId(R.id.cToCButton))
            .check(matches(isClickable()))
    }

    @Test
    fun testFToFButtonIsClickable() {
        onView(withId(R.id.fToFButton))
            .check(matches(isClickable()))
    }

    @Test
    fun testSwitchToCToCRange() {
        // Click the C-to-C radio button
        onView(withId(R.id.cToCButton))
            .perform(click())

        // Verify it's still displayed and enabled after click
        onView(withId(R.id.cToCButton))
            .check(matches(isDisplayed()))
            .check(matches(isEnabled()))
    }

    @Test
    fun testSwitchToFToFRange() {
        // Click the F-to-F radio button
        onView(withId(R.id.fToFButton))
            .perform(click())

        // Verify it's still displayed and enabled after click
        onView(withId(R.id.fToFButton))
            .check(matches(isDisplayed()))
            .check(matches(isEnabled()))
    }

    @Test
    fun testToggleBetweenRanges() {
        // Start with C-to-C
        onView(withId(R.id.cToCButton)).perform(click())

        // Switch to F-to-F
        onView(withId(R.id.fToFButton)).perform(click())

        // Switch back to C-to-C
        onView(withId(R.id.cToCButton)).perform(click())

        // Verify buttons are still functional
        onView(withId(R.id.cToCButton)).check(matches(isEnabled()))
        onView(withId(R.id.fToFButton)).check(matches(isEnabled()))
    }

    @Test
    fun testPitchButtonsAfterRangeChange() {
        // Switch to F-to-F range
        onView(withId(R.id.fToFButton)).perform(click())

        // Verify pitch buttons are still displayed and clickable
        onView(withId(R.id.pitchButton0)).check(matches(isDisplayed()))
        onView(withId(R.id.pitchButton6)).check(matches(isDisplayed()))
        onView(withId(R.id.pitchButton11)).check(matches(isDisplayed()))

        // Click a pitch button after range change
        onView(withId(R.id.pitchButton0)).perform(click())
    }

    // ==================== Integration Tests ====================

    @Test
    fun testPitchPipeFragmentFullInteraction() {
        // Verify initial state - all main elements displayed
        onView(withId(R.id.bottomNavigation)).check(matches(isDisplayed()))
        onView(withId(R.id.viewPager)).check(matches(isDisplayed()))
        onView(withId(R.id.pitchButton0)).check(matches(isDisplayed()))
        onView(withId(R.id.cToCButton)).check(matches(isDisplayed()))
        onView(withId(R.id.fToFButton)).check(matches(isDisplayed()))

        // Test range toggle
        onView(withId(R.id.cToCButton)).perform(click())

        // Test several pitch buttons
        onView(withId(R.id.pitchButton0)).perform(click())
        onView(withId(R.id.pitchButton4)).perform(click())
        onView(withId(R.id.pitchButton7)).perform(click())

        // Switch range and test again
        onView(withId(R.id.fToFButton)).perform(click())
        onView(withId(R.id.pitchButton0)).perform(click())
        onView(withId(R.id.pitchButton5)).perform(click())
    }
}
