package depollsoft.pitchperfect

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import depollsoft.lib.licensing.LicenseChecker
import depollsoft.lib.util.preference
import depollsoft.pitchperfect.lib.Note

object SettingsModel {
    private const val TOGGLE_NOTE_KEY = "depollsoft.pitchperfect.ToggleNote"
    private const val WAKE_LOCK_KEY = "depollsoft.pitchperfect.WakeLock"
    private const val ARE_ADS_REMOVED_KEY = "depollsoft.pitchperfect.AreAdsRemoved"
    private const val REFERENCE_PITCH_KEY = "depollsoft.pitchperfect.ReferencePitch"

    /** The A4 frequencies a stored or synced setting may hold; anything else reads as 440 Hz. */
    val REFERENCE_PITCH_RANGE = 400..480

    private var userRef: DocumentReference? = null
    private var listenerRegistration: ListenerRegistration? = null
    private val attachment = AuthAttachmentState()
    private var restoring = false

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

    /** The A4 the notes are tuned to, in Hz: one of [Note.COMMON_A4_FREQUENCIES]. */
    var referencePitch: Int
        get() = storedReferencePitch.takeIf { it in REFERENCE_PITCH_RANGE } ?: Note.STANDARD_A4.toInt()
        set(value) {
            if (value in REFERENCE_PITCH_RANGE) storedReferencePitch = value
        }

    private var storedReferencePitch: Int by preference(REFERENCE_PITCH_KEY, Note.STANDARD_A4.toInt()) {
        Note.setReferencePitch(referencePitch.toDouble())
        PitchPipeAppWidget.updateWidgets()
        if (!restoring) {
            userRef?.set(mapOf("referencePitch" to it), SetOptions.merge())
        }
    }

    /** Tunes the notes to the stored A4; call once at startup. */
    fun applyReferencePitch() {
        Note.setReferencePitch(referencePitch.toDouble())
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
                    data.getLong("referencePitch")?.toInt()?.let { remote ->
                        if (remote in REFERENCE_PITCH_RANGE && remote != referencePitch) referencePitch = remote
                    }
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
