package depollsoft.tagmaster

import android.app.Activity
import android.app.Application
import android.media.MediaPlayer
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import bolts.TaskCompletionSource
import depollsoft.lib.util.ContentCache
import depollsoft.tagmaster.barbershop.RemoteLocation
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.*
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class MediaPlayerLifecycleTest {
    private fun withPlayer(check: (MediaPlayerView, MediaPlayer, TaskCompletionSource<File>) -> Unit) {
        mockConstruction(MediaPlayer::class.java).use { players ->
            val controller = Robolectric.buildActivity(Activity::class.java)
            controller.get().setTheme(R.style.AppTheme)
            controller.setup()
            val activity = controller.get()
            val view = MediaPlayerView(activity)
            activity.setContentView(view)
            val player = players.constructed().single()
            `when`(player.duration).thenReturn(10000)
            val pending = TaskCompletionSource<File>()
            val cache = mock(ContentCache::class.java)
            `when`(cache.loadContentPublic(anyString(), anyString(), anyBoolean())).thenReturn(pending.task)
            MediaPlayerView::class.java
                .getDeclaredField("cache")
                .apply { isAccessible = true }
                .set(view, cache)
            view.remoteLocation =
                RemoteLocation().apply {
                    uri = "https://example.com/lead.mp3"
                    type = "mp3"
                }
            val prepared = ArgumentCaptor.forClass(MediaPlayer.OnPreparedListener::class.java)
            verify(player).setOnPreparedListener(prepared.capture())
            listeners[player] = prepared.value
            clearInvocations(player)
            try {
                check(view, player, pending)
            } finally {
                controller.pause().stop().destroy()
            }
        }
    }

    private fun start(view: MediaPlayerView) = view.findViewById<View>(R.id.playPauseButton).performClick()

    private fun complete(pending: TaskCompletionSource<File>) {
        val file = File.createTempFile("track", ".mp3")
        try {
            pending.setResult(file)
            shadowOf(Looper.getMainLooper()).idle()
        } finally {
            file.delete()
        }
    }

    private fun preparedListener(player: MediaPlayer): MediaPlayer.OnPreparedListener {
        return listeners.getValue(player)
    }

    private val listeners = mutableMapOf<MediaPlayer, MediaPlayer.OnPreparedListener>()

    @Test fun prepared_track_can_pause_resume_and_stop_from_the_controls() = withPlayer { view, player, pending ->
        start(view)
        complete(pending)
        preparedListener(player).onPrepared(player)
        assertTrue(view.isPlaying)
        verify(player).start()
        start(view)
        verify(player).pause()
        assertFalse(view.isPlaying)
        assertTrue(view.findViewById<View>(R.id.stopButton).isEnabled)
        start(view)
        verify(player, times(2)).start()
        view.findViewById<View>(R.id.stopButton).performClick()
        assertFalse(view.isPlaying)
        assertEquals(0, view.audioPosition)
    }

    @Test fun stopped_player_ignores_late_prepared_callback() = withPlayer { view, player, pending ->
        start(view)
        complete(pending)
        view.stop()
        preparedListener(player).onPrepared(player)
        verify(player, never()).start()
        assertFalse(view.isPlaying)
    }

    @Test fun stop_during_download_prevents_late_prepare() =
        withPlayer { view, player, pending ->
            start(view)
            assertTrue(view.isLoading)
            assertTrue(view.findViewById<View>(R.id.stopButton).isEnabled)
            view.findViewById<View>(R.id.stopButton).performClick()
            complete(pending)
            assertFalse(view.isLoading)
            assertFalse(view.isPlaying)
            verify(player, never()).prepareAsync()
        }

    @Test fun replacing_part_invalidates_old_download() =
        withPlayer { view, player, pending ->
            start(view)
            view.remoteLocation =
                RemoteLocation().apply {
                    uri = "https://example.com/bass.mp3"
                    type = "mp3"
                }
            complete(pending)
            verify(player, never()).prepareAsync()
            assertEquals(0, view.audioPosition)
        }

    @Test fun detach_releases_player_and_ignores_late_download() =
        withPlayer { view, player, pending ->
            start(view)
            (view.parent as ViewGroup).removeView(view)
            complete(pending)
            verify(player).release()
            verify(player, never()).prepareAsync()
            assertFalse(view.isPlaying)
        }

    @Test fun downloaded_track_prepares_asynchronously() =
        withPlayer { view, player, pending ->
            start(view)
            complete(pending)
            verify(player).prepareAsync()
            verify(player, never()).prepare()
            verify(player, never()).start()
            assertTrue(view.isLoading)
        }
}
