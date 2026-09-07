package depollsoft.tagmaster

import android.view.View
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestDeskApplication::class, sdk = [35])
class DeskInsetsTest {
    @Test
    fun bars_cutout_and_keyboard_apply_once_and_restore_when_keyboard_hides() {
        val view = View(RuntimeEnvironment.getApplication())
        view.setPadding(0, 12, 0, 0)
        view.applyDeskInsets()
        val bars =
            WindowInsetsCompat
                .Builder()
                .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(0, 24, 0, 32))
                .setInsets(WindowInsetsCompat.Type.displayCutout(), Insets.of(18, 0, 9, 0))
                .build()
        val keyboard =
            WindowInsetsCompat
                .Builder(bars)
                .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, 300))
                .build()
        repeat(2) { ViewCompat.dispatchApplyWindowInsets(view, keyboard) }
        assertEquals(18, view.paddingLeft)
        assertEquals(9, view.paddingRight)
        assertEquals(12, view.paddingTop)
        assertEquals(300, view.paddingBottom)
        ViewCompat.dispatchApplyWindowInsets(view, bars)
        assertEquals(32, view.paddingBottom)
    }

    @Test
    fun invalid_detail_has_an_error_instead_of_loading_or_retrying_an_invalid_id() {
        val controller = Robolectric.buildActivity(TagDetailActivity::class.java).setup()
        val activity = controller.get()
        assertNull(activity.tag)
        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.detailError).visibility)
        assertEquals(View.GONE, activity.findViewById<View>(R.id.detailProgress).visibility)
        assertEquals(View.GONE, activity.findViewById<View>(R.id.detailRetryButton).visibility)
        assertTrue(
            activity.supportActionBar!!.displayOptions and
                androidx.appcompat.app.ActionBar.DISPLAY_HOME_AS_UP != 0,
        )
        controller.pause().stop().destroy()
    }
}
