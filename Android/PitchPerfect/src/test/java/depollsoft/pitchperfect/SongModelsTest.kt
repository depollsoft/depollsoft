package depollsoft.pitchperfect

import androidx.compose.ui.text.input.TextFieldValue
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.SetOptions
import depollsoft.lib.util.Preferences
import depollsoft.pitchperfect.ComposeScreens.Companion.song
import depollsoft.pitchperfect.lib.Key
import depollsoft.pitchperfect.lib.KeyType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyMap
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The song models behind the Songs tab and the editor: when a list stores itself, and what the
 * editor does with a song before and after Save.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class SongModelsTest {
    private lateinit var document: DocumentReference

    @Before
    fun setUp() {
        Preferences.setTestMode(true)
        Preferences.clearTestValues()
        SongsModel.get().songLists = SongsModel.get().songLists
        document = Mockito.mock(DocumentReference::class.java)
    }

    @After
    fun tearDown() {
        Preferences.setTestMode(false)
    }

    /** A list attached to a mocked Firestore document, to see when it writes. */
    private fun attachedList(): SongList {
        val user = Mockito.mock(DocumentReference::class.java)
        val collection = Mockito.mock(CollectionReference::class.java)
        Mockito.`when`(user.collection("songLists")).thenReturn(collection)
        Mockito.`when`(collection.document("list-1")).thenReturn(document)
        return SongList("list-1").also { it.setParent(user) }
    }

    private fun writes(): Int =
        Mockito.mockingDetails(document).invocations.count { it.method.name == "set" }

    // ==================== Storing ====================

    @Test
    fun everyChangeToTheSongsOrTheNameIsStoredOnce() {
        val list = attachedList()
        list.name = "Saturday show"
        assertEquals(1, writes())
        list.name = "Saturday show"
        assertEquals("an unchanged name is not a change", 1, writes())

        val blue = song("Blue Skies")
        list.addSong(blue)
        list.addSong(blue)
        assertEquals("a song already in the list is not added twice", 2, writes())
        list.addSong(song("Shenandoah"))
        list.moveUp(list.songs[1])
        list.moveDown(list.songs[0])
        list.sortSongs()
        list.removeSong(blue)
        assertEquals(7, writes())
        Mockito.verify(document, Mockito.atLeastOnce()).set(anyMap<String, Any>(), any(SetOptions::class.java))
        assertTrue("storing keeps the list in the saved map", SongsModel.get().songLists.containsKey("list-1"))
    }

    @Test
    fun aDropStoresTheNewOrderOnceAndAnUnchangedOneNotAtAll() {
        val list = attachedList()
        list.addSongs(listOf(song("A"), song("B"), song("C")))
        val ids = list.songs.map { it.id }
        val before = writes()
        list.reorder(ids)
        assertEquals("the same order is not stored", before, writes())
        list.reorder(listOf(ids[1], ids[2], ids[0]))
        assertEquals(listOf("B", "C", "A"), list.songs.map { it.name })
        assertEquals(before + 1, writes())
    }

    @Test
    fun aDropKeepsSongsTheDragDidNotKnowAbout() {
        val list = attachedList()
        list.addSongs(listOf(song("A"), song("B")))
        val ids = list.songs.map { it.id }
        // A song synced in while the drag was held, and one deleted.
        list.addSong(song("C"))
        list.reorder(listOf(ids[1], "gone", ids[0]))
        assertEquals(listOf("B", "A", "C"), list.songs.map { it.name })
    }

    @Test
    fun aDeletedListNeverWritesAgain() {
        val list = attachedList()
        list.deleteRemote()
        list.addSong(song("Blue Skies"))
        Mockito.verify(document).delete()
        assertEquals(0, writes())
        list.undelete()
        list.name = "Back"
        assertEquals(1, writes())
    }

    // ==================== Keys ====================

    @Test
    fun spokenNamesSpellTheAccidentalModeAndCount() {
        assertEquals("A flat major, 4 flats", SongKeys.spokenName(Key.getMajorKeys()[2]))
        assertEquals("F sharp major, 6 sharps", SongKeys.spokenName(Key.getMajorKeys()[12]))
        assertEquals("A minor, no sharps or flats", SongKeys.spokenName(Key.getMinorKeys()[6]))
        assertEquals("D minor, 1 flat", SongKeys.spokenName(Key.getMinorKeys()[5]))
        assertEquals("G major, 1 sharp", SongKeys.spokenName(Key.getMajorKeys()[7]))
    }

    @Test
    fun flippingTheModeKeepsTheSignature() {
        val eFlat = Key.getMajorKeys()[3]
        val relative = SongKeys.relative(eFlat, minor = true)
        assertEquals(KeyType.Minor, relative.keyType)
        assertEquals(eFlat.numAccidentals, relative.numAccidentals)
        assertEquals("c", relative.friendlyName)
        assertEquals("already minor stays put", relative, SongKeys.relative(relative, minor = true))
    }

    // ==================== Editor ====================

    @Test
    fun aNewSongStartsInCMajorAndSavesWithATrimmedTitle() {
        val list = SongList("default")
        val editor = SongEditorState(list, songId = null)
        assertFalse(editor.editing)
        assertEquals("C", editor.key.friendlyName)
        assertFalse(editor.minor)

        assertFalse("no title, no save", editor.save())
        assertTrue(editor.titleMissing)
        editor.titleChanged(TextFieldValue("  Blue Skies "))
        assertFalse("typing clears the complaint", editor.titleMissing)
        editor.setMinor(true)
        editor.choose(Key.getMinorKeys()[2])
        assertTrue(editor.save())
        assertEquals("Blue Skies", list.songs.single().name)
        assertEquals(Key.getMinorKeys()[2], list.songs.single().key)
    }

    @Test
    fun editingChangesTheSongOnlyOnSaveAndKeepsItsId() {
        val list = SongList("default")
        val original = song("Blue Skies", Key.getMajorKeys()[6])
        list.addSong(original)
        val editor = SongEditorState(list, original.id)
        assertTrue(editor.editing)
        assertEquals("Blue Skies", editor.title.text)

        editor.titleChanged(TextFieldValue("Blue Skies (tag)"))
        editor.choose(Key.getMajorKeys()[7])
        assertEquals("Blue Skies", original.name)
        assertTrue(editor.save())
        assertEquals("Blue Skies (tag)", original.name)
        assertEquals("G", original.key.friendlyName)
        assertEquals(1, list.songs.size)

        editor.remove()
        assertTrue(list.songs.isEmpty())
    }

    @Test
    fun anEditorForASongNoLongerInTheListAddsANewOne() {
        val list = SongList("default")
        val editor = SongEditorState(list, "gone")
        assertFalse(editor.editing)
        editor.titleChanged(TextFieldValue("Blue Skies"))
        editor.remove()
        assertTrue(editor.save())
        assertEquals(1, list.songs.size)
    }
}
