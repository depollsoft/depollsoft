package depollsoft.tagmaster

import android.app.Application
import android.content.Intent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Layout rules the screens keep at any size, checked on the composed bounds. The screenshot
 * goldens pin the exact pixels at the recorded sizes; these hold the rules themselves.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class LayoutRegressionTest : ComposeScreenTest() {
    private val fixture = ScreenTestSupport.fixtureTag().apply { sheetMusicUri = ScreenTestSupport.track("sheet").apply { type = "pdf" } }

    private fun bounds(tag: String): Rect = node(tag).fetchSemanticsNode().boundsInRoot

    private fun textBounds(value: String): Rect =
        compose
            .onAllNodes(hasText(value), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .first()
            .boundsInRoot

    private val minimumTarget get() = 48 * app.resources.displayMetrics.density - 0.5f

    private fun detail(): TagDetailActivity {
        ScreenTestSupport.cacheOnDisk(fixture)
        val activity =
            launch(TagDetailActivity::class.java, Intent(app, TagDetailActivity::class.java).putExtra(TagDetailActivity.TAG_ID_EXTRA, fixture.id))
        ScreenTestSupport.awaitTagLoaded(activity)
        idle()
        return activity
    }

    @Test
    fun phoneTabsStayBelowThePages() {
        detail()
        assertTrue(bounds("detailTabs").top >= bounds("detailPager").bottom - 0.5f)
    }

    @Test
    fun theKeyAndSheetActionsShareAColumnAndStayTappable() {
        detail()
        val key = bounds("playKeyNoteButton")
        val sheet = bounds("sheetMusicLink")
        assertEquals(key.left, sheet.left, 0.5f)
        assertEquals(key.right, sheet.right, 0.5f)
        assertTrue(key.height >= minimumTarget && sheet.height >= minimumTarget)
        assertTrue(bounds("rateButton").let { it.width >= minimumTarget && it.height >= minimumTarget })
    }

    @Test
    fun theDetailsTableSharesOneCaptionEdgeAndOneValueEdge() {
        detail()
        click("detailTab:1")
        val captions = listOf(R.string.LastRefreshed, R.string.Downloads, R.string.ArrangedBy, R.string.YearArranged).map { textBounds(string(it)) }
        assertTrue(captions.all { it.left == captions.first().left })
        val values = listOf(fixture.arranger!!, fixture.yearArranged!!).map { textBounds(it) }
        assertTrue(values.all { it.left == values.first().left })
        assertTrue(values.first().left > captions.maxOf { it.right })
    }

    @Test
    fun theTransportButtonsAreSquareTouchTargets() {
        detail()
        click("detailTab:2")
        for (tag in listOf("playPause", "stop")) {
            val face = bounds(tag)
            assertEquals(face.width, face.height, 0.5f)
            assertTrue(face.width >= minimumTarget)
        }
    }

    @Test
    fun everyToolbarActionIsATouchTarget() {
        detail()
        for (tag in listOf("navigateUp", "addFavorite", "addTeachable", "overflowMenu")) {
            val action = bounds(tag)
            assertTrue("$tag: $action", action.height >= minimumTarget && action.width >= 40 * app.resources.displayMetrics.density - 0.5f)
        }
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp-xxhdpi")
    fun theHomeFooterKeepsWholeLinksOnANarrowPhone() {
        launch(MeActivity::class.java)
        node("homeList").performScrollToNode(hasText(string(R.string.Donate)))
        idle()
        val width = app.resources.displayMetrics.widthPixels
        for (link in listOf(R.string.DepollSoft, R.string.TermsOfUse, R.string.Donate)) {
            val box = textBounds(string(link))
            assertTrue("${string(link)} stays on screen: $box", box.left >= 0f && box.right <= width)
        }
    }
}
