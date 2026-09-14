package depollsoft.tagmaster

import android.view.View
import android.widget.EditText
import android.widget.RatingBar
import androidx.appcompat.app.AlertDialog
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import depollsoft.tagmaster.barbershop.TagSortOptions
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PolishFlowRegressionTest {
    @Test fun landscape_tracks_can_scroll_to_and_select_bass() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val location = depollsoft.tagmaster.barbershop.RemoteLocation().apply {
            uri = "https://example.com/bass.mp3"
            type = "mp3"
        }
        depollsoft.tagmaster.barbershop.Tag().apply {
            id = 2147483001
            title = "Landscape regression"
            recordingMethod = "A recording note long enough to use space in a short viewport."
            bassTrackUri = location
            tenorTrackUri = location
            leadTrackUri = location
            baritoneTrackUri = location
        }.cache()
        val intent = android.content.Intent(context, TagDetailActivity::class.java)
            .putExtra(TagDetailActivity.TAG_ID_EXTRA, 2147483001)
        ActivityScenario.launch<TagDetailActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                activity.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager).setCurrentItem(2, false)
            }
            try {
                EspressoTestUtils.shortWait(700) // Allow the requested orientation to settle.
                onView(withId(R.id.bassButton)).perform(scrollTo())
                EspressoTestUtils.shortWait(200)
                onView(withId(R.id.bassButton)).perform(click()).check(matches(isChecked()))
                scenario.onActivity { activity ->
                    assertEquals(location.uri, activity.findViewById<MediaPlayerView>(R.id.mediaPlayer).remoteLocation.uri)
                }
            } finally {
                scenario.onActivity { it.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
            }
        }
    }

    @Test fun search_preserves_query_and_filter_after_recreation() {
        ActivityScenario.launch(TagSearchActivity::class.java).use { scenario ->
            onView(withId(R.id.searchTextBox)).perform(replaceText("love"), closeSoftKeyboard())
            onView(withId(R.id.sortBySpinner)).perform(click())
            onView(withText("Rating")).inRoot(androidx.test.espresso.matcher.RootMatchers.isPlatformPopup()).perform(click())
            scenario.onActivity { activity -> assertEquals(TagSortOptions.Rating, activity.model.sortBy) }
            scenario.recreate()
            scenario.onActivity { activity ->
                assertEquals("love", activity.model.query)
                assertEquals(TagSortOptions.Rating, activity.model.sortBy)
                assertEquals(activity.resources.getStringArray(R.array.SortByChoices)[3],
                    activity.findViewById<MaterialAutoCompleteTextView>(R.id.sortBySpinner).text.toString())
                assertTrue(activity.findViewById<View>(R.id.toolbar).isShown)
            }
        }
    }

    @Test fun home_exposes_teachable_and_rejects_invalid_open_id() {
        ActivityScenario.launch(MeActivity::class.java).use { scenario ->
            // The changelog is a documented first-install prompt, not the assertion under test.
            try { onView(withId(android.R.id.button1)).perform(click()) } catch (_: NoMatchingViewException) { }
            onView(withId(R.id.teachableButton)).check(matches(isDisplayed()))
            onView(withId(R.id.openByIdButton)).perform(click())
            onView(withId(R.id.openTagIdInput)).perform(replaceText("2147483648"))
            onView(withId(android.R.id.button1)).perform(click())
            onView(withText(R.string.home_invalid_tag_id)).check(matches(isDisplayed()))
            onView(withId(android.R.id.button2)).perform(click())
            onView(withId(R.id.teachableButton)).perform(click())
            onView(withId(R.id.teachableEmptyState)).check(matches(isDisplayed()))
        }
    }

    @Test fun rating_cancel_does_not_submit_and_explicit_selection_can_submit() {
        ActivityScenario.launch(TagSearchActivity::class.java).use { scenario ->
            var popup: RatingsPopup? = null
            scenario.onActivity { activity -> popup = RatingsPopup(activity).also { it.show() } }
            onView(withId(android.R.id.button1)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog())
                .check(matches(org.hamcrest.Matchers.not(isEnabled())))
            onView(withId(android.R.id.button2)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(click())
            onView(withId(R.id.ratingBar1)).check(androidx.test.espresso.assertion.ViewAssertions.doesNotExist())
            assertNull(popup!!.rating)
            scenario.onActivity { activity -> popup = RatingsPopup(activity).also { it.show() } }
            onView(withId(R.id.ratingBar1)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(object : androidx.test.espresso.ViewAction {
                override fun getConstraints() = isAssignableFrom(RatingBar::class.java)
                override fun getDescription() = "Choose four stars"
                override fun perform(controller: androidx.test.espresso.UiController, view: View) {
                    (view as RatingBar).rating = 4f
                }
            })
            onView(withId(android.R.id.button1)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(click())
            assertEquals(4, popup!!.rating)
        }
    }
}
