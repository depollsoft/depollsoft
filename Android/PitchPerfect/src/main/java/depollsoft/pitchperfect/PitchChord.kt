package depollsoft.pitchperfect

/**
 * Names the chord the barbershop community lives on. Cells are chromatic
 * steps from the range root, so cell 12 is the root's octave; voicing and
 * doubled roots do not matter, only the pitch classes sounding together.
 */
object PitchChord {
    const val BARBERSHOP_SEVENTH = "BARBERSHOP!"

    /** Root, major third, perfect fifth, minor seventh: the dominant seventh. */
    private val dominantSeventh = setOf(0, 4, 7, 10)

    fun name(cells: Collection<Int>): String? {
        val pitchClasses = cells.map { ((it % 12) + 12) % 12 }.toSet()
        if (pitchClasses.size != dominantSeventh.size) return null
        val isSeventh =
            pitchClasses.any { root ->
                pitchClasses.map { (it - root + 12) % 12 }.toSet() == dominantSeventh
            }
        return if (isSeventh) BARBERSHOP_SEVENTH else null
    }
}
