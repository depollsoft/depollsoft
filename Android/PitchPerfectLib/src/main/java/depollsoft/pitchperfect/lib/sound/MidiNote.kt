package depollsoft.pitchperfect.lib.sound

import java.io.ByteArrayOutputStream
import kotlin.math.ln
import kotlin.math.roundToInt

/**
 * What the Android synth gets wrong about each instrument, measured by
 * scripts/pitchperfect/measure_instruments.py into shared/pitchperfect/instrument-tuning.json:
 * per key, the cents that bring it to true pitch (null where the instrument is silent), and the
 * gain that brings the instrument to the others' loudness.
 */
class InstrumentTuning(
    private val firstKey: Int,
    private val programs: Map<Int, Program>,
) {
    class Program(val gainDb: Double, val correctionCents: List<Double?>)

    /** [program]'s correction at [key] (clamped to the measured keys); null where it is silent. */
    fun correction(program: Int, key: Int): Double? {
        val corrections = programs[program]?.correctionCents ?: return 0.0
        return corrections[(key - firstKey).coerceIn(0, corrections.size - 1)]
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

/** One instrument note as the synth should play it. */
data class MidiNotePlan(val program: Int, val key: Int, val pitchCents: Double, val gainDb: Double)

/**
 * Builds the MIDI for an instrument note (docs/pitchperfect-note-sounds.md): which program and key,
 * bent by the tuning plus the synth's measured error, as a one-note standard MIDI file.
 */
object MidiNote {
    const val VELOCITY = 100

    /** How long the file holds the note; a note held longer is played again. */
    const val HOLD_SECONDS = 30 * 60

    private const val PIANO = 0

    /** The MIDI key nearest [storedFrequency], the note's frequency at A440. */
    @JvmStatic
    fun keyFor(storedFrequency: Double): Int = (69 + 12 * ln(storedFrequency / 440) / ln(2.0)).roundToInt().coerceIn(0, 127)

    @JvmStatic
    fun plan(
        sound: NoteSound,
        storedFrequency: Double,
        referencePitch: Double,
        tuning: InstrumentTuning,
    ): MidiNotePlan {
        require(sound.kind == NoteSound.Kind.INSTRUMENT) { "$sound is not an instrument" }
        val key = keyFor(storedFrequency)
        var program = sound.program
        var correction = tuning.correction(program, key)
        if (correction == null) {
            // This synth's instrument is silent here: its fallback sounds the note if it can,
            // otherwise the piano.
            program = sound.fallbackProgram
            correction = tuning.correction(program, key)
            if (correction == null) {
                program = PIANO
                correction = tuning.correction(PIANO, key) ?: 0.0
            }
        }
        val tuningCents = 1200 * ln(referencePitch / 440) / ln(2.0)
        return MidiNotePlan(program, key, tuningCents + correction, tuning.gainDb(program))
    }

    /**
     * The 14-bit pitch bend for [cents], with the bend range at ±3 semitones:
     * room for the lowest tuning (400 Hz, -165 cents) plus a key's correction.
     */
    @JvmStatic
    fun bend(cents: Double): Int = (8192 + cents / 300 * 8192).roundToInt().coerceIn(0, 16383)

    /** A type-0 standard MIDI file that sets the bend range, program and bend, then holds the note. */
    @JvmStatic
    fun file(plan: MidiNotePlan): ByteArray {
        val bend = bend(plan.pitchCents)
        val track = ByteArrayOutputStream()
        fun event(delta: Int, vararg bytes: Int) {
            track.write(variableLength(delta))
            bytes.forEach { track.write(it) }
        }
        event(0, 0xB0, 0x65, 0x00) // RPN 0, pitch bend sensitivity:
        event(0, 0xB0, 0x64, 0x00)
        event(0, 0xB0, 0x06, 0x03) // ±3 semitones
        event(0, 0xB0, 0x26, 0x00)
        event(0, 0xC0, plan.program)
        event(0, 0xE0, bend and 0x7F, bend shr 7)
        event(0, 0x90, plan.key, VELOCITY)
        event(HOLD_SECONDS * TICKS_PER_SECOND, 0x80, plan.key, 0x00)
        event(0, 0xFF, 0x2F, 0x00)
        val body = track.toByteArray()
        val out = ByteArrayOutputStream()
        out.write("MThd".toByteArray(Charsets.US_ASCII))
        out.write(int32(6))
        out.write(byteArrayOf(0, 0, 0, 1)) // format 0, one track
        out.write(byteArrayOf((TICKS_PER_QUARTER shr 8).toByte(), TICKS_PER_QUARTER.toByte()))
        out.write("MTrk".toByteArray(Charsets.US_ASCII))
        out.write(int32(body.size))
        out.write(body)
        return out.toByteArray()
    }

    // 120 bpm, the default tempo: 480 ticks a quarter is 960 a second.
    private const val TICKS_PER_QUARTER = 480
    private const val TICKS_PER_SECOND = 960

    private fun int32(value: Int) = byteArrayOf((value shr 24).toByte(), (value shr 16).toByte(), (value shr 8).toByte(), value.toByte())

    internal fun variableLength(value: Int): ByteArray {
        val bytes = ArrayDeque<Int>()
        var rest = value
        bytes.addFirst(rest and 0x7F)
        rest = rest shr 7
        while (rest > 0) {
            bytes.addFirst((rest and 0x7F) or 0x80)
            rest = rest shr 7
        }
        return ByteArray(bytes.size) { bytes[it].toByte() }
    }
}
