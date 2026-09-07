package depollsoft.tagmaster

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigationrail.NavigationRailView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Structure and legibility checks on the layouts themselves.
 *
 * Inflating and measuring the real layouts catches the two failures that a
 * build never will: an affordance quietly dropped from a screen, and a row that
 * clips its own text once the system font is turned up.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestDeskApplication::class, sdk = [35])
class DeskLayoutTest {
    private fun themed(): Context = ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.AppTheme)

    private fun inflate(layout: Int): View = LayoutInflater.from(themed()).inflate(layout, null, false)

    private fun View.measureAt(widthDp: Int): View {
        val density = resources.displayMetrics.density
        val widthPx = (widthDp * density).toInt()
        measure(
            View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        layout(0, 0, measuredWidth, measuredHeight)
        return this
    }

    private fun walk(
        view: View,
        visit: (View) -> Unit,
    ) {
        // A hidden branch is never laid out, so it has nothing to clip.
        if (view.visibility == View.GONE) return
        visit(view)
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) walk(view.getChildAt(i), visit)
        }
    }

    /** Every laid-out text view must have room for the lines it actually drew. */
    private fun assertNoClippedText(
        root: View,
        screen: String,
    ) {
        walk(root) { view ->
            if (view is TextView && view.visibility != View.GONE && view.text.isNotEmpty()) {
                val textHeight = view.layout?.height ?: 0
                val available = view.height - view.paddingTop - view.paddingBottom
                assertTrue(
                    "$screen: '${view.text}' needs ${textHeight}px but has ${available}px",
                    textHeight <= available,
                )
            }
        }
    }

    @Test
    fun home_offers_every_way_into_a_tag() {
        val home = inflate(R.layout.homeview)

        assertNotNull("Find a tag", home.findViewById<View>(R.id.findTagButton))
        assertNotNull("Random tag", home.findViewById<View>(R.id.randomTagButton))
        assertNotNull("Open tag ID", home.findViewById<View>(R.id.openByIdButton))
        assertNotNull("Favorites list", home.findViewById<View>(R.id.favoritesItemsControl))
        assertNotNull("Favorites empty state", home.findViewById<View>(R.id.favoritesEmptyTextView))
        assertNotNull("Teachable tags", home.findViewById<View>(R.id.homeTeachableItemsControl))
        // Attribution ships on every install.
        assertNotNull("BarbershopTags.com credit", home.findViewById<View>(R.id.textView4))
        assertNotNull("Terms of use", home.findViewById<View>(R.id.termsHyperlink))
    }

    @Test
    fun search_keeps_every_filter() {
        val search = inflate(R.layout.tagsearchview)

        assertNotNull(search.findViewById<View>(R.id.searchTextBox))
        assertNotNull(search.findViewById<View>(R.id.searchButton))
        assertNotNull(search.findViewById<View>(R.id.sortBySpinner))
        assertNotNull(search.findViewById<View>(R.id.sheetMusicSpinner))
        assertNotNull(search.findViewById<View>(R.id.learningTracksSpinner))
        assertNotNull(search.findViewById<View>(R.id.partsSpinner))
        assertNotNull(search.findViewById<View>(R.id.tagCollectionSpinner))
    }

    @Test
    fun results_have_an_empty_and_error_state_with_a_way_back() {
        val results = inflate(R.layout.tagqueryview)

        assertNotNull(results.findViewById<View>(R.id.queryResultListView))
        assertNotNull(results.findViewById<View>(R.id.statusTextView))
        assertNotNull(results.findViewById<View>(R.id.retryButton))
        assertNotNull(results.findViewById<View>(R.id.loadingProgressBar))
    }

    @Test
    fun tagWorkspaceHasSummaryMaterialAndRetry() {
        val detail = inflate(R.layout.tagdetailview)
        assertNotNull(detail.findViewById<View>(R.id.summaryContainer))
        assertNotNull(detail.findViewById<View>(R.id.materialContainer))
        assertNotNull(detail.findViewById<View>(R.id.detailRetryButton))
        val summary = inflate(R.layout.tagsummaryview)
        assertNotNull(summary.findViewById<View>(R.id.openTracksButton))
        assertNotNull(summary.findViewById<View>(R.id.openDetailsButton))
        assertNotNull(summary.findViewById<View>(R.id.openVideosButton))
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp", fontScale = 2.0f)
    fun phone_screens_survive_the_largest_system_font() {
        assertNoClippedText(inflate(R.layout.settingsview).measureAt(411), "settings")
        assertNoClippedText(inflate(R.layout.mediaplayerview).measureAt(411), "player")
        assertNoClippedText(inflate(R.layout.tagitemview).measureAt(411), "tag row")
        assertNoClippedText(inflate(R.layout.tagsummaryview).measureAt(411), "summary")
        assertNoClippedText(inflate(R.layout.meviewheader).measureAt(411), "home actions")
    }

    @Test
    @Config(qualifiers = "w840dp-h1024dp", fontScale = 2.0f)
    fun tablet_screens_survive_the_largest_system_font() {
        assertNoClippedText(inflate(R.layout.settingsview).measureAt(840), "settings")
        assertNoClippedText(inflate(R.layout.mediaplayerview).measureAt(840), "player")
        assertNoClippedText(inflate(R.layout.tagtracksview).measureAt(840), "tracks")
    }

    @Test
    @Config(qualifiers = "w840dp-h1024dp")
    fun reading_columns_stay_narrower_than_a_tablet() {
        val settings = inflate(R.layout.settingsview).measureAt(840)
        val column = settings.findViewById<View>(R.id.tableLayout1)
        val cap = settings.resources.getDimensionPixelSize(R.dimen.tm_content_max_width)

        assertTrue(
            "settings column measured ${column.measuredWidth}px against a ${cap}px cap",
            column.measuredWidth <= cap,
        )
    }
}
