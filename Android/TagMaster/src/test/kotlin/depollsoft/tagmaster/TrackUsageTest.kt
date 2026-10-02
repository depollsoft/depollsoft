package depollsoft.tagmaster

import android.app.Application
import android.media.MediaPlayer
import android.os.Looper
import bolts.TaskCompletionSource
import depollsoft.lib.analytics.UsageAnalytics
import depollsoft.lib.util.ContentCache
import depollsoft.tagmaster.barbershop.RemoteLocation
import depollsoft.tagmaster.ui.detail.TrackPlayer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.anyBoolean
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockConstruction
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

/**
 * A learning track counts once it starts playing after loading, under its part's name, and keeps the
 * app "sounding" (so the review prompt stays away) only while it plays.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class TrackUsageTest {
    private val parts = mutableListOf<String>()

    @Before fun record() {
        UsageAnalytics.sink =
            object : UsageAnalytics.Sink {
                override fun logEvent(name: String, params: Map<String, String>) {
                    if (name == "learning_track_played") parts += params.getValue("part")
                }

                override fun setUserProperty(name: String, value: String) {}
            }
    }

    @After fun reset() = UsageAnalytics.resetForTesting()

    @Test
    fun thePartsAreNamedInThePickersOrder() {
        assertEquals(
            listOf("all", "tenor", "lead", "baritone", "bass", "other", "other", "other", "other"),
            (0..8).map(TagMasterAnalytics::trackPart),
        )
    }

    @Test
    fun aTrackCountsWhenItStartsAndNotWhenItResumes() {
        mockConstruction(MediaPlayer::class.java).use { players ->
            val track = TrackPlayer(RuntimeEnvironment.getApplication()) {}
            val pending = TaskCompletionSource<File>()
            val cache = mock(ContentCache::class.java)
            `when`(cache.loadContentPublic(anyString(), anyString(), anyBoolean())).thenReturn(pending.task)
            TrackPlayer::class.java.getDeclaredField("cache").apply { isAccessible = true }.set(track, cache)
            try {
                track.select(RemoteLocation().apply { uri = "https://example.com/lead.mp3"; type = "mp3" }, "lead")
                track.togglePlay()
                val file = File.createTempFile("track", ".mp3")
                pending.setResult(file)
                shadowOf(Looper.getMainLooper()).idle()
                file.delete()
                val player = players.constructed().single()
                `when`(player.duration).thenReturn(10000)
                val captor = ArgumentCaptor.forClass(MediaPlayer.OnPreparedListener::class.java)
                verify(player).setOnPreparedListener(captor.capture())
                captor.value.onPrepared(player)
                assertEquals(listOf("lead"), parts)
                assertTrue(Sounding.any)

                track.togglePlay()
                assertFalse("a paused track is quiet", Sounding.any)
                track.togglePlay()
                assertEquals("resuming is the same playing", listOf("lead"), parts)
                assertTrue(Sounding.any)

                val finished = ArgumentCaptor.forClass(MediaPlayer.OnCompletionListener::class.java)
                verify(player).setOnCompletionListener(finished.capture())
                finished.value.onCompletion(player)
                assertFalse("a finished track is quiet", Sounding.any)
                track.togglePlay()
                assertEquals("playing a finished track starts it over", listOf("lead", "lead"), parts)
            } finally {
                track.release()
            }
            assertFalse("a released track is quiet", Sounding.any)
        }
    }
}
