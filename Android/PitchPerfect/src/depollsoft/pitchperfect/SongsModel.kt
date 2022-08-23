package depollsoft.pitchperfect

import com.bindroid.trackable.TrackableCollection
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import depollsoft.lib.util.Preferences
import depollsoft.lib.util.preference
import depollsoft.lib.util.writeThroughPreference
import depollsoft.pitchperfect.lib.PitchedSong

class SongsModel private constructor() {
    var songLists: Map<String, SongList> by writeThroughPreference(
        SONG_LISTS_KEY,
        mapOf()
    )

    val defaultSongList: SongList
        get() = songLists["default"]!!

    val allListeners: MutableList<ListenerRegistration> = mutableListOf()

    private var userDoc: DocumentReference? = null

    fun attachToFirestore(store: Boolean = false) {
        userDoc = Firebase.firestore.document("/users/${Firebase.auth.currentUser!!.uid}")
        songLists.values.forEach { it.setParent(userDoc!!) }
        if (store) {
            storeAll()
        }
        listenForSongLists()
    }

    private fun listenForSongLists() {
        val listener = userDoc?.collection("songLists")?.addSnapshotListener { snapshot, error ->
            if (error != null) {
                return@addSnapshotListener
            }
            snapshot!!.documentChanges.forEach {
                when (it.type) {
                    DocumentChange.Type.ADDED -> {
                        if (songLists.containsKey(it.document.id)) {
                            songLists[it.document.id]?.restore(it.document)
                        } else {
                            songLists = songLists + (it.document.id to SongList(it.document))
                        }
                    }
                    DocumentChange.Type.MODIFIED -> {
                        songLists[it.document.id]?.restore(it.document)
                    }
                    DocumentChange.Type.REMOVED -> {
                        removeSongList(it.document.id)
                    }
                }
            }
        }
        if (listener != null) {
            allListeners.add(listener)
        }
    }

    fun detachFromFirestore() {
        userDoc = null
        allListeners.forEach { it.remove() }
        allListeners.clear()
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
        private const val OLD_SONGS_KEY = "depollsoft.pitchperfect.SongsModel"
        private const val SONG_LISTS_KEY = "depollsoft.pitchperfect.SongLists"
        private val instanceLazy = lazy { SongsModel() }
        private val instance: SongsModel by instanceLazy

        val isInitialized: Boolean
            get() = instanceLazy.isInitialized()

        @JvmStatic
        fun get(): SongsModel = instance
    }

    init {
        val serializedSongs = Preferences.get<TrackableCollection<PitchedSong>>(OLD_SONGS_KEY)
        if (serializedSongs != null) {
            val list = SongList("default")
            list.name = "Default"
            list.songs = serializedSongs
            songLists = songLists + ("default" to list)
            Preferences.set(OLD_SONGS_KEY, null)
        }
        if (songLists.isEmpty()) {
            val list = SongList("default")
            list.name = "Default"
            songLists = songLists + ("default" to list)
        }
    }
}