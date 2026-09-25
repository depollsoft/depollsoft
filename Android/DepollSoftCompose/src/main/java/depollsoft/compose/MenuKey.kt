package depollsoft.compose

import android.view.KeyEvent
import android.view.KeyboardShortcutGroup
import android.view.Menu
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The hardware or keyboard Menu key, which opened a window action bar's or support toolbar's
 * overflow menu. A screen's top bar registers its overflow here; the most recently shown bar with
 * an overflow opens.
 */
class MenuKey {
    private val openers = mutableListOf<() -> Unit>()

    /** Opens the registered overflow; false when no bar has one. */
    fun press(): Boolean {
        val open = openers.lastOrNull() ?: return false
        open()
        return true
    }

    internal fun register(open: () -> Unit): () -> Unit {
        openers += open
        return { openers.remove(open) }
    }

    /** Sends the Menu key presses the window's views do not use themselves to [press]. */
    fun install(window: Window) {
        val original = window.callback
        window.callback =
            object : Window.Callback by original {
                override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                    if (original.dispatchKeyEvent(event)) return true
                    if (event.keyCode != KeyEvent.KEYCODE_MENU) return false
                    return event.action == KeyEvent.ACTION_DOWN || press()
                }

                // Java default methods, which delegation by `by` leaves out.
                override fun onProvideKeyboardShortcuts(
                    data: MutableList<KeyboardShortcutGroup>?,
                    menu: Menu?,
                    deviceId: Int,
                ) = original.onProvideKeyboardShortcuts(data, menu, deviceId)

                override fun onPointerCaptureChanged(hasCapture: Boolean) = original.onPointerCaptureChanged(hasCapture)
            }
    }
}

val LocalMenuKey = staticCompositionLocalOf<MenuKey?> { null }

/** While this is composed, the Menu key calls [open]. */
@Composable
fun OpensOnMenuKey(open: () -> Unit) {
    val menuKey = LocalMenuKey.current ?: return
    val current = rememberUpdatedState(open)
    DisposableEffect(menuKey) { onDispose(menuKey.register { current.value() }) }
}
