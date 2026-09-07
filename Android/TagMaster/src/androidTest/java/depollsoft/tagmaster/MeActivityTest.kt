package depollsoft.tagmaster

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matchers.allOf
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MeActivityTest : SingingDeskTest() {
    @Test fun homeActionsAreLabeledAndEnabled() {
        listOf(
            R.id.findTagButton to R.string.FindATag,
            R.id.browseTagButton to R.string.BrowseTags,
            R.id.randomTagButton to R.string.RandomTag,
            R.id.openByIdButton to R.string.OpenTagId,
        ).forEach { (id, label) ->
            onView(withId(id)).perform(scrollTo()).check(matches(allOf(isDisplayed(), isEnabled(), withText(label))))
        }
    }

    @Test fun findPushesSearchAndBackReturnsHome() {
        onView(withId(R.id.findTagButton)).perform(scrollTo(), click())
        assertDestination(MeActivity.SEARCH, R.id.search)
        pressBack()
        assertDestination(MeActivity.HOME, R.id.home)
    }

    @Test fun browsePushesCatalogAndBackReturnsHome() {
        onView(withId(R.id.browseTagButton)).perform(scrollTo(), click())
        assertDestination(MeActivity.BROWSE, R.id.browse)
        pressBack()
        assertDestination(MeActivity.HOME, R.id.home)
    }

    @Test fun homeSurvivesRecreationWithoutGlobalNavigation() {
        scenario.recreate()
        assertDestination(MeActivity.HOME, R.id.home)
    }

    @Test fun settingsUtilityReturnsHome() {
        onView(withId(R.id.settingsMenuItem)).perform(click())
        onView(withId(R.id.settingsRoot)).check(matches(isDisplayed()))
        onView(withId(R.id.clearCacheButton)).perform(scrollTo()).check(matches(isEnabled()))
        pressBack()
        assertDestination(MeActivity.HOME, R.id.home)
    }
}
