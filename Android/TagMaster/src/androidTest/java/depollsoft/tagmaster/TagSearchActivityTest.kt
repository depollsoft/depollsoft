package depollsoft.tagmaster

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matchers.allOf
import org.junit.Test
import org.junit.runner.RunWith

/** Search form coverage uses the native Search destination, never a guessed pager swipe. */
@RunWith(AndroidJUnit4::class)
class TagSearchActivityTest : SingingDeskTest() {
    @Test
    fun searchFieldAcceptsTitleOrLyrics() {
        onView(withId(R.id.search)).perform(click())
        onView(withId(R.id.searchTextBox))
            .perform(replaceText("ring a chord"), closeSoftKeyboard())
            .check(matches(withText("ring a chord")))
        onView(withId(R.id.searchButton)).check(matches(allOf(isDisplayed(), isEnabled(), isClickable())))
        assertDestination(MeActivity.SEARCH, R.id.search)
    }

    @Test
    fun submitOpensResultsAndBackKeepsTheQuery() {
        onView(withId(R.id.search)).perform(click())
        onView(withId(R.id.searchTextBox)).perform(replaceText("ring a chord"), closeSoftKeyboard())
        onView(withId(R.id.searchButton)).perform(click())
        onView(withId(R.id.resultsRoot)).check(matches(isDisplayed()))
        androidx.test.espresso.Espresso
            .pressBack()
        onView(withId(R.id.searchTextBox)).check(matches(withText("ring a chord")))
        assertDestination(MeActivity.SEARCH, R.id.search)
    }

    @Test
    fun searchFiltersRemainReachableByScrolling() {
        onView(withId(R.id.search)).perform(click())
        // ViewPager can still be settling after Espresso's click action returns.
        EspressoTestUtils.waitForPagerIdle(R.id.viewPager)
        EspressoTestUtils.waitForView(allOf(withId(R.id.searchTextBox), isCompletelyDisplayed()))
        listOf(
            R.id.sortBySpinner,
            R.id.sheetMusicSpinner,
            R.id.learningTracksSpinner,
            R.id.partsSpinner,
            R.id.tagCollectionSpinner,
        ).forEach { id ->
            onView(withId(id))
                .perform(scrollTo())
                .check(matches(allOf(isDisplayed(), isEnabled(), isClickable())))
        }
    }
}
