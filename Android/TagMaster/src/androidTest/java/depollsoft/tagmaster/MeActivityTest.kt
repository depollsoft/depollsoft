package depollsoft.tagmaster

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.navigationrail.NavigationRailView
import org.hamcrest.Matchers.allOf
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Local shell checks: catalog responses are deliberately not part of these assertions. */
@RunWith(AndroidJUnit4::class)
class MeActivityTest : SingingDeskTest() {
    @Test
    fun homeActionsAreLabeledAndEnabled() {
        listOf(
            R.id.findTagButton to R.string.FindATag,
            R.id.randomTagButton to R.string.RandomTag,
            R.id.openByIdButton to R.string.OpenTagId,
        ).forEach { (id, label) ->
            onView(withId(id))
                .perform(scrollTo())
                .check(matches(allOf(isDisplayed(), isEnabled(), isClickable(), withText(label))))
        }
    }

    @Test
    fun findTagSelectsNativeSearch() {
        onView(withId(R.id.findTagButton)).perform(scrollTo(), click())
        onView(withId(R.id.searchTextBox)).check(matches(isDisplayed()))
        assertDestination(MeActivity.SEARCH, R.id.search)
    }

    @Test
    fun nativeDestinationsSwitchAndReturnHome() {
        onView(withId(R.id.browse)).perform(click())
        onView(withId(R.id.browseTabs)).check(matches(isDisplayed()))
        assertDestination(MeActivity.BROWSE, R.id.browse)
        onView(withId(R.id.search)).perform(click())
        onView(withId(R.id.searchTextBox)).check(matches(isDisplayed()))
        assertDestination(MeActivity.SEARCH, R.id.search)
        onView(withId(R.id.home)).perform(click())
        onView(withId(R.id.findTagButton)).check(matches(isDisplayed()))
        assertDestination(MeActivity.HOME, R.id.home)
    }

    @Test
    fun onlyTheVisibleBrowsePageContributesRefresh() {
        onView(withId(R.id.browse)).perform(click())
        EspressoTestUtils.waitForPagerIdle(R.id.viewPager)
        onView(withId(R.id.browseTabs)).check(matches(isDisplayed()))
        EspressoTestUtils.waitForView(withId(R.id.refreshMenuItem))
        onView(withId(R.id.search)).perform(click())
        EspressoTestUtils.waitForPagerIdle(R.id.viewPager)
        onView(withId(R.id.searchTextBox)).check(matches(isDisplayed()))
        onView(withId(R.id.refreshMenuItem)).check(
            androidx.test.espresso.assertion.ViewAssertions
                .doesNotExist(),
        )
    }

    @Test
    fun searchSelectionSurvivesRecreation() {
        onView(withId(R.id.search)).perform(click())
        assertDestination(MeActivity.SEARCH, R.id.search)
        scenario.recreate()
        onView(withId(R.id.searchTextBox)).check(matches(isDisplayed()))
        assertDestination(MeActivity.SEARCH, R.id.search)
    }

    @Test
    fun navigationTypeMatchesCurrentWindowWidth() {
        scenario.onActivity { activity ->
            val navigation = activity.findViewById<NavigationBarView>(R.id.bottomNavigation)
            // w600dp follows available width, not smallestScreenWidthDp or device identity.
            if (activity.resources.configuration.screenWidthDp >= 600) {
                assertTrue("Expanded windows require a navigation rail", navigation is NavigationRailView)
            } else {
                assertTrue("Compact windows require bottom navigation", navigation is BottomNavigationView)
            }
            assertEquals(3, navigation.menu.size())
            assertEquals(
                listOf(R.id.home, R.id.browse, R.id.search),
                (0 until navigation.menu.size()).map { navigation.menu.getItem(it).itemId },
            )
        }
    }

    @Test
    fun settingsUtilityOpensAndBackReturnsToSelectedDestination() {
        onView(withId(R.id.search)).perform(click())
        onView(withId(R.id.settingsMenuItem)).perform(click())
        onView(withId(R.id.settingsRoot)).check(matches(isDisplayed()))
        onView(withId(R.id.radio_system)).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withId(R.id.clearCacheButton))
            .perform(scrollTo())
            .check(matches(allOf(isDisplayed(), isEnabled())))
        pressBack()
        onView(withId(R.id.searchTextBox)).check(matches(isDisplayed()))
        assertDestination(MeActivity.SEARCH, R.id.search)
    }
}
