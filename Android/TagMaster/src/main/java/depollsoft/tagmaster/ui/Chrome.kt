package depollsoft.tagmaster.ui

import android.content.Intent
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.View
import android.view.accessibility.AccessibilityManager
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.core.view.WindowCompat
import depollsoft.tagmaster.MeActivity
import depollsoft.tagmaster.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Puts a Tag Master screen on this activity: the charcoal status area and light navigation bar
 * every screen had, the theme, and resource-id test tags for UiAutomator-driven store captures.
 *
 * `RichApplication` pads the content view by the system bars and consumes them, so screens only
 * handle the keyboard inset; the padding above the content is painted in the chrome color.
 */
fun AppCompatActivity.setTagMasterContent(content: @Composable () -> Unit) {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
    val menuKey = MenuKey()
    setContent {
        TagMasterTheme {
            CompositionLocalProvider(LocalMenuKey provides menuKey) {
                ProvideSnackbars {
                    Box(Modifier.fillMaxSize().rootSemantics()) { content() }
                }
            }
        }
    }
    menuKey.install(window)
    val contentView = findViewById<View>(android.R.id.content)
    contentView.background =
        object : ColorDrawable(getColor(R.color.brand_chrome)) {
            override fun draw(canvas: Canvas) {
                val save = canvas.save()
                canvas.clipRect(0, 0, bounds.right, contentView.paddingTop)
                super.draw(canvas)
                canvas.restoreToCount(save)
            }
        }
}

@OptIn(ExperimentalComposeUiApi::class)
private fun Modifier.rootSemantics() = semantics { testTagsAsResourceId = true }

/**
 * Up from a child screen: the parent is normally already on the back stack, so Up is simply
 * finish(). When the screen is the task root (deep link, share link), go home instead.
 */
fun AppCompatActivity.navigateUpOrHome() {
    if (isTaskRoot) {
        startActivity(Intent(this, MeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
    }
    finish()
}

/** Shows transient messages at the bottom of the screen, as Material's Snackbar did. */
class Snackbars(
    val host: SnackbarHostState,
    private val scope: CoroutineScope,
    private val accessibility: AccessibilityManager? = null,
) {
    fun show(
        message: String,
        action: String? = null,
        long: Boolean = true,
        indefinite: Boolean = false,
        onAction: () -> Unit = {},
    ) {
        scope.launch { showNow(message, action, long, indefinite, onAction) }
    }

    /**
     * Shows a message for as long as the calling coroutine runs: cancelling it (a keyed
     * LaunchedEffect restarting or leaving) takes the snackbar down, as the View code's explicit
     * `dismiss()` did.
     */
    suspend fun showNow(
        message: String,
        action: String? = null,
        long: Boolean = true,
        indefinite: Boolean = false,
        onAction: () -> Unit = {},
    ) {
        host.currentSnackbarData?.dismiss()
        val shown = if (indefinite) null else shownFor(if (long) LONG_MILLIS else SHORT_MILLIS, action != null)
        val result =
            coroutineScope {
                val timer =
                    shown?.let { millis ->
                        launch {
                            delay(millis)
                            if (host.currentSnackbarData?.visuals?.message == message) host.currentSnackbarData?.dismiss()
                        }
                    }
                host.showSnackbar(message, action, duration = SnackbarDuration.Indefinite).also { timer?.cancel() }
            }
        if (result == SnackbarResult.ActionPerformed) onAction()
    }

    /**
     * How long MDC's Snackbar stayed up: 1500ms short and 2750ms long, stretched to the time the
     * user's accessibility settings ask for, and until dismissed while touch exploration is on
     * before Android 10 when there is an action to reach. Null is until dismissed.
     */
    internal fun shownFor(
        millis: Long,
        hasAction: Boolean,
    ): Long? {
        val manager = accessibility ?: return millis
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val flags = AccessibilityManager.FLAG_CONTENT_TEXT or (if (hasAction) AccessibilityManager.FLAG_CONTENT_CONTROLS else 0)
            return manager.getRecommendedTimeoutMillis(millis.toInt(), flags).toLong()
        }
        return if (hasAction && manager.isTouchExplorationEnabled) null else millis
    }

    companion object {
        const val SHORT_MILLIS = 1500L
        const val LONG_MILLIS = 2750L
    }

    fun dismiss() {
        host.currentSnackbarData?.dismiss()
    }
}

val LocalSnackbars = staticCompositionLocalOf<Snackbars> { error("No snackbar host") }

private const val SLIDE_MILLIS = 250

@Composable
private fun ProvideSnackbars(content: @Composable BoxScope.() -> Unit) {
    val host = remember { SnackbarHostState() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val context = LocalContext.current
    val snackbars = remember(host, scope) { Snackbars(host, scope, context.getSystemService(AccessibilityManager::class.java)) }
    CompositionLocalProvider(LocalSnackbars provides snackbars) {
        Box(Modifier.fillMaxSize()) {
            content()
            SlidingSnackbarHost(host, Modifier.align(androidx.compose.ui.Alignment.BottomCenter))
        }
    }
}

/**
 * The snackbar slides up from the bottom edge and back down, as MDC's did, instead of Compose's
 * fade and scale. Accessibility services hear it and can dismiss it, as with Material's host.
 */
@Composable
private fun SlidingSnackbarHost(
    host: SnackbarHostState,
    modifier: Modifier,
) {
    val current = host.currentSnackbarData
    val last = remember { arrayOfNulls<SnackbarData>(1) }
    if (current != null) last[0] = current
    AnimatedVisibility(
        visible = current != null,
        modifier = modifier,
        enter = slideInVertically(tween(SLIDE_MILLIS, easing = FastOutSlowInEasing)) { it },
        exit = slideOutVertically(tween(SLIDE_MILLIS, easing = FastOutSlowInEasing)) { it },
    ) {
        val data = current ?: last[0] ?: return@AnimatedVisibility
        Snackbar(
            data,
            Modifier.semantics {
                liveRegion = LiveRegionMode.Polite
                dismiss {
                    data.dismiss()
                    true
                }
            },
        )
    }
}
