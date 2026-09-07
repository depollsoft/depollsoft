package depollsoft.tagmaster

import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.util.TreeIterables
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.navigation.NavigationBarView
import org.junit.After
import org.junit.Assert.*
import org.junit.Before

/** Shared launch only. Never waits for catalog data or changes saved lists. */
abstract class SingingDeskTest {
    protected lateinit var scenario: ActivityScenario<MeActivity>

    @Before
    fun launchDesk() {
        scenario = ActivityScenario.launch(MeActivity::class.java)
        // The changelog appears once per app version, including on clean test installs.
        // Inspect the current root rather than catching failed Espresso assertions.
        onView(isRoot()).check { root, missing ->
            assertNull(missing)
            val ok = root.findViewById<Button>(android.R.id.button1)
            if (ok != null) {
                assertTrue(
                    "Only the launch changelog may be dismissed by the fixture",
                    TreeIterables.breadthFirstViewTraversal(root).any {
                        it is TextView && it.text.toString() == "Tag Master Changelog"
                    },
                )
                assertEquals("OK", ok.text.toString())
                assertNotNull(root.findViewById<View>(android.R.id.message))
                ok.performClick()
            }
        }
        assertDestination(MeActivity.HOME, R.id.home)
    }

    @After
    fun closeDesk() {
        if (::scenario.isInitialized) scenario.close()
    }

    protected fun assertDestination(
        page: Int,
        menuId: Int,
    ) {
        val rootId =
            when (page) {
                MeActivity.HOME -> R.id.deskRoot
                MeActivity.BROWSE -> R.id.browseCollectionSpinner
                MeActivity.SEARCH -> R.id.searchTextBox
                else -> error("Unknown route $page")
            }
        onView(withId(rootId)).check(matches(isDisplayed()))
        onView(isRoot()).check { root, missing ->
            assertNull(missing)
            assertFalse(
                TreeIterables.breadthFirstViewTraversal(root).any {
                    it is NavigationBarView || it is ViewPager2
                },
            )
        }
    }
}
