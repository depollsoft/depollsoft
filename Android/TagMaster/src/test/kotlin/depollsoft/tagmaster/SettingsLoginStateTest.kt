package depollsoft.tagmaster

import android.app.Application
import android.os.Looper
import android.view.View
import depollsoft.lib.activity.RichApplication
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

/**
 * The Settings screen's log in / log out buttons follow Firebase's real auth state, not the
 * FirebaseUI activity result: a Facebook sign-in whose result never arrives must still flip
 * them as soon as the auth-state listener reports the new user.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class SettingsLoginStateTest {
    private var signedIn = false
    private var controller: ActivityController<SettingsActivity>? = null

    @Before
    fun setUp() {
        RichApplication::class.java
            .getDeclaredField("context")
            .apply { isAccessible = true }
            .set(null, RuntimeEnvironment.getApplication())
        ListModel.setTestMode(true)
        // SettingsModel's `preference` delegates register their defaults once, into whichever
        // store was selected at that moment. Another class may since have emptied or swapped that
        // store, which would leave SettingsActivity reading null here. Put them back.
        ScreenTestSupport.seedSettingsDefaults()
        AuthState.setTestSource { signedIn }
    }

    @After
    fun tearDown() {
        controller?.destroy()
        controller = null
        AuthState.setTestSource(null)
    }

    @Test
    fun loginButtons_followAuthStateWithoutAnActivityResult() {
        val activity = launchSettings()
        idle()
        val loginButton = activity.findViewById<View>(R.id.loginButton)
        val logoutButton = activity.findViewById<View>(R.id.logoutButton)
        assertEquals(View.VISIBLE, loginButton.visibility)
        assertEquals(View.GONE, logoutButton.visibility)

        signedIn = true
        AuthState.notifyChanged()
        idle()
        assertEquals(View.GONE, loginButton.visibility)
        assertEquals(View.VISIBLE, logoutButton.visibility)

        signedIn = false
        AuthState.notifyChanged()
        idle()
        assertEquals(View.VISIBLE, loginButton.visibility)
        assertEquals(View.GONE, logoutButton.visibility)
    }

    @Test
    fun resumingSettings_repaintsFromAuthState() {
        val activity = launchSettings()
        idle()
        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.loginButton).visibility)

        controller!!.pause()
        signedIn = true
        controller!!.resume()
        idle()

        assertEquals(View.GONE, activity.findViewById<View>(R.id.loginButton).visibility)
        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.logoutButton).visibility)
    }

    private fun launchSettings(): SettingsActivity {
        val built = Robolectric.buildActivity(SettingsActivity::class.java)
        built.get().setTheme(R.style.AppTheme)
        controller = built
        return built.setup().get()
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()
}
