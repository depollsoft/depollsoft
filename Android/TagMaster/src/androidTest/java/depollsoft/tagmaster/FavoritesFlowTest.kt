package depollsoft.tagmaster

import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Home repertoire structure. Does not clear user data or depend on fetched tag rows. */
@RunWith(AndroidJUnit4::class)
class FavoritesFlowTest : SingingDeskTest() {
    @Test
    fun favoritesHeadingIsReachableOnHome() {
        onView(withId(R.id.favoritesHeading))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        onView(withId(R.id.favoritesHeading)).check(matches(withText(R.string.Favorites)))
        scenario.onActivity { activity ->
            assertNotNull(activity.findViewById<View>(R.id.favoritesItemsControl))
            val empty = activity.findViewById<View>(R.id.favoritesEmptyTextView)
            assertEquals(
                if (FavoritesModel.favoriteIds.isEmpty()) View.VISIBLE else View.GONE,
                empty.visibility,
            )
        }
    }

    @Test
    fun teachableSectionFollowsSavedRepertoire() {
        scenario.onActivity { activity ->
            val section = activity.findViewById<View>(R.id.teachableSection)
            assertNotNull(section.findViewById<View>(R.id.homeTeachableItemsControl))
            assertNotNull(section.findViewById<View>(R.id.teachableButton))
            assertEquals(View.VISIBLE, section.visibility)
            assertEquals(
                if (TeachableTagsModel.teachableTagIds.isEmpty()) View.VISIBLE else View.GONE,
                section.findViewById<View>(R.id.teachableEmptyTextView).visibility,
            )
        }
    }
}
