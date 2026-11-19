package depollsoft.lib.auth

import android.app.Activity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SignInResultTest {
    @Test
    fun aSignInCarriesItsProviderAndWhetherTheAccountIsNew() {
        val data = SignInResult.signedIn("google.com", isNewUser = true)
        assertEquals("google.com", SignInResult.provider(data))
        assertTrue(SignInResult.isNewUser(data))
        assertEquals(SignInOutcome.SIGNED_IN, SignInResult.outcome(Activity.RESULT_OK, data, isSignedIn = true))
    }

    @Test
    fun noDataMeansNoProviderAndAnExistingAccount() {
        assertNull(SignInResult.provider(null))
        assertFalse(SignInResult.isNewUser(null))
    }

    @Test
    fun anErrorWithNoUserFails() {
        assertEquals(
            SignInOutcome.FAILED,
            SignInResult.outcome(Activity.RESULT_CANCELED, SignInResult.failed(), isSignedIn = false),
        )
    }

    @Test
    fun anErrorAfterFirebaseSignedThePersonInStillSignsIn() {
        assertEquals(
            SignInOutcome.SIGNED_IN,
            SignInResult.outcome(Activity.RESULT_CANCELED, SignInResult.failed(), isSignedIn = true),
        )
    }

    @Test
    fun leavingWithoutAnErrorCancels() {
        assertEquals(SignInOutcome.CANCELED, SignInResult.outcome(Activity.RESULT_CANCELED, null, isSignedIn = false))
    }

    @Test
    fun anOkResultWithNoUserFails() {
        assertEquals(SignInOutcome.FAILED, SignInResult.outcome(Activity.RESULT_OK, null, isSignedIn = false))
    }
}
