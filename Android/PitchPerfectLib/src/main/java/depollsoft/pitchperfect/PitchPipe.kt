package depollsoft.pitchperfect

import depollsoft.pitchperfect.lib.Note

/** A pitch pipe's thirteen notes, C to C or F to F. */
interface PitchPipe {
    /** The cells, clockwise from the top. */
    val notes: List<Note>

    /** Whether the pipe spans F4 to F5 rather than C4 to C5. */
    var isFromFToF: Boolean
}
