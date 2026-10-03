package depollsoft.pitchperfect

import depollsoft.pitchperfect.lib.Note

/**
 * Forwards to the real player and reports only real sounding-state changes.
 * Page changes stop every note on a page; silent notes must not each cost a
 * widget render.
 */
internal class WidgetAwareNotePlayer(
    private val delegate: Note.NotePlayer,
    private val onSoundingChanged: () -> Unit,
) : Note.NotePlayer {
    private val started = mutableSetOf<Note>()

    /** Whether any note this player started is still sounding, wherever it was played from. */
    val isSounding: Boolean
        get() {
            started.retainAll { it.isPlaying }
            return started.isNotEmpty()
        }

    override fun play(n: Note) {
        val wasSounding = n.isPlaying
        delegate.play(n)
        started.add(n)
        if (!wasSounding) onSoundingChanged()
    }

    override fun stop(n: Note) {
        val wasSounding = n.isPlaying
        // A toggle turning a note off clears its flag before stopping it; the set still knows.
        if (started.remove(n) || wasSounding) PitchPerfectAnalytics.soundStopped()
        delegate.stop(n)
        if (wasSounding) onSoundingChanged()
    }
}
