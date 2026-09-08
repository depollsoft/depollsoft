package depollsoft.tagmaster

import android.app.Application
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class BottomTabsLayoutTest {
    @Test fun phone_tabs_stay_below_content() = checkLayouts(393, 852)

    @Test
    @Config(qualifiers = "land")
    fun landscape_tabs_stay_below_content() = checkLayouts(852, 393)

    @Test
    @Config(qualifiers = "sw600dp-night")
    fun expanded_dark_tabs_stay_below_content() = checkLayouts(800, 1280)

    private fun checkLayouts(
        widthDp: Int,
        heightDp: Int,
    ) {
        for (layout in listOf(R.layout.tagmasterview, R.layout.tagdetailview)) {
            val controller = Robolectric.buildActivity(AppCompatActivity::class.java)
            val activity = controller.get()
            activity.setTheme(R.style.AppTheme)
            controller.setup()
            try {
                activity.setContentView(layout)
                val content = activity.findViewById<ViewGroup>(android.R.id.content)
                val root = content.getChildAt(0) as ViewGroup
                val tabs = root.findViewById<TabLayout>(R.id.tabLayout)
                val pager = root.findViewById<ViewPager2>(R.id.viewPager)
                // Detail keeps its existing hidden-until-loaded behavior.
                assertEquals(if (layout == R.layout.tagdetailview) View.GONE else View.VISIBLE, tabs.visibility)
                tabs.visibility = View.VISIBLE
                val labels =
                    if (layout == R.layout.tagmasterview) {
                        listOf("Latest", "Rating", "Downloads", "Classic")
                    } else {
                        listOf("Summary", "Details", "Tracks", "Videos")
                    }
                labels.forEach { tabs.addTab(tabs.newTab().setText(it).setIcon(R.drawable.ic_tracks)) }
                val density = activity.resources.displayMetrics.density
                val bottomInset = (24 * density).toInt()
                // RichApplication owns the system-bar inset; children must not overlap it.
                content.setPadding(0, (24 * density).toInt(), 0, bottomInset)
                val width = (widthDp * density).toInt()
                val height = (heightDp * density).toInt()
                content.measure(
                    View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
                )
                content.layout(0, 0, width, height)

                fun bounds(view: View): Rect =
                    Rect(0, 0, view.width, view.height).also {
                        content.offsetDescendantRectToMyCoords(view, it)
                    }
                val tabBounds = bounds(tabs)
                assertSame(tabs, root.getChildAt(root.childCount - 1))
                assertEquals(height - bottomInset, tabBounds.bottom)
                assertTrue("Pager clears bottom tabs", bounds(pager).bottom <= tabBounds.top)
                assertTrue("Touch targets remain at least 48dp", tabs.height >= 48 * density)
                assertEquals(TabLayout.MODE_AUTO, tabs.tabMode)
                assertEquals(TabLayout.INDICATOR_GRAVITY_TOP, tabs.tabIndicatorGravity)
                root.findViewById<View>(R.id.searchButton)?.let {
                    assertTrue("Search stays above the tabs", bounds(it).bottom <= tabBounds.top)
                }
                tabs.selectTab(tabs.getTabAt(2))
                assertEquals(2, tabs.selectedTabPosition)
            } finally {
                controller.pause().stop().destroy()
            }
        }
    }
}
