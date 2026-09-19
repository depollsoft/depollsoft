package depollsoft.lib.auth

import org.junit.Assert.assertEquals
import org.junit.Test

class SignInOutcomeTest {
    @Test
    fun okResultWithUser_isSignedIn() {
        assertEquals(
            SignInOutcome.SIGNED_IN,
            SignInOutcome.resolve(isSignedIn = true, resultOk = true, hasError = false),
        )
    }

    @Test
    fun canceledResultWithUser_isStillSignedIn() {
        // FirebaseUI's credential-save step can fail after Firebase sign-in succeeded.
        assertEquals(
            SignInOutcome.SIGNED_IN,
            SignInOutcome.resolve(isSignedIn = true, resultOk = false, hasError = false),
        )
    }

    @Test
    fun errorResultWithUser_isStillSignedIn() {
        assertEquals(
            SignInOutcome.SIGNED_IN,
            SignInOutcome.resolve(isSignedIn = true, resultOk = false, hasError = true),
        )
    }

    @Test
    fun okResultWithoutUser_isFailure() {
        assertEquals(
            SignInOutcome.FAILED,
            SignInOutcome.resolve(isSignedIn = false, resultOk = true, hasError = false),
        )
    }

    @Test
    fun errorResultWithoutUser_isFailure() {
        assertEquals(
            SignInOutcome.FAILED,
            SignInOutcome.resolve(isSignedIn = false, resultOk = false, hasError = true),
        )
    }

    @Test
    fun canceledResultWithoutUser_isCanceled() {
        assertEquals(
            SignInOutcome.CANCELED,
            SignInOutcome.resolve(isSignedIn = false, resultOk = false, hasError = false),
        )
    }
}
