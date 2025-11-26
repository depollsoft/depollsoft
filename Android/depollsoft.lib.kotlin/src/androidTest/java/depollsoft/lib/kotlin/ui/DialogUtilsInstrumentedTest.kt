package depollsoft.lib.kotlin.ui

import androidx.test.core.app.ActivityScenario
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DialogUtilsInstrumentedTest {

    @Test
    fun safeDismiss_dismisses_when_showing() {
        ActivityScenario.launch(TestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val dialog = android.app.Dialog(activity)
                assertFalse(dialog.isShowing)
                dialog.show()
                assertTrue(dialog.isShowing)
                dialog.safeDismiss()
                assertFalse(dialog.isShowing)
            }
        }
    }
}
