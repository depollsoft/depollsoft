package depollsoft.pitchperfect.lib.sound

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import depollsoft.lib.audio.StreamingAudioTrack
import depollsoft.lib.util.Action
import depollsoft.pitchperfect.lib.sound.soundfont.SoundFontBank
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.channels.FileChannel
import java.util.concurrent.Executors

/**
 * Plays every instrument note through one [InstrumentEngine] streamed into one small
 * [StreamingAudioTrack] (docs/pitchperfect-note-sounds.md). The track primes its buffer before it
 * starts and then writes only what the buffer has room for, like a wave's. Half a second after the
 * last release has died away it stops, and the next note starts it again.
 *
 * The SoundFont is an uncompressed asset of the app, memory-mapped so its 8.8 MB stay off the heap;
 * [prepare] maps it and pages in the chosen instrument off the main thread.
 */
object InstrumentPlayer {
    private const val TAG = "InstrumentPlayer"

    /** The phone app's asset; Wear doesn't ship it and never plays instruments. */
    const val BANK_ASSET = "PitchPerfectInstruments.sf2"

    /**
     * How far ahead of playback the synth renders, about 46 ms (two of a typical mixer's periods):
     * a note played while others sound is heard after at most this much more.
     */
    private const val QUEUE_FRAMES = 2048

    /** The fewest frames the filler renders at once. */
    private const val MIN_WRITE_FRAMES = 256

    /** How long the track runs on in silence before it stops. */
    private const val IDLE_STOP_FRAMES = InstrumentEngine.SAMPLE_RATE / 2

    private enum class State { IDLE, RUNNING, STOPPING }

    private val loader = Executors.newSingleThreadExecutor { Thread(it, "InstrumentBank").apply { isDaemon = true } }
    private val bankLock = Any()

    @Volatile private var context: Context? = null

    @Volatile private var bank: SoundFontBank? = null

    // Guarded by filler, which is also the lock StreamingAudioTrack holds while it fills.
    private val filler = Filler()
    private var engine: InstrumentEngine? = null
    private var track: StreamingAudioTrack? = null
    private var buffer = ShortArray(0)
    private var state = State.IDLE
    private var restartWhenStopped = false
    private var silentFrames = 0

    /** Remembers the app context the bank is read from; call once at startup. */
    @JvmStatic
    fun initialize(context: Context) {
        this.context = context.applicationContext
    }

    /** Maps the bank if it isn't yet and pages in [program]'s samples, off the main thread. */
    @JvmStatic
    fun prepare(program: Int) {
        loader.execute {
            val bank = bank() ?: return@execute
            bank.warm(bank.presetIndex(0, program))
        }
    }

    /** Starts [plan]'s note; returns its id for [stop], or 0 when the bank can't be read. */
    fun start(plan: InstrumentNotePlan): Long {
        val bank = bank() ?: return 0L
        synchronized(filler) {
            val engine = engine ?: InstrumentEngine(bank).also { engine = it }
            val note = engine.start(plan)
            silentFrames = 0
            when (state) {
                State.IDLE -> startTrack()
                State.STOPPING -> restartWhenStopped = true
                State.RUNNING -> {}
            }
            return note
        }
    }

    /** Releases [note]; the instrument's release plays out. */
    fun stop(note: Long) {
        synchronized(filler) { engine?.stop(note) }
    }

    private fun bank(): SoundFontBank? {
        bank?.let { return it }
        synchronized(bankLock) {
            bank?.let { return it }
            val context = context ?: return null
            return try {
                open(context).also { bank = it }
            } catch (e: IOException) {
                Log.w(TAG, "Couldn't read $BANK_ASSET; instruments are silent", e)
                null
            }
        }
    }

    private fun open(context: Context): SoundFontBank =
        try {
            context.assets.openFd(BANK_ASSET).use { fd ->
                FileInputStream(fd.fileDescriptor).channel.use { channel ->
                    SoundFontBank.load(channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength))
                }
            }
        } catch (e: FileNotFoundException) {
            // openFd fails for a compressed asset, which can't be mapped: read it onto the heap.
            context.assets.open(BANK_ASSET).use { stream ->
                Log.w(TAG, "$BANK_ASSET is compressed in the APK; reading it into memory")
                SoundFontBank.load(stream)
            }
        }

    // Holding filler.
    private fun startTrack() {
        val track =
            track ?: try {
                createTrack().also { track = it }
            } catch (e: RuntimeException) {
                Log.w(TAG, "Couldn't make an audio track for instruments", e)
                return
            }
        state = State.RUNNING
        track.play() // fills the whole buffer through filler before it starts
    }

    @Suppress("DEPRECATION")
    private fun createTrack(): StreamingAudioTrack {
        val rate = InstrumentEngine.SAMPLE_RATE
        val minBytes = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val bytes = maxOf(minBytes, QUEUE_FRAMES * 2 * 2)
        val capacity = bytes / 2
        buffer = ShortArray(capacity)
        return StreamingAudioTrack(
            AudioManager.STREAM_MUSIC,
            rate,
            AudioFormat.CHANNEL_CONFIGURATION_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bytes,
            AudioTrack.MODE_STREAM,
            // The watcher keeps only this much queued, however big the buffer the platform asks for.
            QUEUE_FRAMES,
        ).apply {
            setBufferFiller(filler)
            setPrimesBeforePlay(true)
        }
    }

    // Holding filler, on the track's watcher thread.
    private fun stopTrack(track: StreamingAudioTrack) {
        state = State.STOPPING
        track.fadeOutAndReset(0) {
            synchronized(filler) {
                state = State.IDLE
                silentFrames = 0
                val engine = engine
                if (restartWhenStopped || (engine != null && !engine.isIdle)) {
                    restartWhenStopped = false
                    startTrack()
                }
            }
        }
    }

    /** Renders the synth into the track, a chunk at a time, and stops the track once it's been silent a while. */
    private class Filler : Action<Int> {
        override fun invoke(requested: Int?) {
            val track = track ?: return
            val engine = engine ?: return
            val wanted = requested ?: 0
            val room = track.capacityFrames - track.queuedFrames
            val frames = minOf(room, maxOf(wanted, MIN_WRITE_FRAMES), buffer.size)
            if (wanted <= 0 || frames <= 0) return
            if (state == State.STOPPING) {
                // This audio is about to be flushed: keep a note started meanwhile for the restart.
                buffer.fill(0, 0, frames)
            } else {
                engine.render(buffer, frames)
            }
            track.write(buffer, 0, frames)
            if (state != State.RUNNING) return
            if (engine.isIdle) {
                silentFrames += frames
                if (silentFrames >= IDLE_STOP_FRAMES) stopTrack(track)
            } else {
                silentFrames = 0
            }
        }
    }
}

/** An instrument note: sounding from [play] until [stop], which lets its release play. */
internal class InstrumentVoice(private val plan: InstrumentNotePlan) : SoundingNote {
    @Volatile private var note = 0L

    override val isSounding: Boolean get() = note != 0L

    override fun play() {
        if (note == 0L) note = InstrumentPlayer.start(plan)
    }

    override fun stop() {
        val playing = note
        note = 0L
        if (playing != 0L) InstrumentPlayer.stop(playing)
    }
}
