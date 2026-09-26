package depollsoft.compose

import android.os.Build
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay

/** How long a snackbar stays up, as MaterialComponents' Snackbar worked it out. */
object SnackbarTiming {
    const val SHORT_MILLIS = 1500L
    const val LONG_MILLIS = 2750L
    const val SLIDE_MILLIS = 250

    /**
     * How long a snackbar asked to stay up [millis] is shown, as MDC's `Snackbar.getDuration` and
     * `SnackbarManager` worked it out. On Android 10 and later MDC handed its duration constant
     * itself (LENGTH_SHORT -1, LENGTH_LONG 0) to the accessibility manager, which returns the
     * larger of it and the timeouts the person asked for; with none set, both come back 0, which
     * SnackbarManager shows for [LONG_MILLIS]. So a short snackbar stays up as long as a long one
     * there. Earlier, a snackbar with an action stays up while touch exploration is on, so a
     * screen reader can reach the action. Null means until dismissed.
     */
    fun shownFor(
        accessibility: AccessibilityManager?,
        millis: Long,
        hasAction: Boolean,
    ): Long? {
        val manager = accessibility ?: return millis
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val requested =
                when (millis) {
                    SHORT_MILLIS -> LENGTH_SHORT
                    LONG_MILLIS -> LENGTH_LONG
                    else -> millis.toInt()
                }
            val flags =
                AccessibilityManager.FLAG_CONTENT_ICONS or AccessibilityManager.FLAG_CONTENT_TEXT or
                    (if (hasAction) AccessibilityManager.FLAG_CONTENT_CONTROLS else 0)
            val recommended = manager.getRecommendedTimeoutMillis(requested, flags)
            return when {
                recommended > 0 -> recommended.toLong()
                recommended == LENGTH_SHORT -> SHORT_MILLIS
                else -> LONG_MILLIS
            }
        }
        return if (hasAction && manager.isTouchExplorationEnabled) null else millis
    }

    // MDC's BaseTransientBottomBar duration constants.
    private const val LENGTH_SHORT = -1
    private const val LENGTH_LONG = 0
}

/** A message on screen: its text, its action's label, and how it can end. */
@Stable
class ShownSnackbar internal constructor(
    val message: String,
    val actionLabel: String?,
    /** How long it stays up once it has slid in; null until dismissed. */
    internal val shownFor: Long?,
) {
    internal val result = CompletableDeferred<Boolean>()

    /** The action was chosen: the snackbar goes, and the caller hears so. */
    fun performAction() {
        result.complete(true)
    }

    fun dismiss() {
        result.complete(false)
    }
}

/**
 * One snackbar at a time at the bottom of the screen, as MaterialComponents' Snackbar showed it:
 * a new one replaces the one showing, and each stays up for its duration once it has slid in.
 */
@Stable
class SnackbarState(
    private val accessibility: AccessibilityManager?,
) {
    var current: ShownSnackbar? by mutableStateOf(null)
        private set

    /**
     * Shows [message] until it times out, is dismissed or its action is chosen, and returns
     * whether the action was. Cancelling the calling coroutine takes the snackbar down.
     * [durationMillis] null keeps it up until dismissed.
     */
    suspend fun show(
        message: String,
        actionLabel: String? = null,
        durationMillis: Long? = SnackbarTiming.LONG_MILLIS,
    ): Boolean {
        current?.dismiss()
        val shown = ShownSnackbar(message, actionLabel, durationMillis?.let { SnackbarTiming.shownFor(accessibility, it, actionLabel != null) })
        current = shown
        try {
            return shown.result.await()
        } finally {
            shown.dismiss()
            if (current === shown) current = null
        }
    }

    /** Takes down the snackbar showing, if any. */
    fun dismiss() {
        current?.dismiss()
    }
}

/** A [SnackbarState] that stretches durations to the accessibility timeouts this device asks for. */
@Composable
fun rememberSnackbarState(): SnackbarState {
    val context = LocalContext.current
    return remember { SnackbarState(context.getSystemService(AccessibilityManager::class.java)) }
}

/**
 * Hosts [state]'s snackbar: it slides up from the bottom edge and back down as MDC's did, a new
 * one waits for the one it replaces to slide away, and its time starts once it is fully in, as
 * MDC's `onViewShown` started it. Screen readers hear it (a polite live region) and can dismiss
 * it, as with Material's own hosts. [snackbar] draws it.
 */
@Composable
fun SlidingSnackbarHost(
    state: SnackbarState,
    modifier: Modifier = Modifier,
    snackbar: @Composable (ShownSnackbar) -> Unit,
) {
    val current = state.current
    // The snackbar on screen: the current one, or the one sliding away before it.
    val displayed = remember { arrayOfNulls<ShownSnackbar>(1) }
    val visibility = remember { MutableTransitionState(false) }
    val hidden = visibility.isIdle && !visibility.currentState
    SideEffect {
        if (hidden && !visibility.targetState) displayed[0] = current
        visibility.targetState = displayed[0] != null && displayed[0] === current
    }
    val shownIn = current != null && displayed[0] === current && visibility.isIdle && visibility.currentState
    LaunchedEffect(current, shownIn) {
        val millis = current?.shownFor ?: return@LaunchedEffect
        if (!shownIn) return@LaunchedEffect
        delay(millis)
        current.dismiss()
    }
    AnimatedVisibility(
        visibleState = visibility,
        modifier = modifier,
        enter = slideInVertically(tween(SnackbarTiming.SLIDE_MILLIS, easing = FastOutSlowInEasing)) { it },
        exit = slideOutVertically(tween(SnackbarTiming.SLIDE_MILLIS, easing = FastOutSlowInEasing)) { it },
    ) {
        val shown = displayed[0] ?: return@AnimatedVisibility
        Box(
            Modifier.semantics {
                liveRegion = LiveRegionMode.Polite
                dismiss {
                    shown.dismiss()
                    true
                }
            },
        ) { snackbar(shown) }
    }
}
