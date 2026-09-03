package depollsoft.pitchperfect

import android.os.SystemClock
import com.bindroid.trackable.TrackableCollection
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import depollsoft.lib.util.Preferences
import depollsoft.lib.util.preference
import depollsoft.lib.util.writeThroughPreference
import depollsoft.pitchperfect.lib.PitchedSong

class SongsModel private constructor() {
    var songLists: Map<String, SongList> by writeThroughPreference(
        SONG_LISTS_KEY,
        mapOf(),
    )

    val defaultSongList: SongList
        get() = songLists["default"]!!

    val allListeners: MutableList<ListenerRegistration> = mutableListOf()

    private var userDoc: DocumentReference? = null
    private val attachment = AuthAttachmentState()

    fun attachToFirestore(store: Boolean = false) {
        val user = Firebase.auth.currentUser ?: return
        if (attachment.isConnectedTo(user.uid, allListeners.isNotEmpty())) {
            if (store) storeAll()
            return
        }
        detachFromFirestore()
        attachment.connect(user.uid)
        userDoc = Firebase.firestore.document("/users/${user.uid}")
        songLists.values.forEach { it.setParent(userDoc!!) }
        if (store) {
            storeAll()
        }
        listenForSongLists(user.uid)
    }

    private fun listenForSongLists(userId: String) {
        val listener =
            userDoc?.collection("songLists")?.addSnapshotListener { snapshot, error ->
                if (error != null || Firebase.auth.currentUser?.uid != userId) {
                    return@addSnapshotListener
                }
                val changes = snapshot?.documentChanges ?: return@addSnapshotListener
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
                            updatedLists = updatedLists - id
                            mapChanged = true
                        }
                    }
                }
                // Persist the map once per snapshot, not once per added/removed
                // document. This avoids quadratic JSON serialization at login.
                if (mapChanged) songLists = updatedLists
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
