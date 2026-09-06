package depollsoft.pitchperfect

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import depollsoft.lib.activity.RichApplication
import depollsoft.lib.licensing.LicenseChecker
import depollsoft.lib.util.Preferences
import depollsoft.lib.util.preference
import kotlinx.coroutines.tasks.await

object SettingsModel {
    private const val TOGGLE_NOTE_KEY = "depollsoft.pitchperfect.ToggleNote"
    private const val WAKE_LOCK_KEY = "depollsoft.pitchperfect.WakeLock"
    private const val ARE_ADS_REMOVED_KEY = "depollsoft.pitchperfect.AreAdsRemoved"

    private var userRef: DocumentReference? = null
    private var listenerRegistration: ListenerRegistration? = null
    private val attachment = AuthAttachmentState()
    private var restoring = false

    val appStore: String
        get() = RichApplication.getAppContext().getString(R.string.app_store)
    var areAdsRemoved: Boolean by preference(ARE_ADS_REMOVED_KEY, false)

    @JvmStatic
    var toggleNotes: Boolean by preference(TOGGLE_NOTE_KEY, false) {
        if (!restoring) {
            userRef?.set(mapOf("toggleNotes" to it), SetOptions.merge())
        }
    }
    var wakeLock: Boolean by preference(WAKE_LOCK_KEY, false) {
        if (!restoring) {
            userRef?.set(mapOf("wakeLock" to it), SetOptions.merge())
        }
    }

    val licensed: Boolean
        get() = LicenseChecker.isLicensed()

    fun attachToFirestore() {
        val user = Firebase.auth.currentUser ?: return
        if (attachment.isConnectedTo(user.uid, listenerRegistration != null)) return
        detachFromFirestore()
        attachment.connect(user.uid)
        userRef = Firebase.firestore.document("users/${user.uid}")
        listenerRegistration =
            userRef!!.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                val data = snapshot ?: return@addSnapshotListener
                restoring = true
                try {
                    wakeLock = data.getBoolean("wakeLock") ?: wakeLock
                    toggleNotes = data.getBoolean("toggleNotes") ?: toggleNotes
                } finally {
                    restoring = false
                }
            }
    }

    fun detachFromFirestore() {
        listenerRegistration?.remove()
        listenerRegistration = null
        userRef = null
        attachment.clear()
    }
}
