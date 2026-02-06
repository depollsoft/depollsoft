package depollsoft.tagmaster

import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import depollsoft.lib.util.Preferences
import java.util.UUID

/**
 * Manages metadata for custom tag lists (name, order).
 * Built-in lists (favorite, teachable) have fixed metadata.
 * Custom list metadata is stored in SharedPreferences and synced to
 * Firestore at users/{uid}/listMeta.
 */
object CustomListsModel {
    private const val LIST_META_KEY = "tagmaster.listMeta"

    private var metadata: MutableMap<String, ListMetadata> = run {
        val stored: Map<String, Map<String, Any>>? = Preferences.get(LIST_META_KEY)
        val result = mutableMapOf<String, ListMetadata>()
        stored?.forEach { (key, map) ->
            val name = map["name"] as? String ?: key
            val order = (map["order"] as? Number)?.toInt() ?: 0
            result[key] = ListMetadata(key, name, order, isBuiltIn = false)
        }
        result
    }

    private var shouldStore = true

    /**
     * For testing: disable Firestore storage.
     */
    @JvmStatic
    fun setTestMode(enabled: Boolean) {
        shouldStore = !enabled
    }

    /**
     * Returns all lists: built-in first, then custom lists sorted by order.
     */
    fun allLists(): List<ListMetadata> {
        val builtIn = listOf(ListMetadata.favorite, ListMetadata.teachable)
        val custom = metadata.values.sortedBy { it.order }
        return builtIn + custom
    }

    /**
     * Returns only custom lists sorted by order.
     */
    fun customLists(): List<ListMetadata> {
        return metadata.values.sortedBy { it.order }
    }

    /**
     * Gets metadata for a list by key, or null if not found.
     * Returns built-in metadata for favorite/teachable keys.
     */
    fun getMetadata(key: String): ListMetadata? {
        return when (key) {
            ListMetadata.FAVORITE_KEY -> ListMetadata.favorite
            ListMetadata.TEACHABLE_KEY -> ListMetadata.teachable
            else -> metadata[key]
        }
    }

    /**
     * Creates a new custom list with the given name.
     * Returns the generated key.
     */
    fun createList(name: String): String {
        val key = UUID.randomUUID().toString()
        val order = (metadata.values.maxOfOrNull { it.order } ?: 1) + 1
        val meta = ListMetadata(key, name, order, isBuiltIn = false)
        metadata[key] = meta
        // Create the backing ListModel so it's ready to use
        ListModel(key)
        storeLocally()
        storeToFirestore(key, meta)
        return key
    }

    /**
     * Renames a custom list. No-op for built-in lists.
     */
    fun renameList(key: String, newName: String) {
        val existing = metadata[key] ?: return
        metadata[key] = existing.copy(name = newName)
        storeLocally()
        storeToFirestore(key, metadata[key]!!)
    }

    /**
     * Deletes a custom list and its tag IDs. No-op for built-in lists.
     */
    fun deleteList(key: String) {
        if (key == ListMetadata.FAVORITE_KEY || key == ListMetadata.TEACHABLE_KEY) return
        metadata.remove(key)
        ListModel.deleteList(key)
        storeLocally()
        deleteFromFirestore(key)
    }

    private fun storeLocally() {
        val toStore = mutableMapOf<String, Map<String, Any>>()
        metadata.forEach { (key, meta) ->
            toStore[key] = mapOf("name" to meta.name, "order" to meta.order)
        }
        Preferences.set(LIST_META_KEY, toStore)
    }

    private fun storeToFirestore(key: String, meta: ListMetadata) {
        if (!shouldStore) return
        val user = Firebase.auth.currentUser ?: return
        val userDoc = Firebase.firestore.document("users/${user.uid}")
        userDoc.set(
            mapOf("listMeta" to mapOf(key to mapOf("name" to meta.name, "order" to meta.order))),
            SetOptions.mergeFields("listMeta.${key}")
        )
    }

    private fun deleteFromFirestore(key: String) {
        if (!shouldStore) return
        val user = Firebase.auth.currentUser ?: return
        val userDoc = Firebase.firestore.document("users/${user.uid}")
        userDoc.set(
            mapOf(
                "lists" to mapOf(key to FieldValue.delete()),
                "listMeta" to mapOf(key to FieldValue.delete())
            ),
            SetOptions.mergeFields("lists.${key}", "listMeta.${key}")
        )
    }

    /**
     * Syncs metadata from a Firestore snapshot data map.
     * Called from ListModel.fromFirestore().
     */
    fun fromFirestore(data: Map<*, *>?) {
        if (data == null) return
        shouldStore = false
        try {
            val newMeta = mutableMapOf<String, ListMetadata>()
            data.forEach { (key, value) ->
                if (key !is String) return@forEach
                val map = value as? Map<*, *> ?: return@forEach
                val name = map["name"] as? String ?: key
                val order = (map["order"] as? Number)?.toInt() ?: 0
                newMeta[key] = ListMetadata(key, name, order, isBuiltIn = false)
            }
            metadata = newMeta
            storeLocally()
        } finally {
            shouldStore = true
        }
    }

    /**
     * Returns metadata as a map suitable for Firestore storage.
     */
    fun toFirestoreMap(): Map<String, Map<String, Any>> {
        val result = mutableMapOf<String, Map<String, Any>>()
        metadata.forEach { (key, meta) ->
            result[key] = mapOf("name" to meta.name, "order" to meta.order)
        }
        return result
    }

    /**
     * Resets all state. For testing only.
     */
    @JvmStatic
    fun resetForTesting() {
        metadata = mutableMapOf()
    }
}
