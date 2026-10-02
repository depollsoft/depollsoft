package depollsoft.lib.audio

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import depollsoft.lib.util.Action
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
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

    @Test
    fun aTrackThePlatformCouldNotMakeRefusesToPlayWithoutStartingAWatcher() {
        // Samsung, Android 8: a new AudioTrack can come back uninitialized when the app holds
        // too many. play() used to start the watcher first, which then crashed the app polling
        // the track from its own thread.
        val broken =
            @Suppress("DEPRECATION")
            object : StreamingAudioTrack(
                AudioManager.STREAM_MUSIC,
                8000,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                16000,
                AudioTrack.MODE_STREAM,
            ) {
                override fun getState() = AudioTrack.STATE_UNINITIALIZED
            }
        broken.setBufferFiller(Action { fills += Fill(it, broken.playState, Thread.currentThread()) })

        try {
            assertThrows(IllegalStateException::class.java) { broken.play() }
            Thread.sleep(50)
            assertTrue("nothing fills a track that can't play", fills.isEmpty())
            assertTrue("no watcher is left polling it", awaitNoWatchers())
        } finally {
            broken.release()
        }
    }

    @Test
    fun aWatcherWhoseTrackLosesItsNativeTrackStopsQuietly() {
        val lost = AtomicBoolean(false)
        val failing =
            @Suppress("DEPRECATION")
            object : StreamingAudioTrack(
                AudioManager.STREAM_MUSIC,
                8000,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                16000,
                AudioTrack.MODE_STREAM,
            ) {
                override fun getPlaybackHeadPosition(): Int {
                    // What AudioTrack throws once its native track is gone.
                    if (lost.get()) throw IllegalStateException("Unable to retrieve AudioTrack pointer for getPosition()")
                    return super.getPlaybackHeadPosition()
                }
            }
        failing.setBufferFiller(Action { requested ->
            val frames = minOf(requested, failing.capacityFrames - failing.queuedFrames)
            if (frames > 0) failing.write(samples, 0, frames)
        })
        val uncaught = CopyOnWriteArrayList<Throwable>()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { _, e -> uncaught += e }
        try {
            failing.play()
            lost.set(true)

            assertTrue("the watcher ends", awaitNoWatchers())
            assertTrue("and nothing escapes it: $uncaught", uncaught.isEmpty())
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(previous)
            lost.set(false)
            failing.pause()
            failing.release()
        }
    }

    @Test
    fun stoppingEndsAWatcherWhoseFillerWritesNothing() {
        // A wave's filler swallows the exception from a track that's gone and writes nothing, so
        // the buffer never fills; stopping must still end the watcher rather than leave it spinning.
        track.setBufferFiller(Action { fills += Fill(it, track.playState, Thread.currentThread()) })
        track.play()
        assertTrue("the watcher asked for audio", awaitFill())

        track.pause()

        assertTrue("the watcher ends", awaitNoWatchers())
    }

    private fun awaitFill(): Boolean {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1)
        while (fills.isEmpty() && System.nanoTime() < deadline) Thread.sleep(5)
        return fills.isNotEmpty()
    }

    /** Whether every buffer-filling thread has ended within a second. */
    private fun awaitNoWatchers(): Boolean {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1)
        while (System.nanoTime() < deadline) {
            if (Thread.getAllStackTraces().keys.none { it.name == StreamingAudioTrack.WATCHER_THREAD_NAME && it.isAlive }) return true
            Thread.sleep(10)
        }
        return false
    }
}
