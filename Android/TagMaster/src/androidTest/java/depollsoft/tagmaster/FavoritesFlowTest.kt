package depollsoft.tagmaster

import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Real peer destinations, never dummy feeds on Home. */
@RunWith(AndroidJUnit4::class)
class FavoritesFlowTest : SingingDeskTest() {
    @Test fun favoritesEntryOpensDedicatedListAndBack() {
        scenario.onActivity { assertNull(it.findViewById<View>(R.id.favoritesItemsControl)) }
        onView(withId(R.id.favoritesButton)).perform(scrollTo(), click())
        onView(withId(R.id.favoritesRoot)).check(matches(isDisplayed()))
        onView(withText(R.string.Favorites)).check(matches(isDisplayed()))
        pressBack()
        onView(withId(R.id.teachableButton)).check(matches(isDisplayed()))
    }

    @Test fun teachableEntryOpensExistingListAndBack() {
        onView(withId(R.id.teachableButton)).perform(scrollTo(), click())
        onView(withId(R.id.teachableRoot)).check(matches(isDisplayed()))
        onView(withText(R.string.TeachableTags)).check(matches(isDisplayed()))
        pressBack()
        onView(withId(R.id.favoritesButton)).check(matches(isDisplayed()))
    }
}
