package depollsoft.pitchperfect.lib.sound

import java.io.File
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The instrument notes of docs/pitchperfect-note-sounds.md, against the shipped tuning table. */
@RunWith(RobolectricTestRunner::class) // for org.json
class MidiNoteTest {
    private val table by lazy { InstrumentTuning.parse(File("../../shared/pitchperfect/instrument-tuning.json").readText()) }

    private fun frequency(key: Int) = 440.0 * Math.pow(2.0, (key - 69) / 12.0)

    @Test
    fun keysComeFromTheStoredA440Frequency() {
        assertEquals(69, MidiNote.keyFor(440.0))
        assertEquals(60, MidiNote.keyFor(261.6255653))
        assertEquals(12, MidiNote.keyFor(16.35)) // C0
        assertEquals(107, MidiNote.keyFor(3951.07)) // B7
        assertEquals(0, MidiNote.keyFor(1.0))
        assertEquals(127, MidiNote.keyFor(100_000.0))
    }

    @Test
    fun theTableIsReadForAndroid() {
        assertEquals(5.3, table.gainDb(0), 1e-9)
        assertEquals(-6.6, table.correction(0, 60)!!, 1e-9)
        assertEquals(-19.2, table.correction(46, 60)!!, 1e-9)
        assertNull("the vibraphone is silent at C1", table.correction(11, 24))
        assertEquals(0.3, table.correction(11, 61)!!, 1e-9)
        // Keys past the measured ones use the nearest end's value.
        assertEquals(table.correction(0, 24), table.correction(0, 12))
        assertEquals(table.correction(0, 107), table.correction(0, 127))
    }

    @Test
    fun aNoteIsBentByTheTuningPlusTheSynthsError() {
        val harp = MidiNote.plan(NoteSound.HARP, frequency(60), 440.0, table)
        assertEquals(MidiNotePlan(46, 60, -19.2, 13.1), harp)
        val at430 = MidiNote.plan(NoteSound.HARP, frequency(60), 430.0, table)
        assertEquals(-19.2 + 1200 * Math.log(430.0 / 440) / Math.log(2.0), at430.pitchCents, 1e-9)
    }

    @Test
    fun whereTheInstrumentIsSilentThePianoPlays() {
        val low = MidiNote.plan(NoteSound.VIBRAPHONE, frequency(24), 440.0, table)
        assertEquals(MidiNotePlan(0, 24, table.correction(0, 24)!!, 5.3), low)
        val accordion = MidiNote.plan(NoteSound.ACCORDION, frequency(100), 440.0, table)
        assertEquals(0, accordion.program)
        assertEquals(21, MidiNote.plan(NoteSound.ACCORDION, frequency(60), 440.0, table).program)
    }

    @Test
    fun withoutATableInstrumentsPlayUncorrected() {
        assertEquals(MidiNotePlan(73, 69, 0.0, 0.0), MidiNote.plan(NoteSound.FLUTE, 440.0, 440.0, InstrumentTuning.NONE))
    }

    @Test
    fun bendCoversTwoSemitonesEachWayAndClamps() {
        assertEquals(8192, MidiNote.bend(0.0))
        assertEquals(12288, MidiNote.bend(100.0))
        assertEquals(4096, MidiNote.bend(-100.0))
        assertEquals(6529, MidiNote.bend(-40.6)) // 8192 - 40.6 / 200 · 8192
        assertEquals(16383, MidiNote.bend(200.0))
        assertEquals(16383, MidiNote.bend(1000.0))
        assertEquals(0, MidiNote.bend(-1000.0))
    }

    @Test
    fun theFileIsAOneNoteType0MidiFile() {
        val bytes = MidiNote.file(MidiNotePlan(program = 46, key = 60, pitchCents = -19.2, gainDb = 13.1))
        // bend = round(8192 - 19.2 / 200 · 8192) = 7406 = 0x1CEE: LSB 0x6E, MSB 0x39.
        // 30 minutes at 960 ticks a second = 1 728 000 = 0x1A5E00 → 0xE9 0xBC 0x00.
        val track =
            intArrayOf(
                0x00, 0xB0, 0x65, 0x00,
                0x00, 0xB0, 0x64, 0x00,
                0x00, 0xB0, 0x06, 0x02,
                0x00, 0xB0, 0x26, 0x00,
                0x00, 0xC0, 46,
                0x00, 0xE0, 0x6E, 0x39,
                0x00, 0x90, 60, 100,
                0xE9, 0xBC, 0x00, 0x80, 60, 0x00,
                0x00, 0xFF, 0x2F, 0x00,
            )
        val expected =
            intArrayOf(
                'M'.code, 'T'.code, 'h'.code, 'd'.code, 0, 0, 0, 6, 0, 0, 0, 1, 0x01, 0xE0,
                'M'.code, 'T'.code, 'r'.code, 'k'.code, 0, 0, 0, track.size,
            ) + track
        assertArrayEquals(ByteArray(expected.size) { expected[it].toByte() }, bytes)
    }

    @Test
    fun variableLengthQuantitiesMatchTheSpec() {
        fun vlq(value: Int) = MidiNote.variableLength(value).map { it.toInt() and 0xFF }
        assertEquals(listOf(0x00), vlq(0))
        assertEquals(listOf(0x7F), vlq(0x7F))
        assertEquals(listOf(0x81, 0x00), vlq(0x80))
        assertEquals(listOf(0xFF, 0x7F), vlq(0x3FFF))
        assertEquals(listOf(0x81, 0x80, 0x00), vlq(0x4000))
    }
}
