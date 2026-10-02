package depollsoft.compose

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
        UsageAnalytics.screen(name)
        onPauseOrDispose {}
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
