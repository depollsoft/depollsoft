package depollsoft.lib.auth

import android.app.Activity
import android.content.Intent

/**
 * What an app's sign-in activity hands back to whatever launched it: the provider that signed the
 * person in, whether the account is new, and whether FirebaseUI reported an error on the way.
 */
object SignInResult {
    private const val EXTRA_PROVIDER = "depollsoft.lib.auth.PROVIDER"
    private const val EXTRA_NEW_USER = "depollsoft.lib.auth.NEW_USER"
    private const val EXTRA_FAILED = "depollsoft.lib.auth.FAILED"

    /** The data for a finished sign-in. [provider] is a Firebase provider ID such as `google.com`. */
    fun signedIn(
        provider: String?,
        isNewUser: Boolean,
    ): Intent = Intent().putExtra(EXTRA_PROVIDER, provider).putExtra(EXTRA_NEW_USER, isNewUser)

    /** The data for a flow that showed an error; it stands until the person signs in or leaves. */
    fun failed(): Intent = Intent().putExtra(EXTRA_FAILED, true)

    fun provider(data: Intent?): String? = data?.getStringExtra(EXTRA_PROVIDER)

    fun isNewUser(data: Intent?): Boolean = data?.getBooleanExtra(EXTRA_NEW_USER, false) == true

    /** Resolves a result against Firebase's auth state, which has the last word. */
    fun outcome(
        resultCode: Int,
        data: Intent?,
        isSignedIn: Boolean,
    ): SignInOutcome =
        SignInOutcome.resolve(
            isSignedIn = isSignedIn,
            resultOk = resultCode == Activity.RESULT_OK,
            hasError = data?.getBooleanExtra(EXTRA_FAILED, false) == true,
        )
}
