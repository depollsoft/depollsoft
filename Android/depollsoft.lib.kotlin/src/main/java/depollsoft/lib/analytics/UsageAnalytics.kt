package depollsoft.lib.analytics

/**
 * The screens and feature use both apps report to Google Analytics, under the names docs/analytics.md
 * lists; iOS reports the same names (iOS/shared/UsageAnalytics.swift).
 *
 * Each app points [sink] at Firebase Analytics, which collects nothing unless the person allowed usage
 * analytics in Privacy choices. Values are short fixed words, never anything someone typed or named.
 */
object UsageAnalytics {
    interface Sink {
        fun logEvent(name: String, params: Map<String, String>)
        fun setUserProperty(name: String, value: String)
    }

    private val nowhere = object : Sink {
        override fun logEvent(name: String, params: Map<String, String>) {}
        override fun setUserProperty(name: String, value: String) {}
    }

    /** Where events go: nowhere until the app sets it, and a recorder in tests. */
    var sink: Sink = nowhere

    /** Reports that [name] is now the screen in front. */
    fun screen(name: String) = sink.logEvent(SCREEN_VIEW, mapOf(SCREEN_NAME to name, SCREEN_CLASS to name))

    fun event(name: String, vararg params: Pair<String, String>) = sink.logEvent(name, params.toMap())

    fun userProperty(name: String, value: String) = sink.setUserProperty(name, value)

    /** Sign-in succeeded with the Firebase provider [providerId] (`google.com`, `password`, ...). */
    fun login(providerId: String?) = event(LOGIN, METHOD to signInMethod(providerId))

    fun signedIn(signedIn: Boolean) = userProperty(SIGNED_IN, if (signedIn) "yes" else "no")

    fun signInMethod(providerId: String?): String = when (providerId) {
        "google.com" -> "google"
        "apple.com" -> "apple"
        "facebook.com" -> "facebook"
        "password", "emailLink" -> "email"
        "phone" -> "phone"
        else -> "other"
    }

    /** Sends events nowhere again; for tests. */
    fun resetForTesting() {
        sink = nowhere
    }

    // Google Analytics' own names for a screen view and a sign-in.
    const val SCREEN_VIEW = "screen_view"
    const val SCREEN_NAME = "screen_name"
    const val SCREEN_CLASS = "screen_class"
    const val LOGIN = "login"
    const val METHOD = "method"

    // Names both apps use.
    const val PITCH_PLAYED = "pitch_played"
    const val SOURCE = "source"
    const val SIGNED_IN = "signed_in"
}
