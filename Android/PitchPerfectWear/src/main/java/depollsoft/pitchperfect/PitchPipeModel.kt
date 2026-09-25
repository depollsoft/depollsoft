package depollsoft.pitchperfect

import depollsoft.lib.state.StateField
import depollsoft.lib.state.StateList
import depollsoft.lib.util.Preferences
import depollsoft.pitchperfect.lib.Accidental
import depollsoft.pitchperfect.lib.Note

/** The watch's thirteen notes, C to C or F to F; the range is a stored preference. */
class PitchPipeModel : PitchPipe {
    private val cToC = octave("C" to 4, "C#" to 4, "D" to 4, "D#" to 4, "E" to 4, "F" to 4, "F#" to 4, "G" to 4, "G#" to 4, "A" to 4, "A#" to 4, "B" to 4, "C" to 5)
    private val fToF = octave("F" to 4, "F#" to 4, "G" to 4, "G#" to 4, "A" to 4, "A#" to 4, "B" to 4, "C" to 5, "C#" to 5, "D" to 5, "D#" to 5, "E" to 5, "F" to 5)

    override var notes: StateList<Note> by StateField(cToC)

    // Mirrors the stored choice so the face redraws the selector when it changes.
    private var highRange by StateField(false)

    override var isFromFToF: Boolean
        get() = highRange
        set(value) {
            Preferences.set(IS_FROM_F_TO_F_KEY, value)
            highRange = value
            notes = if (value) fToF else cToC
        }

    init {
        Preferences.initialize(IS_FROM_F_TO_F_KEY, false)
        highRange = Preferences.get(IS_FROM_F_TO_F_KEY)
        notes = if (highRange) fToF else cToC
    }

    private companion object {
        const val IS_FROM_F_TO_F_KEY = "depollsoft.pitchperfect.PitchPipeModel.IsFromFToF"

        fun octave(vararg names: Pair<String, Int>): StateList<Note> =
            StateList(
                names.map { (name, octave) ->
                    val accidental = if (name.endsWith("#")) Accidental.Sharp else Accidental.Natural
                    Note.findNote(name.removeSuffix("#"), accidental, octave)
                },
            )
    }
}
