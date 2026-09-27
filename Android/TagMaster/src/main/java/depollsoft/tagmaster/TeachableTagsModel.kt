package depollsoft.tagmaster

object TeachableTagsModel {
    // Resolved on every use so this wrapper and ListModel("teachable") can never drift apart, even
    // when a test resets the model cache.
    private val model: ListModel
        get() = ListModel(TagLists.TEACHABLE)

    @JvmStatic
    var teachableTagIds: List<Int>
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
