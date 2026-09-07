package depollsoft.tagmaster

import android.app.Activity
import android.media.MediaPlayer
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import bolts.TaskCompletionSource
import com.google.android.material.button.MaterialButton
import depollsoft.lib.util.ContentCache
import depollsoft.tagmaster.barbershop.RemoteLocation
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.MockedConstruction
import org.mockito.Mockito.*
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.util.ReflectionHelpers
import java.io.File
import java.io.FileDescriptor
import java.io.IOException
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(application = TestDeskApplication::class, sdk = [35])
@LooperMode(LooperMode.Mode.PAUSED)
class MediaPlayerViewTest {
    private lateinit var controller: ActivityController<Activity>
    private lateinit var container: FrameLayout
    private lateinit var view: MediaPlayerView
    private lateinit var cache: ContentCache
    private lateinit var players: MockedConstruction<MediaPlayer>
    private lateinit var audio: File
    private var failPrepare = false
    private val requests = mutableListOf<TaskCompletionSource<File>>()

    @Before
    fun setUp() {
        players =
            mockConstruction(MediaPlayer::class.java) { player, _ ->
                `when`(player.duration).thenReturn(12000)
                `when`(player.currentPosition).thenReturn(250)
                if (failPrepare) doThrow(IOException("bad audio")).`when`(player).prepare()
            }
        controller = Robolectric.buildActivity(Activity::class.java).setup()
        val activity = controller.get()
        activity.setTheme(R.style.AppTheme)
        container = FrameLayout(activity)
        activity.setContentView(container)
        view = MediaPlayerView(activity)
        container.addView(view)
        cache = mock(ContentCache::class.java)
        `when`(cache.loadContentPublic(anyString(), anyString(), eq(false))).thenAnswer {
            TaskCompletionSource<File>().also { requests.add(it) }.task
        }
        ReflectionHelpers.setField(view, "cache", cache)
        view.remoteLocation = location("lead")
        audio = File.createTempFile("track", ".mp3", activity.cacheDir)
    }

    @After
    fun tearDown() {
        view.stop()
        controller.pause().stop().destroy()
        players.close()
        assertTrue(audio.delete())
    }

    private fun location(part: String) =
        RemoteLocation().apply {
            uri = "https://example.com/$part.mp3"
            type = "mp3"
        }

    private fun clickPlay() {
        assertTrue(view.findViewById<MaterialButton>(R.id.playPauseButton).performClick())
    }

    private fun drain() = shadowOf(Looper.getMainLooper()).idle()

    private fun assertLoading(loading: Boolean) {
        assertEquals(if (loading) View.VISIBLE else View.GONE, view.findViewById<View>(R.id.trackLoading).visibility)
        assertEquals(if (loading) View.GONE else View.VISIBLE, view.findViewById<View>(R.id.playPauseButton).visibility)
    }

    private fun startPlayback(): MediaPlayer {
        clickPlay()
        requests.last().setResult(audio)
        drain()
        assertTrue("the fixture must actually start playback", view.isPlaying)
        return players.constructed().last().also { verify(it).start() }
    }

    private fun assertStopped() {
        assertFalse(view.isPlaying)
        assertEquals(0, view.audioPosition)
        assertEquals(0, view.audioLength)
        assertLoading(false)
    }

    @Test
    fun success_prepares_before_start_and_closes_the_file_descriptor() {
        view.balance = 250
        val player = startPlayback()
        val descriptor = ArgumentCaptor.forClass(FileDescriptor::class.java)
        val order = inOrder(player)
        order.verify(player).setDataSource(descriptor.capture())
        order.verify(player).prepare()
        order.verify(player).start()
        assertFalse("the input stream must be closed", descriptor.value.valid())
        verify(player).setVolume(1f, 1f / 3f)
        verify(cache).loadContentPublic("https://example.com/lead.mp3", "mp3", false)
        assertEquals(12000, view.audioLength)
        assertEquals(250, view.audioPosition)
        assertLoading(false)
    }

    @Test
    fun stop_invalidates_an_unfinished_fetch() {
        clickPlay()
        assertLoading(true)
        view.stop()
        requests.single().setResult(audio)
        drain()
        assertStopped()
        assertTrue("stale results must not create a player", players.constructed().isEmpty())
    }

    @Test
    fun background_completion_after_stop_cannot_restart_playback() {
        clickPlay()
        view.stop()
        CompletableFuture.runAsync { requests.single().setResult(audio) }.get(5, TimeUnit.SECONDS)
        drain()
        assertStopped()
        assertTrue(players.constructed().isEmpty())
    }

    @Test
    fun stop_invalidates_a_completion_already_queued_on_the_main_thread() {
        clickPlay()
        requests.single().setResult(audio)
        view.stop()
        drain()
        assertStopped()
        assertTrue(players.constructed().isEmpty())
    }

    @Test
    fun detach_invalidates_fetch_even_after_reattachment() {
        clickPlay()
        container.removeView(view)
        requests.single().setResult(audio)
        container.addView(view)
        drain()
        assertStopped()
        assertTrue(players.constructed().isEmpty())
        startPlayback()
        assertEquals(2, requests.size)
    }

    @Test
    fun an_old_part_cannot_start_or_hide_the_new_parts_loading_control() {
        clickPlay()
        view.remoteLocation = location("bass")
        clickPlay()
        requests[0].setResult(audio)
        drain()
        assertLoading(true)
        assertFalse(view.isPlaying)
        assertTrue(players.constructed().isEmpty())
        requests[1].setResult(audio)
        drain()
        assertTrue(view.isPlaying)
        assertEquals(1, players.constructed().size)
        verify(cache).loadContentPublic("https://example.com/bass.mp3", "mp3", false)
    }

    @Test
    fun an_old_failure_cannot_stop_new_playback() {
        clickPlay()
        view.remoteLocation = location("bass")
        val current = startPlayback()
        requests[0].setError(IOException("old request"))
        drain()
        assertTrue(view.isPlaying)
        verify(current, never()).release()
        assertLoading(false)
    }

    @Test
    fun fetch_failure_retries_the_cache_instead_of_starting_an_idle_player() {
        clickPlay()
        requests.single().setError(IOException("offline"))
        drain()
        assertStopped()
        assertTrue(players.constructed().isEmpty())
        startPlayback()
        verify(cache, times(2)).loadContentPublic("https://example.com/lead.mp3", "mp3", false)
        verify(players.constructed().single()).prepare()
    }

    @Test
    fun decode_failure_releases_the_player_and_retry_prepares_a_new_one() {
        failPrepare = true
        clickPlay()
        requests.single().setResult(audio)
        drain()
        val failed = players.constructed().single()
        verify(failed).release()
        verify(failed, never()).start()
        val descriptor = ArgumentCaptor.forClass(FileDescriptor::class.java)
        verify(failed).setDataSource(descriptor.capture())
        assertFalse(descriptor.value.valid())
        assertStopped()
        failPrepare = false
        val retry = startPlayback()
        assertNotSame(failed, retry)
        verify(retry).prepare()
        verify(cache, times(2)).loadContentPublic("https://example.com/lead.mp3", "mp3", false)
    }

    @Test
    fun cancelled_fetch_clears_loading_and_allows_retry() {
        clickPlay()
        requests.single().setCancelled()
        drain()
        assertStopped()
        startPlayback()
        assertEquals(2, requests.size)
    }

    @Test
    fun repeated_clicks_while_loading_do_not_launch_duplicate_fetches() {
        clickPlay()
        clickPlay()
        assertEquals(1, requests.size)
        assertLoading(true)
    }

    @Test
    fun pause_cancels_polling_and_resume_does_not_refetch_or_prepare_again() {
        val player = startPlayback()
        clickPlay()
        verify(player).pause()
        assertFalse(view.isPlaying)
        clearInvocations(player)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
        verifyNoInteractions(player)
        clickPlay()
        drain()
        verify(player).start()
        verify(player, never()).prepare()
        assertEquals(1, requests.size)
        assertTrue(view.isPlaying)
    }

    @Test
    fun polling_an_errored_player_releases_it_and_does_not_reschedule() {
        val player = startPlayback()
        `when`(player.currentPosition).thenThrow(IllegalStateException("decoder stopped"))
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(25))
        verify(player).release()
        assertStopped()
        clearInvocations(player)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
        verifyNoInteractions(player)
        startPlayback()
        assertEquals(2, requests.size)
    }

    @Test
    fun repeated_stop_releases_once_and_leaves_no_position_polling() {
        val player = startPlayback()
        view.stop()
        view.stop()
        verify(player).release()
        assertStopped()
        clearInvocations(player)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
        verifyNoInteractions(player)
        startPlayback()
        assertEquals(2, players.constructed().size)
    }

    @Test
    fun detaching_releases_active_playback_and_cancels_polling() {
        val player = startPlayback()
        container.removeView(view)
        verify(player).release()
        assertStopped()
        clearInvocations(player)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
        verifyNoInteractions(player)
    }

    @Test
    fun choosing_another_part_releases_the_previous_player_before_fetching() {
        val player = startPlayback()
        view.remoteLocation = location("bass")
        verify(player).release()
        assertStopped()
        assertEquals(1, requests.size)
        val next = startPlayback()
        assertNotSame(player, next)
    }

    @Test
    fun completion_cancels_polling_and_can_replay_the_prepared_player() {
        val player = startPlayback()
        val listener = ArgumentCaptor.forClass(MediaPlayer.OnCompletionListener::class.java)
        verify(player).setOnCompletionListener(listener.capture())
        listener.value.onCompletion(player)
        assertFalse(view.isPlaying)
        assertEquals(12000, view.audioPosition)
        clearInvocations(player)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
        verifyNoInteractions(player)
        clickPlay()
        verify(player).start()
        assertTrue(view.isPlaying)
        assertEquals(1, requests.size)
    }

    @Test
    fun player_error_releases_and_retries_and_old_callbacks_are_ignored() {
        val player = startPlayback()
        val error = ArgumentCaptor.forClass(MediaPlayer.OnErrorListener::class.java)
        val completion = ArgumentCaptor.forClass(MediaPlayer.OnCompletionListener::class.java)
        verify(player).setOnErrorListener(error.capture())
        verify(player).setOnCompletionListener(completion.capture())
        assertTrue(error.value.onError(player, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0))
        verify(player).release()
        assertStopped()
        val retry = startPlayback()
        verify(retry).prepare()
        assertTrue(error.value.onError(player, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0))
        completion.value.onCompletion(player)
        assertTrue(view.isPlaying)
        verify(retry, never()).release()
    }

    // Exercise onPause alone: ViewPager need not stop or detach an offscreen page.
    private fun tracksFragment(): TagTracksFragment =
        TagTracksFragment().also {
            view.id = R.id.mediaPlayer
            ReflectionHelpers.setField(it, "mView", container)
        }

    @Test
    fun tracks_onPause_releases_playback_without_onStop_or_detach() {
        val player = startPlayback()
        tracksFragment().onPause()
        assertTrue(view.isAttachedToWindow)
        verify(player).release()
        assertStopped()
    }

    @Test
    fun tracks_onPause_invalidates_pending_fetch_without_onStop_or_detach() {
        clickPlay()
        tracksFragment().onPause()
        requests.single().setResult(audio)
        drain()
        assertTrue(view.isAttachedToWindow)
        assertStopped()
        assertTrue(players.constructed().isEmpty())
    }

    @Test
    fun disabling_the_player_disables_every_control() {
        view.isEnabled = false
        assertFalse(view.findViewById<MaterialButton>(R.id.playPauseButton).isEnabled)
        assertFalse(view.findViewById<MaterialButton>(R.id.stopButton).isEnabled)
        assertFalse(view.findViewById<View>(R.id.counterSeekBar).isEnabled)
        assertFalse(view.findViewById<View>(R.id.balanceSeekBar).isEnabled)
    }

    @Test
    fun the_balance_control_starts_centred() {
        assertEquals(500, view.balance)
    }
}
