package depollsoft.pitchperfect.lib.sound

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
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
        val sine = values(NoteSound.SINE, 440.0, 3000)
        assertEquals(0.0, sine[0], 0.0)
        for (n in 882 until 3000) assertEquals(0.89 * sin(2 * PI * n * 440.0 / 44100), sine[n], 1e-9)
        assertEquals(0.89, sine.maxOf { abs(it) }, 1e-3)
    }

    @Test
    fun everyWaveFadesInOver20MsOnARaisedCosine() {
        for (sound in listOf(NoteSound.SINE, NoteSound.TRIANGLE, NoteSound.SQUARE, NoteSound.SAWTOOTH)) {
            val wave = values(sound, 100.0, 882)
            for (n in 1 until 882) {
                val expected = 0.5 * (1 - cos(PI * n / 882))
                assertEquals("$sound gain at $n", expected, rampGain(n), 1e-12)
                assertTrue("$sound at $n", abs(wave[n]) <= 0.89 * expected + 1e-9)
            }
            assertEquals(sound.name, 0.0, wave[0], 0.0)
        }
    }

    @Test
    fun theFadeInStartsAndEndsFlat() {
        // A linear ramp starts with a corner; a raised cosine's slope is zero at both ends.
        assertTrue(rampGain(1) < 1e-5)
        assertTrue(1 - rampGain(881) < 1e-5)
        assertEquals(1.0, rampGain(882), 0.0)
        assertEquals(1.0, rampGain(100_000), 0.0)
        for (n in 1..882) assertTrue(rampGain(n) >= rampGain(n - 1))
    }

    @Test
    fun aTriangleStartsAtZeroAndRisesLikeASine() {
        val source = WaveSource(NoteSound.TRIANGLE, 441.0)
        val wave = List(1000) { source.nextValue() }
        // 441 Hz at 44.1 kHz is exactly 100 samples a cycle: a quarter-cycle into a cycle after the fade-in, the peak.
        assertEquals(0.89, wave[925], 1e-9)
        assertEquals(-0.89, wave[975], 1e-9)
        assertEquals(0.0, wave[900], 1e-9)
    }

    @Test
    fun squareAndSawtoothHoldTheirLevelBetweenEdges() {
        val square = values(NoteSound.SQUARE, 441.0, 1000)
        assertEquals(0.89, square[925], 1e-9)
        assertEquals(-0.89, square[975], 1e-9)
        val saw = values(NoteSound.SAWTOOTH, 441.0, 1000)
        assertEquals(0.0, saw[950], 1e-9)
        assertEquals(0.89 * 0.5, saw[975], 1e-9)
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
        val saw = values(NoteSound.SAWTOOTH, 3520.0, 3000).drop(882)
        val biggestStep = saw.zipWithNext().maxOf { (a, b) -> abs(b - a) }
        assertTrue("step $biggestStep", biggestStep < 0.89 * 1.6)
    }
}
