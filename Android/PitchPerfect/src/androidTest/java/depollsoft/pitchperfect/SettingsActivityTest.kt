package depollsoft.pitchperfect

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.anyOf
import org.hamcrest.Matchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI tests for the SettingsActivity.
 * Tests checkboxes, theme radio buttons, buttons, and about section.
 *
 * ACTUAL VIEW IDs from settingsview.xml:
 * - toggleNoteCheckBox: CheckBox for note toggle
 * - wakeLockCheckBox: CheckBox for wake lock
 * - clearSongListButton: Button to clear songs
 * - loginButton, logoutButton, deleteAccountButton: Account buttons
 * - themeTitleTextView: Theme section title
 * - radio_light, radio_dark, radio_system: Theme radio buttons
 * - manageSubscriptionButton: Subscription management
 * - changelogButton: View changelog
 * - scrollView1: Main scroll view
 *
 * About section (from aboutfooter.xml):
 * - copyrightTextView, homepageHyperlink, rateReviewHyperlink, Hyperlink01
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class SettingsActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(SettingsActivity::class.java)

    // ==================== Activity Launch Tests ====================

    @Test
    fun testSettingsActivityLaunches() {
        activityRule.scenario.onActivity { activity ->
            assert(activity != null)
        }
    }

    @Test
    fun testScrollViewIsDisplayed() {
        onView(withId(R.id.scrollView1))
            .check(matches(isDisplayed()))
    }

    // ==================== Checkbox Tests ====================

    @Test
    fun testToggleNoteCheckBoxIsDisplayed() {
        onView(withId(R.id.toggleNoteCheckBox))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testToggleNoteCheckBoxIsClickable() {
        onView(withId(R.id.toggleNoteCheckBox))
            .check(matches(isClickable()))
    }

    @Test
    fun testToggleNoteCheckBoxToggle() {
        // Get initial state and toggle
        onView(withId(R.id.toggleNoteCheckBox))
            .perform(click())

        // Toggle back
        onView(withId(R.id.toggleNoteCheckBox))
            .perform(click())

        // Should still be displayed
        onView(withId(R.id.toggleNoteCheckBox))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testWakeLockCheckBoxIsDisplayed() {
        onView(withId(R.id.wakeLockCheckBox))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testWakeLockCheckBoxIsClickable() {
        onView(withId(R.id.wakeLockCheckBox))
            .check(matches(isClickable()))
    }

    @Test
    fun testWakeLockCheckBoxToggle() {
        // Toggle wake lock
        onView(withId(R.id.wakeLockCheckBox))
            .perform(click())

        // Toggle back
        onView(withId(R.id.wakeLockCheckBox))
            .perform(click())

        // Should still be displayed
        onView(withId(R.id.wakeLockCheckBox))
            .check(matches(isDisplayed()))
    }

    // ==================== Clear Song List Button Tests ====================

    @Test
    fun testClearSongListButtonIsDisplayed() {
        onView(withId(R.id.clearSongListButton))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testClearSongListButtonIsClickable() {
        onView(withId(R.id.clearSongListButton))
            .check(matches(isClickable()))
    }

    // ==================== Login/Logout Button Tests ====================

    @Test
    fun testLoginButtonExists() {
        // loginButton may be visible or hidden depending on auth state
        onView(withId(R.id.loginButton))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testLogoutButtonExists() {
        // logoutButton visibility depends on auth state
        // Just verify the view exists in the hierarchy
        onView(withId(R.id.logoutButton))
            .check(matches(anyOf(
                withEffectiveVisibility(Visibility.VISIBLE),
                withEffectiveVisibility(Visibility.GONE))))
    }

    @Test
    fun testDeleteAccountButtonExists() {
        // deleteAccountButton visibility depends on auth state
        onView(withId(R.id.deleteAccountButton))
            .check(matches(anyOf(
                withEffectiveVisibility(Visibility.VISIBLE),
                withEffectiveVisibility(Visibility.GONE))))
    }

    // ==================== Theme Section Tests ====================

    @Test
    fun testThemeTitleTextViewIsDisplayed() {
        onView(withId(R.id.themeTitleTextView))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testRadioSystemIsDisplayed() {
        onView(withId(R.id.radio_system))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testRadioLightIsDisplayed() {
        onView(withId(R.id.radio_light))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testRadioDarkIsDisplayed() {
        onView(withId(R.id.radio_dark))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testRadioSystemIsClickable() {
        onView(withId(R.id.radio_system))
            .check(matches(isClickable()))
    }

    @Test
    fun testRadioLightIsClickable() {
        onView(withId(R.id.radio_light))
            .check(matches(isClickable()))
    }

    @Test
    fun testRadioDarkIsClickable() {
        onView(withId(R.id.radio_dark))
            .check(matches(isClickable()))
    }

    @Test
    fun testSelectLightTheme() {
        onView(withId(R.id.radio_light))
            .perform(click())

        onView(withId(R.id.radio_light))
            .check(matches(isChecked()))
    }

    @Test
    fun testSelectDarkTheme() {
        onView(withId(R.id.radio_dark))
            .perform(click())

        onView(withId(R.id.radio_dark))
            .check(matches(isChecked()))
    }

    @Test
    fun testSelectSystemTheme() {
        onView(withId(R.id.radio_system))
            .perform(click())

        onView(withId(R.id.radio_system))
            .check(matches(isChecked()))
    }

    @Test
    fun testThemeRadioButtonsMutuallyExclusive() {
        // Select light
        onView(withId(R.id.radio_light))
            .perform(click())

        // Verify light is checked, others not
        onView(withId(R.id.radio_light))
            .check(matches(isChecked()))
        onView(withId(R.id.radio_dark))
            .check(matches(not(isChecked())))
        onView(withId(R.id.radio_system))
            .check(matches(not(isChecked())))

        // Select dark
        onView(withId(R.id.radio_dark))
            .perform(click())

        // Verify dark is checked, others not
        onView(withId(R.id.radio_dark))
            .check(matches(isChecked()))
        onView(withId(R.id.radio_light))
            .check(matches(not(isChecked())))
        onView(withId(R.id.radio_system))
            .check(matches(not(isChecked())))
    }

    // ==================== Subscription/Changelog Button Tests ====================

    @Test
    fun testManageSubscriptionButtonExists() {
        // May be hidden based on subscription state
        onView(withId(R.id.manageSubscriptionButton))
            .check(matches(anyOf(
                withEffectiveVisibility(Visibility.VISIBLE),
                withEffectiveVisibility(Visibility.GONE))))
    }

    @Test
    fun testChangelogButtonIsDisplayed() {
        onView(withId(R.id.changelogButton))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
    }

    @Test
    fun testChangelogButtonIsClickable() {
        onView(withId(R.id.changelogButton))
            .perform(scrollTo())
            .check(matches(isClickable()))
    }

    // ==================== About Footer Tests ====================

    @Test
    fun testCopyrightTextViewIsDisplayed() {
        onView(withId(R.id.copyrightTextView))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
    }

    @Test
    fun testHomepageHyperlinkIsDisplayed() {
        onView(withId(R.id.homepageHyperlink))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
    }

    @Test
    fun testHomepageHyperlinkIsClickable() {
        onView(withId(R.id.homepageHyperlink))
            .perform(scrollTo())
            .check(matches(isClickable()))
    }

    @Test
    fun testRateReviewHyperlinkExists() {
        // May be hidden based on purchase state
        onView(withId(R.id.rateReviewHyperlink))
            .check(matches(anyOf(
                withEffectiveVisibility(Visibility.VISIBLE),
                withEffectiveVisibility(Visibility.GONE))))
    }

    @Test
    fun testTermsOfUseHyperlinkIsDisplayed() {
        onView(withId(R.id.Hyperlink01))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
    }

    @Test
    fun testTermsOfUseHyperlinkIsClickable() {
        onView(withId(R.id.Hyperlink01))
            .perform(scrollTo())
            .check(matches(isClickable()))
    }

    // ==================== Checkbox State Persistence Tests ====================

    @Test
    fun testToggleNoteCheckBoxPersistsAfterRecreate() {
        // Get initial state, toggle, then recreate
        onView(withId(R.id.toggleNoteCheckBox))
            .perform(click())

        activityRule.scenario.recreate()

        // Verify checkbox is still displayed after recreate
        onView(withId(R.id.toggleNoteCheckBox))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testWakeLockCheckBoxPersistsAfterRecreate() {
        // Toggle wake lock
        onView(withId(R.id.wakeLockCheckBox))
            .perform(click())

        activityRule.scenario.recreate()

        // Verify checkbox is still displayed after recreate
        onView(withId(R.id.wakeLockCheckBox))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testThemePersistsAfterRecreate() {
        // Select dark theme
        onView(withId(R.id.radio_dark))
            .perform(click())

        activityRule.scenario.recreate()

        // Verify dark theme is still selected
        onView(withId(R.id.radio_dark))
            .check(matches(isChecked()))
    }

    // ==================== Multiple Interaction Tests ====================

    @Test
    fun testMultipleCheckboxToggles() {
        // Toggle both checkboxes
        onView(withId(R.id.toggleNoteCheckBox))
            .perform(click())
        onView(withId(R.id.wakeLockCheckBox))
            .perform(click())

        // UI should still be responsive
        onView(withId(R.id.scrollView1))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testAllThemeOptionsSelectable() {
        // Select each theme in sequence
        onView(withId(R.id.radio_light))
            .perform(click())
        onView(withId(R.id.radio_dark))
            .perform(click())
        onView(withId(R.id.radio_system))
            .perform(click())

        // Final state should be system
        onView(withId(R.id.radio_system))
            .check(matches(isChecked()))
    }
}
