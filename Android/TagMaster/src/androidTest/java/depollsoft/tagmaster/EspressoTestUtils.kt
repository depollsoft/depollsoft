package depollsoft.tagmaster

import android.os.SystemClock
import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.util.TreeIterables
import org.hamcrest.Matcher

/** Bounded waiting for legacy asynchronous UI tests. Desk tests use Espresso idle sync. */
object EspressoTestUtils {
    fun waitForView(
        viewMatcher: Matcher<View>,
        timeout: Long = 5000,
    ): ViewInteraction {
        require(timeout >= 0)
        onView(isRoot()).perform(
            object : ViewAction {
                override fun getConstraints(): Matcher<View> = isRoot()

                override fun getDescription(): String = "Wait up to $timeout ms for $viewMatcher"

                override fun perform(
                    uiController: UiController,
                    view: View,
                ) {
                    val deadline = SystemClock.uptimeMillis() + timeout
                    do {
                        val found =
                            TreeIterables.breadthFirstViewTraversal(view).any {
                                viewMatcher.matches(it) && isDisplayed().matches(it)
                            }
                        if (found) return
                        if (SystemClock.uptimeMillis() >= deadline) {
                            throw AssertionError("Timed out waiting for visible view: $viewMatcher")
                        }
                        uiController.loopMainThreadForAtLeast(16)
                    } while (true)
                }
            },
        )
        return onView(viewMatcher).check(matches(isDisplayed()))
    }

    fun waitForPagerIdle(pagerId: Int) {
        onView(
            androidx.test.espresso.matcher.ViewMatchers
                .withId(pagerId),
        ).perform(
            object : ViewAction {
                override fun getConstraints(): Matcher<View> =
                    androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom(
                        androidx.viewpager2.widget.ViewPager2::class.java,
                    )

                override fun getDescription() = "Wait for native pager to finish scrolling"

                override fun perform(
                    uiController: UiController,
                    view: View,
                ) {
                    val pager = view as androidx.viewpager2.widget.ViewPager2
                    val deadline = SystemClock.uptimeMillis() + 5000
                    do {
                        uiController.loopMainThreadForAtLeast(16)
                        if (pager.scrollState == androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_IDLE) return
                    } while (SystemClock.uptimeMillis() < deadline)
                    throw AssertionError("Pager did not become idle")
                }
            },
        )
    }

    fun waitFor(millis: Long): ViewAction =
        object : ViewAction {
            override fun getConstraints(): Matcher<View> = isRoot()

            override fun getDescription(): String = "Wait for $millis milliseconds"

            override fun perform(
                uiController: UiController,
                view: View,
            ) {
                uiController.loopMainThreadForAtLeast(millis)
            }
        }

    fun shortWait(millis: Long = 100) {
        onView(isRoot()).perform(waitFor(millis))
    }
}
