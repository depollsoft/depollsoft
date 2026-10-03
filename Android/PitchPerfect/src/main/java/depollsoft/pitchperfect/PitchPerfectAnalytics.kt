package depollsoft.pitchperfect

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import depollsoft.lib.analytics.UsageAnalytics
import depollsoft.lib.review.ReviewPrompt

/**
 * Pitch Perfect's screens, events and user properties (docs/analytics.md), and the review prompt's
 * signals that go with them. iOS sends the same names.
 */
object PitchPerfectAnalytics {
    /** Where a pitch was played from. */
    enum class Source(val value: String) {
        PITCH_PIPE("pitch_pipe"),
        CLASSIC_PITCH_PIPE("classic_pitch_pipe"),
        NOTES("notes"),
        KEYS("keys"),
        SONG("song"),
        WIDGET("widget"),
    }

    fun screenFor(tab: MainTab): String =
        when (tab) {
            MainTab.PITCH_PIPE -> "pitch_pipe"
            MainTab.NOTES -> "notes"
            MainTab.KEYS -> "keys"
            MainTab.SONGS -> "songs"
        }

    const val SCREEN_SET_LISTS = "set_lists"
    const val SCREEN_SONG_EDITOR = "song_editor"
    const val SCREEN_ADD_SONGS = "add_songs"
    const val SCREEN_SETTINGS = "settings"

    fun pitchPlayed(source: Source) {
        UsageAnalytics.event(UsageAnalytics.PITCH_PLAYED, UsageAnalytics.SOURCE to source.value)
        ReviewPrompt.recordUse()
        ReviewPrompt.recordSound()
    }

    /** The app made a sound that isn't a pitch played (Settings' sound preview). */
    fun soundPlayed() = ReviewPrompt.recordSound()

    /**
     * A note stopped sounding. The quiet a review waits for runs from the end of a sound, so a note
     * held (or toggled on) for minutes still counts as recent.
     */
    fun soundStopped() = ReviewPrompt.recordSound()

    /**
     * Sign-in succeeded. FirebaseUI can report a cancellation after Firebase has already accepted the
     * account, with no response to say how; then the account's ID token names the provider, as on iOS.
     */
    fun signedIn(providerType: String?) {
        if (providerType != null) return UsageAnalytics.login(providerType)
        val user = Firebase.auth.currentUser ?: return UsageAnalytics.login(null)
        user.getIdToken(false)
            .addOnSuccessListener { UsageAnalytics.login(it.signInProvider) }
            .addOnFailureListener { UsageAnalytics.login(null) }
    }

    fun songAdded() = taskFinished("song_added")

    fun setListCreated() = taskFinished("set_list_created")

    fun songsAddedToSetList() = taskFinished("songs_added_to_set_list")

    private fun taskFinished(event: String) {
        UsageAnalytics.event(event)
        ReviewPrompt.taskFinished()
    }

    /**
     * Reports the settings Pitch Perfect's user properties follow: at launch, when analytics is
     * allowed, and whenever one changes here or arrives from another device.
     */
    fun reportSettings() {
        UsageAnalytics.userProperty("pitch_pipe_style", if (SettingsModel.classicPitchPipe) "classic" else "radial")
        UsageAnalytics.userProperty("note_sound", SettingsModel.noteSound.id)
        UsageAnalytics.userProperty("reference_pitch", SettingsModel.referencePitch.toString())
    }
}
