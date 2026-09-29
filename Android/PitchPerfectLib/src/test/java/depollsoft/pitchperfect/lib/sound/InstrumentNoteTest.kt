package depollsoft.pitchperfect.lib.sound

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The instrument notes of docs/pitchperfect-note-sounds.md, against the shipped tuning table. */
@RunWith(RobolectricTestRunner::class) // for org.json
class InstrumentNoteTest {
    private val table by lazy { InstrumentTuning.parse(File("../../shared/pitchperfect/instrument-tuning.json").readText()) }

    private fun frequency(key: Int) = 440.0 * Math.pow(2.0, (key - 69) / 12.0)

    @Test
    fun keysComeFromTheStoredA440Frequency() {
        assertEquals(69, InstrumentNote.keyFor(440.0))
        assertEquals(60, InstrumentNote.keyFor(261.6255653))
        assertEquals(12, InstrumentNote.keyFor(16.35)) // C0
        assertEquals(107, InstrumentNote.keyFor(3951.07)) // B7
        assertEquals(0, InstrumentNote.keyFor(1.0))
        assertEquals(127, InstrumentNote.keyFor(100_000.0))
    }

    @Test
    fun theTableIsReadForAndroid() {
        assertEquals(-4.6, table.gainDb(0), 1e-9)
        assertEquals(2.2, table.correction(0, 60), 1e-9)
        assertEquals(-25.4, table.correction(52, 60), 1e-9)
        // TinySoundFont plays every key of the bank: nothing is silent, so nothing falls back.
        assertEquals(4.2, table.correction(11, 24), 1e-9)
        assertEquals(-2.6, table.correction(22, 40), 1e-9)
        // Keys past the measured ones use the nearest end's value.
        assertEquals(table.correction(0, 24), table.correction(0, 12), 0.0)
        assertEquals(table.correction(0, 107), table.correction(0, 127), 0.0)
    }

    @Test
    fun aNoteIsTunedByTheReferencePitchPlusTheSynthsError() {
        val harp = InstrumentNote.plan(NoteSound.HARP, frequency(60), 440.0, table)
        assertEquals(InstrumentNotePlan(46, 60, 2.1, -1.1), harp)
        val at430 = InstrumentNote.plan(NoteSound.HARP, frequency(60), 430.0, table)
        assertEquals(2.1 + 1200 * Math.log(430.0 / 440) / Math.log(2.0), at430.pitchCents, 1e-9)
    }

    @Test
    fun everyInstrumentPlaysItsOwnProgramAtEveryKey() {
        for (sound in NoteSound.entries.filter { it.kind == NoteSound.Kind.INSTRUMENT }) {
            for (key in 12..127) assertEquals(sound.program, InstrumentNote.plan(sound, frequency(key), 440.0, table).program)
        }
    }

    @Test
    fun withoutATableInstrumentsPlayUncorrected() {
        assertEquals(InstrumentNotePlan(73, 69, 0.0, 0.0), InstrumentNote.plan(NoteSound.FLUTE, 440.0, 440.0, InstrumentTuning.NONE))
    }
}
