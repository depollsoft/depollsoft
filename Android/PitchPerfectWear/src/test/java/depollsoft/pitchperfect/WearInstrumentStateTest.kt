package depollsoft.pitchperfect

import android.os.Looper
import android.view.HapticFeedbackConstants
import depollsoft.lib.util.Preferences
import depollsoft.pitchperfect.lib.Note
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** The face's geometry and what fingers, the crown and a screen reader do to the notes. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WearInstrumentStateTest {
    private lateinit var model: PitchPipeModel
    private lateinit var state: WearInstrumentState
    private val haptics = mutableListOf<Int>()
    private var reduceMotion = false
    private val geometry get() = state.geometry
    private val looper get() = shadowOf(Looper.getMainLooper())
    private val silent =
        object : Note.NotePlayer {
            override fun play(n: Note) = Unit

            override fun stop(n: Note) = Unit
        }

    @Before
    fun setUp() {
        Preferences.setTestMode(true)
        Preferences.clearTestValues()
        Note.setPlayer(silent)
        model = PitchPipeModel()
        state = WearInstrumentState(model, haptic = { haptics += it }, reduceMotion = { reduceMotion })
        state.resize(384, 384)
    }

    @After
    fun tearDown() {
        state.stopAll()
        Note.setPlayer(Note.DEFAULT_PLAYER)
        Preferences.clearTestValues()
        Preferences.setTestMode(false)
    }

    private fun center(cell: Int) = geometry.cellCenters[cell]

    private fun down(
        id: Long,
        cell: Int,
        first: Boolean = id == 0L,
    ) = state.down(id, center(cell)[0], center(cell)[1], first)

    private fun move(
        id: Long,
        cell: Int,
    ) = state.move(id, center(cell)[0], center(cell)[1])

    @Test
    fun geometry_placesThirteenCellsInsideTheBezel() {
        assertEquals(13, geometry.cellCenters.size)
        assertEquals(384 * 0.02f, geometry.edgeInset, 0.001f)
        assertEquals(384 * 0.085f, geometry.cellRadius, 0.001f)
        assertEquals(384 * 0.395f, geometry.ringRadius, 0.001f)
        for (c in geometry.cellCenters) {
            val distance = hypot(c[0] - 192f, c[1] - 192f)
            assertEquals(geometry.ringRadius, distance, 0.001f)
            // The farthest point of each entire cell circle must fit, not just its center.
            assertTrue(distance + geometry.cellRadius <= 192f - geometry.edgeInset + 0.001f)
        }
        assertEquals(384 * 0.36f, geometry.rangeLowRect.width(), 0.001f)
        assertEquals(384 * 0.08f, geometry.rangeLowRect.height(), 0.001f)
        assertEquals(192f + 384 * 0.055f, geometry.rangeLowRect.top, 0.001f)
    }

    @Test
    fun sectors_coverCellsAndGapsButExcludeTheHole() {
        geometry.cellCenters.forEachIndexed { index, c ->
            assertEquals(index, geometry.cellAt(c[0], c[1]))
            val angle = Math.toRadians(-90.0 + (index + 1) * 360.0 / 13)
            val gapX = 192f + geometry.ringRadius * cos(angle).toFloat()
            val gapY = 192f + geometry.ringRadius * sin(angle).toFloat()
            assertTrue(geometry.cellAt(gapX, gapY) in listOf(index, (index + 1) % 13))
        }
        assertEquals(-1, geometry.cellAt(192f, 192f))
        assertEquals(-1, geometry.cellAt(192f, 192f - geometry.ringRadius + 1.25f * geometry.cellRadius + 1f))
        assertEquals(0, geometry.cellAt(200f, 0f))
    }

    @Test
    fun sectors_stopAtTheBezelSoSquareCornersAreNotPlayable() {
        // A round face is playable right up to its edge...
        assertEquals(12, geometry.cellAt(192f - 4f, 1f))
        // ...but the corners of a square watch, beyond the cells' outer rims, are score only.
        assertEquals(-1, geometry.cellAt(0f, 0f))
        assertEquals(-1, geometry.cellAt(383f, 383f))
        assertEquals(-1, geometry.cellAt(192f, 192f - geometry.ringRadius - 1.25f * geometry.cellRadius - 1f))

        // A finger that slides off the face into a corner releases its note.
        down(0, 0)
        assertTrue(model.notes[0].isPlaying)
        state.move(0, 2f, 2f)
        assertFalse(model.notes[0].isPlaying)
    }

    @Test
    fun accessibilityClick_pendingStopNeverCutsANewerActivationShort() {
        // Re-activating the same note restarts its 1.5 s window.
        state.accessibilityClick(0)
        looper.idleFor(Duration.ofMillis(1000))
        state.accessibilityClick(0)
        looper.idleFor(Duration.ofMillis(1000))
        assertTrue(model.notes[0].isPlaying)
        looper.idleFor(Duration.ofMillis(600))
        assertFalse(model.notes[0].isPlaying)

        // A finger that takes over the note holds it past the click's window.
        state.accessibilityClick(1)
        looper.idleFor(Duration.ofMillis(1000))
        down(0, 1)
        looper.idleFor(Duration.ofMillis(2000))
        assertTrue(model.notes[1].isPlaying)
        state.endGesture()
        assertFalse(model.notes[1].isPlaying)

        // stopAll clears pending stops so they cannot fire on a later activation.
        state.accessibilityClick(2)
        state.stopAll()
        down(0, 2)
        looper.idleFor(Duration.ofMillis(2000))
        assertTrue(model.notes[2].isPlaying)
        state.endGesture()
    }

    @Test
    fun accessibilityClick_playsForOneAndAHalfSecondsOrToggles() {
        assertTrue(state.accessibilityClick(3))
        assertTrue(model.notes[3].isPlaying)
        looper.idleFor(Duration.ofMillis(1499))
        assertTrue(model.notes[3].isPlaying)
        looper.idleFor(Duration.ofMillis(1))
        assertFalse(model.notes[3].isPlaying)

        state.toggleMode = true
        state.accessibilityClick(4)
        looper.idleFor(Duration.ofMillis(3000))
        assertTrue(model.notes[4].isPlaying)
        state.accessibilityClick(4)
        assertFalse(model.notes[4].isPlaying)
        assertFalse("a cell outside the ring is not a target", state.accessibilityClick(13))
    }

    @Test
    fun lowRangeRow_stopsOldRangeNotesAndRepeatedSelectionKeepsTheRange() {
        model.isFromFToF = true
        val oldNote = model.notes[12] // F5 does not belong to C4-C5.
        oldNote.play()
        val low = geometry.rangeLowRect
        val high = geometry.rangeHighRect
        state.down(0, low.centerX(), low.centerY(), firstFinger = true)
        assertFalse(model.isFromFToF)
        assertFalse(oldNote.isPlaying)
        assertTrue(model.notes.none { it.isPlaying })
        assertEquals(listOf(HapticFeedbackConstants.CLOCK_TICK), haptics)

        model.notes[0].play()
        state.down(0, low.centerX(), low.centerY(), firstFinger = true)
        assertFalse(model.isFromFToF)
        assertTrue(model.notes.none { it.isPlaying })
        assertEquals("choosing the current range does not tick", 1, haptics.size)
        state.down(0, high.centerX(), high.centerY(), firstFinger = true)
        assertTrue(model.isFromFToF)
    }

    @Test
    fun rangeRows_haveAGenerousHitZoneInsideTheHole() {
        val low = geometry.rangeLowRect
        val high = geometry.rangeHighRect
        val rowHeight = low.height()
        // Just above the frame still belongs to the top row; below it, the bottom row runs to the ring.
        assertEquals(0, geometry.rangeRowAt(low.centerX(), low.top - rowHeight * 0.4f))
        assertEquals(1, geometry.rangeRowAt(high.centerX(), high.bottom + rowHeight * 0.8f))
        // Sideways padding, but not the whole hole.
        assertEquals(0, geometry.rangeRowAt(low.left - 384 * 0.04f, low.centerY()))
        assertEquals(-1, geometry.rangeRowAt(low.left - 384 * 0.1f, low.centerY()))
        // The readout above the frame and the ring below it stay clear of the selector.
        assertEquals(-1, geometry.rangeRowAt(192f, low.top - rowHeight))
        assertEquals(-1, geometry.rangeRowAt(192f, 192f + geometry.ringRadius))
        // Cells are never stolen: every cell center resolves to no row.
        geometry.cellCenters.forEach { c -> assertEquals(-1, geometry.rangeRowAt(c[0], c[1])) }

        state.down(0, high.centerX(), high.bottom + rowHeight * 0.8f, firstFinger = true)
        assertTrue(model.isFromFToF)
        state.down(0, low.centerX(), low.top - rowHeight * 0.4f, firstFinger = true)
        assertFalse(model.isFromFToF)
    }

    @Test
    fun exploreByTouchFindsWhatAFingerWouldPlay() {
        // Between two cells, where a finger plays one of them, a screen reader finds one of them too.
        geometry.cellCenters.indices.forEach { index ->
            val angle = Math.toRadians(-90.0 + (index + 1) * 360.0 / 13)
            val gapX = (192f + geometry.ringRadius * cos(angle).toFloat()).toInt()
            val gapY = (192f + geometry.ringRadius * sin(angle).toFloat()).toInt()
            val next = (index + 1) % 13
            assertTrue(
                "the gap after cell $index is covered",
                geometry.cellTarget(index).contains(gapX, gapY) || geometry.cellTarget(next).contains(gapX, gapY),
            )
        }
        // The range rows' padded hit strip, not just their drawn frames.
        val low = geometry.rangeLowRect
        val high = geometry.rangeHighRect
        assertTrue(geometry.rangeTarget(0).contains(low.centerX().toInt(), (low.top - low.height() * 0.4f).toInt()))
        assertTrue(geometry.rangeTarget(0).contains((low.left - 384 * 0.04f).toInt(), low.centerY().toInt()))
        assertTrue(geometry.rangeTarget(1).contains(high.centerX().toInt(), (high.bottom + low.height() * 0.8f).toInt()))
        // And the two rows do not claim each other's space.
        assertFalse(android.graphics.Rect.intersects(geometry.rangeTarget(0), geometry.rangeTarget(1)))
    }

    @Test
    fun onlyTheFirstFingerCanPickARange() {
        down(0, 0)
        val high = geometry.rangeHighRect
        assertFalse(state.down(1, high.centerX(), high.centerY(), firstFinger = false))
        assertFalse(model.isFromFToF)
        assertTrue("the held note keeps sounding", model.notes[0].isPlaying)
    }

    @Test
    fun rotary_selectsRangesAndStopsNotesEvenForRepeatedSelection() {
        val oldNote = model.notes[0]
        oldNote.play()
        state.rotate(-1f)
        assertTrue(model.isFromFToF)
        assertFalse(oldNote.isPlaying)
        model.notes[12].play()
        state.rotate(-1f)
        assertTrue(model.notes.none { it.isPlaying })
        state.rotate(1f)
        assertFalse(model.isFromFToF)
        state.rotate(0f)
        assertFalse("no movement is no choice", model.isFromFToF)
    }

    @Test
    fun aSoundingNoteBreathesUnlessMotionIsReduced() {
        assertFalse(state.breathing)
        model.notes[0].play()
        assertTrue(state.breathing)
        reduceMotion = true
        assertFalse(state.breathing)
        assertTrue(model.notes[0].isPlaying)
        reduceMotion = false
        state.breathePhase = 1f
        state.stopAll()
        assertFalse(state.breathing)
        assertEquals(0f, state.breathePhase)
        assertTrue(model.notes.none { it.isPlaying })
    }

    @Test
    fun theGlowFollowsTheSystemAnimatorDurationScale() {
        // At 2x, as a ValueAnimator would, one breath takes eight seconds.
        assertEquals(Math.PI.toFloat(), InstrumentState.breathePhaseAt(4_000, durationScale = 2f), 1e-4f)
        assertEquals(Math.PI.toFloat(), InstrumentState.breathePhaseAt(1_000, durationScale = 0.5f), 1e-4f)
    }

    @Test
    fun theGlowBreathesOnceEveryFourSeconds() {
        assertEquals(0f, InstrumentState.breathePhaseAt(0), 0f)
        assertEquals(Math.PI.toFloat(), InstrumentState.breathePhaseAt(2000), 0.0001f)
        assertEquals(0f, InstrumentState.breathePhaseAt(4000), 0f)
        assertEquals(Math.PI.toFloat() / 2, InstrumentState.breathePhaseAt(5000), 0.0001f)
    }

    @Test
    fun eachFingerOwnsItsNoteAndTheLastLiftStopsTheRest() {
        down(0, 0)
        down(1, 4)
        assertTrue(model.notes[0].isPlaying)
        assertTrue(model.notes[4].isPlaying)
        assertEquals(2, haptics.count { it == HapticFeedbackConstants.KEYBOARD_TAP })
        state.up(0)
        assertFalse(model.notes[0].isPlaying)
        assertTrue(model.notes[4].isPlaying)
        state.endGesture()
        assertFalse(model.notes[4].isPlaying)
    }

    @Test
    fun twoFingersOnOneCellHoldItUntilBothLeave() {
        down(0, 2)
        down(1, 2)
        state.up(0)
        assertTrue(model.notes[2].isPlaying)
        state.up(1)
        assertFalse(model.notes[2].isPlaying)
    }

    @Test
    fun slidingBetweenCellsTransfersOnlyThatPointersNote() {
        down(0, 0)
        down(1, 4)
        move(0, 1)
        move(1, 4)
        assertFalse(model.notes[0].isPlaying)
        assertTrue(model.notes[1].isPlaying)
        assertTrue(model.notes[4].isPlaying)
        state.endGesture()
        assertTrue(model.notes.none { it.isPlaying })
    }

    @Test
    fun toggleMode_keepsPlayingAfterReleaseAndStopsOnTheNextTap() {
        state.toggleMode = true
        down(0, 0)
        state.endGesture()
        assertTrue(model.notes[0].isPlaying)
        move(0, 1)
        assertFalse("toggle mode ignores slides", model.notes[1].isPlaying)
        down(0, 0)
        assertFalse(model.notes[0].isPlaying)
    }

    @Test
    fun stopAll_clearsPointerOwnership() {
        down(0, 0)
        state.stopAll()
        move(0, 1)
        assertTrue(model.notes.none { it.isPlaying })
    }

    @Test
    fun spokenNamesCoverEveryCell() {
        val descriptions =
            listOf(
                "C, octave 4", "C sharp, D flat, octave 4", "D, octave 4",
                "D sharp, E flat, octave 4", "E, octave 4", "F, octave 4",
                "F sharp, G flat, octave 4", "G, octave 4", "G sharp, A flat, octave 4",
                "A, octave 4", "A sharp, B flat, octave 4", "B, octave 4", "C, octave 5",
            )
        assertEquals(descriptions, model.notes.map(NoteNames::spoken))
        assertEquals("C♯4", NoteNames.readout(model.notes[1]))
        assertEquals("♯/♭", NoteNames.engraved(model.notes[1]))
        assertEquals("C", NoteNames.engraved(model.notes[0]))
    }
}
