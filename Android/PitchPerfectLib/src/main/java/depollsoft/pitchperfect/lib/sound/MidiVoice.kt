package depollsoft.pitchperfect.lib.sound

import android.media.MediaDataSource
import android.media.MediaPlayer
import android.media.audiofx.LoudnessEnhancer
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import kotlin.math.roundToInt

/**
 * An instrument note played by Android's built-in General MIDI synth: [MidiNote.file] handed to a
 * [MediaPlayer] from memory. The player is prepared off the main thread and starts unless the note
 * was stopped first; stopping fades it out over about 30 ms so a held note doesn't end in a click.
 * Every MediaPlayer call happens on one background thread.
 */
class MidiVoice(private val plan: MidiNotePlan) : SoundingNote {
    private var player: MediaPlayer? = null
    private var enhancer: LoudnessEnhancer? = null
    private var prepared = false

    @Volatile private var stopped = false

    @Volatile private var started = false

    override val isSounding: Boolean get() = started && !stopped

    override fun play() {
        if (started) return
        started = true
        val bytes = MidiNote.file(plan)
        handler.post { if (!stopped) prepare(bytes) }
    }

    override fun stop() {
        if (stopped) return
        stopped = true
        handler.post { fadeOut(FADE_STEPS) }
    }

    private fun prepare(bytes: ByteArray) {
        val player = MediaPlayer()
        this.player = player
        try {
            player.setDataSource(MemorySource(bytes))
            player.setOnPreparedListener {
                prepared = true
                if (stopped) {
                    release()
                    return@setOnPreparedListener
                }
                applyGain(it)
                it.start()
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
        // Not prepared yet: onPrepared sees the note has stopped and releases it.
        val player = player?.takeIf { prepared } ?: return
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

        private val handler: Handler by lazy {
            Handler(HandlerThread("PitchPerfectMidi").apply { start() }.looper)
        }
    }
}
