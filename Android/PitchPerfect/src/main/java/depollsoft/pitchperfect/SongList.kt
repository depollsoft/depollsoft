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
    val isRestoring = object : ThreadLocal<Boolean>() {
        override fun initialValue(): Boolean {
            return false
        }
    }

    init {
        track({
            songs.track()
            name
        }) {
            if (!isRestoring.get()!!) {
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
        name = snapshot.getString("name")!!
        @Suppress("UNCHECKED_CAST")
        val rawSongs = snapshot.getField<Any>("songs")!! as List<Any>
        try {
            isRestoring.set(true)
            val newSongs = rawSongs.map {
                @Suppress("UNCHECKED_CAST")
                JsonSerializer.deserialize(JSONObject(it as Map<String, Any?>)) as PitchedSong
            }
            if (songs.size == newSongs.size && songs.zip(newSongs).all {
                    it.first.id == it.second.id &&
                            it.first.name == it.second.name &&
                            it.first.key == it.second.key
                }) {
                // No change -- ignore
                return
            }
            songs.become(newSongs) { left, right ->
                left.id == right.id && left.name == right.name && left.key == right.key
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

    fun storeValue() {
        if (!SongsModel.isInitialized) {
            return
        }
        SongsModel.get().songLists = SongsModel.get().songLists + (id to this)
        val dict = mapOf(
            "name" to name,
            "songs" to songs.map { JsonSerializer.serialize(it).toMap() }
        )
        reference?.set(dict, SetOptions.merge())
    }
}