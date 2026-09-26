package depollsoft.pitchperfect

import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import depollsoft.lib.activity.RichApplication
import depollsoft.lib.privacy.PrivacyChoices
import depollsoft.lib.util.Preferences
import depollsoft.lib.util.Versioning
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * The login prompt through a real recreation: its launcher waits for FirebaseUI's result, so the
 * restored prompt must stay open even when the changelog is also due this run.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = RichApplication::class, qualifiers = "w411dp-h891dp")
class StartupPromptRecreationTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val screens = ComposeScreens(compose)

    private val lastVersion = Versioning::class.java.getDeclaredField("lastVersion").apply { isAccessible = true }
    private val changelogShown = Changelog::class.java.getDeclaredField("shownThisProcess").apply { isAccessible = true }
    private var savedLastVersion = 0
    private var savedChangelogShown = false

    @Before
    fun setUp() {
        ScreenTestSupport.startFromFirstLaunch()
        ScreenTestSupport.ensureFirebaseApp()
        PurchaseService.areAdsRemoved = true
        // A user who already answered the privacy prompt; the startup prompts wait for that.
        PrivacyChoices(RuntimeEnvironment.getApplication()).save(analytics = false, crashes = false)
        savedLastVersion = lastVersion.getInt(null)
        savedChangelogShown = changelogShown.getBoolean(Changelog)
    }

    @After
    fun tearDown() {
        lastVersion.setInt(null, savedLastVersion)
        changelogShown.setBoolean(Changelog, savedChangelogShown)
        PurchaseService.areAdsRemoved = false
        screens.finish()
    }

    @Test
    fun anOpenLoginPromptSurvivesRecreationWhileTheChangelogIsDue() {
        // A returning, signed-out user: the app asks them to log in.
        Preferences.set("depollsoft.lib.RunOnce.firstLaunch", true)
        val controller = screens.launch(PitchPerfectActivity::class.java)
        assertEquals(StartupPrompt.LOGIN, controller.get().startupPrompt)

        // This run is also a new version's first, so the changelog is due too.
        lastVersion.setInt(null, Versioning.getCurrentVersion() - 1)
        changelogShown.setBoolean(Changelog, false)

        controller.recreate()
        screens.settle()
        assertEquals("the restored login prompt stays open", StartupPrompt.LOGIN, controller.get().startupPrompt)
    }
}
