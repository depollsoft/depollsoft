package depollsoft.pitchperfect

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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

/** The Pitch Pipe tab around its face: when it re-reads the toggle setting, and which face it shows. */
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

    @Test
    fun theClassicSettingSwapsTheFaceAndSilencesTheOneItReplaces() {
        val model = PitchPipeModel()
        val state = PitchInstrumentState(model, haptic = {})
        val classic = ClassicPitchPipeState(model, density = 2f, haptic = {})
        SettingsModel.toggleNotes = true
        compose.setContent { PlateTheme { PitchPipeScreen(model, true, state, classic) } }
        compose.waitForIdle()
        compose.onNodeWithTag(TestTags.PITCH_INSTRUMENT).assertExists()
        compose.onNodeWithTag(TestTags.CLASSIC_PITCH_PIPE).assertDoesNotExist()

        // A note latched on the radial face.
        model.notes[4].isPlaying = true
        SettingsModel.classicPitchPipe = true
        compose.waitForIdle()
        compose.onNodeWithTag(TestTags.CLASSIC_PITCH_PIPE).assertExists()
        compose.onNodeWithTag(TestTags.PITCH_INSTRUMENT).assertDoesNotExist()
        assertFalse("changing face silences the old one", model.notes[4].isPlaying)
        assertTrue("the grid takes the toggle setting too", classic.toggleMode)

        SettingsModel.classicPitchPipe = false
        compose.waitForIdle()
        compose.onNodeWithTag(TestTags.PITCH_INSTRUMENT).assertExists()
    }
}
