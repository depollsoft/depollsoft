package depollsoft.tagmaster

import android.app.Application
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Settings shows Log in or Log out from the real auth state, with no activity result needed. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class SettingsLoginStateTest : ComposeScreenTest() {
    private var signedIn = false

    private fun settings(): SettingsActivity {
        ScreenTestSupport.seedSettingsDefaults()
        AuthState.setTestSource { signedIn }
        return launch(SettingsActivity::class.java)
    }

    @Test
    fun theLoginButtonsFollowAuthStateWithoutAnActivityResult() {
        settings()
        assertTrue(exists("loginButton"))
        assertFalse(exists("logoutButton"))
        signedIn = true
        AuthState.notifyChanged()
        idle()
        assertFalse(exists("loginButton"))
        assertTrue(exists("logoutButton"))
        signedIn = false
        AuthState.notifyChanged()
        idle()
        assertTrue(exists("loginButton"))
        assertFalse(exists("logoutButton"))
    }

    @Test
    fun resumingSettingsRepaintsFromAuthState() {
        settings()
        assertTrue(exists("loginButton"))
        controller!!.pause()
        signedIn = true
        controller!!.resume()
        idle()
        assertFalse(exists("loginButton"))
        assertTrue(exists("logoutButton"))
    }

    @Test
    fun theThemeChoiceIsSavedAndApplied() {
        settings()
        click("theme:1")
        assertTrue(TagMasterApplication.themeMode != androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }
}
