package depollsoft.tagmaster.barbershop

import depollsoft.pitchperfect.lib.Accidental
import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, manifest = Config.NONE)
class TagTracksAndKeyNoteTest {

    @Test
    fun keyNote_parses_major_minor_and_accidentals() {
        val tag = Tag()

        tag.writtenKey = "C Major"
        val cMajor = tag.keyNote
        assertNotNull(cMajor)
        assertEquals("C", cMajor!!.friendlyName)
        assertEquals(Accidental.Natural, cMajor.accidental)
        assertEquals(4, cMajor.octave)

        tag.writtenKey = "F# minor"
        val fSharpMinor = tag.keyNote
        assertNotNull(fSharpMinor)
        assertEquals("F", fSharpMinor!!.friendlyName)
        assertEquals(Accidental.Sharp, fSharpMinor.accidental)
        assertEquals(4, fSharpMinor.octave)

        tag.writtenKey = "Bb:"
        val bFlat = tag.keyNote
        assertNotNull(bFlat)
        assertEquals("B", bFlat!!.friendlyName)
        assertEquals(Accidental.Flat, bFlat.accidental)
        assertEquals(4, bFlat.octave)

        tag.writtenKey = null
        assertNull(tag.keyNote)
    }

    @Test
    fun tracks_returns_only_non_null_uris_in_expected_order() {
        val tag = Tag()

        tag.allPartsTrackUri = RemoteLocation().apply { uri = "https://example.com/all.mp3"; type = "mp3" }
        tag.leadTrackUri = RemoteLocation().apply { uri = "https://example.com/lead.mp3"; type = "mp3" }
        tag.bassTrackUri = RemoteLocation().apply { uri = "https://example.com/bass.mp3"; type = "mp3" }
        // intentionally omit tenor + bari
        tag.other2TrackUri = RemoteLocation().apply { uri = "https://example.com/other2.mp3"; type = "mp3" }

        val tracks = tag.tracks
        requireNotNull(tracks)

        assertEquals(listOf("All Parts", "Lead", "Bass", "Other2"), tracks.map { it.title })
    }
}
