package depollsoft.pitchperfect

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performRotaryScrollInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.click
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.Lifecycle
import depollsoft.lib.util.Preferences
import depollsoft.pitchperfect.lib.Note
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The face as the watch hosts it: real pointer, crown and accessibility input. */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w192dp-h192dp-round-xhdpi")
class WearPitchPipeScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var model: PitchPipeModel
    private lateinit var state: WearInstrumentState
    private val silent =
        object : Note.NotePlayer {
            override fun play(n: Note) = Unit

            override fun stop(n: Note) = Unit
        }

    @Before
    fun setUp() {
        Preferences.setTestMode(true)
        Preferences.clearTestValues()
        SettingsModel.setToggleNotes(false)
        Note.setPlayer(silent)
        model = PitchPipeModel()
        compose.setContent {
            state = rememberWearInstrumentState(model)
            WearPitchPipeScreen(model, state)
        }
        compose.waitForIdle()
    }

    @After
    fun tearDown() {
        Note.getCommonNotes().forEach { it.stop() }
        Note.setPlayer(Note.DEFAULT_PLAYER)
        Preferences.clearTestValues()
        Preferences.setTestMode(false)
    }

    /** Cell centers in the face's pixels; the face fills the 384 px display. */
    private fun cell(index: Int): Offset {
        val c = WearInstrumentGeometry(384, 384, 13).cellCenters[index]
        return Offset(c[0], c[1])
    }

    @Test
    fun aScreenReaderFindsThirteenNotesAndTwoRanges() {
        val buttons =
            compose.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
                .fetchSemanticsNodes()
        assertEquals(15, buttons.size)
        compose.onNodeWithContentDescription("C sharp, D flat, octave 4").assertIsNotSelected()
        compose.onNodeWithContentDescription("Octave range C to C").assertIsSelected()
        compose.onNodeWithContentDescription("Octave range F to F").assertIsNotSelected()

        compose.onNodeWithContentDescription("A, octave 4").performSemanticsAction(SemanticsActions.OnClick)
        assertTrue(model.notes[9].isPlaying)
        compose.onNodeWithContentDescription("A, octave 4").assertIsSelected()

        compose.onNodeWithContentDescription("Octave range F to F").performSemanticsAction(SemanticsActions.OnClick)
        assertTrue(model.isFromFToF)
        assertTrue("switching range stops the sounding note", Note.getCommonNotes().none { it.isPlaying })
        compose.onNodeWithContentDescription("Octave range F to F").assertIsSelected()
        assertEquals(1, compose.onAllNodesWithContentDescription("F, octave 5").fetchSemanticsNodes().size)
    }

    @Test
    fun aKeyboardOrRotarySideButtonReachesEachCellAndEnterSoundsIt() {
        val cell = compose.onNodeWithContentDescription("A, octave 4")
        cell.performSemanticsAction(SemanticsActions.RequestFocus)
        cell.performKeyInput { pressKey(Key.Enter) }
        assertTrue(model.notes[9].isPlaying)
    }

    @Test
    fun fingersPlayTheirOwnNotesAndReleaseThem() {
        compose.onRoot().performTouchInput {
            down(0, cell(0))
            down(1, cell(4))
        }
        assertTrue(model.notes[0].isPlaying)
        assertTrue(model.notes[4].isPlaying)
        compose.onRoot().performTouchInput { moveTo(0, cell(1)) }
        assertFalse(model.notes[0].isPlaying)
        assertTrue(model.notes[1].isPlaying)
        compose.onRoot().performTouchInput { up(0) }
        assertFalse(model.notes[1].isPlaying)
        assertTrue(model.notes[4].isPlaying)
        compose.onRoot().performTouchInput { up(1) }
        assertTrue(model.notes.none { it.isPlaying })
    }

    @Test
    fun tappingTheRangeSelectorSwitchesRange() {
        val high = WearInstrumentGeometry(384, 384, 13).rangeHighRect
        compose.onRoot().performTouchInput { click(Offset(high.centerX(), high.centerY())) }
        assertTrue(model.isFromFToF)
    }

    @Test
    fun theCrownPicksTheRange() {
        compose.onRoot().performRotaryScrollInput { rotateToScrollVertically(40f) }
        assertTrue(model.isFromFToF)
        compose.onRoot().performRotaryScrollInput { rotateToScrollVertically(-40f) }
        assertFalse(model.isFromFToF)
    }

    @Test
    fun toggleModeIsReadWhenTheScreenResumes() {
        SettingsModel.setToggleNotes(true)
        compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.onRoot().performTouchInput { click(cell(2)) }
        assertTrue("a toggled note outlives the tap", model.notes[2].isPlaying)
    }

    @Test
    fun pausingStopsEverything() {
        compose.onRoot().performTouchInput { down(cell(3)) }
        assertTrue(model.notes[3].isPlaying)
        compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        assertTrue(model.notes.none { it.isPlaying })
    }
}
