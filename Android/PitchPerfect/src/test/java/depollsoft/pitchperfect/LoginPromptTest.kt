package depollsoft.pitchperfect

import android.app.Application
import android.os.Looper
import android.view.View
import androidx.activity.ComponentActivity
import com.google.android.material.button.MaterialButton
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class LoginPromptTest {
    @Test
    fun providerConfiguration_usesFirebaseUiMethodPicker() {
        assertFalse(LoginPrompt.CREDENTIAL_MANAGER_ENABLED)
        assertTrue(LoginPrompt.ALWAYS_SHOW_PROVIDER_CHOICE)
        assertEquals(
            setOf("password", "google.com", "facebook.com"),
            LoginPrompt.PROVIDER_IDS,
        )
    }

    @Test
    fun reusedDialog_resetsBusyStateEveryTimeItIsShown() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).create().get()
        activity.setTheme(R.style.AppTheme)
        val dialog = LoginPrompt.buildDialog(activity, false)

        dialog.show()
        shadowOf(Looper.getMainLooper()).idle()
        val loginButton = dialog.findViewById<MaterialButton>(R.id.login_button)!!
        val progress = dialog.findViewById<View>(R.id.loginProgress)!!
        val status = dialog.findViewById<View>(R.id.loginStatusText)!!

        loginButton.isEnabled = false
        progress.visibility = View.VISIBLE
        status.visibility = View.VISIBLE
        dialog.dismiss()
        dialog.show()
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(loginButton.isEnabled)
        assertEquals(View.GONE, progress.visibility)
        assertEquals(View.GONE, status.visibility)
    }
}
