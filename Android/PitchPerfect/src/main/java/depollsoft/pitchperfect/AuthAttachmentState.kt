package depollsoft.pitchperfect

/** Tracks one live Firestore attachment without depending on Firebase classes. */
internal class AuthAttachmentState {
    private var userId: String? = null

    fun isConnectedTo(
        candidateUserId: String,
        hasLiveListener: Boolean,
    ): Boolean = userId == candidateUserId && hasLiveListener

    fun connect(userId: String) {
        this.userId = userId
    }

    fun clear() {
        userId = null
    }
}
