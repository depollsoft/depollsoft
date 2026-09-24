package depollsoft.tagmaster

/**
 * A catalog query opened directly (the older entry point to what [TagSearchResultsActivity] now
 * shows), titled with the activity's label rather than search text.
 */
class TagQueryActivity : TagSearchResultsActivity() {
    override val screenTitle: String?
        get() = title?.toString()
}
