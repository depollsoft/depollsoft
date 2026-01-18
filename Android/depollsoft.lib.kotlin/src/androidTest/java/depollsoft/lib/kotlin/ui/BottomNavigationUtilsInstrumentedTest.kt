package depollsoft.lib.kotlin.ui

import androidx.test.core.app.ActivityScenario
import org.junit.Test
import org.junit.Assert.assertEquals

class BottomNavigationUtilsInstrumentedTest {

    @Test
    fun attachToViewPager_updates_selection_and_pages() {
        ActivityScenario.launch(TestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val bottomNav = activity.bottomNav
                val viewPager = activity.viewPager

                val menu = bottomNav.menu
                menu.add(0, 1, 0, "First")
                menu.add(0, 2, 1, "Second")

                bottomNav.attachToViewPager(viewPager) { }

                bottomNav.selectedItemId = 1
                assertEquals(0, viewPager.currentItem)

                bottomNav.selectedItemId = 2
                assertEquals(1, viewPager.currentItem)
            }
        }
    }
}
