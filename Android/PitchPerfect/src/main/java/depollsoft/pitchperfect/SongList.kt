package depollsoft.pitchperfect

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.getField
import depollsoft.lib.json.JsonSerializer
import depollsoft.lib.state.StateField
import depollsoft.lib.state.StateList
import depollsoft.lib.toMap
import depollsoft.pitchperfect.lib.PitchedSong
import org.json.JSONObject

/**
 * One set list: its songs in order, its name and its place among the lists.
 *
 * Every change to the name or the songs is stored at once — to the preferences and, when signed
 * in, to the list's Firestore document — unless it arrives from Firestore ([isRestoring]).
 * Mutate the songs through the methods here so that happens; [songs] itself is observable, so
 * composables that read it recompose.
 */
class SongList constructor() {
    private var reference: DocumentReference? = null
    lateinit var id: String

    private val nameField = StateField("")
    var name: String
        get() = nameField.get()
        set(value) {
            if (nameField.get() == value) return
            nameField.set(value)
            changed()
        }

    private val songsField = StateField(StateList<PitchedSong>())

    /** The songs, in the list's order. Replacing the whole list stores it. */
    var songs: StateList<PitchedSong>
        get() = songsField.get()
        set(value) {
            songsField.set(value)
            changed()
        }

    /**
     * Position among the custom lists, 0-based, or null on a list that has never been ordered.
     *
     * Deliberately not stored on change: a drag rewrites `order` on every custom list, and the
     * design contract commits the new sequence once on drop rather than once per step.
     */
    var order: Long? by StateField(null)

    val isRestoring =
        object : ThreadLocal<Boolean>() {
            override fun initialValue(): Boolean = false
        }

    constructor(id: String) : this() {
        this.id = id
    }

    constructor(snapshot: DocumentSnapshot) : this() {
        this.reference = snapshot.reference
        this.id = snapshot.id
        restore(snapshot)
    }

    fun setParent(userRef: DocumentReference) {
        this.reference = userRef.collection("songLists").document(id)
    }

    fun restore(snapshot: DocumentSnapshot) {
        @Suppress("UNCHECKED_CAST")
        val rawSongs = snapshot.getField<Any>("songs")!! as List<Any>
        try {
            // Set the guard before changing name: a name change is stored, and storing it
            // unguarded would feed the snapshot straight back into Firestore before the songs
            // have been restored.
            isRestoring.set(true)
            name = snapshot.getString("name")!!
            order = snapshot.getLong("order")
            val newSongs =
                rawSongs.map {
                    @Suppress("UNCHECKED_CAST")
                    JsonSerializer.deserialize(JSONObject(it as Map<String, Any?>)) as PitchedSong
                }
            if (!sameSongs(songs, newSongs)) {
                // Songs that did not change keep their instance, so a row or an editor holding
                // one still holds a song in the list.
                val kept = newSongs.map { new -> songs.firstOrNull { sameSong(it, new) } ?: new }
                // A replaced or removed song can no longer be reached from its row to stop it.
                songs.filter { old -> kept.none { it === old } && old.isPlaying }.forEach { it.stop() }
                songs.replaceWith(kept)
            }
        } finally {
            isRestoring.set(false)
        }
    }

    private fun sameSong(
        left: PitchedSong,
        right: PitchedSong,
    ): Boolean = left.id == right.id && left.name == right.name && left.key == right.key

    private fun sameSongs(
        left: List<PitchedSong>,
        right: List<PitchedSong>,
    ): Boolean = left.size == right.size && left.zip(right).all { sameSong(it.first, it.second) }

    fun sortSongs() {
        songs.replaceWith(songs.sortedBy { it.name.lowercase() })
        changed()
    }

    fun addSong(song: PitchedSong) {
        if (songs.contains(song)) return
        songs.add(song)
        changed()
    }

    /** Appends [added] as one change. */
    fun addSongs(added: List<PitchedSong>) {
        if (added.isEmpty()) return
        songs.transaction { addAll(added) }
        changed()
    }

    fun canMoveDown(s: PitchedSong): Boolean = songs.indexOf(s) < songs.size - 1

    fun canMoveUp(s: PitchedSong): Boolean = songs.indexOf(s) > 0

    fun moveDown(s: PitchedSong) {
        if (!canMoveDown(s)) return
        move(songs.indexOf(s), songs.indexOf(s) + 1)
        changed()
    }

    fun moveUp(s: PitchedSong) {
        if (!canMoveUp(s)) return
        move(songs.indexOf(s), songs.indexOf(s) - 1)
        changed()
    }

    /**
     * One step of a drag: moves the song without storing, since a drag commits once, on drop,
     * through [notifyOfChange].
     */
    fun moveWithoutStoring(
        from: Int,
        to: Int,
    ) {
        if (from == to || from !in songs.indices || to !in songs.indices) return
        move(from, to)
    }

    private fun move(
        from: Int,
        to: Int,
    ) {
        songs.transaction {
            val song = removeAt(from)
            add(to, song)
        }
    }

    fun removeSong(song: PitchedSong) {
        if (songs.remove(song)) changed()
    }

    fun resetSongs() {
        songs.clear()
        changed()
    }

    /** Stores the list after a change made to one of its songs, or a drag's drop. */
    fun notifyOfChange() = changed()

    private fun changed() {
        if (!isRestoring.get()!! && this::id.isInitialized) storeValue()
    }

    /**
     * Set once the list has been deleted. A stale holder — an editor opened on one of its songs,
     * a screen not yet recomposed — can still mutate it afterwards, and storing that would put
     * the list straight back into the map and recreate its document.
     */
    var isDeleted: Boolean = false
        private set

    /** Removes this list's document and refuses every later write. The local map is the caller's business. */
    fun deleteRemote() {
        isDeleted = true
        reference?.delete()
    }

    /** Lets an undone delete write again. */
    fun undelete() {
        isDeleted = false
    }

    /** Drops the list from this device without touching the server: it was never there. */
    fun discardLocally() {
        isDeleted = true
    }

    fun storeValue() {
        if (isDeleted || !this::id.isInitialized || !SongsModel.isInitialized) {
            return
        }
        SongsModel.get().songLists = SongsModel.get().songLists + (id to this)
        val dict =
            mutableMapOf<String, Any>(
                "name" to name,
                "songs" to songs.map { JsonSerializer.serialize(it).toMap() },
            )
        // Left out rather than written as null: the write is a merge, and an older app version
        // that stores only {name, songs} must not be able to erase an order this one set.
        order?.let { dict["order"] = it }
        reference?.set(dict, SetOptions.merge())
    }
}
