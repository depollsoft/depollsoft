package depollsoft.tagmaster

import android.os.Handler
import android.os.Looper
import depollsoft.lib.state.StateField
import depollsoft.lib.state.StateList
import depollsoft.tagmaster.barbershop.Tag
import depollsoft.tagmaster.barbershop.TagCollection
import depollsoft.tagmaster.barbershop.TagQueryResult
import depollsoft.tagmaster.barbershop.TagSortOptions

/**
 * One catalog query and the pages of it fetched so far.
 *
 * It travels between screens (and across recreation) as JSON through [depollsoft.lib.json.JsonSerializer],
 * so every property keeps its getter/setter pair and its name.
 */
class QueryModel {
    private var mostRecentResult: TagQueryResult =
        TagQueryResult().apply {
            start = 0
            count = 0
        }
    var maxResults: Int by StateField(50)
    var statusText: String? by StateField(null)
    var collection: TagCollection? by StateField(null)
    var hasMoreResults: Boolean by StateField(true)
    var isLoading: Boolean by StateField(false)
        @JvmName("getIsLoading")
        get

        @JvmName("setIsLoading")
        set
    var query: String? by StateField(null)
    var resultSetSize: Int by StateField(20)
    var parts: Int? by StateField(null)
    var hasLearningTracks: Boolean? by StateField(null)
    var hasSheetMusic: Boolean? by StateField(null)
    var sortBy: TagSortOptions? by StateField(null)
    var tags: StateList<Tag> by StateField(StateList())
    var minimumRating: Double? by StateField(null)
    var minimumDownloads: Int? by StateField(null)

    /**
     * Which run of pages a response belongs to. [refresh] starts a new one, so a page still
     * loading from before it is dropped rather than appended to the new first page. Private and
     * without accessors, so it is not stored with the query.
     */
    private var generation = 0

    /** Starts over from the first page, even when every result had already loaded. */
    fun refresh() {
        generation++
        mostRecentResult = TagQueryResult()
        mostRecentResult.start = 0
        mostRecentResult.count = 0
        tags.clear()
        statusText = null
        hasMoreResults = true
        isLoading = false
        fetchResults()
    }

    /** Fetches the next page, unless one is already loading or there are no more. */
    fun fetchResults() {
        if (isLoading) return
        if (!hasMoreResults) return
        isLoading = true
        val asked = generation
        Tag
            .query(
                query,
                resultSetSize,
                mostRecentResult.start + mostRecentResult.count,
                parts,
                hasLearningTracks,
                hasSheetMusic,
                collection,
                sortBy,
                minimumRating,
                minimumDownloads,
                false,
                null,
            ).continueWith<Void> { task ->
                main.post {
                    if (asked != generation) return@post
                    if (task.isFaulted) {
                        statusText = "An error has occurred: " + task.error.message
                        isLoading = false
                    } else {
                        try {
                            statusText = null
                            mostRecentResult = task.result
                            tags.addAll(task.result.tags)
                            hasMoreResults =
                                mostRecentResult.start + mostRecentResult.count <
                                minOf(mostRecentResult.available, maxResults)
                            if (task.result.available == 0) {
                                statusText = "No tags could be found that matched your query."
                            }
                        } finally {
                            isLoading = false
                        }
                    }
                }
                null
            }
    }

    private companion object {
        val main by lazy { Handler(Looper.getMainLooper()) }
    }
}
