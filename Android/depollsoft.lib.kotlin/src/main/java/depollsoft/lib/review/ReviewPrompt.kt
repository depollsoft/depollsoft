package depollsoft.lib.review

import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Window
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.play.core.review.ReviewManagerFactory
import depollsoft.lib.analytics.UsageAnalytics

/**
 * Asks Google Play for a review, rarely, and never while someone is using the app.
 *
 * [ReviewPolicy] decides whether this person may be asked at all. Beyond that, an ask needs all of:
 *
 * - a task away from the music that someone just finished ([taskFinished]: saving a new song, putting
 *   a tag on a list), within [TASK_WINDOW_MILLIS];
 * - a calm screen in front ([calmScreenShown]): one that nobody reads or plays from while singing, such
 *   as Pitch Perfect's song list or Tag Master's Home, never a pitch pipe, a tag or sheet music;
 * - [CALM_MILLIS] with no touch or key, after which the screen still has the focus (no dialog, menu or
 *   other app in front), no keyboard is up and nothing is sounding ([isBusy]).
 *
 * Each finished task gives one chance: a touch during the wait spends it. iOS behaves the same way
 * (iOS/shared/ReviewPrompt.swift); docs/analytics.md describes both.
 */
object ReviewPrompt {
    /** How long the screen has to stay untouched before asking. */
    const val CALM_MILLIS = 3_000L

    /** How long a finished task waits for a calm screen. */
    const val TASK_WINDOW_MILLIS = 2L * 60 * 1000

    /** Reported to analytics each time the app asks the store (which may still show nothing). */
    const val ASKED_EVENT = "review_prompt_requested"

    /** Whether the app is sounding or otherwise in live use; each app sets it. */
    var isBusy: () -> Boolean = { false }

    /** Shows the store's review card; tests replace it. Debuggable builds only log. */
    var showStoreReview: (Activity) -> Unit = ::showPlayReview

    /** Replaces the stored policy (and its clock) in tests. */
    var policyForTesting: ReviewPolicy? = null

    /** The uptime clock the task window is measured on; tests replace it. */
    var uptimeMillis: () -> Long = SystemClock::uptimeMillis

    private val handler = Handler(Looper.getMainLooper())
    private var installed: ReviewPolicy? = null
    private var calmActivity: Activity? = null
    private var taskFinishedAt: Long? = null
    private var waiting: Waiting? = null

    /** The policy, once the app has called [install]; tests and previews have none and never ask. */
    private val policy: ReviewPolicy? get() = policyForTesting ?: installed

    /** Starts keeping the counts in [context]'s app storage; each app calls it at launch. */
    fun install(context: Context) {
        installed = ReviewPolicy(
            context.applicationContext.getSharedPreferences(ReviewPolicy.PREFS_NAME, Context.MODE_PRIVATE)
        )
    }

    /** Someone did the app's main job (played a pitch, opened a tag). */
    fun recordUse() {
        policy?.recordUse()
    }

    /** The app made a sound for someone (a pitch, a learning track). */
    fun recordSound() {
        policy?.recordSound()
    }

    /** Someone just finished a task away from the music. */
    fun taskFinished() {
        taskFinishedAt = uptimeMillis()
        calmActivity?.let(::startWaiting)
    }

    /** [activity] now shows a calm screen in front. */
    fun calmScreenShown(activity: Activity) {
        calmActivity = activity
        startWaiting(activity)
    }

    /** [activity]'s calm screen is no longer in front. */
    fun calmScreenHidden(activity: Activity) {
        if (calmActivity !== activity) return
        calmActivity = null
        cancelWaiting()
    }

    private fun taskIsFresh(): Boolean {
        val finishedAt = taskFinishedAt ?: return false
        return uptimeMillis() - finishedAt in 0..TASK_WINDOW_MILLIS
    }

    private fun startWaiting(activity: Activity) {
        cancelWaiting()
        if (!taskIsFresh()) return
        val policy = policy ?: return
        val version = versionName(activity) ?: return
        if (!policy.shouldAsk(version)) return
        val next = Waiting(activity, version, InteractionWatcher(activity.window))
        waiting = next
        handler.postDelayed(next.finish, CALM_MILLIS)
    }

    private fun cancelWaiting() {
        val current = waiting ?: return
        waiting = null
        handler.removeCallbacks(current.finish)
        // A touch while the screen was going away still spends the chance.
        if (current.watcher.stop()) taskFinishedAt = null
    }

    private fun finishWaiting(done: Waiting) {
        if (waiting !== done) return
        waiting = null
        val activity = done.activity
        if (done.watcher.stop()) {
            taskFinishedAt = null
            return
        }
        if (calmActivity !== activity || !taskIsFresh() || !isCalm(activity)) return
        val policy = policy ?: return
        if (!policy.shouldAsk(done.version)) return
        taskFinishedAt = null
        policy.recordAsk(done.version)
        UsageAnalytics.event(ASKED_EVENT)
        showStoreReview(activity)
    }

    /** Whether [activity] is in front with the focus, no keyboard is up, and the app is quiet. */
    fun isCalm(activity: Activity): Boolean {
        if (activity.isFinishing || activity.isDestroyed) return false
        val lifecycle = (activity as? LifecycleOwner)?.lifecycle
        if (lifecycle != null && !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return false
        // A dialog, menu or another app's window in front takes the focus away.
        if (!activity.hasWindowFocus()) return false
        val insets = ViewCompat.getRootWindowInsets(activity.window.decorView)
        if (insets?.isVisible(WindowInsetsCompat.Type.ime()) == true) return false
        return !isBusy()
    }

    private fun versionName(context: Context): String? = try {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        info.versionName ?: PackageInfoCompat.getLongVersionCode(info).toString()
    } catch (e: Exception) {
        null
    }

    private fun showPlayReview(activity: Activity) {
        if (activity.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            Log.i("ReviewPrompt", "A release build would ask Google Play for a review now")
            return
        }
        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow().addOnCompleteListener { request ->
            // Getting the card ready takes a moment; someone may have started something meanwhile.
            if (request.isSuccessful && isCalm(activity)) {
                manager.launchReviewFlow(activity, request.result)
            }
        }
    }

    /** Clears what this process remembers; for tests. */
    fun resetForTesting() {
        cancelWaiting()
        calmActivity = null
        taskFinishedAt = null
        isBusy = { false }
        showStoreReview = ::showPlayReview
        policyForTesting = null
        installed = null
        uptimeMillis = SystemClock::uptimeMillis
    }

    private class Waiting(val activity: Activity, val version: String, val watcher: InteractionWatcher) {
        val finish = Runnable { finishWaiting(this) }
    }

    /** Notes any touch, key or pointer input to a window until [stop]. */
    private class InteractionWatcher(private val window: Window) {
        private val original: Window.Callback = window.callback
        private var interacted = false

        private val watching = object : Window.Callback by original {
            override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
                interacted = true
                return original.dispatchTouchEvent(event)
            }

            override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
                interacted = true
                return original.dispatchKeyEvent(event)
            }

            override fun dispatchGenericMotionEvent(event: MotionEvent?): Boolean {
                interacted = true
                return original.dispatchGenericMotionEvent(event)
            }

            override fun dispatchTrackballEvent(event: MotionEvent?): Boolean {
                interacted = true
                return original.dispatchTrackballEvent(event)
            }
        }

        init {
            window.callback = watching
        }

        /** Puts the window's own callback back and says whether anything happened. */
        fun stop(): Boolean {
            if (window.callback === watching) window.callback = original
            return interacted
        }
    }
}
