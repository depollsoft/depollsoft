package depollsoft.tagmaster

import android.app.Application
import android.media.MediaPlayer
import android.os.Looper
import bolts.TaskCompletionSource
import depollsoft.lib.util.ContentCache
import depollsoft.tagmaster.barbershop.RemoteLocation
import depollsoft.tagmaster.ui.detail.TrackPlayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.anyBoolean
import org.mockito.Mockito.anyString
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockConstruction
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

/**
 * The learning-track player: downloads before it prepares, prepares off the UI thread, and never
 * lets a download or a prepare that belongs to a stopped, replaced or released track start playing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class MediaPlayerLifecycleTest {
    private var failures = 0

    private fun withPlayer(check: (TrackPlayer, () -> MediaPlayer, TaskCompletionSource<File>) -> Unit) {
        mockConstruction(MediaPlayer::class.java).use { players ->
            val player = TrackPlayer(RuntimeEnvironment.getApplication()) { failures++ }
            val pending = TaskCompletionSource<File>()
            val cache = mock(ContentCache::class.java)
            `when`(cache.loadContentPublic(anyString(), anyString(), anyBoolean())).thenReturn(pending.task)
            TrackPlayer::class.java
                .getDeclaredField("cache")
                .apply { isAccessible = true }
                .set(player, cache)
            player.select(location("lead"))
            try {
                check(player, {
                    players.constructed().single().also {
                        `when`(it.duration).thenReturn(10000)
                    }
                }, pending)
            } finally {
                player.release()
            }
        }
    }

    private fun location(part: String) =
        RemoteLocation().apply {
            uri = "https://example.com/$part.mp3"
            type = "mp3"
        }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun complete(pending: TaskCompletionSource<File>) {
        val file = File.createTempFile("track", ".mp3")
        try {
            pending.setResult(file)
            idle()
        } finally {
            file.delete()
        }
    }

    private fun prepared(player: MediaPlayer): MediaPlayer.OnPreparedListener {
        val captor = ArgumentCaptor.forClass(MediaPlayer.OnPreparedListener::class.java)
        verify(player).setOnPreparedListener(captor.capture())
        return captor.value
    }

    @Test
    fun aPreparedTrackPausesResumesAndStops() =
        withPlayer { track, media, pending ->
            track.togglePlay()
            complete(pending)
            val player = media()
            prepared(player).onPrepared(player)
            assertTrue(track.isPlaying)
            verify(player).start()
            track.togglePlay()
            verify(player).pause()
            assertFalse(track.isPlaying)
            assertTrue("Stop stays available while paused", track.isPrepared)
            track.togglePlay()
            verify(player, times(2)).start()
            track.stop()
            assertFalse(track.isPlaying)
            assertEquals(0, track.position)
        }

    @Test
    fun aStoppedPlayerIgnoresALatePreparedCallback() =
        withPlayer { track, media, pending ->
            track.play()
            complete(pending)
            val player = media()
            val listener = prepared(player)
            track.stop()
            listener.onPrepared(player)
            verify(player, never()).start()
            assertFalse(track.isPlaying)
        }

    @Test
    fun stoppingDuringTheDownloadPreventsALatePrepare() =
        withPlayer { track, media, pending ->
            track.play()
            assertTrue(track.isLoading)
            track.stop()
            complete(pending)
            assertFalse(track.isLoading)
            assertFalse(track.isPlaying)
            verify(media(), never()).prepareAsync()
        }

    @Test
    fun choosingAnotherPartInvalidatesTheOldDownload() =
        withPlayer { track, media, pending ->
            track.play()
            track.select(location("bass"))
            complete(pending)
            verify(media(), never()).prepareAsync()
            assertEquals(0, track.position)
        }

    @Test
    fun releasingThePlayerIgnoresALateDownload() =
        withPlayer { track, media, pending ->
            track.play()
            val player = media()
            clearInvocations(player)
            track.release()
            complete(pending)
            verify(player).release()
            verify(player, never()).prepareAsync()
            assertFalse(track.isPlaying)
        }

    @Test
    fun aDownloadedTrackPreparesAsynchronously() =
        withPlayer { track, media, pending ->
            track.play()
            complete(pending)
            val player = media()
            verify(player).prepareAsync()
            verify(player, never()).prepare()
            verify(player, never()).start()
            assertTrue(track.isLoading)
        }

    @Test
    fun aFailedDownloadReportsOnceAndStops() =
        withPlayer { track, _, pending ->
            track.play()
            pending.setError(java.io.IOException("offline"))
            idle()
            assertEquals(1, failures)
            assertFalse(track.isLoading)
            assertFalse(track.isPlaying)
        }
}
