package depollsoft.pitchperfect

import kotlin.math.abs

/** Musical name for the ascending interval between two chromatic cells. */
object PitchInterval {
    private val names =
        arrayOf(
            "UNISON",
            "MINOR 2ND",
            "MAJOR 2ND",
            "MINOR 3RD",
            "MAJOR 3RD",
            "PERFECT 4TH",
            "TRITONE",
            "PERFECT 5TH",
            "MINOR 6TH",
            "MAJOR 6TH",
            "MINOR 7TH",
            "MAJOR 7TH",
            "OCTAVE",
        )

    fun name(
        firstCell: Int,
        secondCell: Int,
    ): String = names[abs(secondCell - firstCell).coerceIn(0, names.lastIndex)]
}
