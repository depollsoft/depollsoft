package depollsoft.pitchperfect.lib.sound

import kotlin.math.ln
import kotlin.math.roundToInt

/**
 * What the synth gets wrong about each instrument, measured by
 * scripts/pitchperfect/measure_instruments.py into shared/pitchperfect/instrument-tuning.json:
 * per key, the cents that bring it to true pitch, and the gain that brings the instrument to the
 * others' loudness.
 */
class InstrumentTuning(
    private val firstKey: Int,
    private val programs: Map<Int, Program>,
) {
    class Program(val gainDb: Double, val correctionCents: List<Double?>)

    /** [program]'s correction at [key] (clamped to the measured keys); 0 where it wasn't measured. */
    fun correction(program: Int, key: Int): Double {
        val corrections = programs[program]?.correctionCents ?: return 0.0
        return corrections[(key - firstKey).coerceIn(0, corrections.size - 1)] ?: 0.0
    }

    fun gainDb(program: Int): Double = programs[program]?.gainDb ?: 0.0

    companion object {
        /** No measurements: every instrument as the synth plays it. */
        @JvmField val NONE = InstrumentTuning(24, emptyMap())

        /** Reads the Android half of instrument-tuning.json. */
        @JvmStatic
        fun parse(json: String): InstrumentTuning {
            val root = org.json.JSONObject(json)
            val android = root.getJSONObject("android")
            val programs = mutableMapOf<Int, Program>()
            for (name in android.keys()) {
                val entry = android.getJSONObject(name)
                val values = entry.getJSONArray("correctionCents")
                val corrections = List(values.length()) { if (values.isNull(it)) null else values.getDouble(it) }
                programs[name.toInt()] = Program(entry.getDouble("gainDb"), corrections)
            }
            return InstrumentTuning(root.getInt("firstKey"), programs)
        }
    }
}

/** One instrument note as the synth should play it: [pitchCents] retunes its channel, [gainDb] sets its level. */
data class InstrumentNotePlan(val program: Int, val key: Int, val pitchCents: Double, val gainDb: Double)

/** Which program and key an instrument note plays, and how its channel is tuned (docs/pitchperfect-note-sounds.md). */
object InstrumentNote {
    const val VELOCITY = 100

    /** The MIDI key nearest [storedFrequency], the note's frequency at A440. */
    @JvmStatic
    fun keyFor(storedFrequency: Double): Int = (69 + 12 * ln(storedFrequency / 440) / ln(2.0)).roundToInt().coerceIn(0, 127)

    @JvmStatic
    fun plan(
        sound: NoteSound,
        storedFrequency: Double,
        referencePitch: Double,
        tuning: InstrumentTuning,
    ): InstrumentNotePlan {
        require(sound.kind == NoteSound.Kind.INSTRUMENT) { "$sound is not an instrument" }
        val key = keyFor(storedFrequency)
        val tuningCents = 1200 * ln(referencePitch / 440) / ln(2.0)
        return InstrumentNotePlan(sound.program, key, tuningCents + tuning.correction(sound.program, key), tuning.gainDb(sound.program))
    }
}
