package depollsoft.pitchperfect

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

    fun songAdded() = taskFinished("song_added")

    fun setListCreated() = taskFinished("set_list_created")

    fun songsAddedToSetList() = taskFinished("songs_added_to_set_list")

    private fun taskFinished(event: String) {
        UsageAnalytics.event(event)
        ReviewPrompt.taskFinished()
    }

    /** Reports the settings Pitch Perfect's user properties follow; at launch and when one changes. */
    fun reportSettings() {
        UsageAnalytics.userProperty("pitch_pipe_style", if (SettingsModel.classicPitchPipe) "classic" else "radial")
        UsageAnalytics.userProperty("note_sound", SettingsModel.noteSound.id)
        UsageAnalytics.userProperty("reference_pitch", SettingsModel.referencePitch.toString())
    }
}
