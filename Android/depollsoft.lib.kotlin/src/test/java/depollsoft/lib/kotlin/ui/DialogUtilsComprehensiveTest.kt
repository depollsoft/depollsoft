package depollsoft.lib.kotlin.ui

import android.app.Dialog
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Comprehensive tests for DialogUtils.kt safeDismiss extension
 */
@RunWith(RobolectricTestRunner::class)
class DialogUtilsComprehensiveTest {

    // =====================
    // Basic safeDismiss tests
    // =====================

    @Test
    fun safeDismiss_on_not_showing_dialog_is_safe() {
        val context = RuntimeEnvironment.getApplication()
        val dialog = Dialog(context)
        
        assertFalse(dialog.isShowing)
        
        // Should not throw
        dialog.safeDismiss()
        
        assertFalse(dialog.isShowing)
    }

    @Test
    fun safeDismiss_on_showing_dialog_dismisses() {
        val context = RuntimeEnvironment.getApplication()
        val dialog = Dialog(context)
        
        dialog.show()
        assertTrue(dialog.isShowing)
        
        dialog.safeDismiss()
        
        assertFalse(dialog.isShowing)
    }

    @Test
    fun safeDismiss_can_be_called_multiple_times_safely() {
        val context = RuntimeEnvironment.getApplication()
        val dialog = Dialog(context)
        
        // Call multiple times when not showing
        dialog.safeDismiss()
        dialog.safeDismiss()
        dialog.safeDismiss()
        
        assertFalse(dialog.isShowing)
    }

    @Test
    fun safeDismiss_can_be_called_multiple_times_after_showing() {
        val context = RuntimeEnvironment.getApplication()
        val dialog = Dialog(context)
        
        dialog.show()
        assertTrue(dialog.isShowing)
        
        // First call dismisses
        dialog.safeDismiss()
        assertFalse(dialog.isShowing)
        
        // Subsequent calls should be safe
        dialog.safeDismiss()
        dialog.safeDismiss()
        
        assertFalse(dialog.isShowing)
    }

    // =====================
    // Show/dismiss cycles
    // =====================

    @Test
    fun safeDismiss_works_across_show_dismiss_cycles() {
        val context = RuntimeEnvironment.getApplication()
        val dialog = Dialog(context)
        
        // Cycle 1
        dialog.show()
        assertTrue(dialog.isShowing)
        dialog.safeDismiss()
        assertFalse(dialog.isShowing)
        
        // Cycle 2
        dialog.show()
        assertTrue(dialog.isShowing)
        dialog.safeDismiss()
        assertFalse(dialog.isShowing)
        
        // Cycle 3
        dialog.show()
        assertTrue(dialog.isShowing)
        dialog.safeDismiss()
        assertFalse(dialog.isShowing)
    }

    @Test
    fun safeDismiss_between_show_calls() {
        val context = RuntimeEnvironment.getApplication()
        val dialog = Dialog(context)
        
        dialog.show()
        dialog.safeDismiss()
        
        // Show again
        dialog.show()
        assertTrue(dialog.isShowing)
        
        dialog.safeDismiss()
        assertFalse(dialog.isShowing)
    }

    // =====================
    // Edge cases
    // =====================

    @Test
    fun safeDismiss_on_newly_created_dialog() {
        val context = RuntimeEnvironment.getApplication()
        val dialog = Dialog(context)
        
        // Immediately after creation, should be safe
        dialog.safeDismiss()
        assertFalse(dialog.isShowing)
    }

    @Test
    fun safeDismiss_after_regular_dismiss() {
        val context = RuntimeEnvironment.getApplication()
        val dialog = Dialog(context)
        
        dialog.show()
        dialog.dismiss() // regular dismiss
        
        // safeDismiss should still be safe
        dialog.safeDismiss()
        assertFalse(dialog.isShowing)
    }

    @Test
    fun safeDismiss_preserves_showing_state_after_not_showing() {
        val context = RuntimeEnvironment.getApplication()
        val dialog = Dialog(context)
        
        assertFalse(dialog.isShowing)
        dialog.safeDismiss()
        assertFalse(dialog.isShowing)
        
        // Can still show after safeDismiss
        dialog.show()
        assertTrue(dialog.isShowing)
    }

    // =====================
    // Multiple dialogs
    // =====================

    @Test
    fun safeDismiss_independent_between_dialogs() {
        val context = RuntimeEnvironment.getApplication()
        val dialog1 = Dialog(context)
        val dialog2 = Dialog(context)
        
        dialog1.show()
        dialog2.show()
        
        assertTrue(dialog1.isShowing)
        assertTrue(dialog2.isShowing)
        
        dialog1.safeDismiss()
        
        assertFalse(dialog1.isShowing)
        assertTrue(dialog2.isShowing)
        
        dialog2.safeDismiss()
        
        assertFalse(dialog1.isShowing)
        assertFalse(dialog2.isShowing)
    }

    @Test
    fun safeDismiss_on_multiple_dialogs_none_showing() {
        val context = RuntimeEnvironment.getApplication()
        val dialogs = (1..5).map { Dialog(context) }
        
        // All not showing
        dialogs.forEach { assertFalse(it.isShowing) }
        
        // Safe dismiss all
        dialogs.forEach { it.safeDismiss() }
        
        // Still none showing
        dialogs.forEach { assertFalse(it.isShowing) }
    }

    @Test
    fun safeDismiss_on_multiple_dialogs_all_showing() {
        val context = RuntimeEnvironment.getApplication()
        val dialogs = (1..3).map { Dialog(context) }
        
        // Show all
        dialogs.forEach { it.show() }
        dialogs.forEach { assertTrue(it.isShowing) }
        
        // Safe dismiss all
        dialogs.forEach { it.safeDismiss() }
        
        // All dismissed
        dialogs.forEach { assertFalse(it.isShowing) }
    }

    // =====================
    // Interleaved operations
    // =====================

    @Test
    fun safeDismiss_interleaved_with_show() {
        val context = RuntimeEnvironment.getApplication()
        val dialog = Dialog(context)
        
        dialog.safeDismiss() // not showing
        dialog.show()
        dialog.safeDismiss() // showing
        dialog.safeDismiss() // not showing
        dialog.show()
        dialog.show() // already showing
        dialog.safeDismiss()
        
        assertFalse(dialog.isShowing)
    }
}
