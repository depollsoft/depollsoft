package depollsoft.pitchperfect

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import depollsoft.lib.util.Preferences
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SongListRestoreTest {
    @Before
    fun setUp() {
        Preferences.setTestMode(true)
        Preferences.clearTestValues()
        SongsModel.get()
    }

    @After
    fun tearDown() {
        Preferences.setTestMode(false)
    }

    @Test
    fun firestoreRestore_doesNotWriteSnapshotBack() {
        val reference = Mockito.mock(DocumentReference::class.java)
        val snapshot = Mockito.mock(DocumentSnapshot::class.java)
        Mockito.`when`(snapshot.reference).thenReturn(reference)
        Mockito.`when`(snapshot.id).thenReturn("remote-list")
        Mockito.`when`(snapshot.getString("name")).thenReturn("Remote")
        Mockito.`when`(snapshot.get("songs")).thenReturn(emptyList<Map<String, Any?>>())
        Mockito
            .`when`(snapshot.get("songs", Any::class.java))
            .thenReturn(emptyList<Map<String, Any?>>())

        val list = SongList(snapshot)

        assertEquals("Remote", list.name)
        assertEquals(0, list.songs.size)
        Mockito.verifyNoInteractions(reference)
    }
}
