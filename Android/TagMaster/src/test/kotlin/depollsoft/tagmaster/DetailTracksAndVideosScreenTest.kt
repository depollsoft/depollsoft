package depollsoft.tagmaster

import android.app.Application
import android.content.Intent
import android.view.View
import android.widget.ListView
import android.widget.TextView
import androidx.viewpager2.widget.ViewPager2
import bolts.Task
import depollsoft.tagmaster.ScreenTestSupport.idle
import depollsoft.tagmaster.barbershop.RemoteLocation
import depollsoft.tagmaster.barbershop.Tag
import depollsoft.tagmaster.barbershop.Video
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The tracks page's transport and part list, and the videos page's optional metadata, migrated from
 * the instrumented `LayoutRegressionTest`.
 *
 * The media player's own pending/failure/stop/detach state machine is already covered on the JVM by
 * `MediaPlayerLifecycleTest`; what is added here is how the tracks page reacts to the tag it is
 * given, including a replacement carrying the same id.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28], qualifiers = "w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DetailTracksAndVideosScreenTest {
    private var controller: ActivityController<TagDetailActivity>? = null

    /** A distinct id per test: `Tag` keeps a process-wide in-memory cache keyed by id. */
    @get:org.junit.Rule
    val testName = org.junit.rules.TestName()

    private val tagId get() = 2147470000 + (testName.methodName.hashCode().and(0x7fffffff) % 1000)

    @Before
    fun setUp() {
        ScreenTestSupport.startClean()
        ScreenTestSupport.clearTagCaches()
    }

    @After
    fun tearDown() {
        ScreenTestSupport.finish(controller)
        ScreenTestSupport.clearTagCaches()
    }

    private fun bare() =
        Tag().apply {
            id = tagId
            title = "When the quartet gathers for one more song"
            parts = 4
            writtenKey = "C"
        }

    private fun populated() =
        bare().apply {
            recordingMethod = "Recorded separately so each singer can follow the phrase. ".repeat(8)
            allPartsTrackUri = location("all")
            tenorTrackUri = location("tenor")
            leadTrackUri = location("lead")
            baritoneTrackUri = location("baritone")
            bassTrackUri = location("bass")
            other1TrackUri = location("other1")
            other2TrackUri = location("other2")
            other3TrackUri = location("other3")
            other4TrackUri = location("other4")
        }

    private fun location(part: String) =
        RemoteLocation().apply {
            uri = "https://layout.example.invalid/$part.wav"
            type = "wav"
        }

    /** Resolve the tag through the production disk-cache path, with no network. */
    private fun launch(first: Tag): TagDetailActivity {
        ScreenTestSupport.cacheOnDisk(first)
        val created =
            ScreenTestSupport.build(
                TagDetailActivity::class.java,
                Intent(RuntimeEnvironment.getApplication(), TagDetailActivity::class.java)
                    .putExtra(TagDetailActivity.TAG_ID_EXTRA, tagId),
            )
        controller = created
        created.setup()
        // The disk-cache read runs on Bolts' background executor; wait for it rather than assume
        // one `idle()` was enough.
        assertEquals("the cached tag should load", first.title, ScreenTestSupport.awaitTagLoaded(created.get()).title)
        return created.get()
    }

    private fun TagDetailActivity.tracksPage(): TagTracksFragment {
        findViewById<ViewPager2>(R.id.viewPager).setCurrentItem(2, false)
        idle()
        return detailFragment!!.childFragmentManager.fragments
            .filterIsInstance<TagTracksFragment>()
            .first { it.view != null }
    }

    /** The transport is either an apology or a player, never both. */
    private fun assertTransport(
        root: View,
        empty: Boolean,
    ) {
        assertEquals(
            "the apology shows only when there is nothing to play",
            if (empty) View.VISIBLE else View.GONE,
            root.findViewById<View>(R.id.sorryTextView).visibility,
        )
        for (id in listOf(R.id.mediaPlayer, R.id.partsRadioGroup)) {
            assertEquals(
                "view $id shows only when there is something to play",
                if (empty) View.GONE else View.VISIBLE,
                root.findViewById<View>(id).visibility,
            )
        }
        for (id in listOf(R.id.playPauseButton, R.id.stopButton, R.id.counterSeekBar, R.id.balanceSeekBar)) {
            val control = root.findViewById<View>(id)
            if (empty) {
                assertFalse("a hidden player must not keep control $id on screen", control.isShown)
            } else {
                assertEquals(View.VISIBLE, control.visibility)
            }
        }
    }

    @Test
    fun aTagWithNoTracksHidesThePlayerAndDropsItFromTheFocusOrder() {
        val activity = launch(bare())
        val fragment = activity.tracksPage()
        val root = fragment.requireView()

        assertTransport(root, empty = true)
        assertEquals(
            "with no recording note there is nothing to explain",
            View.GONE,
            root.findViewById<View>(R.id.trackNotesLayout).visibility,
        )
        val focusables = ArrayList<View>()
        root.addFocusables(focusables, View.FOCUS_FORWARD)
        assertFalse(
            "a hidden player must not be reachable by keyboard",
            focusables.any { it.id == R.id.balanceSeekBar || it.id == R.id.playPauseButton },
        )
    }

    @Test
    fun aRecordingNoteIsExplainedBelowTheApology() {
        val activity = launch(bare())
        val fragment = activity.tracksPage()
        val root = fragment.requireView()

        activity.tag = bare().apply { recordingMethod = "No separate parts were supplied. ".repeat(10) }
        idle()

        assertTransport(root, empty = true)
        val apology = root.findViewById<TextView>(R.id.sorryTextView)
        val notes = root.findViewById<TextView>(R.id.trackNotesTextView)
        // The two live in different parents, so compare where they actually land on screen.
        fun top(view: View) = IntArray(2).also { view.getLocationOnScreen(it) }[1]
        assertTrue(
            "the note sits below the apology",
            top(apology) + apology.height <= top(notes),
        )
        assertTrue(
            "and all of it is laid out",
            notes.layout.height <= notes.height - notes.compoundPaddingTop - notes.compoundPaddingBottom,
        )
    }

    @Test
    fun replacingTheTagWithTheSameIdReDerivesItsParts() {
        val activity = launch(bare())
        val fragment = activity.tracksPage()
        val root = fragment.requireView()

        // Only the tenor part survives: the buttons must follow the data, not the id.
        activity.tag =
            populated().apply {
                allPartsTrackUri = null
                leadTrackUri = null
                baritoneTrackUri = null
                bassTrackUri = null
                other1TrackUri = null
                other2TrackUri = null
                other3TrackUri = null
                other4TrackUri = null
            }
        idle()

        assertTransport(root, empty = false)
        assertNull("a replacement selects nothing by default", fragment.selectedTrack)
        assertEquals(View.VISIBLE, root.findViewById<View>(R.id.tenorButton).visibility)
        assertEquals(View.GONE, root.findViewById<View>(R.id.allPartsButton).visibility)

        activity.tag = populated()
        idle()

        assertTransport(root, empty = false)
        for (id in listOf(
            R.id.allPartsButton,
            R.id.tenorButton,
            R.id.leadButton,
            R.id.bariButton,
            R.id.bassButton,
            R.id.other1Button,
            R.id.other2Button,
            R.id.other3Button,
            R.id.other4Button,
        )) {
            assertEquals("part $id returns with the full tag", View.VISIBLE, root.findViewById<View>(id).visibility)
        }

        val last = root.findViewById<View>(R.id.other4Button)
        ScreenTestSupport.scrollTo(last)
        ScreenTestSupport.assertFullyVisible("the last extra part", last)
        last.performClick()
        idle()
        assertEquals(
            "the last extra part selects its own track",
            activity.tag!!.other4TrackUri,
            fragment.selectedTrack,
        )

        // Going back to a tag with no tracks empties the transport and the selection with it.
        activity.tag = bare()
        idle()
        assertTransport(root, empty = true)
        assertNull(fragment.selectedTrack)
    }

    /**
     * Every part and player control can be scrolled to and is then *wholly* reachable.
     *
     * A visible intersection is not enough: the instrumented original required the whole control
     * inside its scrolling viewport, and required each part button to keep a 48dp touch target
     * with its label inside it. Both are restored here.
     */
    @Test
    fun everyPartAndPlayerControlCanBeScrolledTo() {
        val activity = launch(populated())
        val root = activity.tracksPage().requireView()
        for (id in listOf(
            R.id.allPartsButton,
            R.id.tenorButton,
            R.id.leadButton,
            R.id.bariButton,
            R.id.bassButton,
            R.id.other1Button,
            R.id.other2Button,
            R.id.other3Button,
            R.id.other4Button,
            R.id.playPauseButton,
            R.id.stopButton,
            R.id.counterSeekBar,
            R.id.balanceSeekBar,
        )) {
            val name = activity.resources.getResourceEntryName(id)
            val control = ScreenTestSupport.scrollTo(root.findViewById(id))
            ScreenTestSupport.assertFullyVisible("control $name", control)
            if (control is android.widget.RadioButton) ScreenTestSupport.assertTouchTarget(control)
        }
    }

    @Test
    fun videosShowLongMetadataWholeAndCollapseWhatIsMissing() {
        val loaded =
            bare().apply {
                videos =
                    mutableListOf(
                        Video().apply {
                            id = 1
                            sungBy = "The singers from our combined evening rehearsal quartet"
                            sungKey = "B flat"
                            youTubeCode = "layout-fixture"
                        },
                        Video().apply {
                            id = 2
                            sungBy = "Another quartet"
                            youTubeCode = "layout-fixture"
                        },
                    )
            }
        val activity = launch(loaded)
        activity.findViewById<ViewPager2>(R.id.viewPager).setCurrentItem(3, false)
        idle()
        val list = activity.findViewById<ListView>(R.id.videoList)
        assertEquals(2, list.count)
        assertTrue("the video list stays usable", list.height > 0)

        list.setSelection(0)
        idle()
        val first = list.getChildAt(0)
        for (id in listOf(R.id.sungByTextView, R.id.keyTextView)) {
            val text = first.findViewById<TextView>(id)
            assertTrue(
                "video field $id is laid out whole",
                text.layout.height <= text.height - text.compoundPaddingTop - text.compoundPaddingBottom,
            )
            for (line in 0 until text.layout.lineCount) {
                assertEquals("video field $id never ellipsizes", 0, text.layout.getEllipsisCount(line))
            }
        }

        list.setSelection(1)
        idle()
        val second = list.getChildAt(list.childCount - 1)
        assertEquals(
            "a video with no key hides its key row",
            View.GONE,
            second.findViewById<View>(R.id.keyRow).visibility,
        )
        assertEquals(
            "a video with no posted date hides its posted row",
            View.GONE,
            second.findViewById<View>(R.id.postedRow).visibility,
        )
    }
}
