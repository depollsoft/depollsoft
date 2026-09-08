package depollsoft.tagmaster

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DimenRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.google.android.material.appbar.MaterialToolbar

/**
 * Edge-to-edge helpers shared by every Tag Master screen.
 *
 * Every activity layout includes `@layout/app_toolbar` (an AppBarLayout with fitsSystemWindows,
 * so the status bar inset is handled there). Activities call [setUpToolbar] in onCreate and then
 * hand the bottom system-bar inset to whichever views touch the bottom edge (tab strips, list
 * padding, FAB margins) with the extension functions below. Listeners never consume the insets, so
 * sibling views can apply them too.
 */
fun AppCompatActivity.setUpToolbar(showUp: Boolean = true): MaterialToolbar {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
    setSupportActionBar(toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(showUp)
    return toolbar
}

private val insetTypes: Int
    get() = WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()

/** Adds the bottom system-bar inset to this view's bottom padding (keeps the XML padding). */
fun View.applyBottomInsetsAsPadding() {
    val basePadding = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        v.updatePadding(bottom = basePadding + insets.getInsets(insetTypes).bottom)
        insets
    }
}

/** Adds the bottom system-bar inset to this view's bottom margin (for FABs). */
fun View.applyBottomInsetsAsMargin() {
    val baseMargin = (layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin ?: 0
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = baseMargin + insets.getInsets(insetTypes).bottom
        }
        insets
    }
}

/** Adds the left/right system-bar and cutout insets (landscape, cutouts) to horizontal padding. */
fun View.applyHorizontalInsetsAsPadding() {
    val baseLeft = paddingLeft
    val baseRight = paddingRight
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val bars = insets.getInsets(insetTypes)
        v.updatePadding(left = baseLeft + bars.left, right = baseRight + bars.right)
        insets
    }
}

/**
 * Insets and measure for scrolling content in one listener: the bottom system-bar inset as
 * padding (optional), the side insets as padding, and a centered maximum content width so lists
 * and forms keep a readable measure on tablets and in landscape instead of stretching edge to
 * edge. Use this instead of stacking the single-purpose helpers on the same view, because each
 * one replaces the view's insets listener.
 */
fun View.applyContentInsets(
    bottom: Boolean = true,
    @DimenRes maxWidthRes: Int = R.dimen.content_max_width,
) {
    val baseLeft = paddingLeft
    val baseRight = paddingRight
    val baseBottom = paddingBottom
    val maxWidth = resources.getDimensionPixelSize(maxWidthRes)
    var sideInsets = Insets.NONE
    var bottomInset = 0

    fun update(width: Int) {
        val room = width - sideInsets.left - sideInsets.right - maxWidth
        val extra = if (width > 0 && room > 0) room / 2 else 0
        val left = baseLeft + sideInsets.left + extra
        val right = baseRight + sideInsets.right + extra
        val bottomPadding = if (bottom) baseBottom + bottomInset else paddingBottom
        if (left != paddingLeft || right != paddingRight || bottomPadding != paddingBottom) {
            updatePadding(left = left, right = right, bottom = bottomPadding)
        }
    }

    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val bars = insets.getInsets(insetTypes)
        sideInsets = Insets.of(bars.left, 0, bars.right, 0)
        bottomInset = bars.bottom
        update(v.width)
        insets
    }
    addOnLayoutChangeListener { v, left, _, right, _, oldLeft, _, oldRight, _ ->
        val width = right - left
        if (width != oldRight - oldLeft) v.post { update(v.width) }
    }
}

/**
 * Up navigation for child screens: the parent is normally already on the back stack, so Up is
 * simply finish(). When the screen is the task root (deep link, share link), go home instead.
 */
fun AppCompatActivity.navigateUpOrHome(): Boolean {
    if (isTaskRoot) {
        val intent = Intent(this, MeActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }
    finish()
    return true
}

/**
 * Adds the larger of the bottom system-bar inset and the IME inset to this view's bottom padding.
 * Use on screens with text input so the content and FAB stay above the keyboard under
 * edge-to-edge (windowSoftInputMode=adjustResize is ignored once decorFitsSystemWindows is false).
 */
fun View.applyImeAndBarInsetsAsPadding() {
    val basePadding = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val bars = insets.getInsets(insetTypes).bottom
        val ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
        v.updatePadding(bottom = basePadding + maxOf(bars, ime))
        insets
    }
}
