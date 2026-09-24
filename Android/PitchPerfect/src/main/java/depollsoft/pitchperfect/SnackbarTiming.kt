package depollsoft.pitchperfect

import android.content.Context
import android.os.Build
import android.view.accessibility.AccessibilityManager

/** How long a snackbar stays up, as MaterialComponents' Snackbar.getDuration() worked it out. */
object SnackbarTiming {
    const val SHORT_MS = 1_500L
    const val LONG_MS = 2_750L
    const val SLIDE_MS = 250

    /**
     * [ms], stretched to the timeout the person has asked accessibility services for (Android 10
     * and later); earlier, a snackbar with an action stays up while touch exploration is on. Null
     * means it stays until dismissed.
     */
    fun timeoutMillis(
        context: Context,
        ms: Long,
        hasAction: Boolean,
    ): Long? {
        val accessibility = context.getSystemService(AccessibilityManager::class.java) ?: return ms
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val content =
                AccessibilityManager.FLAG_CONTENT_TEXT or
                    (if (hasAction) AccessibilityManager.FLAG_CONTENT_CONTROLS else 0)
            accessibility.getRecommendedTimeoutMillis(ms.toInt(), content).toLong()
        } else if (hasAction && accessibility.isTouchExplorationEnabled) {
            null
        } else {
            ms
        }
    }
}
