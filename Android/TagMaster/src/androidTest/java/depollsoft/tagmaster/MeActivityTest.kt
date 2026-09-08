package depollsoft.tagmaster

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import depollsoft.tagmaster.NavigationTestFixture.Companion.onResumed
import depollsoft.tagmaster.NavigationTestFixture.Companion.selectTab
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Home is one recycling list (header, favorites, footer) with explicit activity navigation, not a pager. */
@RunWith(AndroidJUnit4::class)
@LargeTest
class MeActivityTest {
    @get:Rule val activityRule = ActivityScenarioRule(MeActivity::class.java)

    @Before fun dismissFirstRunChangelog() {
        // This prompt is optional; all screen assertions below are unconditional.
        try {
            onView(withId(android.R.id.button1)).perform(click())
        } catch (_: NoMatchingViewException) {
            // No first-run prompt on an existing installation.
        }
    }

    @Test fun testActivityLaunches() {
        onResumed<MeActivity> { assertEquals(it.getString(R.string.home_title), it.supportActionBar!!.title.toString()) }
    }

    @Test fun testHomeScrollIsDisplayed() {
        onView(withId(R.id.homeList)).check(matches(isDisplayed()))
    }

    @Test fun testSearchActionIsDisplayed() {
        onView(withId(R.id.searchButton)).check(matches(isDisplayed()))
    }

    @Test fun testSearchActionOpensSearch() {
        onView(withId(R.id.searchButton)).perform(click())
        onResumed<TagSearchActivity> { assertNotNull(it.model) }
        onView(withId(R.id.searchTextBox)).check(matches(isDisplayed()))
    }

    @Test fun testBrowseActionOpensBrowse() {
        browse()
        NavigationTestFixture.assertPage(0)
        onView(withId(R.id.viewPager)).check(matches(isDisplayed()))
    }

    @Test fun testBrowseBackReturnsHome() {
        browse()
        pressBack()
        onResumed<MeActivity> { assertFalse(it.isFinishing) }
        onView(withId(R.id.browseButton)).check(matches(isDisplayed()))
    }

    @Test fun testFavoritesItemsControlExists() {
        assertFavoriteRows()
    }

    private fun assertFavoriteRows() {
        // Home is one RecyclerView: header, one recycled row per favorite, footer.
        onResumed<MeActivity> {
            val list = it.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.homeList)
            assertNotNull(list.adapter)
            assertEquals(it.favoriteIds.size, it.favoritesAdapter.itemCount)
            assertEquals(it.favoriteIds.size + 2, list.adapter!!.itemCount)
        }
        onView(withId(R.id.browseButton)).check(matches(isDisplayed()))
    }

    @Test fun testTeachableTagsItemsControlExists() {
        onView(withId(R.id.teachableButton)).perform(click())
        onResumed<TeachableTagsActivity> {
            assertNotNull(it.findViewById<android.view.View>(R.id.teachableTagsItemsControl))
            assertEquals(it.teachableTags.isEmpty(), it.findViewById<android.view.View>(R.id.teachableEmptyState).isShown)
        }
    }

    @Test fun testClearCacheButtonExists() {
        settingsField(R.id.clearCacheButton)
    }

    @Test fun testClearFavoritesButtonExists() {
        settingsField(R.id.clearFavoritesButton)
    }

    @Test fun testChangelogButtonExists() {
        settingsField(R.id.changelogButton)
    }

    @Test fun testAppVersionTextViewExists() {
        homeField(R.id.appVersionTextView)
    }

    @Test fun testAppNameTextViewExists() {
        homeField(R.id.appNameTextView)
    }

    @Test fun testCopyrightTextViewExists() {
        homeField(R.id.copyrightTextView)
    }

    @Test fun testThemeTitleTextViewExists() {
        settingsField(R.id.themeTitleTextView)
    }

    @Test fun testSheetMusicWakeLockSwitchExists() {
        settingsField(R.id.sheetMusicWakeLockCheckBox)
    }

    @Test fun testLearningTracksSpinnerExists() {
        settingsField(R.id.learningTracksSpinner)
    }

    @Test fun testBrowseTabsAndUpReturnHome() {
        browse()
        selectTab(R.string.Rating, 1)
        selectTab(R.string.Downloads, 2)
        selectTab(R.string.classic, 3)
        selectTab(R.string.latest, 0)
        onView(withContentDescription(androidx.appcompat.R.string.abc_action_bar_up_description)).perform(click())
        onResumed<MeActivity> { assertFalse(it.isFinishing) }
    }

    @Test fun testActivitySurvivesRecreation() {
        var favorites: List<Int> = emptyList()
        activityRule.scenario.onActivity { favorites = it.favoriteIds.toList() }
        activityRule.scenario.recreate()
        activityRule.scenario.onActivity { assertEquals(favorites, it.favoriteIds.toList()) }
        assertFavoriteRows()
    }

    @Test fun testSearchNavigationAfterRecreation() {
        activityRule.scenario.recreate()
        onView(withId(R.id.searchButton)).perform(click())
        onResumed<TagSearchActivity> { assertNotNull(it.findViewById<android.view.View>(R.id.searchTextBox)) }
    }

    @Test fun testBrowseActionIsEnabled() {
        onView(withId(R.id.browseButton)).check(matches(isEnabled()))
    }

    @Test fun testSearchActionIsEnabled() {
        onView(withId(R.id.searchButton)).check(matches(isEnabled()))
    }

    private fun browse() {
        onView(withId(R.id.browseButton)).perform(click())
        onResumed<TagBrowserActivity> { assertFalse(it.isFinishing) }
    }

    private fun homeField(id: Int) {
        onView(withId(R.id.homeList))
            .perform(RecyclerViewActions.scrollTo<androidx.recyclerview.widget.RecyclerView.ViewHolder>(hasDescendant(withId(id))))
        onView(withId(id)).check(matches(isDisplayed()))
    }

    private fun settingsField(id: Int) {
        onView(withId(R.id.settingsMenuItem)).perform(click())
        onResumed<SettingsActivity> { assertFalse(it.isFinishing) }
        onView(withId(id)).perform(scrollTo()).check(matches(isDisplayed()))
    }
}
