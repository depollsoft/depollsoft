package depollsoft.tagmaster

import com.bindroid.trackable.TrackableCollection

object TeachableTagsModel {
    private val model = ListModel("teachable")

    @JvmStatic
    var teachableTagIds: TrackableCollection<Int>
        get() = model.ids
        set(value) {
            model.ids = value
        }

    fun addTeachableTag(id: Int) = model.add(id)

    fun canMoveDown(id: Int): Boolean = model.canMoveDown(id)

    fun canMoveUp(id: Int): Boolean = model.canMoveUp(id)

    fun getIsTeachableTag(id: Int): Boolean = model.contains(id)

    fun moveDown(id: Int) = model.moveDown(id)

    fun moveUp(id: Int) = model.moveUp(id)

    fun removeTeachableTag(id: Int) = model.remove(id)

    fun resetTeachableTags() = model.reset()
}
