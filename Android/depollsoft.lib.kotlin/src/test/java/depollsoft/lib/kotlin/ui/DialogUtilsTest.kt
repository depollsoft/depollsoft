package depollsoft.lib.kotlin.ui

import android.app.Dialog
import org.robolectric.RuntimeEnvironment
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DialogUtilsTest {
    @Test
    fun safeDismiss_handles_not_showing_and_showing() {
        val context = RuntimeEnvironment.getApplication()
        val dialog = Dialog(context)

        // Not showing: should be no-op
        assertFalse(dialog.isShowing)
        dialog.safeDismiss()
        assertFalse(dialog.isShowing)

        // Showing: should dismiss
        dialog.show()
        assertTrue(dialog.isShowing)
        dialog.safeDismiss()
        assertFalse(dialog.isShowing)
    }
}
