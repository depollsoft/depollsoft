package depollsoft.tagmaster

import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.navigation.NavigationBarView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestDeskApplication::class, sdk = [35])
class DeskNavigationTest {
    private fun launch() = Robolectric.buildActivity(MeActivity::class.java).setup().get()

    private fun assertNoNavigation(view: View) {
        assertFalse(view is ViewPager2)
        assertFalse(view is NavigationBarView)
        if (view is ViewGroup) for (i in 0 until view.childCount) assertNoNavigation(view.getChildAt(i))
    }

    @Test fun homeHostsOnlyHome() {
        val activity = launch()
        assertEquals(1, activity.supportFragmentManager.fragments.size)
        assertTrue(activity.supportFragmentManager.fragments.single() is HomeFragment)
        assertTrue(activity.findViewById<View>(R.id.deskRoot).background is PoleBackground)
        assertNoNavigation(activity.findViewById(R.id.deskRoot))
    }

    @Test fun findPushesSearch() {
        val activity = launch()
        activity.findViewById<View>(R.id.findTagButton).performClick()
        assertEquals(TagSearchActivity::class.java.name, shadowOf(activity).nextStartedActivity.component!!.className)
    }

    @Test fun browsePushesCatalog() {
        val activity = launch()
        activity.findViewById<View>(R.id.browseTagButton).performClick()
        assertEquals(TagBrowserActivity::class.java.name, shadowOf(activity).nextStartedActivity.component!!.className)
    }

    @Test fun systemSearchPushesSearch() {
        val activity = launch()
        assertTrue(activity.onSearchRequested())
        assertEquals(TagSearchActivity::class.java.name, shadowOf(activity).nextStartedActivity.component!!.className)
    }

    @Test
    @Config(qualifiers = "w840dp-h1024dp")
    fun tabletHasNoGlobalRail() {
        assertNoNavigation(launch().findViewById(R.id.deskRoot))
    }

    @Test fun materialPushAndBackKeepSummaryInstance() {
        val activity = Robolectric.buildActivity(TagDetailActivity::class.java).setup().get()
        val summary = activity.supportFragmentManager.findFragmentByTag("summary")
        activity.openMaterial("tracks")
        assertTrue(activity.supportFragmentManager.findFragmentByTag("tracks") is TagTracksFragment)
        assertEquals(View.GONE, activity.findViewById<View>(R.id.summaryContainer).visibility)
        activity.onBackPressedDispatcher.onBackPressed()
        assertSame(summary, activity.supportFragmentManager.findFragmentByTag("summary"))
        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.summaryContainer).visibility)
        assertFalse(activity.hasOpenMaterial)
    }

    @Test
    @Config(qualifiers = "w840dp-h1024dp")
    fun tabletUsesTwoPanes() {
        val activity = Robolectric.buildActivity(TagDetailActivity::class.java).setup().get()
        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.summaryContainer).visibility)
        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.materialContainer).visibility)
        assertNoNavigation(activity.findViewById(R.id.detailRoot))
    }

    @Test
    @Config(qualifiers = "w840dp-h1024dp", fontScale = 2f)
    fun accessibilityUsesOneColumn() {
        val activity = Robolectric.buildActivity(TagDetailActivity::class.java).setup().get()
        assertEquals(View.GONE, activity.findViewById<View>(R.id.materialContainer).visibility)
        activity.openMaterial("tracks")
        assertEquals(View.GONE, activity.findViewById<View>(R.id.summaryContainer).visibility)
    }
}
