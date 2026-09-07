package depollsoft.tagmaster

import android.view.View
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.navigationrail.NavigationRailView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The desk's navigation contract: three destinations, the control and the pager
 * always agreeing, and the right control for the screen width.
 *
 * These assertions are deliberately unconditional — a missing view or a
 * navigation control that does not move the pager is a failure, not a skip.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestDeskApplication::class, sdk = [35])
class DeskNavigationTest {

    private fun launchDesk(): MeActivity =
        Robolectric.buildActivity(MeActivity::class.java).setup().get()

    @Test
    fun desk_has_three_destinations() {
        val activity = launchDesk()
        val pager = activity.findViewById<ViewPager2>(R.id.viewPager)
        val nav = activity.findViewById<NavigationBarView>(R.id.bottomNavigation)

        assertEquals(MeActivity.DESTINATIONS, pager.adapter!!.itemCount)
        assertEquals(MeActivity.DESTINATIONS, nav.menu.size())
        assertEquals(R.id.home, nav.menu.getItem(MeActivity.HOME).itemId)
        assertEquals(R.id.browse, nav.menu.getItem(MeActivity.BROWSE).itemId)
        assertEquals(R.id.search, nav.menu.getItem(MeActivity.SEARCH).itemId)
    }

    @Test
    fun selecting_a_destination_moves_the_pager() {
        val activity = launchDesk()
        val pager = activity.findViewById<ViewPager2>(R.id.viewPager)
        val nav = activity.findViewById<NavigationBarView>(R.id.bottomNavigation)

        nav.selectedItemId = R.id.search
        assertEquals(MeActivity.SEARCH, pager.currentItem)

        nav.selectedItemId = R.id.browse
        assertEquals(MeActivity.BROWSE, pager.currentItem)

        nav.selectedItemId = R.id.home
        assertEquals(MeActivity.HOME, pager.currentItem)
    }

    @Test
    fun moving_the_pager_moves_the_navigation_control() {
        val activity = launchDesk()
        val pager = activity.findViewById<ViewPager2>(R.id.viewPager)
        val nav = activity.findViewById<NavigationBarView>(R.id.bottomNavigation)

        pager.setCurrentItem(MeActivity.SEARCH, false)
        assertEquals(R.id.search, nav.selectedItemId)

        pager.setCurrentItem(MeActivity.BROWSE, false)
        assertEquals(R.id.browse, nav.selectedItemId)
    }

    @Test
    fun search_request_lands_on_the_search_destination() {
        val activity = launchDesk()
        val pager = activity.findViewById<ViewPager2>(R.id.viewPager)

        assertTrue(activity.onSearchRequested())
        assertEquals(MeActivity.SEARCH, pager.currentItem)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun compact_width_uses_a_navigation_bar() {
        val activity = launchDesk()
        val nav = activity.findViewById<View>(R.id.bottomNavigation)

        assertTrue(
            "compact width should use a bottom navigation bar, was ${nav.javaClass.simpleName}",
            nav is BottomNavigationView,
        )
    }

    @Test
    @Config(qualifiers = "w840dp-h1024dp")
    fun expanded_width_uses_a_navigation_rail() {
        val activity = launchDesk()
        val nav = activity.findViewById<View>(R.id.bottomNavigation)

        assertTrue(
            "expanded width should use a navigation rail, was ${nav.javaClass.simpleName}",
            nav is NavigationRailView,
        )
        // The rail is persistent: it is laid out beside the workspace, not over it.
        assertNotNull(activity.findViewById<View>(R.id.viewPager))
        assertEquals(View.VISIBLE, nav.visibility)
    }

    @Test
    @Config(qualifiers = "w840dp-h1024dp")
    fun the_rail_still_drives_the_pager() {
        val activity = launchDesk()
        val pager = activity.findViewById<ViewPager2>(R.id.viewPager)
        val nav = activity.findViewById<NavigationBarView>(R.id.bottomNavigation)

        nav.selectedItemId = R.id.search
        assertEquals(MeActivity.SEARCH, pager.currentItem)
    }
}
