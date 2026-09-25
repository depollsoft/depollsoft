package depollsoft.pitchperfect

import depollsoft.pitchperfect.lib.Accidental
import depollsoft.pitchperfect.lib.Note

/** How a pitch-pipe face names notes: engraved on a cell, in the readout, and to a screen reader. */
object NoteNames {
    private const val LETTERS = "CDEFGAB"

    private fun sharpName(note: Note): String {
        if (note.accidental == Accidental.Sharp) return note.friendlyName
        return LETTERS[(LETTERS.indexOf(note.friendlyName[0]) + 6) % LETTERS.length].toString()
    }

    private fun flatName(note: Note): String {
        if (note.accidental == Accidental.Flat) return note.friendlyName
        return LETTERS[(LETTERS.indexOf(note.friendlyName[0]) + 1) % LETTERS.length].toString()
    }

    /** A physical pitch pipe engraves the accidental cells with the glyphs alone. */
    fun engraved(note: Note): String =
        if (note.accidental == Accidental.Natural) note.friendlyName else "♯/♭"

    /** The centre readout: "C4", "F♯4". */
    fun readout(note: Note): String =
        (if (note.accidental == Accidental.Natural) note.friendlyName else "${sharpName(note)}♯") + note.octave

    /** What a screen reader says for a cell. */
    fun spoken(note: Note): String =
        when (note.accidental) {
            Accidental.Natural -> "${note.friendlyName}, octave ${note.octave}"
            else -> "${sharpName(note)} sharp, ${flatName(note)} flat, octave ${note.octave}"
        }
}
