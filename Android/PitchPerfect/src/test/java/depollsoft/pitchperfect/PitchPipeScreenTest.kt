package depollsoft.pitchperfect

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import depollsoft.lib.activity.RichApplication
import depollsoft.lib.util.Preferences
import depollsoft.pitchperfect.lib.Note
import depollsoft.pitchperfect.ui.PlateTheme
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The Pitch Pipe tab around its face: when it re-reads the toggle setting. */
@RunWith(RobolectricTestRunner::class)
@Config(application = RichApplication::class, qualifiers = "w411dp-h891dp")
class PitchPipeScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var widgets: MockedStatic<PitchPipeAppWidget>

    @Before
    fun setUp() {
        Preferences.setTestMode(true)
        Preferences.clearTestValues()
        ScreenTestSupport.seedSettingsDefaults()
        widgets = Mockito.mockStatic(PitchPipeAppWidget::class.java)
        Note.setPlayer(ScreenTestSupport.silentPlayer)
    }

    @After
    fun tearDown() {
        Note.setPlayer(Note.DEFAULT_PLAYER)
        widgets.close()
        ScreenTestSupport.seedSettingsDefaults()
        Preferences.setTestMode(false)
    }

    @Test
    fun comingBackToTheTabPicksUpAChangedToggleSetting() {
        val model = PitchPipeModel()
        val state = PitchInstrumentState(model, haptic = {})
        var current by mutableStateOf(true)
        compose.setContent { PlateTheme { PitchPipeScreen(model, current, state) } }
        compose.waitForIdle()
        assertFalse(state.toggleMode)

        // Changed on another tab (or synced from another device) while the Pitch Pipe was away.
        current = false
        compose.waitForIdle()
        SettingsModel.toggleNotes = true
        current = true
        compose.waitForIdle()
        assertTrue("the fragment's onResume re-read it on every visit", state.toggleMode)
    }
}
