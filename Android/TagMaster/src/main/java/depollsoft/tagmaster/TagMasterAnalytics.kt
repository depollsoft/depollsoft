package depollsoft.tagmaster

import depollsoft.lib.analytics.UsageAnalytics
import depollsoft.lib.review.ReviewPrompt

/**
 * Tag Master's screens and events (docs/analytics.md), and the review prompt's signals that go
 * with them. iOS sends the same names.
 */
object TagMasterAnalytics {
    const val SCREEN_HOME = "home"
    const val SCREEN_BROWSE = "browse"
    const val SCREEN_SEARCH = "search"
    const val SCREEN_SEARCH_RESULTS = "search_results"
    const val SCREEN_TAG_LIST = "tag_list"
    const val SCREEN_TEACHABLE_TAGS = "teachable_tags"
    const val SCREEN_SHEET_MUSIC = "sheet_music"
    const val SCREEN_SETTINGS = "settings"

    /** A tag's page (Summary, Details, Tracks, Videos) by its index. */
    fun tagPageScreen(page: Int): String = TAG_PAGES.getOrElse(page) { TAG_PAGES[0] }

    private val TAG_PAGES = listOf("tag_summary", "tag_details", "tag_tracks", "tag_videos")

    /** Where a key note was played from. */
    const val SOURCE_TAG = "tag"
    const val SOURCE_SHEET_MUSIC = "sheet_music"

    /**
     * The learning-track part at [index] of the parts the Tracks page lists: all parts, tenor,
     * lead, baritone, bass, then up to four others.
     */
    fun trackPart(index: Int): String = TRACK_PARTS.getOrElse(index) { "other" }

    private val TRACK_PARTS = listOf("all", "tenor", "lead", "baritone", "bass")

    fun tagViewed() {
        UsageAnalytics.event("tag_viewed")
        ReviewPrompt.recordUse()
    }

    fun keyNotePlayed(source: String) {
        UsageAnalytics.event(UsageAnalytics.PITCH_PLAYED, UsageAnalytics.SOURCE to source)
        ReviewPrompt.recordSound()
    }

    fun learningTrackPlayed(part: String) {
        UsageAnalytics.event("learning_track_played", "part" to part)
        ReviewPrompt.recordSound()
    }

    /** A paused learning track played on: a sound, but not another play. */
    fun learningTrackResumed() {
        ReviewPrompt.recordSound()
    }

    fun videoOpened() {
        UsageAnalytics.event("video_opened")
        ReviewPrompt.recordSound()
    }

    /** Someone put a tag on the list [key]; undo, sync and migration don't count. */
    fun tagAddedToList(key: String) {
        val list =
            when (key) {
                TagLists.FAVORITE -> "favorites"
                TagLists.TEACHABLE -> "teachable"
                else -> "custom"
            }
        UsageAnalytics.event("tag_added_to_list", "list" to list)
        ReviewPrompt.taskFinished()
    }

    fun tagListCreated() {
        UsageAnalytics.event("tag_list_created")
        ReviewPrompt.taskFinished()
    }
}

/**
 * What is sounding right now (a key note, a learning track), so the review prompt never asks over
 * it. Main thread only.
 */
object Sounding {
    private val sources = mutableSetOf<Any>()

    val any: Boolean get() = sources.isNotEmpty()

    fun started(source: Any) {
        sources.add(source)
    }

    fun stopped(source: Any) {
        sources.remove(source)
    }
}
