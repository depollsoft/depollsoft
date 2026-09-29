package depollsoft.lib.audio

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import depollsoft.lib.util.Action
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** How a streaming track starts and resets: the fixes for waves that started twice ("ba-aaaa"). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class StreamingAudioTrackTest {
    private data class Fill(val requested: Int, val playState: Int, val thread: Thread)

    private val track =
        @Suppress("DEPRECATION")
        StreamingAudioTrack(
            AudioManager.STREAM_MUSIC,
            44100,
            AudioFormat.CHANNEL_CONFIGURATION_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            88200,
            AudioTrack.MODE_STREAM,
        )
    private val fills = CopyOnWriteArrayList<Fill>()
    private val samples = ShortArray(44100)

    /** A filler that, like a wave's, writes only what the buffer has room for. */
    private fun useBoundedFiller() {
        track.setBufferFiller(
            Action { requested ->
                fills += Fill(requested, track.playState, Thread.currentThread())
                val frames = minOf(requested, track.capacityFrames - track.queuedFrames)
                if (frames > 0) track.write(samples, 0, frames)
            },
        )
    }

    @After
    fun release() {
        track.pause()
        track.release()
    }

    @Test
    fun aPrimingTrackFillsItsWholeBufferBeforeItStarts() {
        useBoundedFiller()
        track.setPrimesBeforePlay(true)

        track.play()

        val first = fills.first()
        assertEquals("the start threshold is the whole buffer", 44100, first.requested)
        assertNotEquals(AudioTrack.PLAYSTATE_PLAYING, first.playState)
        assertEquals(Thread.currentThread(), first.thread)
        assertTrue(track.writtenFrames >= 44100)
    }

    @Test
    fun aTrackThatDoesNotPrimeStartsAsItAlwaysHas() {
        // The original pitch pipe's tracks: filled only from the watcher thread.
        useBoundedFiller()

        track.play()

        assertTrue(fills.none { it.thread == Thread.currentThread() })
    }

    @Test
    fun fadeOutAndResetLeavesNothingQueuedAndStopsTheFiller() {
        useBoundedFiller()
        track.setPrimesBeforePlay(true)
        track.play()

        val done = CountDownLatch(1)
        track.fadeOutAndReset(20, Action { done.countDown() })

        assertTrue(done.await(5, TimeUnit.SECONDS))
        assertEquals(AudioTrack.PLAYSTATE_PAUSED, track.playState)
        assertEquals("flushed", 0, track.writtenFrames)
        val fillsAfterReset = fills.size
        Thread.sleep(50)
        assertEquals("the filler can't write after the flush", fillsAfterReset, fills.size)
    }

    @Test
    fun aResetTrackPrimesAFreshBufferWhenPlayedAgain() {
        useBoundedFiller()
        track.setPrimesBeforePlay(true)
        track.play()
        val done = CountDownLatch(1)
        track.fadeOutAndReset(20, Action { done.countDown() })
        assertTrue(done.await(5, TimeUnit.SECONDS))
        fills.clear()

        track.play()

        assertEquals(44100, fills.first().requested)
    }
}
