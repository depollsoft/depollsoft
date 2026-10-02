package depollsoft.pitchperfect.lib

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A note the platform can't give an audio track stays unlit, so the next press plays it again. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NotePlaybackFailureTest {
    private var failing = true
    private var plays = 0

    private val player =
        object : Note.NotePlayer {
            override fun play(n: Note) {
                plays++
                if (failing) throw IllegalStateException("play() called on uninitialized AudioTrack.")
            }

            override fun stop(n: Note) {}
        }

    private val note = Note("A", 4, Accidental.Natural, 440.0)

    @Before
    fun usePlayer() = Note.setPlayer(player)

    @After
    fun restorePlayer() = Note.setPlayer(Note.DEFAULT_PLAYER)

    @Test
    fun aNoteThatCannotSoundIsNotMarkedPlaying() {
        note.play()
        assertFalse(note.getIsPlaying())

        failing = false
        note.play()

        assertTrue("the next press plays it", note.getIsPlaying())
        assertEquals(2, plays)
    }

    @Test
    fun turningOnANoteThatCannotSoundLeavesItOff() {
        note.setIsPlaying(true)

        assertFalse(note.getIsPlaying())
    }
}
