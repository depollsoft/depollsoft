package depollsoft.pitchperfect.lib

import android.media.AudioFormat
import android.media.AudioTrack
import depollsoft.lib.audio.StreamingAudioTrack
import depollsoft.pitchperfect.lib.sound.NoteSound
import depollsoft.pitchperfect.lib.sound.WAVE_SAMPLE_RATE
import depollsoft.pitchperfect.lib.sound.WaveSource
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Wave tracks start primed and are reset before reuse, so a note never starts twice. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WaveTrackTest {
    @Suppress("DEPRECATION")
    private val mono = AudioFormat.CHANNEL_CONFIGURATION_MONO

    private fun waveTrack(): StreamingAudioTrack =
        PitchAudioTrackGenerator.getWaveAudioTrack(WaveSource(NoteSound.SINE, 440.0), WAVE_SAMPLE_RATE, mono, 2000)
            as StreamingAudioTrack

    @Test
    fun chunksOfAWaveJoinWithoutASeam() {
        val whole = ShortArray(3000)
        PitchAudioTrackGenerator.fillWave(WaveSource(NoteSound.SAWTOOTH, 440.0), whole, 3000, 1)

        val source = WaveSource(NoteSound.SAWTOOTH, 440.0)
        val parts = ShortArray(3000)
        val chunk = ShortArray(1024)
        var at = 0
        for (size in listOf(1024, 7, 1024, 945)) {
            PitchAudioTrackGenerator.fillWave(source, chunk, size, 1)
            chunk.copyInto(parts, at, 0, size)
            at += size
        }
        assertArrayEquals(whole, parts)
    }

    @Test
    fun stereoGetsTheSameSampleOnBothChannels() {
        val stereo = ShortArray(200)
        PitchAudioTrackGenerator.fillWave(WaveSource(NoteSound.SINE, 440.0), stereo, 100, 2)
        for (frame in 0 until 100) assertEquals(stereo[frame * 2], stereo[frame * 2 + 1])
    }

    @Test
    fun aWaveTrackIsFullBeforeItStarts() {
        val track = waveTrack()
        track.play()
        // Robolectric's track doesn't advance, so everything primed is still queued.
        assertEquals(track.capacityFrames, track.queuedFrames)
        track.pause()
        track.flush()
    }

    @Test
    fun aStoppedWaveTrackIsReusedWithNothingLeftFromItsLastNote() {
        val first = waveTrack()
        first.play()
        PitchAudioTrackGenerator.stopWave(first)
        val deadline = System.currentTimeMillis() + 5000
        while (!(first.playState == AudioTrack.PLAYSTATE_PAUSED && first.queuedFrames == 0) &&
            System.currentTimeMillis() < deadline
        ) {
            Thread.sleep(10)
        }
        Thread.sleep(50) // the pool takes it back just after the reset

        val second = waveTrack()

        assertSame("the pool hands the track back", first, second)
        assertEquals("the last note's audio was discarded", 0, second.queuedFrames)
        second.play()
        assertTrue(second.queuedFrames == second.capacityFrames)
        second.pause()
        second.flush()
    }
}
