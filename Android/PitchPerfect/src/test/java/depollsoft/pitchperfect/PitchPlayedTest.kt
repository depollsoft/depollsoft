package depollsoft.pitchperfect

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import depollsoft.lib.activity.RichApplication
import depollsoft.lib.analytics.UsageAnalytics
import depollsoft.lib.util.Preferences
import depollsoft.pitchperfect.lib.Note
import depollsoft.pitchperfect.ui.PlateTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Which presses count as playing a pitch: a finger landing, a toggle turning a note on, and a screen
 * reader's activation; not a finger sliding on to the next note, nor a toggle turning one off.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = RichApplication::class, qualifiers = "w411dp-h891dp")
class PitchPlayedTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var widgets: MockedStatic<PitchPipeAppWidget>
    private lateinit var state: PitchInstrumentState
    private val sources = mutableListOf<String>()

    @Before
    fun setUp() {
        Preferences.setTestMode(true)
        Preferences.clearTestValues()
        ScreenTestSupport.seedSettingsDefaults()
        widgets = Mockito.mockStatic(PitchPipeAppWidget::class.java)
        Note.setPlayer(ScreenTestSupport.silentPlayer)
        UsageAnalytics.sink =
            object : UsageAnalytics.Sink {
                override fun logEvent(name: String, params: Map<String, String>) {
                    if (name == UsageAnalytics.PITCH_PLAYED) sources += params.getValue(UsageAnalytics.SOURCE)
                }

                override fun setUserProperty(name: String, value: String) {}
            }
        state = PitchInstrumentState(PitchPipeModel(), onNoteStarted = { PitchPerfectAnalytics.pitchPlayed(PitchPerfectAnalytics.Source.PITCH_PIPE) })
        compose.setContent {
            PlateTheme { PitchInstrument(state, Modifier.size(411.dp, 700.dp)) }
        }
        compose.waitForIdle()
    }

    @After
    fun tearDown() {
        state.stopAll()
        UsageAnalytics.resetForTesting()
        Note.setPlayer(Note.DEFAULT_PLAYER)
        widgets.close()
        Preferences.setTestMode(false)
    }

    private fun cell(index: Int): Offset = state.geometry.cellCenters[index].let { Offset(it[0], it[1]) }

    private fun face() = compose.onNodeWithTag(TestTags.PITCH_INSTRUMENT)

    @Test
    fun aPressCountsAndSlidingOnToTheNextNoteDoesNot() {
        face().performTouchInput {
            down(cell(2))
            moveTo(cell(3))
            moveTo(cell(4))
            up()
        }
        compose.waitForIdle()
        assertEquals(listOf("pitch_pipe"), sources)
    }

    @Test
    fun eachFingerOfAChordCounts() {
        face().performTouchInput {
            down(0, cell(0))
            down(1, cell(4))
            up(0)
            up(1)
        }
        compose.waitForIdle()
        assertEquals(listOf("pitch_pipe", "pitch_pipe"), sources)
    }

    @Test
    fun inToggleModeOnlyTurningANoteOnCounts() {
        state.toggleMode = true
        face().performTouchInput {
            down(cell(5))
            up()
        }
        face().performTouchInput {
            down(cell(5))
            up()
        }
        compose.waitForIdle()
        assertEquals(listOf("pitch_pipe"), sources)
    }

    @Test
    fun aScreenReaderActivationCounts() {
        compose.runOnIdle { state.accessibilityClick(1) }
        assertEquals(listOf("pitch_pipe"), sources)
    }
}
