package depollsoft.pitchperfect

import android.media.AudioTrack
import androidx.test.ext.junit.runners.AndroidJUnit4
import depollsoft.pitchperfect.lib.sound.InstrumentPlayer
import depollsoft.pitchperfect.lib.sound.NoteSound
import depollsoft.pitchperfect.lib.sound.NoteVoices
import depollsoft.pitchperfect.lib.sound.SoundingNote
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Notes keep sounding through many presses on a real audio stack (device-only: Robolectric's
 * AudioTrack has no mixer). A review of 5.2.3 said the sound stopped after a while until the app
 * restarted: an instrument note pressed while the idle instrument track was being reset restarted
 * it a few frames short of full, the platform never started it, and every instrument note after
 * that was silent.
 */
@RunWith(AndroidJUnit4::class)
class SoundKeepsPlayingTest {
    private fun field(owner: Class<*>, name: String, of: Any?): Any? =
        owner.getDeclaredField(name).apply { isAccessible = true }.get(of)

    private fun instrumentTrack(): AudioTrack? = field(InstrumentPlayer::class.java, "track", null) as AudioTrack?

    private fun instrumentState(): String = field(InstrumentPlayer::class.java, "state", null).toString()

    private fun trackOf(voice: SoundingNote): AudioTrack = field(voice.javaClass, "track", voice) as AudioTrack

    /** Whether [track]'s playback head moves over the next 150 ms. */
    private fun isPlaying(track: AudioTrack?): Boolean {
        val before = track?.playbackHeadPosition ?: return false
        Thread.sleep(150)
        return track.playbackHeadPosition > before
    }

    @Test fun instrumentNotesPressedAsTheIdleTrackResetsStillSound() {
        // Warm the bank, so the first note isn't still loading when it's checked.
        NoteVoices.create(NoteSound.ORGAN, 261.63, 440.0).apply { play(); Thread.sleep(1000); stop() }
        // Offsets into the reset's fade, which lasts about 40 ms, and a little past it.
        val offsets = longArrayOf(0, 1, 3, 5, 10, 20, 30, 38, 42, 45, 50, 60, 80)
        val silent = mutableListOf<Int>()
        for (round in 0 until offsets.size * 2) {
            // Wait for the track to go idle and start resetting, then press during the reset.
            val deadline = System.currentTimeMillis() + 10_000
            while (instrumentState() == "RUNNING" && System.currentTimeMillis() < deadline) Thread.sleep(1)
            Thread.sleep(offsets[round % offsets.size])
            val voice = NoteVoices.create(NoteSound.ORGAN, 261.63 * (1 + round % 3), 440.0)
            voice.play()
            Thread.sleep(250)
            if (!isPlaying(instrumentTrack())) silent += round
            voice.stop()
        }
        assertEquals("rounds whose note was silent", emptyList<Int>(), silent)
    }

    private fun pressRepeatedly(sound: NoteSound) {
        // Gaps around the moment a stopped track goes back to the pool (a wave's fade is 60 ms).
        val gaps = longArrayOf(0, 10, 30, 55, 60, 62, 65, 70, 80, 100, 300, 600)
        val silent = mutableListOf<Int>()
        for (round in 0 until gaps.size * 2) {
            val voice = NoteVoices.create(sound, 261.63 * (1 + round % 3), 440.0)
            voice.play()
            Thread.sleep(250)
            if (!isPlaying(trackOf(voice))) silent += round
            voice.stop()
            Thread.sleep(gaps[round % gaps.size])
        }
        assertEquals("rounds whose note was silent", emptyList<Int>(), silent)
    }

    @Test fun pitchPipeNotesKeepSounding() = pressRepeatedly(NoteSound.PITCH_PIPE)

    @Test fun waveNotesKeepSounding() = pressRepeatedly(NoteSound.SINE)
}
