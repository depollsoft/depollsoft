package depollsoft.pitchperfect

import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matcher
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Utility functions for Espresso UI tests that avoid Thread.sleep() 
 * and use proper Espresso synchronization instead.
 * 
 * Using Thread.sleep() in tests causes:
 * - Slow test execution (waiting longer than necessary)
 * - Flaky tests (sometimes not waiting long enough)
 * - Poor CI performance (cumulative delays add minutes)
 */
object EspressoTestUtils {

    /**
     * Wait for a view to appear with a timeout.
     * Uses Espresso's built-in waiting mechanism which is much more efficient
     * than Thread.sleep() as it polls quickly and moves on immediately when ready.
     * 
     * @param viewMatcher The matcher for the view to wait for
     * @param timeout Maximum time to wait in milliseconds
     * @return ViewInteraction for chaining
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
                // Short yield instead of hard sleep
                Thread.yield()
            }
        } while (true)
    }

    /**
     * A custom ViewAction that waits for a specified duration.
     * This is more efficient than Thread.sleep() because it integrates
     * with Espresso's synchronization mechanism.
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
     * Perform a short wait that integrates with Espresso's idle sync.
     * Use this instead of Thread.sleep(200-500) for short waits.
     * 
     * @param millis Time to wait (default 100ms - usually sufficient for UI to settle)
     */
    fun shortWait(millis: Long = 100) {
        onView(isRoot()).perform(waitFor(millis))
    }

    /**
     * Dismiss common startup dialogs (login, changelog, etc.)
     * Much faster implementation that doesn't rely on fixed sleeps.
     * 
     * @param maxAttempts Maximum number of dialog dismissal attempts
     * @param buttonTexts List of button text patterns to try clicking
     */
    fun dismissStartupDialogs(
        maxAttempts: Int = 3,
        buttonTexts: List<String> = listOf("Skip", "OK", "Cancel", "Dismiss", "Later")
    ) {
        for (attempt in 1..maxAttempts) {
            var dialogDismissed = false
            
            for (buttonText in buttonTexts) {
                try {
                    onView(withText(buttonText))
                        .inRoot(isDialog())
                        .perform(click())
                    dialogDismissed = true
                    // Give UI a moment to settle after dismissal
                    shortWait(50)
                    break
                } catch (e: Exception) {
                    // Button not found, try next
                }
            }
            
            if (!dialogDismissed) {
                // No dialogs found, we're done
                break
            }
        }
    }

    /**
     * Idling resource for waiting on specific conditions.
     * Register this when you need to wait for async operations.
     */
    class ConditionIdlingResource(
        private val resourceName: String,
        private val condition: () -> Boolean
    ) : IdlingResource {
        
        @Volatile
        private var callback: IdlingResource.ResourceCallback? = null

        override fun getName(): String = resourceName

        override fun isIdleNow(): Boolean {
            val idle = condition()
            if (idle) {
                callback?.onTransitionToIdle()
            }
            return idle
        }

        override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
            this.callback = callback
        }
    }

    /**
     * Execute an action and wait for a condition to become true.
     * 
     * @param resourceName Name for debugging
     * @param timeout Maximum wait time
     * @param condition Lambda that returns true when the condition is met
     * @param action The action to perform
     */
    fun <T> waitForCondition(
        resourceName: String,
        timeout: Long = 5000,
        condition: () -> Boolean,
        action: () -> T
    ): T {
        val idlingResource = ConditionIdlingResource(resourceName, condition)
        IdlingRegistry.getInstance().register(idlingResource)
        return try {
            action()
        } finally {
            IdlingRegistry.getInstance().unregister(idlingResource)
        }
    }
}

/**
 * Extension function to add a short wait to ViewInteraction.
 * Usage: onView(withId(R.id.button)).performWithWait(click())
 */
fun ViewInteraction.performWithWait(vararg actions: ViewAction, waitMillis: Long = 100): ViewInteraction {
    return this.perform(*actions).also {
        EspressoTestUtils.shortWait(waitMillis)
    }
}
