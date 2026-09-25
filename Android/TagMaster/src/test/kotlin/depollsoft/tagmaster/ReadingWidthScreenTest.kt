package depollsoft.tagmaster

import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * On a window wider than a reading measure but short of the two-pane layout (a phone on its
 * side), lists and forms stay 640dp wide and centred, as the View screens' content insets kept them.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35], qualifiers = "w915dp-h411dp-land-xxhdpi")
class ReadingWidthScreenTest : ComposeScreenTest() {
    private val density get() = app.resources.displayMetrics.density

    @Test
    fun homeRowsKeepAReadingMeasure() {
        val activity = launch(MeActivity::class.java)
        val row = node("browseButton").fetchSemanticsNode().boundsInRoot
        assertEquals("the row is 640dp wide", 640f, row.width / density, 1f)
        assertEquals("and centred", activity.window.decorView.width / 2f, row.center.x, density)
    }

    @Test
    fun theSearchFormKeepsAReadingMeasure() {
        val activity = launch(TagSearchActivity::class.java)
        val field = node("searchTextBox").fetchSemanticsNode().boundsInRoot
        // The text sits inside the centred 640dp measure and the form's 16dp margins.
        val window = activity.window.decorView.width / density
        val margin = (window - 640f) / 2f + 16f
        assertTrue("left ${field.left / density}dp", field.left / density >= margin - 1f)
        assertTrue("right ${field.right / density}dp", field.right / density <= window - margin + 1f)
    }
}
