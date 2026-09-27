package depollsoft.tagmaster

object FavoritesModel {
    // Resolved on every use so this wrapper and ListModel("favorite") can never drift apart, even
    // when a test resets the model cache.
    private val model: ListModel
        get() = ListModel(TagLists.FAVORITE)
    var favoriteIds: List<Int>
        get() = model.ids
        set(value) {
            model.ids = value
        }

    fun addFavorite(id: Int) = model.add(id)

    fun canMoveDown(id: Int): Boolean = model.canMoveDown(id)

    fun canMoveUp(id: Int): Boolean = model.canMoveUp(id)

    fun getIsFavorite(id: Int): Boolean = model.contains(id)

    fun moveDown(id: Int) = model.moveDown(id)

    fun moveUp(id: Int) = model.moveUp(id)

    fun removeFavorite(id: Int) = model.remove(id)

    fun resetFavorites() = model.reset()
}
