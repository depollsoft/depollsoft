package depollsoft.lib.auth

/** What a finished FirebaseUI sign-in flow means for the app. */
enum class SignInOutcome {
    /** Firebase has a signed-in user, whatever FirebaseUI reported. */
    SIGNED_IN,

    /** FirebaseUI reported success or an error, but Firebase has no user. */
    FAILED,

    /** The person backed out before signing in. */
    CANCELED,
    ;

    companion object {
        /**
         * Resolves a FirebaseUI activity result against the real Firebase auth state.
         *
         * FirebaseUI can finish with a canceled or error result after Firebase itself
         * has already signed the user in (for example when its post-sign-in credential
         * save step fails for a Facebook account with no email address). The auth
         * state is therefore the source of truth: a signed-in user is always a success.
         */
        fun resolve(
            isSignedIn: Boolean,
            resultOk: Boolean,
            hasError: Boolean,
        ): SignInOutcome =
            when {
                isSignedIn -> SIGNED_IN
                resultOk || hasError -> FAILED
                else -> CANCELED
            }
    }
}
