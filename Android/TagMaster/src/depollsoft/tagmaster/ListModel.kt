package depollsoft.tagmaster

import com.bindroid.trackable.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
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
        ids.transaction {
            val index: Int = indexOf(id)
            removeAt(index)
            add(index + 1, id)
        }
    }

    fun moveUp(id: Int) {
        ids.transaction {
            val index: Int = ids.indexOf(id)
            ids.removeAt(index)
            ids.add(index - 1, id)
        }
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
        if (!shouldStore) {
            return
        }
        Companion.storeValue(false)
        val user = Firebase.auth.currentUser
        if (user != null) {
            val userDoc = Firebase.firestore.document("users/${user.uid}")
            userDoc.set(
                mapOf(
                    "lists" to mapOf(
                        listName to if (ids.isEmpty()) FieldValue.delete() else ids
                    )
                ), SetOptions.mergeFields("lists.${listName}")
            )
        }
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

        private fun storeValue(toFirestore: Boolean) {
            Preferences.set(LISTS_KEY, preferences)
            if (toFirestore) {
                toFirestore()
            }
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

            if (Preferences.get<Any?>(LISTS_KEY) != null) {
                Preferences.set(favoritesKey, null)
                Preferences.set(teachablesKey, null)
                return
            }

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

        private var shouldStore = true
        private fun fromFirestore(data: Map<*, *>) {
            shouldStore = false
            try {
                // Remove lists that aren't in the data
                preferences.keys.subtract(data.keys).forEach {
                    if (it !is String) {
                        return
                    }
                    val cur = ListModel.invoke(it)
                    cur.ids.clear()
                }
                // Update existing lists
                data.keys.forEach {
                    if (it !is String) {
                        return
                    }
                    val cur = ListModel.invoke(it)
                    var newValue = (data[it] as? List<*>)?.mapNotNull { (it as? Long)?.toInt() }
                        ?.toMutableList() ?: mutableListOf();
                    if (!newValue.equals(cur.ids)) {
                        cur.ids.replaceBackingStore(newValue)
                    }
                }

            } finally {
                shouldStore = true
            }
            storeValue(false)
        }

        private fun toFirestore() {
            val user = Firebase.auth.currentUser
            if (user != null) {
                val userDoc = Firebase.firestore.document("users/${user.uid}")
                userDoc.set(
                    mapOf(
                        "lists" to preferences
                    ), SetOptions.merge()
                )
            }
        }

        var registration: ListenerRegistration? = null
        fun connectToFirestore() {
            registration?.remove()
            val user = Firebase.auth.currentUser
            if (user != null) {
                val userDoc = Firebase.firestore.document("users/${user.uid}")
                registration = userDoc.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        print(error)
                        return@addSnapshotListener
                    }

                    if (!snapshot!!.exists()) {
                        // There was no existing user, so initialize the user
                        toFirestore()
                    }

                    fromFirestore(
                        snapshot.get("lists") as? Map<*, *> ?: mutableMapOf<String, List<Long>>()
                    )
                }
            }
        }

        init {
            migrateOldFavorites()
        }
    }
}