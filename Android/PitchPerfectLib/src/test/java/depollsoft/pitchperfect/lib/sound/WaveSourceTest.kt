package depollsoft.pitchperfect.lib.sound

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The waves of docs/pitchperfect-note-sounds.md. */
class WaveSourceTest {
    private fun values(sound: NoteSound, frequency: Double, count: Int): List<Double> {
        val source = WaveSource(sound, frequency)
        return List(count) { source.nextValue() }
    }

    @Test
    fun aSineIsAPureToneAtMinusOneDbfsAfterItsRamp() {
        val sine = values(NoteSound.SINE, 440.0, 2000)
        assertEquals(0.0, sine[0], 0.0)
        for (n in 220 until 2000) assertEquals(0.89 * sin(2 * PI * n * 440.0 / 44100), sine[n], 1e-9)
        assertEquals(0.89, sine.maxOf { abs(it) }, 1e-3)
    }

    @Test
    fun everyWaveRampsInOver5Ms() {
        for (sound in listOf(NoteSound.SINE, NoteSound.TRIANGLE, NoteSound.SQUARE, NoteSound.SAWTOOTH)) {
            val wave = values(sound, 100.0, 220)
            // Half-way through the ramp, nothing is louder than half the level.
            assertTrue(sound.name, wave.take(110).all { abs(it) <= 0.89 * 110 / 220 + 1e-9 })
        }
    }

    @Test
    fun aTriangleStartsAtZeroAndRisesLikeASine() {
        val source = WaveSource(NoteSound.TRIANGLE, 441.0)
        val wave = List(500) { source.nextValue() }
        // 441 Hz at 44.1 kHz is exactly 100 samples a cycle: a quarter-cycle in, the peak.
        assertEquals(0.89, wave[325], 1e-9)
        assertEquals(-0.89, wave[375], 1e-9)
        assertEquals(0.0, wave[300], 1e-9)
    }

    @Test
    fun squareAndSawtoothHoldTheirLevelBetweenEdges() {
        val square = values(NoteSound.SQUARE, 441.0, 500)
        assertEquals(0.89, square[325], 1e-9)
        assertEquals(-0.89, square[375], 1e-9)
        val saw = values(NoteSound.SAWTOOTH, 441.0, 500)
        assertEquals(0.0, saw[350], 1e-9)
        assertEquals(0.89 * 0.5, saw[375], 1e-9)
    }

    @Test
    fun polyBlepSmoothsOnlyTheSamplesBesideAnEdge() {
        val dt = 0.1
        assertEquals(-1.0, polyBlep(0.0, dt), 1e-12)
        assertEquals(0.0, polyBlep(0.5, dt), 1e-12)
        assertEquals(1.0, polyBlep(1.0 - 1e-12, dt), 1e-9)
        assertEquals(0.0, polyBlep(dt, dt), 1e-12)
    }

    @Test
    fun aHighSawtoothJumpsLessThanANaiveOne() {
        // At 3.5 kHz a naive sawtooth drops the full 2 × 0.89 in one sample; PolyBLEP spreads it.
        val saw = values(NoteSound.SAWTOOTH, 3520.0, 2000).drop(220)
        val biggestStep = saw.zipWithNext().maxOf { (a, b) -> abs(b - a) }
        assertTrue("step $biggestStep", biggestStep < 0.89 * 1.6)
    }
}
