package depollsoft.pitchperfect

import org.junit.Assert.assertEquals
import org.junit.Test

class PitchIntervalTest {
    @Test
    fun namesAscendingChromaticIntervals() {
        assertEquals("MINOR 2ND", PitchInterval.name(0, 1))
        assertEquals("MAJOR 3RD", PitchInterval.name(0, 4))
        assertEquals("TRITONE", PitchInterval.name(2, 8))
        assertEquals("PERFECT 5TH", PitchInterval.name(0, 7))
        assertEquals("MAJOR 7TH", PitchInterval.name(0, 11))
    }

    @Test
    fun orderDoesNotChangeTheIntervalName() {
        assertEquals("PERFECT 4TH", PitchInterval.name(10, 5))
    }
}
