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
    override fun play(n: Note) {
        val wasSounding = n.isPlaying
        delegate.play(n)
        if (!wasSounding) onSoundingChanged()
    }

    override fun stop(n: Note) {
        val wasSounding = n.isPlaying
        delegate.stop(n)
        if (wasSounding) onSoundingChanged()
    }
}
