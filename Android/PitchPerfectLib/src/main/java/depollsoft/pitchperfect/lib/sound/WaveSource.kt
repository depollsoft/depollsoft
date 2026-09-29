package depollsoft.pitchperfect.lib.sound

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** The rate the waves play at; the original pitch pipe keeps its own 8 kHz. */
const val WAVE_SAMPLE_RATE = 44100

private const val LEVEL = 0.89 // -1 dBFS
private const val RAMP_SAMPLES = 220 // 5 ms at 44.1 kHz

/** PolyBLEP: smooths a wave's jump at phase 0 over one sample either side, so it doesn't alias. */
internal fun polyBlep(t: Double, dt: Double): Double =
    when {
        t < dt -> {
            val x = t / dt
            x + x - x * x - 1
        }
        t > 1 - dt -> {
            val x = (t - 1) / dt
            x * x + x + x + 1
        }
        else -> 0.0
    }

/** An endless sine, triangle, square or sawtooth at [frequency], ramping in over its first 5 ms. */
class WaveSource(
    private val sound: NoteSound,
    frequency: Double,
    sampleRate: Int = WAVE_SAMPLE_RATE,
) {
    init {
        require(sound.kind == NoteSound.Kind.WAVE) { "$sound is not a wave" }
    }

    private val dt = frequency / sampleRate
    private var phase = 0.0
    private var count = 0

    /** The next sample as a float in [-1, 1]. */
    fun nextValue(): Double {
        val p = phase
        val value =
            when (sound) {
                NoteSound.SINE -> sin(2 * PI * p)
                NoteSound.TRIANGLE -> 1 - 4 * abs((p + 0.25) % 1.0 - 0.5)
                NoteSound.SQUARE -> (if (p < 0.5) 1.0 else -1.0) + polyBlep(p, dt) - polyBlep((p + 0.5) % 1.0, dt)
                else -> (2 * p - 1) - polyBlep(p, dt)
            }
        val sample = LEVEL * value * min(1.0, count.toDouble() / RAMP_SAMPLES)
        count++
        phase += dt
        if (phase >= 1) phase -= 1
        return sample
    }

    /** The next sample as 16-bit PCM. */
    fun nextSample(): Short = (nextValue() * 32767).roundToInt().coerceIn(-32768, 32767).toShort()
}
