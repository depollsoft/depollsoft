package depollsoft.pitchperfect

import depollsoft.lib.json.JsonSerializer
import depollsoft.lib.state.StateList
import depollsoft.lib.util.Preferences
import depollsoft.pitchperfect.lib.Accidental
import depollsoft.pitchperfect.lib.KeyType
import depollsoft.pitchperfect.lib.Note
import depollsoft.pitchperfect.lib.PitchedSong
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Lists stored by the Bindroid-era app still load. The fixtures in `src/test/resources/fixtures`
 * were written by that code, before its models moved to snapshot state.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class StoredDataTest {
    @Before
    fun setUp() {
        Preferences.setTestMode(true)
        Preferences.clearTestValues()
        PitchPerfectApplication.registerStorageAliases()
    }

    @After
    fun tearDown() {
        Preferences.setTestMode(false)
    }

    private fun fixture(name: String): String =
        requireNotNull(javaClass.classLoader!!.getResource("fixtures/$name")) { "missing fixture $name" }.readText()

    @Test
    fun setListsSavedByTheBindroidEraAppLoad() {
        @Suppress("UNCHECKED_CAST")
        val mappings = JsonSerializer.deserialize(fixture("song_lists_v5.json")) as Preferences.MappingList
        val lists = mappings.associate { it.key as String to it.value as SongList }

        assertEquals(listOf("default", "contest-set-ab12", "tags-9z9z"), lists.keys.toList())
        val home = lists.getValue("default")
        assertEquals("Default", home.name)
        assertTrue("the songs come back observable", home.songs is StateList<*>)
        assertEquals(listOf("Blue Skies", "Shenandoah"), home.songs.map { it.name })
        assertEquals(listOf("1b6f", "2c7a"), home.songs.map { it.id })
        val shenandoah = home.songs[1].key
        assertEquals(KeyType.Minor, shenandoah.keyType)
        assertEquals("C", shenandoah.note.friendlyName)
        assertEquals(-3, shenandoah.numAccidentals)
        assertNull(home.order)

        val contest = lists.getValue("contest-set-ab12")
        assertEquals("Contest Set", contest.name)
        assertEquals(0L, contest.order)
        val adeline = contest.songs.single()
        assertEquals("Sweet Adeline", adeline.name)
        assertEquals(Accidental.Flat, adeline.key.accidental)
        assertEquals("B", adeline.key.friendlyName)

        assertTrue(lists.getValue("tags-9z9z").songs.isEmpty())
    }

    @Test
    fun theOldestSingleListFormatLoadsIntoMySongs() {
        @Suppress("UNCHECKED_CAST")
        val songs = JsonSerializer.deserialize(fixture("songs_legacy.json")) as StateList<PitchedSong>
        assertEquals(listOf("Heart of My Heart", "The Old Songs"), songs.map { it.name })
        assertEquals(-6, songs[0].key.numAccidentals)
        assertEquals(KeyType.Minor, songs[1].key.keyType)
    }

    @Test
    fun aStoredSongNeverStartsPlayingWhenItLoads() {
        val played = mutableListOf<Note>()
        Note.setPlayer(
            object : Note.NotePlayer {
                override fun play(n: Note) {
                    played += n
                }

                override fun stop(n: Note) = Unit
            },
        )
        try {
            val sounding = ComposeScreens.song("Blue Skies")
            sounding.key.note.isPlaying = true
            val saved = JsonSerializer.serialize(sounding).toString()
            sounding.key.note.isPlaying = false
            played.clear()
            assertTrue(saved, !saved.contains("IsPlaying"))

            // Songs stored before this fix carry the flag; a playing one must load silent.
            val flag = Regex("(\"IsPlaying\":\\s*\\{[^}]*\"Value\":\\s*)false")
            val original = fixture("songs_legacy.json")
            assertEquals("the fixture has flags to flip", 2, flag.findAll(original).count())
            val stale = original.replace(flag, "$1true")
            @Suppress("UNCHECKED_CAST")
            val songs = JsonSerializer.deserialize(stale) as StateList<PitchedSong>
            assertEquals(2, songs.size)
            assertEquals("loading plays nothing", emptyList<Note>(), played)
            assertTrue(songs.none { it.key.note.isPlaying })
        } finally {
            Note.setPlayer(Note.DEFAULT_PLAYER)
        }
    }

    @Test
    fun listsSaveUnderTheSameNamesTheyLoadedFrom() {
        val list = SongList("default")
        list.name = "Default"
        list.addSong(ComposeScreens.song("Blue Skies"))
        val json = JsonSerializer.serialize(list).toString()
        assertTrue(json, json.contains("\"*type\":\"SongList\""))
        assertTrue("a Bindroid-era app can still read the songs back", json.contains("\"*type\":\"List\""))
        assertTrue(json.contains("\"*type\":\"PitchedSong\""))
    }
}
