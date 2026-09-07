package depollsoft.tagmaster

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.Menu
import android.view.View
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.navigation.NavigationBarView

/**
 * Shared behaviour for the singing desk shell.
 *
 * The navigation helpers work against [NavigationBarView] rather than
 * BottomNavigationView so a compact layout (navigation bar) and an expanded one
 * (navigation rail) can share the same wiring and the same ids.
 */

/** Native Up follows the existing stack, preserving the selected desk destination. */
fun androidx.appcompat.app.AppCompatActivity.enableDeskBack() {
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    addMenuProvider(
        object : androidx.core.view.MenuProvider {
            override fun onCreateMenu(
                menu: Menu,
                inflater: android.view.MenuInflater,
            ) {}

            override fun onMenuItemSelected(item: android.view.MenuItem): Boolean {
                if (item.itemId != android.R.id.home) return false
                if (supportFragmentManager.backStackEntryCount > 0 ||
                    this@enableDeskBack is TagDetailActivity && this@enableDeskBack.hasOpenMaterial
                ) {
                    onBackPressedDispatcher.onBackPressed()
                } else if (isTaskRoot) {
                    startActivity(android.content.Intent(this@enableDeskBack, MeActivity::class.java))
                    finish()
                } else {
                    onBackPressedDispatcher.onBackPressed()
                }
                return true
            }
        },
        this,
    )
}

private fun itemIndex(
    itemId: Int,
    menu: Menu,
): Int = (0 until menu.size()).firstOrNull { menu.getItem(it).itemId == itemId } ?: -1

private var NavigationBarView.selectedIndex: Int
    get() = itemIndex(this.selectedItemId, this.menu)
    set(value) {
        if (value in 0 until this.menu.size()) {
            this.selectedItemId = this.menu.getItem(value).itemId
        }
    }

/** Keeps a navigation bar or rail and a pager on the same destination. */
fun NavigationBarView.attachToPager(
    viewPager: ViewPager2,
    onChange: (Int) -> Unit = {},
) {
    this.setOnItemSelectedListener {
        val index = itemIndex(it.itemId, this.menu)
        if (index >= 0) {
            viewPager.currentItem = index
            onChange(index)
        }
        index >= 0
    }

    viewPager.registerOnPageChangeCallback(
        object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                this@attachToPager.selectedIndex = position
                onChange(position)
            }
        },
    )
}

/**
 * Applies the system bar, cutout and keyboard insets as padding.
 *
 * The app targets an SDK where edge-to-edge is enforced, so nothing may sit
 * behind the status bar, the gesture bar or the IME. The action bar pads itself;
 * this covers the sides and the bottom, and grows for the keyboard so a search
 * field stays visible while it is being typed into.
 */
fun View.applyDeskInsets(applyBottom: Boolean = true) {
    if (id in setOf(R.id.deskRoot, R.id.pageContainer, R.id.detailRoot, R.id.resultsRoot, R.id.settingsRoot, R.id.teachableRoot)) {
        background = PoleBackground(context)
    }
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val bars =
            windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
            )
        val ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
        view.updatePadding(
            left = bars.left,
            right = bars.right,
            bottom = if (applyBottom) maxOf(bars.bottom, ime.bottom) else 0,
        )
        WindowInsetsCompat
            .Builder(windowInsets)
            .setInsets(
                WindowInsetsCompat.Type.systemBars(),
                Insets.of(bars.left, bars.top, bars.right, 0),
            ).build()
    }
    ViewCompat.requestApplyInsets(this)
}

/**
 * The one piece of touch feedback in the app: a save or a selection that the
 * singer needs to trust landed — favouriting, marking teachable, choosing a
 * part. Not decoration, and never in place of a visible state change.
 */
fun View.confirmHaptic() {
    val constant =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.VIRTUAL_KEY
        }
    performHapticFeedback(constant)
}
