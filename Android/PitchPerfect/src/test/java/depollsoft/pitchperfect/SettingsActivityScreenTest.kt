package depollsoft.pitchperfect

import android.view.View
import android.widget.CompoundButton
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.radiobutton.MaterialRadioButton
import depollsoft.lib.activity.RichApplication
import depollsoft.lib.util.Preferences
import depollsoft.pitchperfect.ScreenTestSupport.assertDisplayed
import depollsoft.pitchperfect.ScreenTestSupport.idle
import depollsoft.pitchperfect.ScreenTestSupport.scrollTo
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

/**
 * The Settings screen's controls, migrated from the instrumented `SettingsActivityTest`.
 *
 * The screen is taller than a phone viewport, so the lower controls are brought into view with
 * `requestRectangleOnScreen` — the same thing Espresso's `scrollTo()` did.
 *
 * Three of the instrumented tests asserted `anyOf(VISIBLE, GONE)`, which no state can fail. Those
 * are migrated as the definite signed-out visibility the screen actually has.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = RichApplication::class, qualifiers = "w411dp-h891dp")
class SettingsActivityScreenTest {
    private var controller: ActivityController<SettingsActivity>? = null

    @Before
    fun setUp() {
        ScreenTestSupport.startFromFirstLaunch()
        ScreenTestSupport.ensureFirebaseApp()
    }

    @After
    fun tearDown() {
        ScreenTestSupport.finishScreenTest(controller)
    }

    private fun launch(): SettingsActivity {
        val created = Robolectric.buildActivity(SettingsActivity::class.java)
        controller = created
        created.setup()
        idle()
        return created.get()
    }

    private fun SettingsActivity.shown(id: Int): View =
        findViewById<View>(id).also {
            assertNotNull("view $id should exist in the Settings hierarchy", it)
            scrollTo(it)
        }

    // ==================== Activity launch ====================

    @Test
    fun settingsActivityLaunches() {
        assertEquals(
            androidx.lifecycle.Lifecycle.State.RESUMED,
            launch().lifecycle.currentState,
        )
    }

    @Test
    fun scrollViewIsDisplayed() {
        assertDisplayed("scrollView1", launch().findViewById(R.id.scrollView1))
    }

    // ==================== Checkboxes ====================

    @Test
    fun toggleNoteCheckBoxIsDisplayedAndClickable() {
        val activity = launch()
        val box = activity.shown(R.id.toggleNoteCheckBox)
        assertDisplayed("toggleNoteCheckBox", box)
        assertTrue("toggleNoteCheckBox should be clickable", box.isClickable)
    }

    @Test
    fun toggleNoteCheckBoxTogglesTheSetting() {
        val activity = launch()
        val box = activity.shown(R.id.toggleNoteCheckBox) as CompoundButton
        val original = SettingsModel.toggleNotes
        assertEquals("the control starts from the stored setting", original, box.isChecked)

        box.performClick()
        idle()
        assertEquals("a tap writes through to the model", !original, SettingsModel.toggleNotes)
        assertEquals(!original, box.isChecked)

        box.performClick()
        idle()
        assertEquals("a second tap restores it", original, SettingsModel.toggleNotes)
        assertDisplayed("toggleNoteCheckBox", box)
    }

    @Test
    fun wakeLockCheckBoxIsDisplayedAndClickable() {
        val activity = launch()
        val box = activity.shown(R.id.wakeLockCheckBox)
        assertDisplayed("wakeLockCheckBox", box)
        assertTrue("wakeLockCheckBox should be clickable", box.isClickable)
    }

    @Test
    fun wakeLockCheckBoxTogglesTheSetting() {
        val activity = launch()
        val box = activity.shown(R.id.wakeLockCheckBox) as CompoundButton
        val original = SettingsModel.wakeLock

        box.performClick()
        idle()
        assertEquals(!original, SettingsModel.wakeLock)

        box.performClick()
        idle()
        assertEquals(original, SettingsModel.wakeLock)
        assertDisplayed("wakeLockCheckBox", box)
    }

    // ==================== Clear song list ====================

    @Test
    fun clearSongListButtonIsDisplayedAndClickable() {
        val activity = launch()
        val button = activity.shown(R.id.clearSongListButton)
        assertDisplayed("clearSongListButton", button)
        assertTrue(button.isClickable)
    }

    // ==================== Account buttons ====================

    @Test
    fun signedOutAccountButtonsOfferLoginOnly() {
        val activity = launch()
        assertDisplayed("loginButton", activity.shown(R.id.loginButton))
        // The instrumented originals allowed either visibility here, which nothing could fail.
        // Signed out, the two account actions are definitely hidden.
        assertEquals(
            "logoutButton is hidden while signed out",
            View.GONE,
            activity.findViewById<View>(R.id.logoutButton).visibility,
        )
        assertEquals(
            "deleteAccountButton is hidden while signed out",
            View.GONE,
            activity.findViewById<View>(R.id.deleteAccountButton).visibility,
        )
    }

    // ==================== Theme ====================

    @Test
    fun themeTitleAndRadioButtonsAreDisplayedAndClickable() {
        val activity = launch()
        assertDisplayed("themeTitleTextView", activity.shown(R.id.themeTitleTextView))
        for (id in listOf(R.id.radio_system, R.id.radio_light, R.id.radio_dark)) {
            val radio = activity.shown(id)
            assertDisplayed("radio $id", radio)
            assertTrue("radio $id should be clickable", radio.isClickable)
        }
    }

    @Test
    fun selectingLightThemeChecksItAndAppliesTheMode() {
        val activity = launch()
        val light = activity.shown(R.id.radio_light) as MaterialRadioButton
        light.performClick()
        idle()
        assertTrue(light.isChecked)
        assertEquals(AppCompatDelegate.MODE_NIGHT_NO, PitchPerfectApplication.themeMode)
    }

    @Test
    fun selectingDarkThemeChecksItAndAppliesTheMode() {
        val activity = launch()
        val dark = activity.shown(R.id.radio_dark) as MaterialRadioButton
        dark.performClick()
        idle()
        assertTrue(dark.isChecked)
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, PitchPerfectApplication.themeMode)
    }

    @Test
    fun selectingSystemThemeChecksItAndAppliesTheMode() {
        val activity = launch()
        val dark = activity.shown(R.id.radio_dark) as MaterialRadioButton
        dark.performClick()
        idle()
        val system = activity.shown(R.id.radio_system) as MaterialRadioButton
        system.performClick()
        idle()
        assertTrue(system.isChecked)
        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, PitchPerfectApplication.themeMode)
    }

    @Test
    fun themeRadioButtonsAreMutuallyExclusive() {
        val activity = launch()
        val light = activity.shown(R.id.radio_light) as MaterialRadioButton
        val dark = activity.shown(R.id.radio_dark) as MaterialRadioButton
        val system = activity.shown(R.id.radio_system) as MaterialRadioButton

        light.performClick()
        idle()
        assertTrue(light.isChecked)
        assertFalse(dark.isChecked)
        assertFalse(system.isChecked)

        dark.performClick()
        idle()
        assertTrue(dark.isChecked)
        assertFalse(light.isChecked)
        assertFalse(system.isChecked)
    }

    @Test
    fun allThemeOptionsAreSelectableInSequence() {
        val activity = launch()
        (activity.shown(R.id.radio_light) as MaterialRadioButton).performClick()
        idle()
        (activity.shown(R.id.radio_dark) as MaterialRadioButton).performClick()
        idle()
        val system = activity.shown(R.id.radio_system) as MaterialRadioButton
        system.performClick()
        idle()
        assertTrue("the last selection wins", system.isChecked)
    }

    // ==================== Subscription and changelog ====================

    @Test
    fun manageSubscriptionIsHiddenUntilAdsAreRemoved() {
        val activity = launch()
        assertEquals(
            "manageSubscriptionButton is hidden while ads have not been removed",
            View.GONE,
            activity.findViewById<View>(R.id.manageSubscriptionButton).visibility,
        )
    }

    @Test
    fun changelogButtonIsDisplayedAndClickable() {
        val activity = launch()
        val button = activity.shown(R.id.changelogButton)
        assertDisplayed("changelogButton", button)
        assertTrue(button.isClickable)
    }

    // ==================== About footer ====================

    @Test
    fun copyrightTextViewIsDisplayed() {
        assertDisplayed("copyrightTextView", launch().shown(R.id.copyrightTextView))
    }

    @Test
    fun homepageHyperlinkIsDisplayedAndClickable() {
        val activity = launch()
        val link = activity.shown(R.id.homepageHyperlink)
        assertDisplayed("homepageHyperlink", link)
        assertTrue(link.isClickable)
    }

    @Test
    fun rateReviewHyperlinkExistsInTheHierarchy() {
        val activity = launch()
        assertNotNull(
            "rateReviewHyperlink is part of the about footer regardless of purchase state",
            activity.findViewById<View>(R.id.rateReviewHyperlink),
        )
    }

    @Test
    fun termsOfUseHyperlinkIsDisplayedAndClickable() {
        val activity = launch()
        val link = activity.shown(R.id.Hyperlink01)
        assertDisplayed("terms of use hyperlink", link)
        assertTrue(link.isClickable)
    }

    // ==================== Persistence across recreation ====================

    @Test
    fun toggleNoteCheckBoxPersistsAfterRecreation() {
        val created = Robolectric.buildActivity(SettingsActivity::class.java)
        controller = created
        created.setup()
        idle()
        val box = created.get().shown(R.id.toggleNoteCheckBox) as CompoundButton
        box.performClick()
        idle()
        val chosen = SettingsModel.toggleNotes

        created.recreate()
        idle()

        val restored = created.get().shown(R.id.toggleNoteCheckBox) as CompoundButton
        assertDisplayed("toggleNoteCheckBox after recreation", restored)
        assertEquals("the stored setting survives recreation", chosen, restored.isChecked)
    }

    @Test
    fun wakeLockCheckBoxPersistsAfterRecreation() {
        val created = Robolectric.buildActivity(SettingsActivity::class.java)
        controller = created
        created.setup()
        idle()
        val box = created.get().shown(R.id.wakeLockCheckBox) as CompoundButton
        box.performClick()
        idle()
        val chosen = SettingsModel.wakeLock

        created.recreate()
        idle()

        val restored = created.get().shown(R.id.wakeLockCheckBox) as CompoundButton
        assertDisplayed("wakeLockCheckBox after recreation", restored)
        assertEquals(chosen, restored.isChecked)
    }

    @Test
    fun themePersistsAfterRecreation() {
        val created = Robolectric.buildActivity(SettingsActivity::class.java)
        controller = created
        created.setup()
        idle()
        (created.get().shown(R.id.radio_dark) as MaterialRadioButton).performClick()
        idle()

        created.recreate()
        idle()

        val restored = created.get().shown(R.id.radio_dark) as MaterialRadioButton
        assertTrue("the dark theme is still selected after recreation", restored.isChecked)
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, PitchPerfectApplication.themeMode)
    }

    // ==================== Multiple interactions ====================

    @Test
    fun multipleCheckboxTogglesLeaveTheScreenResponsive() {
        val activity = launch()
        (activity.shown(R.id.toggleNoteCheckBox) as CompoundButton).performClick()
        idle()
        (activity.shown(R.id.wakeLockCheckBox) as CompoundButton).performClick()
        idle()
        assertDisplayed("scrollView1", activity.findViewById(R.id.scrollView1))
    }
}
