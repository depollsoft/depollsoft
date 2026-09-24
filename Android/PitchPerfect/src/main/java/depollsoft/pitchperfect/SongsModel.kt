package depollsoft.pitchperfect

import android.os.SystemClock
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.firestore
import depollsoft.lib.activity.RichApplication
import depollsoft.lib.state.ChangeSignal
import depollsoft.lib.state.StateList
import depollsoft.lib.util.Preferences
import depollsoft.lib.util.preference
import depollsoft.lib.util.writeThroughPreference
import depollsoft.pitchperfect.lib.PitchedSong
import kotlin.random.Random

class SongsModel private constructor() {
    /**
     * Fires whenever the *set* of lists or the current list changes.
     *
     * The map itself is a plain preference, not snapshot state, so the selector and the manage
     * screen would otherwise never hear about a list being created, deleted or switched to. Names
     * and orders are snapshot state on [SongList] and are read through [trackLists], so anything
     * reading [trackLists] (a composable, a [depollsoft.lib.state.watchState]) sees every change
     * the list-level UI cares about — local or remote.
     */
    private val listsChanged = ChangeSignal()

    var songLists: Map<String, SongList> by writeThroughPreference(
        SONG_LISTS_KEY,
        mapOf(),
    ) { listsChanged.changed() }

    /**
     * The current list id, as stored on this device.
     *
     * Nullable on purpose: `preference` registers its default exactly once, so a test that empties
     * the store would otherwise be handed a null for a non-null String for the rest of the run.
     */
    private var storedCurrentListId: String? by preference<String?>(CURRENT_LIST_KEY, DEFAULT_ID)

    /** A per-device choice, never synced; falls back to [DEFAULT_ID] when it names no list. */
    var currentListId: String
        get() {
            listsChanged.read()
            val stored = storedCurrentListId ?: DEFAULT_ID
            return if (songLists.containsKey(stored)) stored else DEFAULT_ID
        }
        set(value) {
            val target = if (songLists.containsKey(value)) value else DEFAULT_ID
            if (storedCurrentListId == target) return
            storedCurrentListId = target
            listsChanged.changed()
        }

    val defaultSongList: SongList
        get() = songLists[DEFAULT_ID]!!

    /** The list the Songs tab is showing. */
    val currentList: SongList
        get() = songLists[currentListId] ?: defaultSongList

    val allListeners: MutableList<ListenerRegistration> = mutableListOf()

    private var userDoc: DocumentReference? = null
    private val attachment = AuthAttachmentState()

    // MARK: - Tracking

    /** Reads the set of lists, their names, orders and the current one, so a reader sees them change. */
    fun trackLists() {
        listsChanged.read()
        songLists.values.forEach {
            it.name
            it.order
        }
    }

    fun notifyListsChanged() {
        listsChanged.changed()
    }

    // MARK: - Reading

    /** `default` first, then custom lists by `order`, then unordered ones by display name. */
    val orderedLists: List<SongList>
        get() {
            trackLists()
            val lists = songLists
            val custom = lists.values.filter { it.id != DEFAULT_ID }
            val ordered = custom.filter { it.order != null }.sortedBy { it.order!! }
            val unordered =
                custom.filter { it.order == null }.sortedBy { displayName(it).lowercase() }
            return listOfNotNull(lists[DEFAULT_ID]) + ordered + unordered
        }

    /**
     * The name to show. The home list's legacy seed `"Default"` and a blank name both read as
     * "My Songs"; a custom list with no name (legacy data only) shows its id.
     */
    fun displayName(list: SongList): String {
        val name = list.name.trim()
        if (list.id == DEFAULT_ID) {
            if (name.isEmpty() || name.equals(LEGACY_DEFAULT_NAME, ignoreCase = true)) {
                return mySongsName()
            }
            return name
        }
        return name.ifEmpty { list.id }
    }

    fun displayName(id: String): String = songLists[id]?.let { displayName(it) } ?: mySongsName()

    /** The list with [id], or the current one — the fallback every UI entry point wants. */
    fun listOrCurrent(id: String?): SongList = songLists[id] ?: currentList

    private fun mySongsName(): String =
        RichApplication.getAppContext()?.getString(R.string.SetListDefaultName) ?: "My Songs"

    // MARK: - Names

    enum class NameError { EMPTY, TOO_LONG, RESERVED, DUPLICATE }

    /** Collapses runs of whitespace to one space and trims, per the shared contract. */
    fun normalizeName(name: String): String = name.trim().replace(WHITESPACE, " ")

    /**
     * The reason [name] cannot be used, or null when it can.
     *
     * [excludingId] lets a rename keep its own current name.
     */
    fun validateName(
        name: String,
        excludingId: String? = null,
    ): NameError? {
        val normalized = normalizeName(name)
        if (normalized.isEmpty()) return NameError.EMPTY
        if (normalized.length > MAX_NAME_LENGTH) return NameError.TOO_LONG
        if (normalized.equals(DEFAULT_ID, ignoreCase = true)) return NameError.RESERVED
        val taken =
            songLists.values.any {
                it.id != excludingId && displayName(it).equals(normalized, ignoreCase = true)
            }
        return if (taken) NameError.DUPLICATE else null
    }

    /** A stable slug key made from the name plus four random base-36 characters. */
    fun slugId(name: String): String {
        val slug =
            normalizeName(name)
                .lowercase()
                .map { if (it in 'a'..'z' || it in '0'..'9') it else '-' }
                .joinToString("")
                .replace(DASHES, "-")
                .trim('-')
                .take(MAX_SLUG_LENGTH)
                .trim('-')
        val base = if (slug.isEmpty()) "list" else slug
        var candidate = "$base-${randomSuffix()}"
        while (songLists.containsKey(candidate)) {
            candidate = "$base-${randomSuffix()}"
        }
        return candidate
    }

    private fun randomSuffix(): String =
        (1..SUFFIX_LENGTH)
            .map { BASE36[Random.nextInt(BASE36.length)] }
            .joinToString("")

    // MARK: - Editing

    /** Creates an empty list ordered after every existing custom list; returns its id. */
    fun createList(name: String): String {
        val normalized = normalizeName(name)
        val id = slugId(normalized)
        val list = SongList(id)
        list.order = nextOrder()
        userDoc?.let { list.setParent(it) }
        list.name = normalized
        list.storeValue()
        songLists = songLists + (id to list)
        return id
    }

    fun renameList(
        id: String,
        name: String,
    ) {
        val list = songLists[id] ?: return
        list.name = normalizeName(name)
        list.storeValue()
        listsChanged.changed()
    }

    /**
     * Copies [id] under "<name> copy", with deep copies of every song under fresh ids.
     *
     * Song ids are identity inside one list, so a copy that kept them would make two lists share
     * the same songs the moment either one was edited.
     */
    fun duplicateList(id: String): String? {
        val source = songLists[id] ?: return null
        val name = copyNameFor(displayName(source))
        val newId = slugId(name)
        val copy = SongList(newId)
        copy.order = nextOrder()
        userDoc?.let { copy.setParent(it) }
        copy.name = name
        copy.songs.addAll(source.songs.map { copyOf(it) })
        copy.storeValue()
        songLists = songLists + (newId to copy)
        return newId
    }

    fun deleteList(id: String) {
        if (id == DEFAULT_ID) return
        val list = songLists[id] ?: return
        list.deleteRemote()
        songLists = songLists - id
        if (storedCurrentListId == id) storedCurrentListId = DEFAULT_ID
        listsChanged.changed()
    }

    /** Puts a deleted list back, songs, order and id intact, here and on the server. */
    fun restoreList(list: SongList) {
        if (list.id == DEFAULT_ID || songLists.containsKey(list.id)) return
        list.undelete()
        userDoc?.let { list.setParent(it) }
        songLists = songLists + (list.id to list)
        list.storeValue()
        listsChanged.changed()
    }

    /** Rewrites `order` densely over every custom list, [ids] first, and stores each one once. */
    fun reorderLists(ids: List<String>) {
        val custom = orderedLists.filter { it.id != DEFAULT_ID }
        val sequence =
            (ids.mapNotNull { songLists[it] }.filter { it.id != DEFAULT_ID } + custom)
                .distinctBy { it.id }
        sequence.forEachIndexed { index, list ->
            list.order = index.toLong()
            list.storeValue()
        }
        listsChanged.changed()
    }

    /**
     * The songs of every other list that [targetId] does not already have, by list.
     *
     * "Already has" is a title match ignoring case plus an equal key: a song copied between lists
     * keeps neither its id nor any other identity.
     */
    fun addableSongs(targetId: String): List<Pair<SongList, List<PitchedSong>>> {
        val target = songLists[targetId] ?: return emptyList()
        val present = target.songs.map { fingerprint(it) }.toSet()
        return orderedLists
            .filter { it.id != targetId }
            .mapNotNull { source ->
                val songs = source.songs.filter { fingerprint(it) !in present }
                if (songs.isEmpty()) null else source to songs
            }
    }

    fun hasAddableSongs(targetId: String): Boolean = addableSongs(targetId).isNotEmpty()

    /** Appends deep copies of [songs], with fresh ids, to the end of [toId]. */
    fun copySongs(
        songs: List<PitchedSong>,
        toId: String,
    ) {
        val target = songLists[toId] ?: return
        target.addSongs(songs.map { copyOf(it) })
    }

    /** Empties My Songs and deletes every other list, here and on the server. */
    fun clearAll() {
        songLists.values.filter { it.id != DEFAULT_ID }.forEach { it.deleteRemote() }
        songLists = songLists.filterKeys { it == DEFAULT_ID }
        storedCurrentListId = DEFAULT_ID
        defaultSongList.resetSongs()
        listsChanged.changed()
    }

    /**
     * Title (ignoring case) plus key, as a string: [depollsoft.pitchperfect.lib.Key] defines
     * `equals` but not `hashCode`, and every deserialized song carries its own Key instance, so a
     * set of keys would never match.
     */
    private fun fingerprint(song: PitchedSong): String {
        val key = song.key
        val keyPart =
            if (key == null) "" else "${key.note?.friendlyName}|${key.keyType}|${key.numAccidentals}"
        return song.name.orEmpty().trim().lowercase() + "\u001F" + keyPart
    }

    private fun copyOf(song: PitchedSong): PitchedSong =
        PitchedSong().also {
            it.name = song.name
            it.key = song.key
        }

    private fun nextOrder(): Long = songLists.values.count { it.id != DEFAULT_ID }.toLong()

    /** "<name> copy", then "copy 2", "copy 3" …, each trimmed to fit the name limit. */
    private fun copyNameFor(base: String): String {
        fun candidate(suffix: String): String =
            normalizeName(base.take(MAX_NAME_LENGTH - suffix.length)) + suffix
        var name = candidate(" copy")
        var index = 2
        while (songLists.values.any { displayName(it).equals(name, ignoreCase = true) }) {
            name = candidate(" copy $index")
            index++
        }
        return name
    }

    // MARK: - Firestore

    fun attachToFirestore(store: Boolean = false) {
        val user = Firebase.auth.currentUser ?: return
        // A brand-new account keeps what this device has (the login flow uploads it); signing
        // in to an existing account means the account's lists replace the local ones.
        val remoteWins = !store && !isNewAccount(user.metadata?.creationTimestamp, user.metadata?.lastSignInTimestamp)
        attachToFirestore(Firebase.firestore.document("/users/${user.uid}"), user.uid, store, remoteWins)
    }

    /**
     * Whether the account was created by the sign-in that just happened: Firebase stamps both
     * times in the same request, so they differ by no more than the request took.
     */
    internal fun isNewAccount(
        createdAt: Long?,
        lastSignInAt: Long?,
    ): Boolean {
        if (createdAt == null || lastSignInAt == null) return false
        return kotlin.math.abs(lastSignInAt - createdAt) < NEW_ACCOUNT_WINDOW_MS
    }

    /** Set while a sign-in is waiting for its first server snapshot to decide which lists stay. */
    private var pruneOnServerSnapshot = false

    /**
     * The injectable seam the emulator tests use: a user document and the uid that owns it.
     *
     * With [remoteWins], the first snapshot confirmed by the server decides the set of lists:
     * any list on this device that the account does not have is dropped, exactly as the
     * account's default list has always replaced the local one.
     */
    internal fun attachToFirestore(
        userDoc: DocumentReference,
        uid: String,
        store: Boolean,
        remoteWins: Boolean = !store,
    ) {
        if (attachment.isConnectedTo(uid, allListeners.isNotEmpty())) {
            if (store) {
                pruneOnServerSnapshot = false
                storeAll()
            }
            return
        }
        detachFromFirestore()
        attachment.connect(uid)
        this.userDoc = userDoc
        pruneOnServerSnapshot = remoteWins
        songLists.values.forEach { it.setParent(userDoc) }
        if (store) {
            storeAll()
        }
        listenForSongLists(uid)
    }

    /**
     * Remote wins: keeps the default list and every list the account has; discards the rest.
     * Returns the surviving map, and marks the discarded lists so a stale holder cannot write
     * them back.
     */
    internal fun localListsAfterRemoteWins(
        lists: Map<String, SongList>,
        remoteIds: Set<String>,
    ): Map<String, SongList> {
        val survivors = lists.filter { (id, _) -> id == DEFAULT_ID || id in remoteIds }
        lists.values.filter { it.id !in survivors }.forEach { it.discardLocally() }
        return survivors
    }

    private fun listenForSongLists(userId: String) {
        val listener =
            // Metadata changes are included so the moment the server confirms the cached view
            // is reported even when no document changed: that confirmation is what settles a
            // sign-in (see `pruneOnServerSnapshot`).
            userDoc?.collection("songLists")?.addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null || Firebase.auth.currentUser?.uid != userId) {
                    return@addSnapshotListener
                }
                val changes = snapshot?.documentChanges ?: return@addSnapshotListener
                if (changes.isEmpty() && !(pruneOnServerSnapshot && !snapshot.metadata.isFromCache)) {
                    return@addSnapshotListener
                }
                val startedAt = SystemClock.elapsedRealtime()
                var updatedLists = songLists
                var mapChanged = false

                changes.forEach { change ->
                    val id = change.document.id
                    when (change.type) {
                        DocumentChange.Type.ADDED -> {
                            val existing = updatedLists[id]
                            if (existing != null) {
                                existing.restore(change.document)
                            } else {
                                updatedLists = updatedLists + (id to SongList(change.document))
                                mapChanged = true
                            }
                        }

                        DocumentChange.Type.MODIFIED -> {
                            updatedLists[id]?.restore(change.document)
                        }

                        DocumentChange.Type.REMOVED -> {
                            // Flagged first: an editor still holding the list would otherwise
                            // store it back and recreate the document the other device deleted.
                            updatedLists[id]?.discardLocally()
                            updatedLists = updatedLists - id
                            mapChanged = true
                        }
                    }
                }
                // The first server-confirmed snapshot of a sign-in settles which lists exist
                // here. A cached (possibly empty) snapshot must not: it says nothing about the
                // account.
                if (pruneOnServerSnapshot && snapshot.metadata.isFromCache.not()) {
                    pruneOnServerSnapshot = false
                    val remoteIds = snapshot.documents.map { it.id }.toSet()
                    val pruned = localListsAfterRemoteWins(updatedLists, remoteIds)
                    if (pruned.size != updatedLists.size) {
                        updatedLists = pruned
                        mapChanged = true
                        if (storedCurrentListId !in pruned) storedCurrentListId = DEFAULT_ID
                    }
                }
                // Persist the map once per snapshot, not once per added/removed
                // document. This avoids quadratic JSON serialization at login.
                if (mapChanged) songLists = updatedLists
                // A rename or a reorder from another device changes no key, so the map assignment
                // above does not fire; the selector and the manage screen still have to re-render.
                listsChanged.changed()
                PerformanceDiagnostics.logDuration(
                    "Firestore song snapshot applied",
                    startedAt,
                    "changes=${changes.size}; lists=${updatedLists.size}; mapChanged=$mapChanged",
                )
            }
        if (listener != null) allListeners.add(listener)
    }

    fun detachFromFirestore() {
        allListeners.forEach { it.remove() }
        allListeners.clear()
        userDoc = null
        pruneOnServerSnapshot = false
        attachment.clear()
    }

    fun removeSongList(key: String) {
        songLists = songLists - key
    }

    fun storeAll() {
        songLists.values.forEach {
            it.storeValue()
        }
    }

    companion object {
        const val DEFAULT_ID = "default"
        private const val LEGACY_DEFAULT_NAME = "Default"
        private const val OLD_SONGS_KEY = "depollsoft.pitchperfect.SongsModel"
        private const val SONG_LISTS_KEY = "depollsoft.pitchperfect.SongLists"
        private const val CURRENT_LIST_KEY = "depollsoft.pitchperfect.CurrentSongList"
        private const val MAX_NAME_LENGTH = 60
        private const val NEW_ACCOUNT_WINDOW_MS = 60_000L
        private const val MAX_SLUG_LENGTH = 40
        private const val SUFFIX_LENGTH = 4
        private const val BASE36 = "abcdefghijklmnopqrstuvwxyz0123456789"
        private val WHITESPACE = Regex("\\s+")
        private val DASHES = Regex("-+")
        private val instanceLazy = lazy { SongsModel() }
        private val instance: SongsModel by instanceLazy

        val isInitialized: Boolean
            get() = instanceLazy.isInitialized()

        @JvmStatic
        fun get(): SongsModel = instance
    }

    init {
        val serializedSongs = Preferences.get<StateList<PitchedSong>>(OLD_SONGS_KEY)
        if (serializedSongs != null) {
            val list = SongList(DEFAULT_ID)
            list.name = LEGACY_DEFAULT_NAME
            list.songs = serializedSongs
            songLists = songLists + (DEFAULT_ID to list)
            Preferences.set(OLD_SONGS_KEY, null)
        }
        if (songLists.isEmpty()) {
            val list = SongList(DEFAULT_ID)
            list.name = LEGACY_DEFAULT_NAME
            songLists = songLists + (DEFAULT_ID to list)
        }
    }
}
