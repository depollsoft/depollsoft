package depollsoft.pitchperfect.lib.sound

import android.content.Context
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import depollsoft.pitchperfect.lib.Note
import depollsoft.pitchperfect.lib.PitchAudioTrackGenerator
import java.util.concurrent.Executors

/** One note sounding in some voice. */
interface SoundingNote {
    /** Whether stopping would silence something. */
    val isSounding: Boolean

    fun play()

    fun stop()
}

/** A note streamed through an [AudioTrack]: the original pitch pipe, or a wave. */
private class TrackVoice(
    private val track: AudioTrack,
    private val stopTrack: (AudioTrack) -> Unit,
) : SoundingNote {
    override val isSounding: Boolean get() = track.playState == AudioTrack.PLAYSTATE_PLAYING

    override fun play() = track.play()

    override fun stop() = stopTrack(track)
}

/**
 * Makes the voice a note sounds in (docs/pitchperfect-note-sounds.md), and holds the instrument
 * tuning table the lib ships as an asset.
 */
object NoteVoices {
    private const val TAG = "NoteVoices"
    private const val TUNING_ASSET = "instrument-tuning.json"

    /**
     * Reads the instrument tuning table in the background, and gets the instrument bank ready if the
     * chosen sound is an instrument; call once at startup.
     */
    @JvmStatic
    fun initialize(context: Context) {
        val app = context.applicationContext
        InstrumentPlayer.initialize(app)
        prepare(Note.getSound())
        Executors.newSingleThreadExecutor().apply {
            execute {
                try {
                    instrumentTuning = InstrumentTuning.parse(app.assets.open(TUNING_ASSET).bufferedReader().use { it.readText() })
                } catch (e: Exception) {
                    Log.w(TAG, "Couldn't read $TUNING_ASSET; instruments play uncorrected", e)
                }
            }
            shutdown()
        }
    }

    /** Maps the instrument bank and pages in [sound]'s samples off the main thread, when it's an instrument. */
    @JvmStatic
    fun prepare(sound: NoteSound) {
        if (sound.kind == NoteSound.Kind.INSTRUMENT) InstrumentPlayer.prepare(sound.program)
    }

    /** The measured instrument errors in use; none until [initialize] has read them. */
    @Volatile
    @JvmStatic
    var instrumentTuning: InstrumentTuning = InstrumentTuning.NONE

    /**
     * A note at [storedFrequency] (its A440 frequency) in [sound], tuned so A4 is [referencePitch];
     * silent until played. The pitch pipe keeps its original 8 kHz generator.
     */
    @JvmStatic
    fun create(
        sound: NoteSound,
        storedFrequency: Double,
        referencePitch: Double,
    ): SoundingNote {
        val tuned = storedFrequency * referencePitch / 440
        return when (sound.kind) {
            NoteSound.Kind.PITCH_PIPE ->
                TrackVoice(
                    PitchAudioTrackGenerator.getPitchAudioTrack(tuned, 8000, AudioFormat.CHANNEL_CONFIGURATION_MONO, 2000),
                    PitchAudioTrackGenerator::stop,
                )
            NoteSound.Kind.WAVE ->
                TrackVoice(
                    PitchAudioTrackGenerator.getWaveAudioTrack(
                        WaveSource(sound, tuned),
                        WAVE_SAMPLE_RATE,
                        AudioFormat.CHANNEL_CONFIGURATION_MONO,
                        2000,
                    ),
                    PitchAudioTrackGenerator::stopWave,
                )
            NoteSound.Kind.INSTRUMENT -> InstrumentVoice(InstrumentNote.plan(sound, storedFrequency, referencePitch, instrumentTuning))
        }
    }
}
