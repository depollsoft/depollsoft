package depollsoft.pitchperfect.lib.sound

import depollsoft.pitchperfect.lib.Note
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class NoteSoundTest {
    @After
    fun tearDown() {
        Note.setSound(NoteSound.DEFAULT)
    }

    @Test
    fun theIdsOrderAndProgramsAreTheContracts() {
        assertEquals(
            listOf(
                "pitchPipe", "sine", "triangle", "square", "sawtooth", "piano", "electricPiano", "harpsichord",
                "vibraphone", "organ", "reedOrgan", "accordion", "harmonica", "guitar", "harp", "strings", "choir", "trumpet", "clarinet", "flute",
            ),
            NoteSound.entries.map { it.id },
        )
        assertEquals(
            listOf(0, 4, 6, 11, 19, 20, 21, 22, 24, 46, 48, 52, 56, 71, 73),
            NoteSound.entries.filter { it.kind == NoteSound.Kind.INSTRUMENT }.map { it.program },
        )
    }

    @Test
    fun anUnknownIdReadsAsThePitchPipe() {
        assertEquals(NoteSound.PITCH_PIPE, NoteSound.fromId("kazoo"))
        assertEquals(NoteSound.PITCH_PIPE, NoteSound.fromId("bagpipes"))
        assertEquals(NoteSound.PITCH_PIPE, NoteSound.fromId(null))
        assertEquals(NoteSound.CHOIR, NoteSound.fromId("choir"))
        assertEquals(NoteSound.HARMONICA, NoteSound.fromId("harmonica"))
    }

    @Test
    fun notesStartInThePitchPipeAndFollowTheChoice() {
        assertEquals(NoteSound.PITCH_PIPE, Note.getSound())
        Note.setSound(NoteSound.FLUTE)
        assertEquals(NoteSound.FLUTE, Note.getSound())
    }
}
