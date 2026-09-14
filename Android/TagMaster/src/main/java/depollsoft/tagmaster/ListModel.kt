package depollsoft.tagmaster

import com.bindroid.trackable.*
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import depollsoft.lib.util.Preferences
import java.lang.ref.WeakReference

class ListModel private constructor(
    val listName: String,
) {
    var ids: TrackableCollection<Int>
        by trackable(preferences[listName] ?: TrackableCollection())

    private var revision = 0L

    /** A main-thread editing baseline. Identity and revision also detect replace/reset-and-restore. */
    class Snapshot internal constructor(
        internal val source: TrackableCollection<Int>,
        internal val revision: Long,
        val ids: List<Int>,
    )

    fun snapshot() = Snapshot(ids, revision, ids.toList())

    /** Commit a complete permutation once, only if nothing changed since editing began. */
    fun reorder(
        expected: Snapshot,
        order: List<Int>,
    ): Boolean {
        val current = ids
        if (current !== expected.source || revision != expected.revision || current.toList() != expected.ids) return false
        if (order == expected.ids || order.size != expected.ids.size ||
            order.toSet().size != order.size || order.toSet() != expected.ids.toSet()
        ) {
            return false
        }
        current.transaction {
            clear()
            addAll(order)
        }
        return true
    }

    /** Move by stable tag ID, rejecting missing IDs, out-of-range destinations and no-ops. */
    fun move(
        id: Int,
        destination: Int,
    ): Boolean {
        val baseline = snapshot()
        val from = baseline.ids.indexOf(id)
        if (from < 0 || destination !in baseline.ids.indices || from == destination) return false
        val order = baseline.ids.toMutableList()
        order.add(destination, order.removeAt(from))
        return reorder(baseline, order)
    }

    fun add(id: Int) {
        if (!ids.contains(id)) ids.add(id)
    }

    fun canMoveDown(id: Int): Boolean {
        val index: Int = ids.indexOf(id)
        return index >= 0 && index < ids.size - 1
    }

    fun canMoveUp(id: Int): Boolean {
        val index: Int = ids.indexOf(id)
        return index > 0
    }

    fun contains(id: Int): Boolean = ids.contains(id)

    fun moveDown(id: Int) {
        if (canMoveDown(id)) move(id, ids.indexOf(id) + 1)
    }

    fun moveUp(id: Int) {
        if (canMoveUp(id)) move(id, ids.indexOf(id) - 1)
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
                    "lists" to
                        mapOf(
                            listName to if (ids.isEmpty()) FieldValue.delete() else ids,
                        ),
                ),
                SetOptions.mergeFields("lists.$listName"),
            )
        }
    }

    init {
        Trackable.track(
            object : Tracker {
                override fun update() {
                    revision++
                    storeValue()
                    Trackable.track(this, { ids.track() })
                }
            },
            { ids.track() },
        )
    }

    companion object {
        private const val LISTS_KEY = "tagmaster.lists"

        private val preferences: MutableMap<String, TrackableCollection<Int>> by lazy {
            Preferences.get(LISTS_KEY) ?: mutableMapOf()
        }

        private fun storeValue(toFirestore: Boolean) {
            Preferences.setAsync(LISTS_KEY, preferences)
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

        /**
         * For testing: disable Firebase storage to allow unit testing without Firebase initialization.
         * Call this in @Before methods of tests that create ListModel instances.
         */
        @JvmStatic
        fun setTestMode(enabled: Boolean) {
            shouldStore = !enabled
        }

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
                    var newValue =
                        (data[it] as? List<*>)
                            ?.mapNotNull { (it as? Long)?.toInt() }
                            ?.toMutableList() ?: mutableListOf()
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
                        "lists" to preferences,
                    ),
                    SetOptions.merge(),
                )
            }
        }

        var registration: ListenerRegistration? = null

        fun connectToFirestore() {
            registration?.remove()
            val user = Firebase.auth.currentUser
            if (user != null) {
                val userDoc = Firebase.firestore.document("users/${user.uid}")
                registration =
                    userDoc.addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            print(error)
                            return@addSnapshotListener
                        }

                        if (!snapshot!!.exists()) {
                            // There was no existing user, so initialize the user
                            toFirestore()
                        }

                        fromFirestore(
                            snapshot.get("lists") as? Map<*, *> ?: mutableMapOf<String, List<Long>>(),
                        )
                    }
            }
        }

        init {
            migrateOldFavorites()
        }
    }
}
