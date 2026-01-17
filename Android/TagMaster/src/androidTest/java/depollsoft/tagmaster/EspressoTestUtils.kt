package depollsoft.tagmaster

import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import org.hamcrest.Matcher

/**
 * Utility helpers for Espresso UI tests that avoid Thread.sleep().
 */
object EspressoTestUtils {

    /**
     * Wait for a view to appear with a timeout.
     */
    fun waitForView(viewMatcher: Matcher<View>, timeout: Long = 5000): ViewInteraction {
        val startTime = System.currentTimeMillis()
        val endTime = startTime + timeout

        do {
            try {
                return onView(viewMatcher).check(matches(isDisplayed()))
            } catch (e: Exception) {
                if (System.currentTimeMillis() > endTime) {
                    throw e
                }
                Thread.yield()
            }
        } while (true)
    }

    /**
     * A custom ViewAction that waits for a specified duration.
     */
    fun waitFor(millis: Long): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> = isRoot()

            override fun getDescription(): String = "Wait for $millis milliseconds"

            override fun perform(uiController: UiController, view: View) {
                uiController.loopMainThreadForAtLeast(millis)
            }
        }
    }

    /**
     * Short wait that integrates with Espresso's idle sync.
     */
    fun shortWait(millis: Long = 100) {
        onView(isRoot()).perform(waitFor(millis))
    }
}
