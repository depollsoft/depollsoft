package depollsoft.pitchperfect

import android.os.Looper
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import depollsoft.lib.activity.RichApplication
import depollsoft.lib.util.Preferences
import depollsoft.pitchperfect.lib.Note
import depollsoft.pitchperfect.ui.PlateTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration
import kotlin.math.PI

/**
 * The pitch-pipe face: fingers, chords, slides, the range selector, toggle mode and the screen
 * reader's view of it. Touches go through Compose's real pointer input at the geometry the face
 * draws with.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = RichApplication::class, qualifiers = "w411dp-h891dp")
class PitchInstrumentTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var widgets: MockedStatic<PitchPipeAppWidget>
    private lateinit var model: PitchPipeModel
    private lateinit var state: PitchInstrumentState
    private val haptics = mutableListOf<Int>()

    @Before
    fun setUp() {
        Preferences.setTestMode(true)
        Preferences.clearTestValues()
        ScreenTestSupport.seedSettingsDefaults()
        // Switching range redraws the home-screen widget in production; that has its own tests.
        widgets = Mockito.mockStatic(PitchPipeAppWidget::class.java)
        Note.setPlayer(SilentPlayer)
        model = PitchPipeModel()
        state = PitchInstrumentState(model, haptic = { haptics += it })
        compose.setContent {
            PlateTheme { PitchInstrument(state, Modifier.size(411.dp, 700.dp)) }
        }
        compose.waitForIdle()
    }

    @After
    fun tearDown() {
        state.stopAll()
        Note.setPlayer(Note.DEFAULT_PLAYER)
        widgets.close()
        Preferences.setTestMode(false)
    }

    private object SilentPlayer : Note.NotePlayer {
        override fun play(n: Note) = Unit

        override fun stop(n: Note) = Unit
    }

    private fun cell(index: Int): Offset = state.geometry.cellCenters[index].let { Offset(it[0], it[1]) }

    private fun range(high: Boolean): Offset {
        val rect = if (high) state.geometry.rangeHighRect else state.geometry.rangeLowRect
        return Offset(rect.exactCenterX(), rect.exactCenterY())
    }

    private fun face() = compose.onNodeWithTag(TestTags.PITCH_INSTRUMENT)

    private fun playing(): List<Int> = state.notes.indices.filter { state.notes[it].isPlaying }

    @Test
    fun everyCellSoundsForExactlyAsLongAsItIsHeld() {
        for (index in state.notes.indices) {
            face().performTouchInput { down(cell(index)) }
            compose.waitForIdle()
            assertEquals("cell $index sounds while held", listOf(index), playing())
            face().performTouchInput { up() }
            compose.waitForIdle()
            assertEquals("cell $index falls silent on release", emptyList<Int>(), playing())
        }
        assertTrue("a note starting ticks the keyboard", haptics.all { it == HapticFeedbackConstants.KEYBOARD_TAP })
    }

    @Test
    fun severalFingersSoundAChord() {
        face().performTouchInput {
            down(0, cell(0))
            down(1, cell(4))
            down(2, cell(7))
        }
        compose.waitForIdle()
        assertEquals(listOf(0, 4, 7), playing())

        face().performTouchInput { up(1) }
        compose.waitForIdle()
        assertEquals("lifting one finger stops only its note", listOf(0, 7), playing())

        face().performTouchInput {
            up(0)
            up(2)
        }
        compose.waitForIdle()
        assertEquals(emptyList<Int>(), playing())
    }

    @Test
    fun aSlidingFingerTakesItsNoteWithIt() {
        face().performTouchInput {
            down(cell(2))
            moveTo(cell(3))
        }
        compose.waitForIdle()
        assertEquals(listOf(3), playing())

        face().performTouchInput { moveTo(Offset(state.geometry.faceCx, state.geometry.faceCy - state.geometry.ringRadius * 0.6f)) }
        compose.waitForIdle()
        assertEquals("off the ring nothing sounds", emptyList<Int>(), playing())
        face().performTouchInput { up() }
    }

    @Test
    fun theRangeSelectorSwitchesOctavesAndSilencesTheFace() {
        face().performTouchInput { down(1, cell(0)) }
        compose.waitForIdle()

        face().performTouchInput {
            up(1)
            down(range(high = true))
            up()
        }
        compose.waitForIdle()
        assertTrue("the high row selects F to F", model.isFromFToF)
        assertEquals("F", state.notes[0].friendlyName)
        assertEquals(4, state.notes[0].octave)
        assertTrue(haptics.contains(HapticFeedbackConstants.CLOCK_TICK))

        // The new cells play.
        face().performTouchInput { down(cell(6)) }
        compose.waitForIdle()
        assertEquals(listOf(6), playing())
        face().performTouchInput { up() }

        face().performTouchInput {
            down(range(high = false))
            up()
        }
        compose.waitForIdle()
        assertFalse("the low row selects C to C again", model.isFromFToF)
        assertEquals("C", state.notes[0].friendlyName)
    }

    @Test
    fun switchingRangeStopsANoteTheNewRangeNoLongerShows() {
        state.toggleMode = true
        face().performTouchInput {
            down(cell(0))
            up()
        }
        compose.waitForIdle()
        val c4 = state.notes[0]
        assertTrue(c4.isPlaying)

        state.selectRange(high = true)
        assertFalse("C4 is not on the F-to-F face, so it must not keep sounding", c4.isPlaying)
    }

    @Test
    fun toggleModeLatchesNotesUntilTheyAreTappedAgain() {
        state.toggleMode = true
        face().performTouchInput {
            down(cell(5))
            up()
        }
        compose.waitForIdle()
        assertEquals("a tap latches the note", listOf(5), playing())

        face().performTouchInput {
            down(cell(9))
            up()
        }
        compose.waitForIdle()
        assertEquals(listOf(5, 9), playing())

        face().performTouchInput {
            down(cell(5))
            up()
        }
        compose.waitForIdle()
        assertEquals("a second tap releases it", listOf(9), playing())
    }

    @Test
    fun aScreenReaderFindsEveryCellAndTheRangeRows() {
        val a4 = state.notes.indexOfFirst { it.friendlyName == "A" && it.accidental == depollsoft.pitchperfect.lib.Accidental.Natural }
        compose.onNodeWithContentDescription("A, octave 4").assertExists()
        compose.onNodeWithContentDescription("C sharp, D flat, octave 4").assertExists()
        val low = compose.onNodeWithContentDescription(RichApplication.getAppContext().getString(R.string.RangeLowDescription))
        assertEquals(true, low.fetchSemanticsNode().config.getOrNull(SemanticsProperties.Selected))

        // Activating a cell sounds it for a moment rather than for the instant of a double tap.
        compose.onNodeWithContentDescription("A, octave 4").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        assertEquals(listOf(a4), playing())
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(PitchInstrumentState.ACCESSIBILITY_NOTE_MS + 100))
        compose.waitForIdle()
        assertEquals(emptyList<Int>(), playing())

        compose
            .onNodeWithContentDescription(RichApplication.getAppContext().getString(R.string.RangeHighDescription))
            .performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        assertTrue(model.isFromFToF)
    }

    @Test
    fun stopAllSilencesTheFaceAndRestsTheGlow() {
        state.toggleMode = true
        face().performTouchInput {
            down(cell(1))
            up()
        }
        compose.waitForIdle()
        state.breathePhase = 1f
        state.stopAll()
        assertEquals(emptyList<Int>(), playing())
        assertEquals(0f, state.breathePhase)
    }

    @Test
    fun theGlowBreathesOnceEveryFourSeconds() {
        assertEquals(0f, PitchInstrumentState.breathePhaseAt(0))
        assertEquals((PI / 2).toFloat(), PitchInstrumentState.breathePhaseAt(1_000), 1e-4f)
        assertEquals(PI.toFloat(), PitchInstrumentState.breathePhaseAt(2_000), 1e-4f)
        assertEquals("the cycle repeats", 0f, PitchInstrumentState.breathePhaseAt(4_000))
    }

    @Test
    fun theGlowOnlyBreathesWhileANoteSoundsAndMotionIsAllowed() {
        var reduced = false
        val breathing = PitchInstrumentState(model, reduceMotion = { reduced })
        assertFalse(breathing.breathing)
        model.notes[0].play()
        assertTrue(breathing.breathing)
        reduced = true
        assertFalse("reduce motion keeps the glow still", breathing.breathing)
        model.notes[0].stop()
    }
}
