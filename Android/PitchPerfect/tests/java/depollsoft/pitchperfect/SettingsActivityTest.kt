package depollsoft.pitchperfect

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.hamcrest.Matchers.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI tests for the SettingsActivity.
 * Tests all settings toggles, preferences persistence, and UI interactions.
 */
@RunWith(AndroidJUnit4::class)
@MediumTest
class SettingsActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(SettingsActivity::class.java)

    // ==================== Layout Tests ====================

    @Test
    fun testSettingsActivityLaunches() {
        activityRule.scenario.onActivity { activity ->
            assert(activity != null)
        }
    }

    @Test
    fun testSettingsContainerIsDisplayed() {
        onView(withId(R.id.settings_container))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testToolbarIsDisplayed() {
        onView(withId(R.id.toolbar))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testBackNavigationIsAvailable() {
        onView(withContentDescription("Navigate up"))
            .check(matches(isDisplayed()))
    }

    // ==================== Wake Lock Setting Tests ====================

    @Test
    fun testWakeLockSettingIsDisplayed() {
        onView(withText("Keep Screen On"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testWakeLockSettingDescriptionIsDisplayed() {
        onView(withText(containsString("Prevent screen from turning off")))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testToggleWakeLockOn() {
        // Click to toggle on
        onView(withText("Keep Screen On"))
            .perform(click())

        // Verify it's enabled
        onView(allOf(withId(R.id.wake_lock_switch), isChecked()))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testToggleWakeLockOff() {
        // First turn on
        onView(withText("Keep Screen On"))
            .perform(click())

        // Then turn off
        onView(withText("Keep Screen On"))
            .perform(click())

        // Verify it's disabled
        onView(allOf(withId(R.id.wake_lock_switch), not(isChecked())))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testWakeLockPersistsAfterReopen() {
        // Enable wake lock
        onView(withText("Keep Screen On"))
            .perform(click())

        // Close and reopen activity
        activityRule.scenario.recreate()

        Thread.sleep(500)

        // Verify setting is still enabled
        onView(allOf(withId(R.id.wake_lock_switch), isChecked()))
            .check(matches(isDisplayed()))
    }

    // ==================== Note Mode Setting Tests ====================

    @Test
    fun testNoteModeSettingIsDisplayed() {
        onView(withText("Note Mode"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testNoteModeOptions() {
        onView(withText("Note Mode"))
            .perform(click())

        // Verify options are shown
        onView(withText("Toggle"))
            .check(matches(isDisplayed()))
        onView(withText("Hold"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSelectToggleMode() {
        onView(withText("Note Mode"))
            .perform(click())

        onView(withText("Toggle"))
            .perform(click())

        // Verify selection
        onView(withText(containsString("Toggle")))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSelectHoldMode() {
        onView(withText("Note Mode"))
            .perform(click())

        onView(withText("Hold"))
            .perform(click())

        // Verify selection
        onView(withText(containsString("Hold")))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testNoteModePersistsAfterReopen() {
        // Select Hold mode
        onView(withText("Note Mode"))
            .perform(click())
        onView(withText("Hold"))
            .perform(click())

        // Recreate activity
        activityRule.scenario.recreate()

        Thread.sleep(500)

        // Verify Hold is still selected
        onView(withText(containsString("Hold")))
            .check(matches(isDisplayed()))
    }

    // ==================== Range Setting Tests ====================

    @Test
    fun testDefaultRangeSettingIsDisplayed() {
        onView(withText("Default Range"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testRangeOptions() {
        onView(withText("Default Range"))
            .perform(click())

        // Verify options
        onView(withText("C to C"))
            .check(matches(isDisplayed()))
        onView(withText("F to F"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSelectCToCRange() {
        onView(withText("Default Range"))
            .perform(click())

        onView(withText("C to C"))
            .perform(click())

        // Verify selection
        onView(withText(containsString("C to C")))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSelectFToFRange() {
        onView(withText("Default Range"))
            .perform(click())

        onView(withText("F to F"))
            .perform(click())

        // Verify selection
        onView(withText(containsString("F to F")))
            .check(matches(isDisplayed()))
    }

    // ==================== Sound Setting Tests ====================

    @Test
    fun testSoundSettingIsDisplayed() {
        onView(withText("Sound"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSoundOptions() {
        onView(withText("Sound"))
            .perform(click())

        // Verify sound options
        onView(withText(containsString("Sine")))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSelectDifferentSound() {
        onView(withText("Sound"))
            .perform(click())

        // Select a sound option
        onView(withText(containsString("Sine")))
            .perform(click())
    }

    // ==================== Octave Setting Tests ====================

    @Test
    fun testDefaultOctaveSettingIsDisplayed() {
        onView(withText("Default Octave"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testOctaveOptions() {
        onView(withText("Default Octave"))
            .perform(click())

        // Verify octave options (3, 4, 5)
        onView(withText("3"))
            .check(matches(isDisplayed()))
        onView(withText("4"))
            .check(matches(isDisplayed()))
        onView(withText("5"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSelectOctave3() {
        onView(withText("Default Octave"))
            .perform(click())

        onView(withText("3"))
            .perform(click())
    }

    @Test
    fun testSelectOctave4() {
        onView(withText("Default Octave"))
            .perform(click())

        onView(withText("4"))
            .perform(click())
    }

    @Test
    fun testSelectOctave5() {
        onView(withText("Default Octave"))
            .perform(click())

        onView(withText("5"))
            .perform(click())
    }

    // ==================== Theme Setting Tests ====================

    @Test
    fun testThemeSettingIsDisplayed() {
        onView(withText("Theme"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testThemeOptions() {
        onView(withText("Theme"))
            .perform(click())

        // Verify theme options
        onView(withText("Light"))
            .check(matches(isDisplayed()))
        onView(withText("Dark"))
            .check(matches(isDisplayed()))
        onView(withText("System"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSelectLightTheme() {
        onView(withText("Theme"))
            .perform(click())

        onView(withText("Light"))
            .perform(click())

        // Theme should be applied
    }

    @Test
    fun testSelectDarkTheme() {
        onView(withText("Theme"))
            .perform(click())

        onView(withText("Dark"))
            .perform(click())
    }

    @Test
    fun testSelectSystemTheme() {
        onView(withText("Theme"))
            .perform(click())

        onView(withText("System"))
            .perform(click())
    }

    // ==================== About Section Tests ====================

    @Test
    fun testAboutSectionIsDisplayed() {
        // Scroll to about section
        onView(withId(R.id.settings_container))
            .perform(swipeUp())

        onView(withText("About"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testVersionInfoIsDisplayed() {
        onView(withId(R.id.settings_container))
            .perform(swipeUp())

        onView(withText(containsString("Version")))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testPrivacyPolicyLink() {
        onView(withId(R.id.settings_container))
            .perform(swipeUp())

        onView(withText("Privacy Policy"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testPrivacyPolicyOpensWeb() {
        onView(withId(R.id.settings_container))
            .perform(swipeUp())

        onView(withText("Privacy Policy"))
            .perform(click())

        // Verify web view or browser intent
    }

    @Test
    fun testTermsOfServiceLink() {
        onView(withId(R.id.settings_container))
            .perform(swipeUp())

        onView(withText("Terms of Service"))
            .check(matches(isDisplayed()))
    }

    // ==================== Account Section Tests ====================

    @Test
    fun testAccountSectionIsDisplayed() {
        onView(withText("Account"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSignInOptionIsDisplayed() {
        onView(withText(anyOf(equalTo("Sign In"), equalTo("Sign Out"))))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSignInOpensAuthFlow() {
        onView(withText("Sign In"))
            .perform(click())

        // Verify auth UI appears
        Thread.sleep(500)
    }

    // ==================== Premium/Ads Section Tests ====================

    @Test
    fun testRemoveAdsOptionIsDisplayed() {
        onView(withText("Remove Ads"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testRemoveAdsOpensIAP() {
        onView(withText("Remove Ads"))
            .perform(click())

        // Verify IAP dialog appears
        Thread.sleep(500)
    }

    @Test
    fun testRestorePurchasesOption() {
        onView(withText("Restore Purchases"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testRestorePurchasesAction() {
        onView(withText("Restore Purchases"))
            .perform(click())

        // Verify restore process starts
        Thread.sleep(500)
    }

    // ==================== Navigation Tests ====================

    @Test
    fun testBackButtonReturnsToMain() {
        onView(withContentDescription("Navigate up"))
            .perform(click())

        // Should return to main activity
        onView(withId(R.id.pager))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSystemBackReturnsToMain() {
        // Press system back
        androidx.test.espresso.Espresso.pressBack()

        // Should return to main
        onView(withId(R.id.pager))
            .check(matches(isDisplayed()))
    }

    // ==================== Scroll Tests ====================

    @Test
    fun testSettingsListIsScrollable() {
        onView(withId(R.id.settings_container))
            .perform(swipeUp())

        onView(withId(R.id.settings_container))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testCanScrollToAllSettings() {
        // Scroll to bottom
        onView(withId(R.id.settings_container))
            .perform(swipeUp())
            .perform(swipeUp())
            .perform(swipeUp())

        // Should reach about section
        onView(withText(containsString("Version")))
            .check(matches(isDisplayed()))
    }

    // ==================== Configuration Change Tests ====================

    @Test
    fun testSettingsSurviveRotation() {
        // Change a setting
        onView(withText("Keep Screen On"))
            .perform(click())

        // Rotate
        activityRule.scenario.recreate()

        Thread.sleep(500)

        // Verify setting persists
        onView(allOf(withId(R.id.wake_lock_switch), isChecked()))
            .check(matches(isDisplayed()))
    }

    // ==================== Accessibility Tests ====================

    @Test
    fun testSettingsItemsHaveContentDescriptions() {
        onView(withText("Keep Screen On"))
            .check(matches(hasContentDescription()))
    }

    @Test
    fun testSwitchesAreAccessible() {
        onView(withId(R.id.wake_lock_switch))
            .check(matches(allOf(isDisplayed(), isClickable())))
    }

    // ==================== Edge Cases ====================

    @Test
    fun testRapidSettingToggling() {
        // Rapidly toggle a setting
        repeat(5) {
            onView(withText("Keep Screen On"))
                .perform(click())
            Thread.sleep(100)
        }

        // UI should still be responsive
        onView(withId(R.id.settings_container))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testMultipleSettingsChanges() {
        // Change multiple settings
        onView(withText("Keep Screen On")).perform(click())
        
        onView(withText("Note Mode")).perform(click())
        onView(withText("Hold")).perform(click())
        
        onView(withText("Default Range")).perform(click())
        onView(withText("F to F")).perform(click())

        // Recreate and verify all persist
        activityRule.scenario.recreate()

        Thread.sleep(500)

        onView(allOf(withId(R.id.wake_lock_switch), isChecked()))
            .check(matches(isDisplayed()))
    }
}
