package depollsoft.pitchperfect

import android.os.Handler
import android.os.Looper
import depollsoft.pitchperfect.lib.Note

/**
 * Plays C4 for a second in the sound just chosen, at the current tuning, so Settings lets the user
 * hear a sound without leaving the screen. The note goes through [Note]'s player like any other.
 */
internal class SoundPreview(
    private val handler: Handler = Handler(Looper.getMainLooper()),
) {
    private var note: Note? = null
    private val stopLater = Runnable { stop() }

    fun play() {
        stop()
        val c4 = Note.getC4()
        // Its own note, so the preview never lights C4 on another screen or the widget.
        val preview = Note(c4.friendlyName, c4.octave, c4.accidental, c4.frequency)
        note = preview
        preview.play()
        // A sound (no review for a while after it), though not a pitch played.
        PitchPerfectAnalytics.soundPlayed()
        handler.postDelayed(stopLater, DURATION_MS)
    }

    fun stop() {
        handler.removeCallbacks(stopLater)
        note?.stop()
        note = null
    }

    companion object {
        const val DURATION_MS = 1000L
    }
}
