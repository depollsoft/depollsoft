package depollsoft.tagmaster

import com.bindroid.trackable.Trackable
import com.bindroid.trackable.TrackableCollection
import com.bindroid.trackable.Tracker
import com.bindroid.trackable.trackable
import depollsoft.lib.util.Preferences
import java.lang.ref.WeakReference

class ListModel private constructor(val listName: String) {

    var ids: TrackableCollection<Int>
            by trackable(preferences[listName] ?: TrackableCollection())

    fun add(id: Int) {
        if (!ids.contains(id)) ids.add(id)
    }

    fun canMoveDown(id: Int): Boolean {
        val index: Int = ids.indexOf(id)
        return index < ids.size - 1
    }

    fun canMoveUp(id: Int): Boolean {
        val index: Int = ids.indexOf(id)
        return index > 0
    }

    fun contains(id: Int): Boolean {
        return ids.contains(id)
    }

    fun moveDown(id: Int) {
        val index: Int = ids.indexOf(id)
        ids.removeAt(index)
        ids.add(index + 1, id)
    }

    fun moveUp(id: Int) {
        val index: Int = ids.indexOf(id)
        ids.removeAt(index)
        ids.add(index - 1, id)
    }

    fun remove(id: Int) {
        ids.remove(Integer.valueOf(id))
    }

    fun reset() {
        ids.clear()
    }

    private fun storeValue() {
        if (ids.isEmpty()) {
            preferences.remove(listName)
        } else {
            preferences[listName] = ids
        }
        Companion.storeValue()
        // TODO: Write to Firestore
    }

    init {
        Trackable.track(object : Tracker {
            override fun update() {
                storeValue()
                Trackable.track(this, { ids.track() })
            }
        }, { ids.track() })
    }

    companion object {
        private const val LISTS_KEY = "tagmaster.lists"

        private val preferences: MutableMap<String, TrackableCollection<Int>> by lazy {
            Preferences.get(LISTS_KEY) ?: mutableMapOf()
        }

        private fun storeValue() {
            Preferences.set(LISTS_KEY, preferences)
        }

        private val modelInstances: MutableMap<String, WeakReference<ListModel>> = mutableMapOf()
        operator fun invoke(listName: String): ListModel {
            var model = modelInstances[listName]?.get()
            if (model == null) {
                model = ListModel(listName)
                modelInstances[listName] = WeakReference(model)
            }
            return model
        }

        private fun migrateOldFavorites() {
            val favoritesKey = "tagmaster.Favorites"
            val teachablesKey = "tagmaster.TeachableTags"

            val favorites: TrackableCollection<Int>? = Preferences.get(favoritesKey)
            val teachables: TrackableCollection<Int>? = Preferences.get(teachablesKey)

            if (favorites != null) {
                val favModel = ListModel.invoke("favorite")
                favModel.ids = favorites
                Preferences.set(favoritesKey, null)
            }

            if (teachables != null) {
                val teachableModel = ListModel.invoke("teachable")
                teachableModel.ids = teachables
                Preferences.set(teachablesKey, null)
            }
        }

        init {
            migrateOldFavorites()
        }
    }
}