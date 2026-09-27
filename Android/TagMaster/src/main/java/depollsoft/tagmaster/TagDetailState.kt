package depollsoft.tagmaster

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.setValue
import bolts.Task
import depollsoft.lib.util.ContentCache
import depollsoft.tagmaster.barbershop.Tag

/**
 * One tag's detail as a screen shows it: which tag, the loaded [tag], whether a load is running or
 * failed, and which page (Summary, Details, Tracks, Videos) is open.
 *
 * The full-screen detail keeps one of these; a list screen's detail pane keeps one for every tag
 * it shows, so switching tags keeps the open page. [loader] is the tag source — the tag cache in
 * the app, a controlled task in tests.
 */
@Stable
class TagDetailState(
    initialTagId: Int,
    private val context: Context,
    var loader: (Int, Boolean) -> Task<Tag> = { id, refresh -> Tag.loadTagById(id, refresh) },
) {
    var tagId by mutableIntStateOf(initialTagId)
        private set

    /** The tag on display. Compared by identity: a refreshed tag is a new tag to show. */
    var tag: Tag? by mutableStateOf(null, referentialEqualityPolicy())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var loadFailed by mutableStateOf(false)
        private set

    /** Set when a refresh of an already-shown tag fails; the screen offers Retry. */
    var refreshFailed by mutableStateOf(false)
        private set

    /** The open page: 0 Summary, 1 Details, 2 Tracks, 3 Videos. */
    var page by mutableIntStateOf(0)

    /** True when the next loaded tag should fade in (a tag chosen in the pane, not the first). */
    var revealPending by mutableStateOf(false)
        internal set

    private var requestGeneration = 0
    private var retryRefresh = false
    private var active = true
    private val main = Handler(Looper.getMainLooper())

    /** Starts the first load, unless a tag is already shown or loading. */
    fun start() {
        active = true
        if (tag == null && !isLoading) load(false)
    }

    /** Stops delivering results to a screen that has gone away. */
    fun stop() {
        active = false
        ++requestGeneration
        isLoading = false
    }

    /** Switches to another tag, keeping the open page. */
    fun showTag(id: Int) {
        if (id == tagId && (tag != null || isLoading)) return
        ++requestGeneration
        tagId = id
        tag = null
        isLoading = false
        loadFailed = false
        refreshFailed = false
        revealPending = true
        load(false)
    }

    fun refresh() = load(true)

    fun retry() = load(retryRefresh)

    private fun load(refresh: Boolean) {
        if (isLoading || !active) return
        // No tag was named (an intent without the extra): there is nothing to load.
        if (tagId <= 0) return
        val requestedId = tagId
        val generation = ++requestGeneration
        val original = tag
        retryRefresh = refresh
        refreshFailed = false
        isLoading = true
        loadFailed = false

        val request =
            if (refresh && original != null) {
                Task
                    .callInBackground<Void> {
                        val cache = ContentCache(context)
                        for (location in listOfNotNull(
                            original.allPartsTrackUri,
                            original.baritoneTrackUri,
                            original.bassTrackUri,
                            original.leadTrackUri,
                            original.notationUri,
                            original.other1TrackUri,
                            original.other2TrackUri,
                            original.other3TrackUri,
                            original.other4TrackUri,
                            original.tenorTrackUri,
                            original.sheetMusicUri,
                        )) {
                            cache.deletePrivateContent(location.uri, location.type)
                            cache.deletePublicContent(location.uri, location.type)
                        }
                        null
                    }.continueWithTask { loader(requestedId, refresh) }
            } else {
                try {
                    loader(requestedId, refresh)
                } catch (error: Exception) {
                    Task.forError<Tag>(error)
                }
            }
        request.continueWith { task ->
            main.post {
                if (!active || generation != requestGeneration || tagId != requestedId) return@post
                isLoading = false
                if (!task.isFaulted && !task.isCancelled && task.result?.id == requestedId) {
                    tag = task.result
                } else {
                    // A refresh failure must not discard an already-visible tag.
                    loadFailed = true
                    if (tag != null) refreshFailed = true
                }
            }
            null
        }
    }

    /** For tests: shows [value] as if it had loaded. */
    internal fun showLoaded(value: Tag) {
        ++requestGeneration
        tagId = value.id
        isLoading = false
        loadFailed = false
        tag = value
    }
}
