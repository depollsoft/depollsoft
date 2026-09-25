package depollsoft.pitchperfect

import android.content.Intent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import depollsoft.lib.activity.RichApplication
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** The settings screen, signed out, and the prompts it opens. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = RichApplication::class, qualifiers = "w411dp-h891dp")
class SettingsScreenTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val screens = ComposeScreens(compose)

    @Before
    fun setUp() {
        ScreenTestSupport.startFromFirstLaunch()
        ScreenTestSupport.ensureFirebaseApp()
    }

    @After
    fun tearDown() {
        screens.finish()
    }

    private fun settings(): SettingsActivity = screens.launch(SettingsActivity::class.java).get()

    private fun toggleState(tag: String): ToggleableState? =
        compose.onNodeWithTag(tag).fetchSemanticsNode().config.getOrNull(SemanticsProperties.ToggleableState)

    private fun tapScrolled(tag: String) {
        compose.onNodeWithTag(tag).performScrollTo().performClick()
        screens.settle()
    }

    // ==================== Pitch pipe ====================

    @Test
    fun notesPlayUntilPressedAgainTogglesTheSetting() {
        settings()
        assertEquals(ToggleableState.Off, toggleState(TestTags.TOGGLE_NOTES))
        screens.click(TestTags.TOGGLE_NOTES)
        assertTrue(SettingsModel.toggleNotes)
        assertEquals(ToggleableState.On, toggleState(TestTags.TOGGLE_NOTES))
        screens.click(TestTags.TOGGLE_NOTES)
        assertFalse(SettingsModel.toggleNotes)
    }

    @Test
    fun keepTheScreenOnTogglesTheSetting() {
        settings()
        screens.click(TestTags.WAKE_LOCK)
        assertTrue(SettingsModel.wakeLock)
        assertEquals(ToggleableState.On, toggleState(TestTags.WAKE_LOCK))
        screens.click(TestTags.WAKE_LOCK)
        assertFalse(SettingsModel.wakeLock)
    }

    @Test
    fun theSwitchesShowTheSavedSettingsAfterRecreation() {
        val controller = screens.launch(SettingsActivity::class.java)
        screens.click(TestTags.TOGGLE_NOTES)
        screens.click(TestTags.WAKE_LOCK)
        controller.recreate()
        screens.settle()
        assertEquals(ToggleableState.On, toggleState(TestTags.TOGGLE_NOTES))
        assertEquals(ToggleableState.On, toggleState(TestTags.WAKE_LOCK))
    }

    @Test
    fun clearAllSongsAsksFirstThenEmptiesEveryList() {
        SongsModel.get().defaultSongList.addSong(ComposeScreens.song("Blue Skies"))
        val other = SongsModel.get().createList("Saturday show")
        settings()
        screens.click(TestTags.CLEAR_SONGS)
        compose.onNodeWithText("NO").performClick()
        screens.settle()
        assertEquals(1, SongsModel.get().defaultSongList.songs.size)

        screens.click(TestTags.CLEAR_SONGS)
        compose.onNodeWithText("YES").performClick()
        screens.settle()
        assertTrue(SongsModel.get().defaultSongList.songs.isEmpty())
        assertFalse(SongsModel.get().songLists.containsKey(other))
    }

    // ==================== Account ====================

    @Test
    fun signedOutTheAccountSectionOffersLoginOnly() {
        settings()
        compose.onNodeWithTag(TestTags.LOG_IN).performScrollTo().assertIsDisplayed()
        assertFalse(screens.exists(TestTags.LOG_OUT))
        assertFalse(screens.exists(TestTags.DELETE_ACCOUNT))
    }

    @Test
    fun logInOpensTheSignInPromptAndNotNowClosesIt() {
        settings()
        tapScrolled(TestTags.LOG_IN)
        compose.onNodeWithText("Log in to Pitch Perfect").assertIsDisplayed()
        compose.onNodeWithTag(TestTags.LOGIN_BUTTON).assertIsEnabled()

        compose.onNodeWithText("NOT NOW").performClick()
        screens.settle()
        compose.onNodeWithText("Log in to Pitch Perfect").assertDoesNotExist()
    }

    @Test
    fun theSignInButtonOpensFirebaseUiAndShowsItIsBusy() {
        // FirebaseUI reads its configuration from the context the real app hands it at startup.
        com.firebase.ui.auth.AuthUI.setApplicationContext(RichApplication.getAppContext())
        val activity = settings()
        tapScrolled(TestTags.LOG_IN)
        compose.onNodeWithTag(TestTags.LOGIN_BUTTON).performClick()
        screens.settle()
        val started = shadowOf(activity).nextStartedActivityForResult
        assertTrue(
            "the provider choice is FirebaseUI's",
            started.intent.component!!.className.startsWith("com.firebase.ui.auth"),
        )
        compose.onNodeWithTag(TestTags.LOGIN_BUTTON).assertIsNotEnabled()
        // "Opening sign-in" is announced outright on the press; a live region would repeat it.
        val opening = compose.onAllNodesWithText(activity.getString(R.string.OpeningSignIn)).fetchSemanticsNodes()
        assertTrue(opening.isNotEmpty())
        assertTrue(opening.none { SemanticsProperties.LiveRegion in it.config })

        // A prompt opened again starts fresh, not stuck on the last attempt.
        compose.onNodeWithText("NOT NOW").performClick()
        screens.settle()
        tapScrolled(TestTags.LOG_IN)
        compose.onNodeWithTag(TestTags.LOGIN_BUTTON).assertIsEnabled()
    }

    @Test
    @Config(qualifiers = "w640dp-h320dp-land")
    fun onAShortLandscapeScreenTheDialogsKeepTheirButtonsOnScreen() {
        val activity = settings()
        tapScrolled(TestTags.CHANGELOG)
        compose.onNodeWithText(activity.getString(android.R.string.ok)).assertIsDisplayed()
        compose.onNodeWithText(activity.getString(android.R.string.ok)).performClick()
        screens.settle()

        tapScrolled(TestTags.LOG_IN)
        compose.onNodeWithText("NOT NOW").assertIsDisplayed()
        compose.onNodeWithTag(TestTags.LOGIN_BUTTON).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun aSignInResultArrivingAfterRecreationReachesThePrompt() {
        com.firebase.ui.auth.AuthUI.setApplicationContext(RichApplication.getAppContext())
        val controller = screens.launch(SettingsActivity::class.java)
        tapScrolled(TestTags.LOG_IN)
        compose.onNodeWithTag(TestTags.LOGIN_BUTTON).performClick()
        screens.settle()
        val started = shadowOf(controller.get()).nextStartedActivityForResult

        // The phone rotates while FirebaseUI is up; its result comes back to the new activity.
        controller.recreate()
        screens.settle()
        controller.get().activityResultRegistry.dispatchResult(started.requestCode, android.app.Activity.RESULT_CANCELED, null)
        screens.settle()

        compose
            .onNodeWithText(controller.get().getString(R.string.SignInCanceled))
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        compose.onNodeWithTag(TestTags.LOGIN_BUTTON).assertIsEnabled()
    }

    @Test
    fun theSignInPromptUsesFirebaseUisMethodPicker() {
        assertFalse(LoginPrompt.CREDENTIAL_MANAGER_ENABLED)
        assertTrue(LoginPrompt.ALWAYS_SHOW_PROVIDER_CHOICE)
        assertEquals(setOf("password", "google.com", "facebook.com"), LoginPrompt.PROVIDER_IDS)
    }

    // ==================== Appearance ====================

    private fun selected(tag: String): Boolean = screens.isSelected(tag)

    @Test
    fun theThemeChoicesAreExclusiveAndApplyTheMode() {
        settings()
        assertTrue(selected(TestTags.THEME_SYSTEM))
        for ((tag, mode) in listOf(
            TestTags.THEME_LIGHT to AppCompatDelegate.MODE_NIGHT_NO,
            TestTags.THEME_DARK to AppCompatDelegate.MODE_NIGHT_YES,
            TestTags.THEME_SYSTEM to AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
        )) {
            tapScrolled(tag)
            assertEquals(mode, PitchPerfectApplication.themeMode)
            assertEquals(mode, AppCompatDelegate.getDefaultNightMode())
            listOf(TestTags.THEME_SYSTEM, TestTags.THEME_LIGHT, TestTags.THEME_DARK).forEach {
                assertEquals("$it after choosing $tag", it == tag, selected(it))
            }
        }
    }

    @Test
    fun theThemeChoiceSurvivesRecreation() {
        val controller = screens.launch(SettingsActivity::class.java)
        tapScrolled(TestTags.THEME_DARK)
        controller.recreate()
        screens.settle()
        assertTrue(selected(TestTags.THEME_DARK))
    }

    // ==================== Subscription and about ====================

    @Test
    fun manageSubscriptionShowsOnlyOnceAdsAreRemoved() {
        settings()
        assertFalse(screens.exists(TestTags.MANAGE_SUBSCRIPTION))
        screens.finish()

        SettingsModel.areAdsRemoved = true
        val activity = settings()
        tapScrolled(TestTags.MANAGE_SUBSCRIPTION)
        val opened = shadowOf(activity).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, opened.action)
        assertTrue(opened.data.toString().startsWith("https://play.google.com/store/account/subscriptions"))
    }

    @Test
    fun theChangelogOpensInADialog() {
        settings()
        tapScrolled(TestTags.CHANGELOG)
        compose.onNodeWithText("Pitch Perfect Changelog").assertIsDisplayed()
        compose.onNodeWithText("OK").performClick()
        screens.settle()
        compose.onNodeWithText("Pitch Perfect Changelog").assertDoesNotExist()
    }

    @Test
    fun theAboutLinksOpenTheirPages() {
        val activity = settings()
        for ((label, url) in listOf(
            "apps.depoll.com" to "http://apps.depoll.com",
            "Terms of Use" to "http://apps.depoll.com/terms-of-use",
            "DepollSoft" to "http://apps.depoll.com",
        )) {
            compose.onNodeWithText(label).performScrollTo().performClick()
            screens.settle()
            assertEquals(url, shadowOf(activity).nextStartedActivity.data.toString())
        }
        compose.onNodeWithText("Pitch Perfect · Version", substring = true).assertDoesNotExist()
    }

    @Test
    fun privacyChoicesOpensTheConsentPrompt() {
        settings()
        tapScrolled(TestTags.PRIVACY_CHOICES)
        // The consent prompt is the shared library's own dialog.
        assertTrue(org.robolectric.shadows.ShadowDialog.getLatestDialog().isShowing)
    }
}
