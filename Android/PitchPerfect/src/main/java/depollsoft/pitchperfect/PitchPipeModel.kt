package depollsoft.pitchperfect

import depollsoft.lib.state.StateField
import depollsoft.lib.util.Preferences
import depollsoft.pitchperfect.lib.Accidental
import depollsoft.pitchperfect.lib.Note

/**
 * The pitch pipe's thirteen notes, C to C or F to F; the range is a stored preference.
 * [updateWidgets] redraws the home-screen widgets after the range changes.
 */
class PitchPipeModel @JvmOverloads constructor(
    private val updateWidgets: () -> Unit = PitchPipeAppWidget::updateWidgets,
) {
    /** The cells, clockwise from the top. */
    var notes: List<Note> by StateField(emptyList())

    /** Whether the pipe spans F4 to F5 rather than C4 to C5. Setting it redraws the widget too. */
    var isFromFToF: Boolean
        get() = Preferences.get(IS_FROM_F_TO_F_KEY)
        set(value) {
            Preferences.set(IS_FROM_F_TO_F_KEY, value)
            notes = if (value) F_TO_F else C_TO_C
            updateWidgets()
        }

    init {
        Preferences.initialize(IS_FROM_F_TO_F_KEY, false)
        notes = if (isFromFToF) F_TO_F else C_TO_C
    }

    companion object {
        private const val IS_FROM_F_TO_F_KEY = "depollsoft.pitchperfect.PitchPipeModel.IsFromFToF"

        private fun octave(
            vararg names: Pair<String, Int>,
            start: Int,
        ): List<Note> =
            names.map { (name, offset) ->
                val accidental = if (name.endsWith("#")) Accidental.Sharp else Accidental.Natural
                Note.findNote(name.removeSuffix("#"), accidental, start + offset)
            }

        private val C_TO_C: List<Note> by lazy {
            octave(
                "C" to 0, "C#" to 0, "D" to 0, "D#" to 0, "E" to 0, "F" to 0, "F#" to 0,
                "G" to 0, "G#" to 0, "A" to 0, "A#" to 0, "B" to 0, "C" to 1,
                start = 4,
            )
        }

        private val F_TO_F: List<Note> by lazy {
            octave(
                "F" to 0, "F#" to 0, "G" to 0, "G#" to 0, "A" to 0, "A#" to 0, "B" to 0,
                "C" to 1, "C#" to 1, "D" to 1, "D#" to 1, "E" to 1, "F" to 1,
                start = 4,
            )
        }
    }
}
