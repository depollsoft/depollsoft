package depollsoft.pitchperfect

import depollsoft.pitchperfect.lib.Note
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetAwareNotePlayerTest {
    private class RecordingPlayer : Note.NotePlayer {
        var plays = 0
        var stops = 0

        override fun play(n: Note) {
            plays++
        }

        override fun stop(n: Note) {
            stops++
        }
    }

    @After
    fun restorePlayer() {
        Note.setPlayer(Note.DEFAULT_PLAYER)
    }

    @Test
    fun reportsOnlyWhenSoundingStateChanges() {
        val delegate = RecordingPlayer()
        var changes = 0
        Note.setPlayer(WidgetAwareNotePlayer(delegate) { changes++ })
        val note = Note.getCommonNotes()[0]

        note.stop()
        assertEquals("stopping a silent note is not a change", 0, changes)
        assertEquals(1, delegate.stops)

        note.play()
        assertTrue(note.isPlaying)
        assertEquals(1, changes)

        note.play()
        assertEquals("replaying a sounding note is not a change", 1, changes)

        note.stop()
        assertFalse(note.isPlaying)
        assertEquals(2, changes)

        note.stop()
        assertEquals(2, changes)
    }
}
