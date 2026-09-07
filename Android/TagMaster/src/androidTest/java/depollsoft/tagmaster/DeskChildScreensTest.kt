package depollsoft.tagmaster

import android.content.Intent
import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

/** Real child activities and stack behavior, independent of catalog responses. */
@RunWith(AndroidJUnit4::class)
class DeskChildScreensTest : SingingDeskTest() {
    @Test
    fun teachableEntryOpensEvenWhenEmptyAndUpReturnsHome() {
        onView(withId(R.id.teachableButton)).perform(scrollTo(), click())
        onView(withId(R.id.teachableRoot)).check(matches(isDisplayed()))
        onView(withContentDescription(androidx.appcompat.R.string.abc_action_bar_up_description)).perform(click())
        assertDestination(MeActivity.HOME, R.id.home)
    }

    @Test
    fun settingsUpPreservesSearchSelection() {
        onView(withId(R.id.settingsMenuItem)).perform(click())
        onView(withId(R.id.settingsRoot)).check(matches(isDisplayed()))
        onView(withContentDescription(androidx.appcompat.R.string.abc_action_bar_up_description)).perform(click())
        assertDestination(MeActivity.HOME, R.id.home)
    }

    @Test
    fun standaloneSearchBrowseAndResultsHaveWorkingUp() {
        val screens =
            listOf(
                TagSearchActivity::class.java to R.id.pageContainer,
                TagBrowserActivity::class.java to R.id.browseCollectionSpinner,
                TagSearchResultsActivity::class.java to R.id.resultsRoot,
                TagQueryActivity::class.java to R.id.resultsRoot,
                TagDetailActivity::class.java to R.id.detailError,
            )
        for ((screen, root) in screens) {
            scenario.onActivity { it.startActivity(Intent(it, screen)) }
            onView(withId(root)).check(matches(isDisplayed()))
            onView(withContentDescription(androidx.appcompat.R.string.abc_action_bar_up_description)).perform(click())
            assertDestination(MeActivity.HOME, R.id.home)
        }
    }

    @Test
    fun webBrowserInsetsAndSystemBackReturnToDesk() {
        scenario.onActivity {
            it.startActivity(
                Intent(it, TagMasterBrowserActivity::class.java)
                    .putExtra(depollsoft.lib.activity.BrowserActivity.HTML_DATA_EXTRA, "<h1>Terms test</h1>"),
            )
        }
        onView(isAssignableFrom(android.webkit.WebView::class.java)).check(matches(isDisplayed()))
        onView(withId(android.R.id.content)).check { view, missing ->
            org.junit.Assert.assertNull(missing)
            val insets =
                androidx.core.view.ViewCompat
                    .getRootWindowInsets(view)!!
                    .getInsets(
                        androidx.core.view.WindowInsetsCompat.Type
                            .systemBars(),
                    )
            org.junit.Assert.assertTrue(view.paddingTop >= insets.top)
            org.junit.Assert.assertTrue(view.paddingBottom >= insets.bottom)
        }
        pressBack()
        assertDestination(MeActivity.HOME, R.id.home)
    }
}
