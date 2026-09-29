package depollsoft.pitchperfect.lib.sound

import android.media.MediaDataSource
import android.media.MediaPlayer
import android.media.audiofx.LoudnessEnhancer
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.Log
import kotlin.math.roundToInt

/**
 * An instrument note played by Android's built-in General MIDI synth: [MidiNote.file] handed to a
 * [MediaPlayer] from memory. The player is prepared off the main thread. Stopping fades the note
 * out over about 30 ms so it doesn't end in a click, but never before it has sounded for
 * [MIN_SOUNDING_MS]: a quick tap can be released before the player is even prepared, and should
 * still be heard, as the pitch pipe's track plays out its buffer. Every MediaPlayer call happens on
 * one background thread.
 */
class MidiVoice(private val plan: MidiNotePlan) : SoundingNote {
    private var player: MediaPlayer? = null
    private var enhancer: LoudnessEnhancer? = null
    private var prepared = false
    private var soundingSince = 0L

    @Volatile private var stopped = false

    @Volatile private var started = false

    override val isSounding: Boolean get() = started && !stopped

    override fun play() {
        if (started) return
        started = true
        val bytes = MidiNote.file(plan)
        handler.post { prepare(bytes) }
    }

    override fun stop() {
        if (stopped) return
        stopped = true
        handler.post { fadeWhenHeard() }
    }

    /** Fades out once the note has sounded long enough; before it is prepared, onPrepared does this. */
    private fun fadeWhenHeard() {
        if (!prepared) return
        val wait = soundingSince + MIN_SOUNDING_MS - SystemClock.uptimeMillis()
        handler.postDelayed({ fadeOut(FADE_STEPS) }, wait.coerceAtLeast(0))
    }

    private fun prepare(bytes: ByteArray) {
        val player = MediaPlayer()
        this.player = player
        try {
            player.setDataSource(MemorySource(bytes))
            player.setOnPreparedListener {
                prepared = true
                applyGain(it)
                it.start()
                soundingSince = SystemClock.uptimeMillis()
                if (stopped) fadeWhenHeard()
            }
            // The file holds the note for 30 minutes; a note still held then sounds again.
            player.setOnCompletionListener { if (!stopped) it.start() }
            player.setOnErrorListener { _, what, extra ->
                Log.w(TAG, "MIDI note $plan failed: $what/$extra")
                release()
                true
            }
            player.prepareAsync()
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't play MIDI note $plan", e)
            release()
        }
    }

    private fun applyGain(player: MediaPlayer) {
        val millibels = (plan.gainDb * 100).roundToInt()
        if (millibels <= 0) return
        try {
            enhancer = LoudnessEnhancer(player.audioSessionId).apply {
                setTargetGain(millibels)
                enabled = true
            }
        } catch (e: Exception) {
            // Some devices refuse audio effects; the note still plays, only quieter.
            Log.w(TAG, "No loudness enhancer for $plan", e)
        }
    }

    private fun fadeOut(stepsLeft: Int) {
        val player = player ?: return
        if (stepsLeft <= 0) {
            release()
            return
        }
        val volume = (stepsLeft - 1).toFloat() / FADE_STEPS
        player.setVolume(volume, volume)
        handler.postDelayed({ fadeOut(stepsLeft - 1) }, FADE_STEP_MS)
    }

    private fun release() {
        player?.let {
            try {
                if (it.isPlaying) it.stop()
            } catch (_: IllegalStateException) {
            }
            it.release()
        }
        player = null
        enhancer?.release()
        enhancer = null
    }

    private class MemorySource(private val bytes: ByteArray) : MediaDataSource() {
        override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
            if (position >= bytes.size) return -1
            val count = minOf(size.toLong(), bytes.size - position).toInt()
            System.arraycopy(bytes, position.toInt(), buffer, offset, count)
            return count
        }

        override fun getSize(): Long = bytes.size.toLong()

        override fun close() {}
    }

    companion object {
        private const val TAG = "MidiVoice"
        private const val FADE_STEPS = 6
        private const val FADE_STEP_MS = 5L
        private const val MIN_SOUNDING_MS = 250L

        private val handler: Handler by lazy {
            Handler(HandlerThread("PitchPerfectMidi").apply { start() }.looper)
        }
    }
}
