package depollsoft.tagmaster

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.*
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.textfield.TextInputEditText
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Tests the Home-to-detail intent boundary without fetching a live tag or playing audio. */
@RunWith(AndroidJUnit4::class)
class TagDetailActivityTest : SingingDeskTest() {
    @Before
    fun interceptDetailLaunches() {
        Intents.init()
        Intents
            .intending(hasComponent(TagDetailActivity::class.java.name))
            .respondWith(ActivityResult(Activity.RESULT_CANCELED, null))
    }

    @After
    fun releaseIntents() {
        Intents.release()
    }

    @Test
    fun validTagIdIsPassedToDetail() {
        openIdDialog()
        enterId("123")
        onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click())
        Intents.intended(
            allOf(
                hasComponent(TagDetailActivity::class.java.name),
                hasExtra(TagDetailActivity.TAG_ID_EXTRA, 123),
            ),
        )
        Intents.assertNoUnverifiedIntents()
        assertDestination(MeActivity.HOME, R.id.home)
    }

    @Test
    fun blankTagIdDoesNotLaunchDetail() {
        openIdDialog()
        onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click())
        assertInvalidIdIsStillEditable()
    }

    @Test
    fun overflowingTagIdDoesNotLaunchDetail() {
        openIdDialog()
        enterId("999999999999999999999999")
        onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click())
        assertInvalidIdIsStillEditable()
    }

    @Test
    fun zeroTagIdDoesNotLaunchDetail() {
        openIdDialog()
        enterId("0")
        onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click())
        assertInvalidIdIsStillEditable()
    }

    private fun assertInvalidIdIsStillEditable() {
        onView(withText(R.string.InvalidTagId)).inRoot(isDialog()).check(matches(isDisplayed()))
        Intents.assertNoUnverifiedIntents()
        onView(withId(android.R.id.button2)).inRoot(isDialog()).perform(click())
        assertDestination(MeActivity.HOME, R.id.home)
    }

    @Test
    fun cancelDoesNotOpenEnteredTag() {
        openIdDialog()
        enterId("123")
        onView(withId(android.R.id.button2)).inRoot(isDialog()).perform(click())
        assertDialogDismissedWithoutLaunch()
    }

    @Test
    fun backDismissesIdDialogWithoutLeavingHome() {
        openIdDialog()
        enterId("123")
        pressBack()
        assertDialogDismissedWithoutLaunch()
    }

    private fun openIdDialog() {
        onView(withId(R.id.openByIdButton)).perform(scrollTo(), click())
        onView(withText(R.string.EnterTagId)).inRoot(isDialog()).check(matches(isDisplayed()))
    }

    private fun enterId(value: String) {
        onView(isAssignableFrom(TextInputEditText::class.java))
            .inRoot(isDialog())
            .perform(replaceText(value), closeSoftKeyboard())
    }

    private fun assertDialogDismissedWithoutLaunch() {
        onView(withText(R.string.EnterTagId)).check(doesNotExist())
        onView(withId(R.id.openByIdButton)).check(matches(isDisplayed()))
        Intents.assertNoUnverifiedIntents()
        assertDestination(MeActivity.HOME, R.id.home)
    }
}
