package depollsoft.tagmaster

import androidx.annotation.VisibleForTesting
import depollsoft.lib.state.ChangeSignal
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

/**
 * The app's single, observable view of whether someone is signed in.
 *
 * [TagMasterApplication] calls [notifyChanged] from its Firebase auth-state listener, so any
 * screen that reads [isSignedIn] recomposes whenever Firebase's real auth state changes. Screens
 * must not infer sign-in from FirebaseUI's activity result alone: that result never arrives if
 * FirebaseUI dies after Firebase has already accepted the account.
 */
object AuthState {
    private val signal = ChangeSignal()
    private val firebaseSignedIn: () -> Boolean = { Firebase.auth.currentUser != null }

    @Volatile
    private var signedInSource: () -> Boolean = firebaseSignedIn

    val isSignedIn: Boolean
        get() {
            signal.read()
            return signedInSource()
        }

    /** Tells everything that read [isSignedIn] to read it again. */
    fun notifyChanged() {
        signal.changed()
    }

    /**
     * Replaces Firebase as the source of truth in unit tests; pass null to restore it.
     * Does not notify, so readers left over from an earlier test never reach Firebase.
     */
    @VisibleForTesting
    internal fun setTestSource(source: (() -> Boolean)?) {
        signedInSource = source ?: firebaseSignedIn
    }
}
