package depollsoft.tagmaster

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import depollsoft.lib.state.ChangeSignal
import depollsoft.lib.util.Preferences
import java.util.Locale

/**
 * The registry of a user's tag lists.
 *
 * Every list is a key into the `lists` map of the Firestore user document (`users/{uid}`), whose
 * value is the ordered array of tag ids and which [ListModel] keeps in sync. Two keys are special:
 * [FAVORITE] and [TEACHABLE] always exist, cannot be renamed or deleted, and are surfaced at the
 * top level of the app. Every other key is a user-created list. Its display name and position
 * live in a sibling `listInfo` map (`listInfo.{key} = { name, order }`); a key with ids but no
 * `listInfo` entry (data written before this app version) is shown under its raw key.
 *
 * Keys are stable slugs, so renaming a list never moves its tags and an edit made offline on
 * another device still lands in the right list. All mutation goes through this object so the
 * local copy (in [Preferences]) and the cloud copy stay aligned; readers of [version] recompose.
 */
object TagLists {
    const val FAVORITE = "favorite"
    const val TEACHABLE = "teachable"
    val RESERVED: Set<String> = setOf(FAVORITE, TEACHABLE)

    /** The longest name accepted by [validateName]. */
    const val MAX_NAME_LENGTH = 60

    private const val NAMES_KEY = "tagmaster.listNames"
    private const val ORDER_KEY = "tagmaster.listOrder"

    enum class NameError { EMPTY, TOO_LONG, DUPLICATE, RESERVED }

    /** Bumps on every registry change (create, rename, delete, reorder, remote sync). */
    private val versionSignal = ChangeSignal()
    private var versionValue = 0L
    val version: Long
        get() {
            ensureLoaded()
            versionSignal.read()
            return versionValue
        }

    private var customKeyList: List<String> = emptyList()

    /** Ordered keys of the user-created lists. Read-only; mutate through this object. */
    val customKeys: List<String>
        get() {
            ensureLoaded()
            versionSignal.read()
            return customKeyList
        }

    private val names: MutableMap<String, String> = mutableMapOf()

    private val order: MutableList<String> = mutableListOf()

    private var loaded = false

    /**
     * Reads the stored registry the first time it is needed. Deliberately not a `by lazy`: a lazy
     * initialiser runs once per process, so [resetForTest] could never make this object read the
     * preferences again.
     */
    private fun ensureLoaded() {
        if (loaded) return
        loaded = true
        names.clear()
        Preferences.get<Map<*, *>>(NAMES_KEY)?.forEach { (k, v) ->
            if (k is String && v is String) names[k] = v
        }
        order.clear()
        Preferences.get<Collection<*>>(ORDER_KEY)?.filterIsInstance<String>()?.let(order::addAll)
        // The first read may come inside a read-only snapshot (a snapshotFlow, say), where writing
        // state throws. Nothing can have read the keys or the version yet, so the load signals
        // nothing; later changes go through [rebuild].
        customKeyList = orderedKeys()
        versionValue++
    }

    fun isCustom(key: String): Boolean = key !in RESERVED

    /** The stored name of a custom list, or the key itself for a list without metadata. */
    fun name(key: String): String {
        ensureLoaded()
        versionSignal.read()
        return names[key] ?: key
    }

    /** The user-facing name of any list, including the two special ones. */
    fun displayName(
        context: Context,
        key: String,
    ): String =
        when (key) {
            FAVORITE -> context.getString(R.string.Favorites)
            TEACHABLE -> context.getString(R.string.TeachableTags)
            else -> name(key)
        }

    /** Every list key in display order: favorites, teachable, then the custom lists. */
    fun allKeys(): List<String> {
        ensureLoaded()
        return listOf(FAVORITE, TEACHABLE) + customKeys.toList()
    }

    /** The keys of every list containing [tagId], in display order. */
    fun keysContaining(tagId: Int): List<String> = allKeys().filter { ListModel(it).contains(tagId) }

    fun normalizeName(name: String): String = name.trim().replace(Regex("\\s+"), " ")

    /** Why [name] cannot be used for a list, or null when it can. [excludingKey] is the list being renamed. */
    fun validateName(
        name: String,
        excludingKey: String? = null,
    ): NameError? {
        ensureLoaded()
        val normalized = normalizeName(name)
        if (normalized.isEmpty()) return NameError.EMPTY
        if (normalized.length > MAX_NAME_LENGTH) return NameError.TOO_LONG
        val folded = normalized.lowercase(Locale.ROOT)
        if (folded == "favorites" || folded == "favorite" || folded == "teachable tags" || folded == "teachable") {
            return NameError.RESERVED
        }
        val clash =
            customKeys.any { key ->
                key != excludingKey && name(key).lowercase(Locale.ROOT) == folded
            }
        return if (clash) NameError.DUPLICATE else null
    }

    /**
     * Creates a list named [name] and returns its key.
     * @throws IllegalArgumentException when [validateName] rejects the name.
     */
    fun create(name: String): String {
        ensureLoaded()
        val normalized = normalizeName(name)
        validateName(normalized)?.let { throw IllegalArgumentException("Invalid list name: $it") }
        val key = newKey(normalized)
        names[key] = normalized
        order.add(key)
        commit(changedKeys = listOf(key))
        return key
    }

    fun rename(
        key: String,
        name: String,
    ) {
        ensureLoaded()
        require(isCustom(key)) { "The $key list cannot be renamed" }
        // A list deleted elsewhere while its rename dialog was open must not come back.
        if (key !in customKeys) return
        val normalized = normalizeName(name)
        validateName(normalized, excludingKey = key)?.let { throw IllegalArgumentException("Invalid list name: $it") }
        if (names[key] == normalized) return
        names[key] = normalized
        if (key !in order) order.add(key)
        commit(changedKeys = listOf(key))
    }

    /** Deletes a custom list along with its tags, locally and in the cloud. */
    fun delete(key: String) {
        ensureLoaded()
        require(isCustom(key)) { "The $key list cannot be deleted" }
        names.remove(key)
        order.remove(key)
        ListModel.discard(key)
        commit(changedKeys = emptyList(), deletedKeys = listOf(key))
    }

    /** Moves the custom list [key] to [destination] among the custom lists. Returns whether anything moved. */
    fun move(
        key: String,
        destination: Int,
    ): Boolean {
        ensureLoaded()
        val current = customKeys.toMutableList()
        val from = current.indexOf(key)
        if (from < 0 || destination !in current.indices || from == destination) return false
        current.add(destination, current.removeAt(from))
        return reorder(current)
    }

    fun canMoveUp(key: String): Boolean = customKeys.indexOf(key) > 0

    fun canMoveDown(key: String): Boolean = customKeys.indexOf(key).let { it >= 0 && it < customKeys.size - 1 }

    fun moveUp(key: String): Boolean = if (canMoveUp(key)) move(key, customKeys.indexOf(key) - 1) else false

    fun moveDown(key: String): Boolean = if (canMoveDown(key)) move(key, customKeys.indexOf(key) + 1) else false

    /** Applies a complete new order of the custom lists. Returns false if [keys] is not a permutation. */
    fun reorder(keys: List<String>): Boolean {
        ensureLoaded()
        val current = customKeys.toList()
        if (keys == current || keys.size != current.size || keys.toSet() != current.toSet()) return false
        order.clear()
        order.addAll(keys)
        commit(changedKeys = keys)
        return true
    }

    /** Replaces the registry with the cloud copy. [info] is the raw `listInfo` map; [listKeys] the keys of `lists`. */
    internal fun applyRemote(
        info: Map<*, *>?,
        listKeys: Collection<String>,
    ) {
        ensureLoaded()
        names.clear()
        val ordered = mutableListOf<Pair<Long, String>>()
        info?.forEach { (rawKey, rawValue) ->
            val key = rawKey as? String ?: return@forEach
            if (!isCustom(key)) return@forEach
            val value = rawValue as? Map<*, *> ?: return@forEach
            (value["name"] as? String)?.let { names[key] = it }
            val position = (value["order"] as? Number)?.toLong() ?: Long.MAX_VALUE
            ordered.add(position to key)
        }
        order.clear()
        ordered.sortedWith(compareBy({ it.first }, { names[it.second] ?: it.second })).mapTo(order) { it.second }
        persist()
        rebuild(extraKeys = listKeys)
    }

    /** The cloud shape of the registry, for seeding a brand-new user document. */
    internal fun remoteInfo(): Map<String, Map<String, Any>> {
        ensureLoaded()
        return customKeys.withIndex().associate { (index, key) -> key to infoEntry(key, index) }
    }

    /** Clears every custom list from this device only; used when the signed-in user changes. */
    internal fun resetLocal() {
        ensureLoaded()
        names.clear()
        order.clear()
        persist()
        rebuild()
    }

    private fun infoEntry(
        key: String,
        index: Int,
    ): Map<String, Any> = mapOf("name" to name(key), "order" to index)

    private fun commit(
        changedKeys: List<String>,
        deletedKeys: List<String> = emptyList(),
    ) {
        persist()
        rebuild()
        if (!ListModel.shouldStore) return
        val user = Firebase.auth.currentUser ?: return
        val info = mutableMapOf<String, Any>()
        val paths = mutableListOf<FieldPath>()
        deletedKeys.forEach { key ->
            info[key] = FieldValue.delete()
            paths.add(FieldPath.of("listInfo", key))
            paths.add(FieldPath.of("lists", key))
        }
        val positions = customKeys.withIndex().associate { (index, key) -> key to index }
        changedKeys.filter { it in positions }.forEach { key ->
            info[key] = infoEntry(key, positions.getValue(key))
            paths.add(FieldPath.of("listInfo", key))
        }
        if (paths.isEmpty()) return
        val payload = mutableMapOf<String, Any>("listInfo" to info)
        if (deletedKeys.isNotEmpty()) {
            payload["lists"] = deletedKeys.associateWith { FieldValue.delete() }
        }
        Firebase.firestore
            .document("users/${user.uid}")
            .set(payload, SetOptions.mergeFieldPaths(paths))
    }

    private fun persist() {
        Preferences.setAsync(NAMES_KEY, if (names.isEmpty()) null else HashMap(names))
        Preferences.setAsync(ORDER_KEY, if (order.isEmpty()) null else ArrayList(order))
    }

    /**
     * Recomputes [customKeys]: the explicitly ordered keys first, then any list that has tags but no
     * metadata, by name. [extraKeys] are list keys known to the caller (for example the keys of a
     * cloud snapshot) that may not have been seen by this object yet.
     */
    private fun rebuild(extraKeys: Collection<String> = emptyList()) {
        customKeyList = orderedKeys(extraKeys)
        versionValue++
        versionSignal.changed()
    }

    private fun orderedKeys(extraKeys: Collection<String> = emptyList()): List<String> {
        val known = LinkedHashSet<String>()
        order.forEach { if (isCustom(it)) known.add(it) }
        val unordered =
            (names.keys + ListModel.storedKeys() + extraKeys)
                .filter { isCustom(it) && it !in known }
                .distinct()
                .sortedBy { (names[it] ?: it).lowercase(Locale.ROOT) }
        known.addAll(unordered)
        return known.toList()
    }

    private fun newKey(name: String): String {
        val slug =
            name
                .lowercase(Locale.ROOT)
                .replace(Regex("[^a-z0-9]+"), "-")
                .trim('-')
                .take(24)
                .trim('-')
                .ifEmpty { "list" }
        val alphabet = "abcdefghijklmnopqrstuvwxyz0123456789"
        while (true) {
            val suffix = (1..4).map { alphabet.random() }.joinToString("")
            val key = "$slug-$suffix"
            if (key !in RESERVED && key !in names && key !in order && key !in ListModel.storedKeys()) return key
        }
    }

    /** Forgets in-memory state so a test can start from fresh preferences. */
    internal fun resetForTest() {
        names.clear()
        order.clear()
        loaded = false
        customKeyList = emptyList()
    }
}
