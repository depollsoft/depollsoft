package depollsoft.tagmaster

/**
 * Metadata for a tag list. Built-in lists (favorite, teachable) have fixed
 * display names and cannot be renamed or deleted.
 */
data class ListMetadata(
    val key: String,
    val name: String,
    val order: Int,
    val isBuiltIn: Boolean
) {
    companion object {
        const val FAVORITE_KEY = "favorite"
        const val TEACHABLE_KEY = "teachable"

        val favorite = ListMetadata(FAVORITE_KEY, "Favorites", 0, isBuiltIn = true)
        val teachable = ListMetadata(TEACHABLE_KEY, "Teachable Tags", 1, isBuiltIn = true)
    }
}
