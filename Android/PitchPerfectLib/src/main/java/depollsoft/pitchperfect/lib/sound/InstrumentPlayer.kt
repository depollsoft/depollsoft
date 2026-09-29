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
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Plays every instrument note through one [InstrumentEngine] streamed into one small
 * [StreamingAudioTrack] (docs/pitchperfect-note-sounds.md). The track primes its buffer before it
 * starts and then writes only what the buffer has room for, like a wave's. Half a second after the
 * last release has died away it stops, and the next note starts it again.
 *
 * The SoundFont is an uncompressed asset of the app, memory-mapped so its 8.8 MB stay off the heap;
 * [prepare] maps it and pages in the chosen instrument off the main thread. Notes start on the same
 * loader thread, so a note played before the bank is ready waits for it there, never on the UI
 * thread.
 */
object InstrumentPlayer : InstrumentStarter {
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

    /** Maps the bank and starts notes, in order; tests replace it to run work inline. */
    @Volatile
    internal var loader: Executor = Executors.newSingleThreadExecutor { Thread(it, "InstrumentBank").apply { isDaemon = true } }
    private val bankLock = Any()

    // Set by initialize; tests clear it to stand for a missing bank.
    @Volatile internal var context: Context? = null

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

    /**
     * Starts the note [plan] describes on the loader thread, once the bank is mapped. [plan] runs
     * there too, so it can read the tuning table without blocking the caller.
     */
    override fun start(
        plan: () -> InstrumentNotePlan,
        onStarted: (Long) -> Unit,
        onFailed: () -> Unit,
    ) {
        loader.execute {
            val note =
                try {
                    val bank = bank()
                    if (bank == null) 0L else startNote(bank, plan())
                } catch (e: RuntimeException) {
                    Log.w(TAG, "Couldn't start an instrument note", e)
                    0L
                }
            if (note == 0L) onFailed() else onStarted(note)
        }
    }

    /** Releases [note]; the instrument's release plays out. */
    override fun stop(note: Long) {
        synchronized(filler) { engine?.stop(note) }
    }

    // On the loader thread. Returns the note's id, or 0 when there's no track to play it on.
    private fun startNote(
        bank: SoundFontBank,
        plan: InstrumentNotePlan,
    ): Long =
        synchronized(filler) {
            val engine = engine ?: InstrumentEngine(bank).also { engine = it }
            val note = engine.start(plan)
            silentFrames = 0
            val playing =
                when (state) {
                    State.IDLE -> startTrack()
                    State.STOPPING -> true.also { restartWhenStopped = true }
                    State.RUNNING -> true
                }
            if (playing) {
                note
            } else {
                engine.stop(note)
                0L
            }
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

    // Holding filler. Returns whether the track is now playing.
    private fun startTrack(): Boolean {
        val track =
            track ?: try {
                createTrack().also { track = it }
            } catch (e: RuntimeException) {
                Log.w(TAG, "Couldn't make an audio track for instruments", e)
                return false
            }
        state = State.RUNNING
        return try {
            track.play() // fills the whole buffer through filler before it starts
            true
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Couldn't start the instrument track", e)
            state = State.IDLE
            false
        }
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

/** Starts and stops instrument notes; [InstrumentPlayer] is the real one. */
internal interface InstrumentStarter {
    /**
     * Starts the note [plan] returns, off the calling thread: [onStarted] gets its id for [stop], or
     * [onFailed] runs when it can't sound (no bank, no audio track).
     */
    fun start(
        plan: () -> InstrumentNotePlan,
        onStarted: (Long) -> Unit,
        onFailed: () -> Unit,
    )

    fun stop(note: Long)
}

/**
 * An instrument note: sounding from [play] until [stop], which lets its release play. The note
 * starts asynchronously; a stop that arrives first releases it as soon as it has started, and the
 * engine's minimum length keeps a quick tap audible. If the instrument can't sound, the note plays
 * in [fallback] (the original voice) instead of staying lit and silent.
 */
internal class InstrumentVoice(
    internal val plan: () -> InstrumentNotePlan,
    private val fallback: () -> SoundingNote,
    private val starter: InstrumentStarter = InstrumentPlayer,
) : SoundingNote {
    private val lock = Any()
    private var held = false
    private var pending = false
    private var note = 0L
    private var fallbackVoice: SoundingNote? = null

    override val isSounding: Boolean get() = synchronized(lock) { held }

    override fun play() {
        synchronized(lock) {
            if (held) return
            held = true
            fallbackVoice?.let {
                it.play()
                return
            }
            if (pending || note != 0L) return
            pending = true
        }
        starter.start(plan, ::started, ::failed)
    }

    override fun stop() {
        val release: Long
        val fallbackToStop: SoundingNote?
        synchronized(lock) {
            if (!held) return
            held = false
            release = note
            note = 0L
            fallbackToStop = fallbackVoice
            fallbackVoice = null
        }
        if (release != 0L) starter.stop(release)
        fallbackToStop?.stop()
    }

    private fun started(id: Long) {
        val releaseNow =
            synchronized(lock) {
                pending = false
                if (held) {
                    note = id
                    false
                } else {
                    true
                }
            }
        if (releaseNow) starter.stop(id)
    }

    private fun failed() {
        synchronized(lock) {
            pending = false
            if (!held) return
            fallbackVoice = fallback().also { it.play() }
        }
    }
}
