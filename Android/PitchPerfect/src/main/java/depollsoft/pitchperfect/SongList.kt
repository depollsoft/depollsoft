package depollsoft.pitchperfect

import com.bindroid.trackable.*
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.getField
import depollsoft.lib.json.JsonSerializer
import depollsoft.lib.toMap
import depollsoft.pitchperfect.lib.PitchedSong
import org.json.JSONObject

class SongList constructor() {
    private var reference: DocumentReference? = null
    lateinit var id: String
    var name: String by trackable("")
    var songs: TrackableCollection<PitchedSong> by trackable(TrackableCollection<PitchedSong>())

    /**
     * Position among the custom lists, 0-based, or null on a list that has never been ordered.
     *
     * Deliberately outside the store-on-change tracker below: a drag rewrites `order` on every
     * custom list, and the design contract commits the new sequence once on drop rather than once
     * per step.
     */
    var order: Long? by trackable()
    val isRestoring =
        object : ThreadLocal<Boolean>() {
            override fun initialValue(): Boolean = false
        }

    init {
        track({
            songs.track()
            name
        }) {
            if (!isRestoring.get()!! && this@SongList::id.isInitialized) {
                storeValue()
            }
            keepTracking
        }
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
            // Set the guard before changing name: name is trackable and used to
            // write this document, so restoring it unguarded feeds the snapshot
            // straight back into Firestore before songs have been restored.
            isRestoring.set(true)
            name = snapshot.getString("name")!!
            order = snapshot.getLong("order")
            val newSongs =
                rawSongs.map {
                    @Suppress("UNCHECKED_CAST")
                    JsonSerializer.deserialize(JSONObject(it as Map<String, Any?>)) as PitchedSong
                }
            val changed =
                songs.size != newSongs.size ||
                    songs.zip(newSongs).any {
                        it.first.id != it.second.id ||
                            it.first.name != it.second.name ||
                            it.first.key != it.second.key
                    }
            if (changed) {
                songs.become(newSongs) { left, right ->
                    left.id == right.id && left.name == right.name && left.key == right.key
                }
            }
        } finally {
            isRestoring.set(false)
        }
    }

    fun sortSongs() {
        songs.transaction {
            songs.sortBy { it.name.lowercase() }
        }
    }

    fun addSong(song: PitchedSong) {
        if (!songs.contains(song)) songs.add(song)
    }

    fun canMoveDown(s: PitchedSong): Boolean {
        val index = songs.indexOf(s)
        return index < songs.size - 1
    }

    fun canMoveUp(s: PitchedSong): Boolean {
        val index = songs.indexOf(s)
        return index > 0
    }

    fun moveDown(s: PitchedSong) {
        if (!canMoveDown(s)) return
        songs.transaction {
            val index = indexOf(s)
            removeAt(index)
            add(index + 1, s)
        }
    }

    fun moveUp(s: PitchedSong) {
        if (!canMoveUp(s)) return
        songs.transaction {
            val index = indexOf(s)
            removeAt(index)
            add(index - 1, s)
        }
    }

    fun removeSong(song: PitchedSong) {
        songs.remove(song)
    }

    fun resetSongs() {
        songs.clear()
    }

    fun notifyOfChange() {
        songs.updateTrackers()
    }

    /**
     * Set once the list has been deleted. A stale holder — an editor opened on one of its songs,
     * an adapter not yet swapped — can still mutate it afterwards, and the store-on-change tracker
     * would otherwise put the list straight back into the map and recreate its document.
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
