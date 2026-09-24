package depollsoft.tagmaster

import androidx.lifecycle.ViewModel

/**
 * An activity's queries, kept across configuration changes the way the retained TagQueryFragment
 * kept them: a rotated Browse or results screen still has the pages it loaded, and its list comes
 * back where it was instead of starting over from page one.
 */
class RetainedQueries : ViewModel() {
    private var models: List<QueryModel>? = null

    /** Which of [models] have fetched their first page. */
    val started = mutableSetOf<Int>()

    /** The retained queries, or [create]'s, which then are retained. */
    fun models(create: () -> List<QueryModel>): List<QueryModel> = models ?: create().also { models = it }

    /** Whether the queries were retained from an earlier instance of the activity. */
    val isRetained: Boolean get() = models != null
}
