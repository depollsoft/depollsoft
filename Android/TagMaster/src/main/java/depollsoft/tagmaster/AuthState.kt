package depollsoft.tagmaster

import androidx.annotation.VisibleForTesting
import com.bindroid.trackable.Trackable
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

/**
 * The app's single, trackable view of whether someone is signed in.
 *
 * [TagMasterApplication] calls [notifyChanged] from its Firebase auth-state listener, so any
 * binding that reads [isSignedIn] repaints whenever Firebase's real auth state changes. Screens
 * must not infer sign-in from FirebaseUI's activity result alone: that result never arrives if
 * FirebaseUI dies after Firebase has already accepted the account.
 */
object AuthState {
    private val trackable = Trackable()
    private val firebaseSignedIn: () -> Boolean = { Firebase.auth.currentUser != null }

    @Volatile
    private var signedInSource: () -> Boolean = firebaseSignedIn

    val isSignedIn: Boolean
        get() {
            trackable.track()
            return signedInSource()
        }

    /** Re-evaluates every binding that read [isSignedIn]. Call on the main thread. */
    fun notifyChanged() {
        trackable.updateTrackers()
    }

    /**
     * Replaces Firebase as the source of truth in unit tests; pass null to restore it.
     * Does not notify, so trackers left over from an earlier test never reach Firebase.
     */
    @VisibleForTesting
    internal fun setTestSource(source: (() -> Boolean)?) {
        signedInSource = source ?: firebaseSignedIn
    }
}
