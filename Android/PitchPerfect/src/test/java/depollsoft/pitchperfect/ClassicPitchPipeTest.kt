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
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import depollsoft.lib.activity.RichApplication
import depollsoft.lib.util.Preferences
import depollsoft.pitchperfect.lib.Accidental
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

/**
 * The classic pitch pipe: the old grid's layout, and the radial face's fingers, chords, slides,
 * range choice, toggle mode and screen reader on it. Touches go through Compose's real pointer
 * input at the geometry the grid draws with.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = RichApplication::class, qualifiers = "w411dp-h891dp")
class ClassicPitchPipeTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var widgets: MockedStatic<PitchPipeAppWidget>
    private lateinit var model: PitchPipeModel
    private lateinit var state: ClassicPitchPipeState
    private val haptics = mutableListOf<Int>()

    @Before
    fun setUp() {
        Preferences.setTestMode(true)
        Preferences.clearTestValues()
        ScreenTestSupport.seedSettingsDefaults()
        widgets = Mockito.mockStatic(PitchPipeAppWidget::class.java)
        Note.setPlayer(ScreenTestSupport.silentPlayer)
        model = PitchPipeModel()
        state = ClassicPitchPipeState(model, RichApplication.getAppContext().resources.displayMetrics.density, haptic = { haptics += it })
        compose.setContent {
            PlateTheme { ClassicPitchPipe(state, Modifier.size(411.dp, 700.dp)) }
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

    private val geometry get() = state.geometry

    private fun cell(index: Int): Offset = geometry.cellCenters[index].let { Offset(it[0], it[1]) }

    private fun range(high: Boolean): Offset = geometry.rangeRow(if (high) 1 else 0).let { Offset(it.centerX(), it.centerY()) }

    private fun face() = compose.onNodeWithTag(TestTags.CLASSIC_PITCH_PIPE)

    private fun playing(): List<Int> = state.notes.indices.filter { state.notes[it].isPlaying }

    private fun names(): List<String> = state.notes.map { "${it.friendlyName}${if (it.accidental == Accidental.Sharp) "#" else ""}${it.octave}" }

    @Test
    fun theGridHoldsOneOctaveClockwiseFromTheTopLeft() {
        assertEquals(
            "C to B, as the old grid had it: the pipe's upper C is not on it",
            listOf("C4", "C#4", "D4", "D#4", "E4", "F4", "F#4", "G4", "G#4", "A4", "A#4", "B4"),
            names(),
        )
        val quarterWidth = geometry.width / 4f
        val quarterHeight = geometry.height / 4f

        fun slotOf(index: Int) = cell(index).let { (it.x / quarterWidth).toInt() to (it.y / quarterHeight).toInt() }
        assertEquals("C across the top", listOf(0 to 0, 1 to 0, 2 to 0, 3 to 0), (0..3).map(::slotOf))
        assertEquals("E and F down the right", listOf(3 to 1, 3 to 2), (4..5).map(::slotOf))
        assertEquals("F sharp to A back along the bottom", listOf(3 to 3, 2 to 3, 1 to 3, 0 to 3), (6..9).map(::slotOf))
        assertEquals("A sharp and B up the left", listOf(0 to 2, 0 to 1), (10..11).map(::slotOf))
    }

    @Test
    fun theHighRangeRunsFromFToE() {
        state.selectRange(high = true)
        compose.waitForIdle()
        assertEquals(listOf("F4", "F#4", "G4", "G#4", "A4", "A#4", "B4", "C5", "C#5", "D5", "D#5", "E5"), names())
    }

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
    fun aFingerInTheMarginBetweenButtonsStillPlays() {
        // Just inside the top-left slot's corner, outside the drawn C button.
        face().performTouchInput { down(Offset(1f, 1f)) }
        compose.waitForIdle()
        assertEquals(listOf(0), playing())
        face().performTouchInput { up() }
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
            down(cell(1))
            moveTo(cell(2))
        }
        compose.waitForIdle()
        assertEquals(listOf(2), playing())

        face().performTouchInput { moveTo(Offset(geometry.width / 2f, geometry.height / 2f)) }
        compose.waitForIdle()
        assertEquals("in the middle nothing sounds", emptyList<Int>(), playing())
        face().performTouchInput { up() }
    }

    @Test
    fun theMiddleOffTheRangeChoicesIsBarePanel() {
        val middle = Offset(geometry.width / 2f, geometry.height / 2f)
        assertEquals(-1, geometry.cellAt(middle.x, middle.y))
        assertEquals(-1, geometry.rangeRowAt(middle.x, middle.y))
        assertFalse("the panel does not take the finger, so a swipe there still pages", state.down(0, middle.x, middle.y, firstFinger = true))
        state.endGesture()
    }

    @Test
    fun theRangeChoicesSwitchOctavesAndSilenceTheGrid() {
        state.toggleMode = true
        face().performTouchInput {
            down(cell(0))
            up()
        }
        compose.waitForIdle()
        val c4 = state.notes[0]
        assertTrue(c4.isPlaying)

        face().performTouchInput {
            down(range(high = true))
            up()
        }
        compose.waitForIdle()
        assertTrue("the lower choice selects F to E", model.isFromFToF)
        assertFalse("C4 is not on the F-to-E grid, so it must not keep sounding", c4.isPlaying)
        assertTrue(haptics.contains(HapticFeedbackConstants.CLOCK_TICK))

        face().performTouchInput {
            down(range(high = false))
            up()
        }
        compose.waitForIdle()
        assertFalse("the upper choice selects C to B again", model.isFromFToF)
    }

    @Test
    fun toggleModeLatchesNotesUntilTheyAreTappedAgain() {
        state.toggleMode = true
        face().performTouchInput {
            down(cell(5))
            up()
        }
        compose.waitForIdle()
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
    fun aScreenReaderFindsTwelveCellsAndTheRangeChoices() {
        compose.onNodeWithContentDescription("C, octave 4").assertExists()
        compose.onNodeWithContentDescription("B, octave 4").assertExists()
        compose.onNodeWithContentDescription("C sharp, D flat, octave 4").assertExists()
        assertEquals("no upper C", 0, compose.onAllNodesWithContentDescription("C, octave 5").fetchSemanticsNodes().size)

        val context = RichApplication.getAppContext()
        val low = compose.onNodeWithContentDescription(context.getString(R.string.ClassicCtoB))
        assertEquals(true, low.fetchSemanticsNode().config.getOrNull(SemanticsProperties.Selected))

        compose.onNodeWithContentDescription("A, octave 4").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        assertEquals(listOf(9), playing())
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(InstrumentState.ACCESSIBILITY_NOTE_MS + 100))
        compose.waitForIdle()
        assertEquals(emptyList<Int>(), playing())

        compose.onNodeWithContentDescription(context.getString(R.string.ClassicFtoE)).performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        assertTrue(model.isFromFToF)
    }
}
