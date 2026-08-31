package depollsoft.tagmaster

import com.bindroid.trackable.TrackableCollection

object FavoritesModel {
    private val model = ListModel("favorite")
    var favoriteIds: TrackableCollection<Int>
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
