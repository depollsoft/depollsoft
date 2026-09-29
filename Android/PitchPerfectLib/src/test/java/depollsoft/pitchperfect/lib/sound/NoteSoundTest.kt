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
    fun theIdsOrderSectionsAndProgramsAreTheContracts() {
        assertEquals(
            listOf(
                "pitchPipe",
                "organ", "reedOrgan", "accordion", "harmonica", "strings", "choir", "trumpet", "clarinet", "flute",
                "sine", "triangle", "square", "sawtooth",
                "piano", "electricPiano", "harpsichord", "vibraphone", "guitar", "harp",
            ),
            NoteSound.entries.map { it.id },
        )
        assertEquals(
            listOf(NoteSound.Section.DEFAULT) + List(9) { NoteSound.Section.SUSTAINED } +
                List(4) { NoteSound.Section.WAVES } + List(6) { NoteSound.Section.PLUCKED_AND_STRUCK },
            NoteSound.entries.map { it.section },
        )
        assertEquals(
            listOf(19, 20, 21, 22, 48, 52, 56, 71, 73, 0, 4, 6, 11, 24, 46),
            NoteSound.entries.filter { it.kind == NoteSound.Kind.INSTRUMENT }.map { it.program },
        )
        assertEquals(NoteSound.Kind.WAVE, NoteSound.SAWTOOTH.kind)
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
