package depollsoft.pitchperfect

import depollsoft.pitchperfect.lib.Accidental
import depollsoft.pitchperfect.lib.Key
import depollsoft.pitchperfect.lib.KeyType

/** How keys are chosen and spoken in the song editor. */
object SongKeys {
    fun accidentalCount(numAccidentals: Int): String =
        when {
            numAccidentals == 0 -> "no sharps or flats"
            numAccidentals == 1 -> "1 sharp"
            numAccidentals == -1 -> "1 flat"
            numAccidentals > 0 -> "$numAccidentals sharps"
            else -> "${-numAccidentals} flats"
        }

    /** "A flat major, 4 flats": what a screen reader says for a key row. */
    fun spokenName(key: Key): String {
        val letter = key.note.friendlyName.uppercase()
        val spelled =
            when (key.accidental) {
                Accidental.Sharp -> "$letter sharp"
                Accidental.Flat -> "$letter flat"
                else -> letter
            }
        val mode = if (key.keyType == KeyType.Minor) "minor" else "major"
        return "$spelled $mode, ${accidentalCount(key.numAccidentals)}"
    }

    /**
     * "Shenandoah, C minor, 3 flats": a song row read aloud. The row shows its key in a music
     * font whose letters would otherwise be spelled out.
     */
    fun spokenSong(
        title: String?,
        key: Key?,
    ): String = listOfNotNull(title?.takeIf { it.isNotEmpty() }, key?.let(::spokenName)).joinToString(", ")

    fun keysOf(minor: Boolean): List<Key> = if (minor) Key.getMinorKeys() else Key.getMajorKeys()

    /** Flipping the mode keeps the signature: a relative key shares it. */
    fun relative(
        key: Key,
        minor: Boolean,
    ): Key {
        if ((key.keyType == KeyType.Minor) == minor) return key
        val list = keysOf(minor)
        return list.firstOrNull { it.numAccidentals == key.numAccidentals } ?: list[list.size / 2]
    }
}
