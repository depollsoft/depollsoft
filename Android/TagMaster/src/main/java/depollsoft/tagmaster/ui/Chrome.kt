package depollsoft.tagmaster.ui

import android.content.Intent
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.view.View
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.core.view.WindowCompat
import depollsoft.compose.LocalMenuKey
import depollsoft.compose.MenuKey
import depollsoft.compose.ShownSnackbar
import depollsoft.compose.SlidingSnackbarHost
import depollsoft.compose.SnackbarState
import depollsoft.compose.SnackbarTiming
import depollsoft.compose.rememberSnackbarState
import depollsoft.tagmaster.MeActivity
import depollsoft.tagmaster.R
import kotlinx.coroutines.CoroutineScope
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

/** Shows transient messages at the bottom of the screen, as MaterialComponents' Snackbar did. */
class Snackbars(
    val state: SnackbarState,
    private val scope: CoroutineScope,
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
     * LaunchedEffect restarting or leaving) takes the snackbar down.
     */
    suspend fun showNow(
        message: String,
        action: String? = null,
        long: Boolean = true,
        indefinite: Boolean = false,
        onAction: () -> Unit = {},
    ) {
        val duration =
            when {
                indefinite -> null
                long -> SnackbarTiming.LONG_MILLIS
                else -> SnackbarTiming.SHORT_MILLIS
            }
        if (state.show(message, action, duration)) onAction()
    }
}

val LocalSnackbars = staticCompositionLocalOf<Snackbars> { error("No snackbar host") }

@Composable
private fun ProvideSnackbars(content: @Composable BoxScope.() -> Unit) {
    val state = rememberSnackbarState()
    val scope = rememberCoroutineScope()
    val snackbars = remember(state, scope) { Snackbars(state, scope) }
    CompositionLocalProvider(LocalSnackbars provides snackbars) {
        Box(Modifier.fillMaxSize()) {
            content()
            SlidingSnackbarHost(state, Modifier.align(Alignment.BottomCenter)) { Snackbar(it.asMaterial()) }
        }
    }
}

/** [this] as Material's snackbar data, so Material's own Snackbar draws it. */
private fun ShownSnackbar.asMaterial(): SnackbarData {
    val shown = this
    return object : SnackbarData {
        override val visuals =
            object : SnackbarVisuals {
                override val message = shown.message
                override val actionLabel = shown.actionLabel
                override val withDismissAction = false
                override val duration = SnackbarDuration.Indefinite
            }

        override fun performAction() = shown.performAction()

        override fun dismiss() = shown.dismiss()
    }
}
