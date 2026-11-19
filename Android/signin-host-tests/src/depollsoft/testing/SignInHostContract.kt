package depollsoft.testing

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.FirebaseAuthUI
import com.google.firebase.auth.FirebaseUser
import depollsoft.lib.auth.SignInOutcome
import depollsoft.lib.auth.SignInResult
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

/** Drive FirebaseUI's state flow through the real activity, then read what its caller receives. */
abstract class SignInHostContract<A : AppCompatActivity> {
    protected abstract val host: Class<A>
    protected abstract val uploadsSongsForNewAccount: Boolean

    protected abstract fun prepareSignedOutApp()

    private lateinit var controller: ActivityController<A>
    private val activity get() = controller.get()
    private val result get() = shadowOf(activity)

    @Before
    fun openHost() {
        prepareSignedOutApp()
        FirebaseAuthUI.getInstance().updateAuthState(AuthState.Idle)
        controller = Robolectric.buildActivity(host).setup()
        drain()
    }

    @After
    fun closeHost() {
        controller.close()
        FirebaseAuthUI.getInstance().updateAuthState(AuthState.Idle)
        drain()
    }

    private fun drain() = shadowOf(Looper.getMainLooper()).idle()

    private fun emit(state: AuthState) {
        FirebaseAuthUI.getInstance().updateAuthState(state)
        drain()
    }

    private fun success() {
        val state = mock(AuthState.Success::class.java, RETURNS_DEEP_STUBS)
        `when`(state.result!!.additionalUserInfo!!.providerId).thenReturn("google.com")
        `when`(state.isNewUser).thenReturn(true)
        emit(state)
    }

    private fun unverified(
        created: Long,
        lastSignIn: Long,
    ) {
        val user = mock(FirebaseUser::class.java, RETURNS_DEEP_STUBS)
        `when`(user.metadata!!.creationTimestamp).thenReturn(created)
        `when`(user.metadata!!.lastSignInTimestamp).thenReturn(lastSignIn)
        emit(AuthState.RequiresEmailVerification(user, "choir@example.invalid"))
    }

    private fun assertSignedIn(
        provider: String,
        isNew: Boolean,
    ) {
        assertTrue(activity.isFinishing)
        assertEquals(RESULT_OK, result.resultCode)
        assertEquals(provider, SignInResult.provider(result.resultIntent))
        assertEquals(isNew, SignInResult.isNewUser(result.resultIntent))
    }

    @Test
    fun newUnverifiedEmailAccount() {
        unverified(100_000L, 100_001L)
        assertSignedIn("password", uploadsSongsForNewAccount)
    }

    @Test
    fun returningUnverifiedEmailAccountNeverUploadsLocalSongs() {
        unverified(100_000L, 100_000L + 86_400_000L)
        assertSignedIn("password", false)
    }

    @Test
    fun successfulRetryReplacesTheErrorWithProviderAndNewUserExtras() {
        emit(AuthState.Error(IllegalStateException("try again")))
        assertFalse(activity.isFinishing)
        success()
        assertSignedIn("google.com", true)
    }

    @Test
    fun abortReturnsCancellationWithoutAnError() {
        emit(AuthState.Aborted)
        assertTrue(activity.isFinishing)
        assertEquals(RESULT_CANCELED, result.resultCode)
        assertNull(result.resultIntent)
    }

    @Test
    fun backAfterErrorStillReportsFailureAfterRecreation() {
        emit(AuthState.Error(IllegalStateException("try again")))
        assertFalse(activity.isFinishing)
        val before = activity
        controller.recreate()
        drain()
        assertNotSame(before, activity)
        assertFalse(activity.isFinishing)
        activity.onBackPressedDispatcher.onBackPressed()
        drain()
        assertTrue(activity.isFinishing)
        assertEquals(RESULT_CANCELED, result.resultCode)
        assertEquals(SignInOutcome.FAILED, SignInResult.outcome(result.resultCode, result.resultIntent, isSignedIn = false))
    }
}
