package depollsoft.compose

import android.os.Handler
import android.os.Looper
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.LifecycleResumeEffect
import depollsoft.lib.analytics.UsageAnalytics
import depollsoft.lib.review.ReviewPrompt

/**
 * Reports [name] as the screen in front (docs/analytics.md) each time this composition's activity
 * resumes, and again whenever [name] or [key] changes (another tab, page or item).
 */
@Composable
fun ScreenView(
    name: String,
    key: Any? = null,
) {
    LifecycleResumeEffect(name, key) {
        FrontScreen.shown(name)
        onPauseOrDispose {}
    }
}

/**
 * Screens that come to the front together, such as a list and the tag beside it as the app comes
 * back, count once: as the one composed last, which is the one in front. iOS's ScreenTracker
 * settles the same way.
 */
private object FrontScreen {
    private val main = Handler(Looper.getMainLooper())
    private var pending: String? = null
    private val report =
        Runnable {
            val name = pending ?: return@Runnable
            pending = null
            UsageAnalytics.screen(name)
        }

    fun shown(name: String) {
        if (pending == null) main.post(report)
        pending = name
    }
}

/**
 * Tells the review prompt this screen is calm while [calm] is true and its activity is resumed:
 * one nobody reads or plays from while singing, so a review card may follow a finished task here.
 */
@Composable
fun ReviewCalmScreen(calm: Boolean) {
    val activity = LocalActivity.current ?: return
    LifecycleResumeEffect(calm, activity) {
        if (calm) ReviewPrompt.calmScreenShown(activity)
        onPauseOrDispose { if (calm) ReviewPrompt.calmScreenHidden(activity) }
    }
}
