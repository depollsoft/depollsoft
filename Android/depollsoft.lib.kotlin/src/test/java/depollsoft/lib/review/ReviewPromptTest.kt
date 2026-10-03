package depollsoft.lib.review

import android.app.Activity
import android.content.Context
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import depollsoft.lib.analytics.UsageAnalytics
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ReviewPromptTest {
    private val prefs = RuntimeEnvironment.getApplication().getSharedPreferences("review_prompt_test", Context.MODE_PRIVATE)
    private var now = 1_790_881_200_000L
    private val policy = ReviewPolicy(prefs, { now })
    private val asked = mutableListOf<Activity>()
    private val events = mutableListOf<String>()
    private lateinit var controller: ActivityController<ComponentActivity>
    private val activity get() = controller.get()

    @Before fun setUp() {
        prefs.edit().clear().commit()
        ReviewPrompt.resetForTesting()
        ReviewPrompt.policyForTesting = policy
        ReviewPrompt.showStoreReview = { asked += it }
        UsageAnalytics.sink =
            object : UsageAnalytics.Sink {
                override fun logEvent(name: String, params: Map<String, String>) {
                    events += name
                }

                override fun setUserProperty(name: String, value: String) {}
            }
        controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        controller.windowFocusChanged(true)
    }

    @After fun tearDown() {
        ReviewPrompt.resetForTesting()
        UsageAnalytics.resetForTesting()
    }

    /** Five days of use over two weeks, with nothing played since. */
    private fun becomeEligible() {
        repeat(ReviewPolicy.MIN_ACTIVE_DAYS) {
            policy.recordUse()
            now += DAY
        }
        now += ReviewPolicy.MIN_DAYS_SINCE_FIRST_USE * DAY
    }

    private fun idle(millis: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis))

    private fun touch() {
        val time = SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(time, time, MotionEvent.ACTION_DOWN, 10f, 10f, 0)
        activity.window.decorView.dispatchTouchEvent(down)
        down.recycle()
    }

    @Test fun asksOnACalmScreenThreeQuietSecondsAfterAFinishedTask() {
        becomeEligible()
        ReviewPrompt.calmScreenShown(activity)
        ReviewPrompt.taskFinished()
        idle(ReviewPrompt.CALM_MILLIS - 1)
        assertEquals(emptyList<Activity>(), asked)
        idle(1)
        assertEquals(listOf<Activity>(activity), asked)
        assertEquals(listOf(ReviewPrompt.ASKED_EVENT), events)
    }

    @Test fun aTaskFinishedElsewhereWaitsForTheCalmScreen() {
        becomeEligible()
        ReviewPrompt.taskFinished()
        idle(30_000)
        assertEquals(emptyList<Activity>(), asked)
        ReviewPrompt.calmScreenShown(activity)
        idle(ReviewPrompt.CALM_MILLIS)
        assertEquals(1, asked.size)
    }

    @Test fun aTaskOlderThanTwoMinutesIsForgotten() {
        becomeEligible()
        ReviewPrompt.taskFinished()
        idle(ReviewPrompt.TASK_WINDOW_MILLIS + 1)
        ReviewPrompt.calmScreenShown(activity)
        idle(ReviewPrompt.CALM_MILLIS)
        assertEquals(emptyList<Activity>(), asked)
    }

    @Test fun aCalmScreenAloneNeverAsks() {
        becomeEligible()
        ReviewPrompt.calmScreenShown(activity)
        idle(60_000)
        assertEquals(emptyList<Activity>(), asked)
    }

    @Test fun aTouchDuringTheWaitSpendsTheChance() {
        becomeEligible()
        ReviewPrompt.calmScreenShown(activity)
        ReviewPrompt.taskFinished()
        idle(1_000)
        touch()
        idle(ReviewPrompt.CALM_MILLIS)
        assertEquals(emptyList<Activity>(), asked)
        // Coming back to the calm screen does not bring the spent chance back.
        ReviewPrompt.calmScreenHidden(activity)
        ReviewPrompt.calmScreenShown(activity)
        idle(ReviewPrompt.CALM_MILLIS)
        assertEquals(emptyList<Activity>(), asked)
        // The next finished task is a new chance.
        ReviewPrompt.taskFinished()
        idle(ReviewPrompt.CALM_MILLIS)
        assertEquals(1, asked.size)
    }

    @Test fun theWindowGetsItsOwnCallbackBackAfterWaiting() {
        val original = activity.window.callback
        becomeEligible()
        ReviewPrompt.calmScreenShown(activity)
        ReviewPrompt.taskFinished()
        assertTrue(activity.window.callback !== original)
        idle(ReviewPrompt.CALM_MILLIS)
        assertTrue(activity.window.callback === original)
    }

    @Test fun leavingTheCalmScreenCancelsTheWait() {
        becomeEligible()
        ReviewPrompt.calmScreenShown(activity)
        ReviewPrompt.taskFinished()
        idle(1_000)
        ReviewPrompt.calmScreenHidden(activity)
        idle(ReviewPrompt.CALM_MILLIS)
        assertEquals(emptyList<Activity>(), asked)
        // It left without a touch (rotation, say): the task can still find a calm screen.
        ReviewPrompt.calmScreenShown(activity)
        idle(ReviewPrompt.CALM_MILLIS)
        assertEquals(1, asked.size)
    }

    @Test fun leavingTheAppSpendsTheTask() {
        becomeEligible()
        ReviewPrompt.install(RuntimeEnvironment.getApplication())
        ReviewPrompt.policyForTesting = policy
        ReviewPrompt.taskFinished()
        controller.pause().stop()
        controller.restart().start().resume()
        controller.windowFocusChanged(true)
        // Coming back to a calm screen within the task's two minutes is like a launch: no ask.
        ReviewPrompt.calmScreenShown(activity)
        idle(ReviewPrompt.CALM_MILLIS)
        assertEquals(emptyList<Activity>(), asked)
    }

    @Test fun turningTheScreenKeepsTheTask() {
        becomeEligible()
        ReviewPrompt.install(RuntimeEnvironment.getApplication())
        ReviewPrompt.policyForTesting = policy
        ReviewPrompt.taskFinished()
        controller.recreate()
        controller.windowFocusChanged(true)
        ReviewPrompt.calmScreenShown(activity)
        idle(ReviewPrompt.CALM_MILLIS)
        assertEquals(1, asked.size)
    }

    @Test fun theThreeSecondsStartOnceNothingCoversTheScreen() {
        becomeEligible()
        // The task's dialog is still up when the task finishes.
        controller.windowFocusChanged(false)
        ReviewPrompt.calmScreenShown(activity)
        ReviewPrompt.taskFinished()
        idle(2_000)
        controller.windowFocusChanged(true)
        idle(ReviewPrompt.CALM_MILLIS - 500)
        assertEquals("the dialog's time doesn't count", emptyList<Activity>(), asked)
        idle(1_000)
        assertEquals(1, asked.size)
    }

    @Test fun aDialogOrMenuInFrontBlocksTheAsk() {
        becomeEligible()
        ReviewPrompt.calmScreenShown(activity)
        ReviewPrompt.taskFinished()
        controller.windowFocusChanged(false)
        idle(ReviewPrompt.CALM_MILLIS)
        assertEquals(emptyList<Activity>(), asked)
    }

    @Test fun somethingSoundingBlocksTheAsk() {
        becomeEligible()
        ReviewPrompt.isBusy = { true }
        ReviewPrompt.calmScreenShown(activity)
        ReviewPrompt.taskFinished()
        idle(ReviewPrompt.CALM_MILLIS)
        assertEquals(emptyList<Activity>(), asked)
    }

    @Test fun aRecentSoundBlocksTheAsk() {
        becomeEligible()
        ReviewPrompt.recordSound()
        ReviewPrompt.calmScreenShown(activity)
        ReviewPrompt.taskFinished()
        idle(ReviewPrompt.CALM_MILLIS)
        assertEquals(emptyList<Activity>(), asked)
    }

    @Test fun aPausedActivityIsNotAsked() {
        becomeEligible()
        ReviewPrompt.calmScreenShown(activity)
        ReviewPrompt.taskFinished()
        controller.pause()
        idle(ReviewPrompt.CALM_MILLIS)
        assertEquals(emptyList<Activity>(), asked)
    }

    @Test fun someoneNotYetEligibleIsNeverAsked() {
        ReviewPrompt.calmScreenShown(activity)
        ReviewPrompt.taskFinished()
        idle(ReviewPrompt.CALM_MILLIS)
        assertEquals(emptyList<Activity>(), asked)
    }

    @Test fun asksOnlyOncePerVersion() {
        becomeEligible()
        ReviewPrompt.calmScreenShown(activity)
        ReviewPrompt.taskFinished()
        idle(ReviewPrompt.CALM_MILLIS)
        becomeEligible()
        now += ReviewPolicy.MIN_DAYS_BETWEEN_ASKS * DAY
        ReviewPrompt.taskFinished()
        idle(ReviewPrompt.CALM_MILLIS)
        assertEquals(1, asked.size)
    }

    @Test fun withoutAnInstalledPolicyNothingIsCountedOrAsked() {
        ReviewPrompt.resetForTesting()
        ReviewPrompt.showStoreReview = { asked += it }
        repeat(30) { ReviewPrompt.recordUse() }
        ReviewPrompt.calmScreenShown(activity)
        ReviewPrompt.taskFinished()
        idle(ReviewPrompt.CALM_MILLIS)
        assertEquals(emptyList<Activity>(), asked)
        assertEquals(0, prefs.all.size)
    }

    private companion object {
        const val DAY = 24L * 60 * 60 * 1000
    }
}
