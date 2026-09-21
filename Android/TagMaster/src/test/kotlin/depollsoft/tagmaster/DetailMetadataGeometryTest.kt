package depollsoft.tagmaster

import android.app.Application
import android.content.Intent
import android.graphics.Rect
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.children
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import depollsoft.tagmaster.ScreenTestSupport.idle
import depollsoft.tagmaster.barbershop.RemoteLocation
import depollsoft.tagmaster.barbershop.Tag
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Date

/**
 * The detail screen's metadata geometry, migrated from the instrumented `DetailCompositionTest`.
 *
 * The transport button faces and the key/sheet action alignment that test also covered are already
 * covered on the JVM by `TransportFaceTest` and `DetailActionAlignmentTest`; what is added here is
 * the metadata group's own contract — one caption mode per group, a shared value edge, matched
 * baselines, role-minimum row heights — and the optional groups collapsing when their data is
 * absent. Real text metrics are required, hence native graphics.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28], qualifiers = "w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DetailMetadataGeometryTest {
    private var controller: ActivityController<TagDetailActivity>? = null
    private val tagId = 2147471809

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

    private fun tag() =
        Tag().apply {
            id = tagId
            title = "Lost"
            alternativeTitle = "In Your Eyes"
            parts = 4
            rating = 3.49
            tagType = "Barbershop"
            writtenKey = "Minor:G"
            downloadCount = 12345
            lastRefreshed = Date(1600000000000)
            posted = lastRefreshed
            provider = "David Wright"
            arranger = "David Wright"
            sungBy = "The New Tradition"
            yearArranged = "2020"
            sungYear = "2021"
            providerWebsite = "https://example.invalid/provider"
            sungByWebsite = "https://example.invalid/quartet"
            lyrics = "And I will wait to face the skies,\never roaming in your eyes."
            notes = "Hold the last chord."
            allPartsTrackUri =
                RemoteLocation().apply {
                    uri = "https://example.invalid/composition.mp3"
                    type = "mp3"
                }
            sheetMusicUri =
                RemoteLocation().apply {
                    uri = "https://example.invalid/composition.pdf"
                    type = "pdf"
                }
        }

    private fun launch(model: Tag): TagDetailActivity {
        ScreenTestSupport.cacheOnDisk(model)
        val created =
            ScreenTestSupport.build(
                TagDetailActivity::class.java,
                Intent(RuntimeEnvironment.getApplication(), TagDetailActivity::class.java)
                    .putExtra(TagDetailActivity.TAG_ID_EXTRA, model.id),
            )
        controller = created
        created.setup()
        // The disk-cache read runs on Bolts' background executor; the geometry below is only
        // meaningful once the tag it describes has actually arrived.
        ScreenTestSupport.awaitTagLoaded(created.get())
        return created.get()
    }

    private fun bounds(view: View): Rect =
        Rect(0, 0, view.width, view.height).also { rect ->
            val xy = IntArray(2)
            view.getLocationOnScreen(xy)
            rect.offset(xy[0], xy[1])
        }

    private inline fun <reified F : androidx.fragment.app.Fragment> TagDetailActivity.page(at: Int): View {
        findViewById<ViewPager2>(R.id.viewPager).setCurrentItem(at, false)
        idle()
        return detailFragment!!.childFragmentManager.fragments
            .filterIsInstance<F>()
            .first { it.view != null }
            .requireView()
    }

    /**
     * Every visible caption/value pair in one group shares a mode, a value edge and a baseline.
     */
    private fun assertMetadataGroup(group: DetailMetadataLayout) {
        val pairs =
            group.children
                .filterIsInstance<DetailPairLayout>()
                .filter { it.visibility != View.GONE }
                .toList()
        assertTrue("the group should have visible rows", pairs.isNotEmpty())
        val mode = pairs.first().captionWidth
        val axis = bounds(pairs.first().getChildAt(1)).left
        val density = group.resources.displayMetrics.density

        pairs.forEach { pair ->
            val caption = pair.getChildAt(0) as TextView
            val value = pair.getChildAt(1)
            assertEquals("the whole group uses one caption mode", mode, pair.captionWidth)
            assertEquals("'${caption.text}' shares the value edge", axis, bounds(value).left)
            assertTrue(
                "'${caption.text}' meets its role minimum height",
                pair.height >= (pair.rowHeight * density).toInt(),
            )
            assertTrue(
                "'${caption.text}' leaves a useful width for its value",
                value.width >= 96 * density,
            )
            if (mode >= 0 && value is TextView && value.lineCount == 1 &&
                pair.resources.configuration.fontScale == 1f
            ) {
                assertEquals(
                    "'${caption.text}' uses the uniform default role height",
                    (pair.rowHeight * density).toInt(),
                    pair.height,
                )
            }
            if (mode >= 0 && value.baseline >= 0) {
                assertEquals(
                    "'${caption.text}' sits on its value's baseline",
                    bounds(caption).top + caption.baseline,
                    bounds(value).top + value.baseline,
                )
            }
            if (value is TextView && value.layout != null) {
                assertTrue(
                    "'${caption.text}' shows its final line",
                    value.height >= value.compoundPaddingTop + value.layout.height + value.compoundPaddingBottom,
                )
            }
        }
    }

    @Test
    fun theSummaryFactsShareOneCaptionModeEdgeAndBaseline() {
        val activity = launch(tag())
        val summary = activity.page<TagSummaryFragment>(0)
        assertMetadataGroup(summary.findViewById(R.id.summaryFacts))
    }

    @Test
    fun theDetailsTableSharesOneCaptionModeEdgeAndBaseline() {
        val activity = launch(tag())
        val details = activity.page<TagMiscFragment>(1)
        assertMetadataGroup(details.findViewById(R.id.tableLayout1))
    }

    @Test
    fun theSummaryProseAndTitleShareOneReadingEdge() {
        val activity = launch(tag())
        val summary = activity.page<TagSummaryFragment>(0)
        val leading = bounds(summary.findViewById(R.id.titleTextView)).left
        for (id in listOf(R.id.akaLayout, R.id.versionLayout, R.id.savedStatusLayout)) {
            assertEquals(
                "view $id lines up with the title",
                leading,
                bounds(summary.findViewById(id)).left,
            )
        }
        val columns = summary.findViewById<SummaryColumnsLayout>(R.id.summaryColumns)
        if (columns.orientation == LinearLayout.VERTICAL) {
            for (id in listOf(
                R.id.lyticsTitleTextView,
                R.id.lyricsTextView,
                R.id.textView6,
                R.id.notesTextView,
            )) {
                assertEquals(
                    "prose view $id keeps the reading edge",
                    leading,
                    bounds(summary.findViewById(id)).left,
                )
            }
        }
    }

    @Test
    fun theKeyAndSheetActionsShareAColumnAndStayTappable() {
        val activity = launch(tag())
        val summary = activity.page<TagSummaryFragment>(0)
        val density = summary.resources.displayMetrics.density
        val key = summary.findViewById<View>(R.id.playKeyNoteButton)
        val sheet = summary.findViewById<MaterialButton>(R.id.sheetMusicLink)
        val keyFrame = bounds(key)
        val sheetFrame = bounds(sheet)

        assertEquals("the two actions share a left edge", keyFrame.left, sheetFrame.left)
        assertEquals("and a right edge", keyFrame.right, sheetFrame.right)
        assertTrue("the sheet action is tappable", sheet.height >= 48 * density)
        assertTrue("the key action is tappable", key.height >= 48 * density)
        assertEquals("no inset eats into the sheet action", 0, sheet.insetTop)
        assertEquals(0, sheet.insetBottom)

        val columns = summary.findViewById<SummaryColumnsLayout>(R.id.summaryColumns)
        assertEquals(
            "the action face starts at its column",
            bounds(columns.getChildAt(0)).left,
            keyFrame.left,
        )
        assertEquals(
            "and fills it",
            bounds(columns.getChildAt(0)).right,
            keyFrame.right,
        )
        assertEquals(
            "the content sits 16dp from the edge",
            16f,
            (keyFrame.left - bounds(summary).left) / density,
            1f,
        )
    }

    @Test
    fun theActionFramesSurviveABusyLayoutPass() {
        val activity = launch(tag())
        val summary = activity.page<TagSummaryFragment>(0)
        val idleKey = bounds(summary.findViewById(R.id.playKeyNoteButton))
        val idleSheet = bounds(summary.findViewById(R.id.sheetMusicLink))

        for (busy in listOf(true, false)) {
            summary.findViewById<BarberPoleLoadingView>(R.id.sheetMusicProgress).loading = busy
            summary.findViewById<View>(R.id.sheetMusicLink).isEnabled = !busy
            idle()
            assertEquals(
                "the key frame does not move when the sheet action goes busy=$busy",
                idleKey,
                bounds(summary.findViewById(R.id.playKeyNoteButton)),
            )
            assertEquals(
                "nor does the sheet frame, busy=$busy",
                idleSheet,
                bounds(summary.findViewById(R.id.sheetMusicLink)),
            )
        }
    }

    @Test
    fun optionalGroupsCollapseWhenTheirDataIsAbsent() {
        val activity = launch(tag())
        val summary = activity.page<TagSummaryFragment>(0)
        // The screen loads its own instance out of the disk cache, so the bindings follow that
        // object, not the one written to disk.
        val model = activity.tag!!

        for (prose in 0..3) {
            model.sheetMusicUri = null
            model.writtenKey = null
            model.alternativeTitle = null
            model.version = null
            model.lyrics = if (prose and 1 != 0) "Only lyrics" else null
            model.notes = if (prose and 2 != 0) "Only notes" else null
            idle()

            // savedStatusLayout is not in this list: it is the chips row, and a loaded tag always
            // offers at least Add to list there, however little else it has.
            for (id in listOf(
                R.id.linearLayout3,
                R.id.keyRow,
                R.id.akaLayout,
                R.id.versionLayout,
            )) {
                assertEquals(
                    "group ${summary.resources.getResourceEntryName(id)} collapses when it has " +
                        "nothing to show (prose=$prose)",
                    View.GONE,
                    summary.findViewById<View>(id).visibility,
                )
            }
            val columns = summary.findViewById<SummaryColumnsLayout>(R.id.summaryColumns)
            assertEquals(
                "the prose column shows exactly when there is prose (prose=$prose)",
                prose == 0,
                columns.getChildAt(1).visibility == View.GONE,
            )
            if (prose == 0) {
                assertEquals(
                    "with no prose the columns stack",
                    LinearLayout.VERTICAL,
                    columns.orientation,
                )
            }
        }
    }
}
