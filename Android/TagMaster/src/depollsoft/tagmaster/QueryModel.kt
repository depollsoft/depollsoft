package depollsoft.tagmaster

import depollsoft.tagmaster.barbershop.TagQueryResult
import com.bindroid.trackable.TrackableField
import depollsoft.tagmaster.barbershop.TagCollection
import depollsoft.tagmaster.barbershop.TagSortOptions
import com.bindroid.trackable.TrackableCollection
import com.bindroid.trackable.trackable
import depollsoft.lib.ui.ThreadSwitchContext
import depollsoft.tagmaster.barbershop.Tag
import kotlin.Throws

class QueryModel {
    private var mostRecentResult: TagQueryResult = TagQueryResult().apply {
        start = 0
        count = 0
    }
    var maxResults: Int by trackable(50)
    var statusText: String? by trackable()
    var collection: TagCollection? by trackable()
    var hasMoreResults: Boolean by trackable(true)
    var isLoading: Boolean by trackable(false)
        @JvmName("getIsLoading")
        get
        @JvmName("setIsLoading")
        set
    var query: String? by trackable()
    var resultSetSize: Int by trackable(20)
    var parts: Int? by trackable()
    var hasLearningTracks: Boolean? by trackable()
    var hasSheetMusic: Boolean? by trackable()
    var sortBy: TagSortOptions? by trackable()
    var tags: TrackableCollection<Tag> by trackable(TrackableCollection<Tag>())
    var minimumRating: Double? by trackable()
    var minimumDownloads: Int? by trackable()
    fun refresh(context: ThreadSwitchContext) {
        mostRecentResult = TagQueryResult()
        mostRecentResult.start = 0
        mostRecentResult.count = 0
        tags.clear()
        fetchResults(context)
    }

    fun fetchResults(context: ThreadSwitchContext) {
        if (isLoading) return
        if (!hasMoreResults) return
        isLoading = true
        Tag.query(
            query, resultSetSize.toInt(),
            mostRecentResult.start + mostRecentResult.count, parts,
            hasLearningTracks, hasSheetMusic, collection,
            sortBy, minimumRating, minimumDownloads, false, null
        )
            .continueWith<Void> { task ->
                if (task.isFaulted) {
                    context.post {
                        statusText = "An error has occurred: " + task.error.message
                        isLoading = false
                    }
                } else {
                    context.post {
                        try {
                            statusText = null
                            mostRecentResult = task.result
                            for (t in task.result.tags) tags.add(t)
                            if (mostRecentResult.start
                                + mostRecentResult.count >= Math.min(
                                    mostRecentResult.available,
                                    maxResults
                                )
                            ) hasMoreResults = false else hasMoreResults = true
                            if (task.result.available == 0) statusText =
                                "No tags could be found that matched your query."
                        } finally {
                            isLoading = false
                        }
                    }
                }
                null
            }
    }
}