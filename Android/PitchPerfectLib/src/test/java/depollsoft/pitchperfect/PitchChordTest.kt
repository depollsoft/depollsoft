package depollsoft.pitchperfect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PitchChordTest {
    @Test
    fun rootPositionDominantSeventhOnEveryRootIsBarbershop() {
        for (root in 0..2) {
            assertEquals("BARBERSHOP!", PitchChord.name(listOf(root, root + 4, root + 7, root + 10)))
        }
    }

    @Test
    fun voicingAndDoubledRootDoNotMatter() {
        // First inversion of G7 inside one C-to-C octave: B4 D5(=2) F4 G4.
        assertEquals("BARBERSHOP!", PitchChord.name(listOf(11, 2, 5, 7)))
        // C7 with the root doubled at the octave cell.
        assertEquals("BARBERSHOP!", PitchChord.name(listOf(0, 4, 7, 10, 12)))
        assertEquals("BARBERSHOP!", PitchChord.name(listOf(10, 7, 4, 0)))
    }

    @Test
    fun otherChordsStayCounted() {
        assertNull("major seventh", PitchChord.name(listOf(0, 4, 7, 11)))
        assertNull("minor seventh", PitchChord.name(listOf(0, 3, 7, 10)))
        assertNull("diminished", PitchChord.name(listOf(0, 3, 6, 9)))
        assertNull("triad", PitchChord.name(listOf(0, 4, 7)))
        assertNull("five distinct notes", PitchChord.name(listOf(0, 2, 4, 7, 10)))
        assertNull("octave only", PitchChord.name(listOf(0, 12)))
    }
}
