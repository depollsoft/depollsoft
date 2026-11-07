package depollsoft.lib.audio

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import depollsoft.lib.util.Action
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
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

    /**
     * A 44.1 kHz mono track whose platform takes what it's given (Robolectric's takes nothing), at
     * most [nextWriteTakes] frames of the next write, and whose playback head is [head].
     */
    private class FakePlatformTrack(bufferSizeInBytes: Int, threshold: Int) :
        @Suppress("DEPRECATION")
        StreamingAudioTrack(
            AudioManager.STREAM_MUSIC,
            44100,
            AudioFormat.CHANNEL_CONFIGURATION_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSizeInBytes,
            AudioTrack.MODE_STREAM,
            threshold,
        ) {
        @Volatile var nextWriteTakes = Int.MAX_VALUE

        @Volatile var head = 0

        /** Whether the head moves on every read, as a playing track's does between the watcher's polls. */
        @Volatile var headMovesOnEveryRead = false

        /** The platform's buffer in frames, when it's bigger than the one asked for. */
        @Volatile var platformFrames = 0

        /** When set, a write waits for it, as one blocked in the platform does. */
        @Volatile var holdWrites: CountDownLatch? = null

        /** Counted down when a write reaches the platform. */
        val writing = CountDownLatch(1)

        /** How many times the watcher has read the head. */
        val watcherReads = AtomicInteger()

        private val reads = AtomicInteger()

        override fun writeToPlatform(
            audioData: ShortArray,
            offsetInShorts: Int,
            sizeInShorts: Int,
        ): Int {
            writing.countDown()
            holdWrites?.await(5, TimeUnit.SECONDS)
            val taken = minOf(sizeInShorts, nextWriteTakes)
            nextWriteTakes = Int.MAX_VALUE
            return taken
        }

        override fun platformBufferFrames() = platformFrames

        override fun getPlaybackHeadPosition(): Int {
            if (Thread.currentThread().name == StreamingAudioTrack.WATCHER_THREAD_NAME) watcherReads.incrementAndGet()
            return if (headMovesOnEveryRead) head + reads.incrementAndGet() else head
        }
    }

    // A wave's track: a second's buffer, kept a fifth of a second ahead.
    private val track = FakePlatformTrack(88200, 8820)
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

    /** A track like the instrument player's: 4096 frames, kept 2048 ahead. */
    private fun instrumentLikeTrack() = FakePlatformTrack(8192, 2048)

    private fun boundedFiller(target: StreamingAudioTrack) =
        Action<Int> { requested ->
            fills += Fill(requested, target.playState, Thread.currentThread())
            val frames = minOf(requested, target.capacityFrames - target.queuedFrames)
            if (frames > 0) target.write(samples, 0, frames)
        }

    private fun awaitWritten(
        target: StreamingAudioTrack,
        frames: Int,
    ): Boolean {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1)
        while (target.writtenFrames != frames && System.nanoTime() < deadline) Thread.sleep(5)
        return target.writtenFrames == frames
    }

    @Test
    fun onlyTheFramesThePlatformTakesCountAsWritten() {
        val short = instrumentLikeTrack()
        try {
            short.nextWriteTakes = 4047

            assertEquals(4047, short.write(samples, 0, 4096))

            assertEquals(4047, short.writtenFrames)
            assertEquals(4047, short.queuedFrames)
        } finally {
            short.release()
        }
    }

    @Test
    fun aTrackPrimedShortOfFullIsFilledTheRestOfTheWay() {
        // The 5.2.3 review's "the sound stops working until I restart the app": an instrument note
        // pressed while the idle instrument track was reset restarted it before the server had
        // discarded the old audio, so the prime got 4047 of its 4096 frames in. The platform holds
        // a track back until its buffer is full, and the watcher, counting 4096 and filling only to
        // the threshold, never sent the rest: the track never started, and every later instrument
        // note was silent.
        val short = instrumentLikeTrack()
        try {
            short.setBufferFiller(boundedFiller(short))
            short.setPrimesBeforePlay(true)
            short.nextWriteTakes = 4047

            short.play()

            assertTrue("the watcher tops it up to the whole buffer: $fills", awaitWritten(short, 4096))
        } finally {
            short.pause()
            short.release()
        }
    }

    @Test
    fun aPlayingTrackWhoseHeadStallsIsFilledToTheWholeBuffer() {
        // The mixer drops a track that runs dry, and holds it back again until its buffer is full.
        val short = instrumentLikeTrack()
        try {
            short.setBufferFiller(boundedFiller(short))
            short.setPrimesBeforePlay(true)
            short.play()
            assertTrue(awaitWritten(short, 4096))
            // Let the watcher see the head at 0 first, so it sees it move and takes the track as started.
            assertTrue(awaitWatcherRead(short))

            // The head moves once, then stands still with 3996 frames queued, more than the threshold.
            short.head = 100

            assertTrue("after a stall it's filled to the whole buffer", awaitWritten(short, 4196))
        } finally {
            short.pause()
            short.release()
        }
    }

    @Test
    fun aPlayingTrackIsKeptOnlyToItsThreshold() {
        // Filling the whole buffer while a track plays would double how far ahead an instrument
        // renders, and so how late a note played over others is heard.
        val short = instrumentLikeTrack()
        try {
            short.setBufferFiller(boundedFiller(short))
            short.setPrimesBeforePlay(true)
            short.play()
            assertTrue(awaitWritten(short, 4096))
            fills.clear()

            // A frame a poll: the queue stays above the 2048-frame threshold for over a second.
            short.headMovesOnEveryRead = true
            Thread.sleep(200)

            assertTrue("nothing more was asked for: $fills", fills.none { it.requested > 0 })
            assertEquals(4096, short.writtenFrames)
        } finally {
            short.pause()
            short.release()
        }
    }

    @Test
    fun aTrackWhoseBufferThePlatformGrowsIsFilledToItsNewSize() {
        // A track recreated for another output (Bluetooth headphones) gets that output's minimum
        // buffer, which can be bigger than the one asked for. The platform holds it back until the
        // bigger buffer is full, so filling to the old size left it silent.
        val grown = instrumentLikeTrack()
        try {
            grown.setBufferFiller(boundedFiller(grown))
            grown.setPrimesBeforePlay(true)
            grown.play()
            assertTrue(awaitWritten(grown, 4096))

            grown.platformFrames = 6144

            assertEquals(6144, grown.capacityFrames)
            assertTrue("the watcher fills the bigger buffer: $fills", awaitWritten(grown, 6144))
        } finally {
            grown.pause()
            grown.release()
        }
    }

    @Test
    fun aWriteInFlightAsTheTrackIsFlushedDoesNotCountAfterIt() {
        // A reset waits only so long for the watcher before it flushes. A write that outlasted the
        // wait must not count against the next note's prime, which would then come up short.
        val flushed = instrumentLikeTrack()
        try {
            val filler = Action<Int> { flushed.write(samples, 0, it) }
            flushed.setBufferFiller(filler)
            val release = CountDownLatch(1)
            flushed.holdWrites = release
            // As the watcher does, the write holds the filler's lock.
            val writer = Thread { synchronized(filler) { filler.invoke(1024) } }.apply { start() }
            assertTrue(flushed.writing.await(5, TimeUnit.SECONDS))

            val flusher = Thread { flushed.flush() }.apply { start() }
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
            while (flusher.isAlive && flusher.state != Thread.State.BLOCKED && System.nanoTime() < deadline) Thread.sleep(1)
            release.countDown()
            writer.join(5000)
            flusher.join(5000)

            assertEquals("the flush discarded the write, so its frames don't count", 0, flushed.writtenFrames)
        } finally {
            flushed.release()
        }
    }

    /** Whether the watcher reads [target]'s head within a second. */
    private fun awaitWatcherRead(target: FakePlatformTrack): Boolean {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1)
        while (target.watcherReads.get() == 0 && System.nanoTime() < deadline) Thread.sleep(1)
        return target.watcherReads.get() > 0
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
