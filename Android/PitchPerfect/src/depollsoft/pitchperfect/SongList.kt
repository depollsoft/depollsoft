package depollsoft.pitchperfect

import com.bindroid.trackable.TrackableCollection
import com.bindroid.trackable.track
import com.bindroid.trackable.trackable
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.getField
import depollsoft.lib.json.JsonSerializer
import depollsoft.lib.toMap
import depollsoft.pitchperfect.lib.PitchedSong
import org.json.JSONObject

class SongList {
    private var reference: DocumentReference? = null
    val id: String
    var name: String by trackable("")
    var songs: TrackableCollection<PitchedSong> by trackable(TrackableCollection())

    init {
        track({ songs.track() }) {
            storeValue()
            keepTracking
        }
    }

    constructor(id: String) {
        this.id = id
    }

    constructor(snapshot: DocumentSnapshot) {
        this.reference = snapshot.reference
        this.id = snapshot.id
        restore(snapshot)
    }

    fun setParent(userRef: DocumentReference) {
        userRef.collection("songLists").document(id)
    }

    fun restore(snapshot: DocumentSnapshot) {
        name = snapshot.getString("name")!!
        @Suppress("UNCHECKED_CAST")
        songs = JsonSerializer.deserialize(
            JSONObject(
                snapshot.getField<Map<String, Any?>>("songs")!!
            )
        ) as TrackableCollection<PitchedSong>
    }

    fun sortSongs() {
        songs.sortBy { it.name.lowercase() }
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
        val index = songs.indexOf(s)
        songs.removeAt(index)
        songs.add(index + 1, s)
    }

    fun moveUp(s: PitchedSong) {
        if (!canMoveUp(s)) return
        val index = songs.indexOf(s)
        songs.removeAt(index)
        songs.add(index - 1, s)
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
        SongsModel.get().songLists = SongsModel.get().songLists + (id to this)
        val dict = mapOf(
            "name" to name,
            "songs" to JsonSerializer.serialize(songs).toMap()
        )
        reference?.set(dict, SetOptions.merge())
    }
}