package depollsoft.tagmaster

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import depollsoft.lib.state.StateList
import depollsoft.lib.util.Preferences

/**
 * One list of tag ids, kept in [Preferences] and, when someone is signed in, in the `lists` map of
 * their Firestore user document.
 *
 * [ids] is snapshot state, so composables showing a list recompose when it changes. Every change
 * goes through this class, which stores it on the spot: the list is saved before the call that
 * changed it returns.
 */
class ListModel private constructor(
    val listName: String,
) {
    private val store: StateList<Int> = preferences[listName] ?: StateList()

    /** The tags in this list, in order. Assigning replaces the contents as one change. */
    var ids: List<Int>
        get() = store
        set(value) {
            val next = value.toList()
            store.replaceWith(next)
            changed()
        }

    private var revision = 0L

    /** A main-thread editing baseline; the revision detects any change made since it was taken. */
    class Snapshot internal constructor(
        internal val revision: Long,
        val ids: List<Int>,
    )

    fun snapshot() = Snapshot(revision, store.snapshot())

    /** Commit a complete permutation once, only if nothing changed since editing began. */
    fun reorder(
        expected: Snapshot,
        order: List<Int>,
    ): Boolean {
        if (revision != expected.revision || store.snapshot() != expected.ids) return false
        if (order == expected.ids || order.size != expected.ids.size ||
            order.toSet().size != order.size || order.toSet() != expected.ids.toSet()
        ) {
            return false
        }
        store.replaceWith(order)
        changed()
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
        if (store.contains(id)) return
        store.add(id)
        changed()
    }

    fun canMoveDown(id: Int): Boolean {
        val index: Int = store.indexOf(id)
        return index >= 0 && index < store.size - 1
    }

    fun canMoveUp(id: Int): Boolean {
        val index: Int = store.indexOf(id)
        return index > 0
    }

    fun contains(id: Int): Boolean = store.contains(id)

    fun moveDown(id: Int) {
        if (canMoveDown(id)) move(id, store.indexOf(id) + 1)
    }

    fun moveUp(id: Int) {
        if (canMoveUp(id)) move(id, store.indexOf(id) - 1)
    }

    fun remove(id: Int) {
        if (store.remove(Integer.valueOf(id))) changed()
    }

    fun reset() {
        if (store.isEmpty()) return
        store.clear()
        changed()
    }

    /** Replaces the contents with a cloud copy; stored locally only, as [fromFirestore] requires. */
    private fun replaceFromRemote(value: List<Int>) {
        if (value == store.snapshot()) return
        store.replaceWith(value)
        changed()
    }

    private fun changed() {
        revision++
        storeValue()
    }

    private fun storeValue() {
        if (store.isEmpty()) {
            preferences.remove(listName)
        } else {
            preferences[listName] = store
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
                            listName to if (store.isEmpty()) FieldValue.delete() else store.toList(),
                        ),
                ),
                // A FieldPath keeps list keys with dots or other punctuation intact.
                SetOptions.mergeFieldPaths(listOf(FieldPath.of("lists", listName))),
            )
        }
    }

    companion object {
        private const val LISTS_KEY = "tagmaster.lists"
        private const val SYNCED_UID_KEY = "tagmaster.syncedUid"

        private val preferences: MutableMap<String, StateList<Int>> by lazy {
            decodeLists(Preferences.get(LISTS_KEY))
        }

        /**
         * Every stored list as the observable list the models use. Stored lists come back as
         * [StateList] (the app registers it under the alias the Bindroid collection was written
         * with); anything else that is a collection is converted rather than dropped.
         */
        @Suppress("UNCHECKED_CAST")
        internal fun decodeLists(stored: Map<*, *>?): MutableMap<String, StateList<Int>> {
            val lists = mutableMapOf<String, StateList<Int>>()
            stored?.forEach { (key, value) ->
                if (key is String && value is Collection<*>) {
                    lists[key] = value as? StateList<Int> ?: StateList(value.filterIsInstance<Int>())
                }
            }
            return lists
        }

        private fun storeValue(toFirestore: Boolean) {
            Preferences.setAsync(LISTS_KEY, preferences)
            if (toFirestore) {
                toFirestore()
            }
        }

        // Held strongly: a model is tiny, there is one per list, and composables read its `ids`.
        // A weak cache let an empty list (no stored preference) be rebuilt around a fresh
        // collection once the old model was collected, silently orphaning those readers.
        private val modelInstances: MutableMap<String, ListModel> = mutableMapOf()

        operator fun invoke(listName: String): ListModel = modelInstances.getOrPut(listName) { ListModel(listName) }

        private fun migrateOldFavorites() {
            val favoritesKey = "tagmaster.Favorites"
            val teachablesKey = "tagmaster.TeachableTags"

            if (Preferences.get<Any?>(LISTS_KEY) != null) {
                Preferences.set(favoritesKey, null)
                Preferences.set(teachablesKey, null)
                return
            }

            val favorites: Collection<*>? = Preferences.get(favoritesKey)
            val teachables: Collection<*>? = Preferences.get(teachablesKey)

            if (favorites != null) {
                ListModel.invoke(TagLists.FAVORITE).ids = favorites.filterIsInstance<Int>()
                Preferences.set(favoritesKey, null)
            }

            if (teachables != null) {
                ListModel.invoke(TagLists.TEACHABLE).ids = teachables.filterIsInstance<Int>()
                Preferences.set(teachablesKey, null)
            }
        }

        internal var shouldStore = true
            private set

        /** The keys of every list with at least one tag on this device. */
        internal fun storedKeys(): Set<String> = preferences.keys.toSet()

        /**
         * Drops every list on this device without touching the cloud. Used when a different
         * account signs in: the lists here belong to the account that last synced them.
         */
        internal fun forgetLocalLists() {
            val wasStoring = shouldStore
            shouldStore = false
            try {
                (preferences.keys + TagLists.RESERVED).toSet().forEach { ListModel.invoke(it).reset() }
                preferences.clear()
                TagLists.resetLocal()
            } finally {
                shouldStore = wasStoring
            }
            storeValue(false)
        }

        /**
         * Records which account this device's lists belong to. Signing in as a different account
         * than the one that last synced here drops the local lists first, so one user's lists are
         * never uploaded into another user's brand-new document. Signing out keeps the lists, and
         * the same account signing back in finds them untouched. Returns whether lists were dropped.
         */
        internal fun prepareLocalLists(forUid: String): Boolean {
            val synced = Preferences.get<String>(SYNCED_UID_KEY)
            Preferences.set(SYNCED_UID_KEY, forUid)
            if (synced == null || synced == forUid) return false
            forgetLocalLists()
            return true
        }

        /**
         * Empties [listName] on this device without writing to Firestore. [TagLists.delete] uses it
         * and then deletes the cloud field together with the list's metadata in one write.
         */
        internal fun discard(listName: String) {
            val wasStoring = shouldStore
            shouldStore = false
            try {
                ListModel.invoke(listName).reset()
            } finally {
                shouldStore = wasStoring
            }
            preferences.remove(listName)
            storeValue(false)
        }

        /**
         * For testing: disable Firebase storage to allow unit testing without Firebase initialization.
         * Call this in @Before methods of tests that create ListModel instances.
         */
        @JvmStatic
        fun setTestMode(enabled: Boolean) {
            shouldStore = !enabled
        }

        private fun fromFirestore(
            data: Map<*, *>,
            info: Map<*, *>?,
        ) {
            // Restored rather than forced on: a snapshot applied while storing is off (test mode)
            // must not switch cloud writes back on behind the caller's back.
            val wasStoring = shouldStore
            shouldStore = false
            try {
                // Remove lists that aren't in the data
                preferences.keys.subtract(data.keys).forEach {
                    if (it !is String) {
                        return
                    }
                    ListModel.invoke(it).reset()
                }
                // Update existing lists
                data.keys.forEach {
                    if (it !is String) {
                        return
                    }
                    val newValue =
                        (data[it] as? List<*>)
                            ?.mapNotNull { (it as? Long)?.toInt() }
                            ?: emptyList()
                    ListModel.invoke(it).replaceFromRemote(newValue)
                }
                TagLists.applyRemote(info, data.keys.filterIsInstance<String>())
            } finally {
                shouldStore = wasStoring
            }
            storeValue(false)
        }

        private fun toFirestore() {
            val user = Firebase.auth.currentUser
            if (user != null) {
                val userDoc = Firebase.firestore.document("users/${user.uid}")
                userDoc.set(
                    mapOf(
                        "lists" to preferences.mapValues { it.value.toList() },
                        "listInfo" to TagLists.remoteInfo(),
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
                prepareLocalLists(forUid = user.uid)
                val userDoc = Firebase.firestore.document("users/${user.uid}")
                // Metadata changes are included so the server's confirmation of a missing
                // document arrives even when nothing else changed; see applyUserSnapshot.
                registration =
                    userDoc.addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                        if (error != null) {
                            print(error)
                            return@addSnapshotListener
                        }
                        applyUserSnapshot(
                            exists = snapshot!!.exists(),
                            fromCache = snapshot.metadata.isFromCache,
                            lists = snapshot.get("lists") as? Map<*, *>,
                            info = snapshot.get("listInfo") as? Map<*, *>,
                        )
                    }
            }
        }

        /**
         * One user-document snapshot after sign-in.
         *
         * The account's document is the truth: its lists replace whatever this device had, so
         * signing in never carries local-only lists into an existing account. The one time this
         * device's lists are uploaded is when the server says the account has no document yet,
         * which is a brand-new account. A cache miss says nothing about the account (it is what an
         * offline start looks like), so it neither seeds the document nor clears the local lists;
         * the server-confirmed snapshot that follows decides.
         */
        internal fun applyUserSnapshot(
            exists: Boolean,
            fromCache: Boolean,
            lists: Map<*, *>?,
            info: Map<*, *>?,
            seed: () -> Unit = ::toFirestore,
        ) {
            if (!exists) {
                if (!fromCache) seed()
                return
            }
            fromFirestore(lists ?: emptyMap<String, List<Long>>(), info)
        }

        init {
            migrateOldFavorites()
        }
    }
}
