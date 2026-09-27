package depollsoft.tagmaster

import android.app.Application
import android.content.Intent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import depollsoft.tagmaster.barbershop.Tag
import depollsoft.tagmaster.barbershop.Video
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The Tracks and Videos pages: what a tag without tracks or videos shows, and what a full one lists. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w411dp-h891dp-xxhdpi")
class DetailTracksAndVideosScreenTest : ComposeScreenTest() {
    private fun open(
        tag: Tag,
        page: Int,
    ): TagDetailActivity {
        ScreenTestSupport.cacheOnDisk(tag)
        val activity =
            launch(TagDetailActivity::class.java, Intent(app, TagDetailActivity::class.java).putExtra(TagDetailActivity.TAG_ID_EXTRA, tag.id))
        ScreenTestSupport.awaitTagLoaded(activity)
        idle()
        click("detailTab:$page")
        return activity
    }

    private fun shows(value: String) = compose.onAllNodes(hasText(value, substring = true), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun bare() =
        ScreenTestSupport.fixtureTag().apply {
            allPartsTrackUri = null
            tenorTrackUri = null
            leadTrackUri = null
            baritoneTrackUri = null
            bassTrackUri = null
            videos = mutableListOf()
        }

    @Test
    fun aTagWithNoTracksHidesThePlayerAndExplainsTheRecordingNote() {
        open(bare(), 2)
        assertTrue(shows(string(R.string.SorryNoTracks)))
        assertTrue(shows(ScreenTestSupport.fixtureTag().recordingMethod!!))
        assertFalse(exists("playPause"))
        assertFalse(exists("balance"))
        assertFalse(exists("part:0"))
    }

    @Test
    fun replacingTheTagWithTheSameIdReDerivesItsParts() {
        val activity = open(ScreenTestSupport.fixtureTag(), 2)
        assertTrue(exists("part:4"))
        activity.detail.showLoaded(ScreenTestSupport.fixtureTag().apply { bassTrackUri = null })
        idle()
        assertFalse(exists("part:4"))
        assertTrue(exists("part:0"))
    }

    @Test
    fun aRefreshThatDropsTheChosenPartLetsGoOfItsTrack() {
        val activity = open(ScreenTestSupport.fixtureTag(), 2)
        click("part:4")
        node("playPause").assertIsEnabled()
        // The same tag, refreshed without its bass track: a new Tag equal to the old by id.
        activity.detail.showLoaded(ScreenTestSupport.fixtureTag().apply { bassTrackUri = null })
        idle()
        node("playPause").assertIsNotEnabled()
    }

    /**
     * Runs [block] with the first part's track started and its download held open, so the player
     * stays on until something stops it. The download is let go and wound up before returning, so
     * its background load doesn't finish inside the next test.
     */
    private fun withTrackPlaying(block: () -> Unit) {
        val download = java.util.concurrent.CountDownLatch(1)
        val answered = java.util.concurrent.atomic.AtomicBoolean(false)
        ScreenTestSupport.withTransport({
            download.await(10, java.util.concurrent.TimeUnit.SECONDS)
            answered.set(true)
            java.io.ByteArrayInputStream(ByteArray(0))
        }) {
            try {
                open(ScreenTestSupport.fixtureTag(), 2)
                click("part:0")
                click("playPause")
                node("playPause").assertIsNotEnabled()
                node("stop").assertIsEnabled()
                block()
            } finally {
                download.countDown()
                ScreenTestSupport.await("the held track download to wind up") {
                    answered.get() && Thread.getAllStackTraces().keys.none { it.name.startsWith("pool-") && it.state == Thread.State.RUNNABLE }
                }
            }
        }
    }

    /** Leaving the app stops a learning track, even one still downloading. */
    @Test
    fun pausingStopsATrack() =
        withTrackPlaying {
            // The rule only sees resumed activities; coming back does not restart the track.
            controller!!.pause().resume()
            idle()
            node("playPause").assertIsEnabled()
            node("stop").assertIsNotEnabled()
        }

    /**
     * The track stops only once the pager settles on another page, as ViewPager2 paused the
     * fragment: a swipe held past halfway and brought back leaves it playing.
     */
    @Test
    fun aSwipeBroughtBackBeforeSettlingKeepsTheTrackPlaying() =
        withTrackPlaying {
            node("detailPager").performTouchInput {
                down(center)
                moveBy(Offset(-width * 0.7f, 0f))
            }
            idle()
            node("stop").assertIsEnabled()
            node("detailPager").performTouchInput {
                repeat(7) { moveBy(Offset(width * 0.1f, 0f), delayMillis = 50) }
                // Still before lifting, so no fling carries it on.
                repeat(4) { moveBy(Offset.Zero, delayMillis = 100) }
                up()
            }
            idle()
            node("stop").assertIsEnabled()
            click("detailTab:3")
            node("stop").assertIsNotEnabled()
        }

    @Test
    fun everyPartAndPlayerControlIsPresent() {
        open(ScreenTestSupport.fixtureTag(), 2)
        for (tag in listOf("playPause", "stop", "position", "balance", "part:0", "part:1", "part:2", "part:3", "part:4")) {
            assertTrue(tag, exists(tag))
        }
    }

    @Test
    fun videosShowTheirMetadataAndCollapseWhatIsMissing() {
        val tag =
            ScreenTestSupport.fixtureTag().apply {
                videos =
                    mutableListOf(
                        Video().apply {
                            id = 1
                            sungBy = "A quartet with a very long name that has to wrap onto another line"
                            sungKey = "Bb"
                            youTubeCode = "long"
                        },
                        Video().apply {
                            id = 2
                            youTubeCode = "bare"
                        },
                    )
            }
        open(tag, 3)
        assertTrue("first video", exists("video:1"))
        assertTrue("second video", exists("video:2"))
        assertTrue("sung by: " + text("video:1"), shows("A quartet with a very long name"))
        assertFalse(text("video:2").contains("null"))
    }

    @Test
    fun aVideoListedTwiceShowsTwiceInsteadOfCrashing() {
        val tag =
            ScreenTestSupport.fixtureTag().apply {
                videos = MutableList(2) { Video().apply { id = 7; sungBy = "The Same Quartet"; youTubeCode = "twice" } }
            }
        open(tag, 3)
        assertEquals(2, compose.onAllNodes(hasTestTag("video:7"), useUnmergedTree = true).fetchSemanticsNodes().size)
    }

    @Test
    fun aTagWithNoVideosSaysSo() {
        open(bare(), 3)
        assertFalse(exists("video:1"))
    }

    @Test
    fun theTeachingVideoStaysWhileThePerformancesScroll() {
        val tag =
            ScreenTestSupport.fixtureTag().apply {
                teachingVideo = "teach"
                videos = (1..20).map { index -> Video().apply { id = index; sungBy = "Quartet $index"; youTubeCode = "v$index" } }.toMutableList()
            }
        open(tag, 3)
        val before = node("teachingVideo").fetchSemanticsNode().boundsInRoot
        node("videoList").performScrollToIndex(19)
        idle()
        assertTrue(exists("video:20"))
        assertEquals(before, node("teachingVideo").fetchSemanticsNode().boundsInRoot)
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp-xxhdpi")
    fun theVideosKeepTheReadingWidthOnAWideScreen() {
        val tag =
            ScreenTestSupport.fixtureTag().apply {
                teachingVideo = "teach"
                videos = (1..3).map { index -> Video().apply { id = index; sungBy = "Quartet $index"; youTubeCode = "v$index" } }.toMutableList()
            }
        open(tag, 3)
        val density = app.resources.displayMetrics.density
        val screen = app.resources.configuration.screenWidthDp * density
        for (id in listOf("teachingVideo", "video:1")) {
            val bounds = node(id).fetchSemanticsNode().boundsInRoot
            assertTrue("$id within a centred 640dp column: $bounds", bounds.width <= 640 * density + 1f)
            assertEquals("$id centred", screen / 2f, bounds.center.x, density * 2f)
        }
    }

    @Test
    fun theWindowIsTitledWithTheTag() {
        val tag = ScreenTestSupport.fixtureTag()
        val activity = open(tag, 0)
        assertEquals(tag.title, activity.title.toString())
        assertEquals(tag.title, node("toolbarTitle").fetchSemanticsNode().let { text("toolbarTitle") })
    }
}
