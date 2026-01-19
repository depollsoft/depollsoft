package depollsoft.tagmaster.barbershop

import depollsoft.lib.xml.XmlAttribute
import depollsoft.lib.xml.XmlElement
import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, manifest = Config.NONE)
class TagParseFromXmlTest {

    private fun prop(name: String, value: String?, type: String? = null): XmlElement {
        val e = XmlElement()
        e.name = name
        e.value = value
        if (type != null) {
            val typeAttr = XmlAttribute()
            typeAttr.name = "type"
            typeAttr.value = type
            e.attributes.add(typeAttr)
        }
        return e
    }

    @Test
    fun parseFromXml_parses_basic_string_fields() {
        val root = XmlElement().apply { name = "tag" }
        root.elements.add(prop("Title", "  Test Tag  "))
        root.elements.add(prop("AltTitle", "Alternative Title"))
        root.elements.add(prop("Version", "v2"))
        root.elements.add(prop("WritKey", "Bb Major"))
        root.elements.add(prop("Type", "Barbershop"))
        root.elements.add(prop("Recording", "Studio"))
        root.elements.add(prop("TeachVid", "https://youtube.com/vid"))
        root.elements.add(prop("Lyrics", "La la la"))
        root.elements.add(prop("Notes", "Some notes"))
        root.elements.add(prop("Arranger", "John Doe"))
        root.elements.add(prop("ArrWebsite", "https://arranger.com"))
        root.elements.add(prop("SungBy", "The Quartet"))
        root.elements.add(prop("SungWebsite", "https://quartet.com"))
        root.elements.add(prop("Quartet", "Learning Quartet"))
        root.elements.add(prop("QWebsite", "https://lq.com"))
        root.elements.add(prop("Teacher", "Jane Doe"))
        root.elements.add(prop("TWebsite", "https://teacher.com"))
        root.elements.add(prop("Provider", "Tag Provider"))
        root.elements.add(prop("ProvWebsite", "https://provider.com"))

        val tag = Tag()
        tag.parseFromXmlPublic(root)

        // Values should be trimmed
        assertEquals("Test Tag", tag.title)
        assertEquals("Alternative Title", tag.alternativeTitle)
        assertEquals("v2", tag.version)
        assertEquals("Bb Major", tag.writtenKey)
        assertEquals("Barbershop", tag.tagType)
        assertEquals("Studio", tag.recordingMethod)
        assertEquals("https://youtube.com/vid", tag.teachingVideo)
        assertEquals("La la la", tag.lyrics)
        assertEquals("Some notes", tag.notes)
        assertEquals("John Doe", tag.arranger)
        assertEquals("https://arranger.com", tag.arrangerWebsite)
        assertEquals("The Quartet", tag.sungBy)
        assertEquals("https://quartet.com", tag.sungByWebsite)
        assertEquals("Learning Quartet", tag.learningTrackQuartet)
        assertEquals("https://lq.com", tag.learningTrackQuartetWebsite)
        assertEquals("Jane Doe", tag.teacher)
        assertEquals("https://teacher.com", tag.teacherWebsite)
        assertEquals("Tag Provider", tag.provider)
        assertEquals("https://provider.com", tag.providerWebsite)
    }

    @Test
    fun parseFromXml_parses_numeric_fields() {
        val root = XmlElement().apply { name = "tag" }
        root.elements.add(prop("id", "12345"))
        root.elements.add(prop("Parts", "4"))
        root.elements.add(prop("Classic", "42"))
        root.elements.add(prop("Rating", "4.5"))
        root.elements.add(prop("Downloaded", "1,234"))

        val tag = Tag()
        tag.parseFromXmlPublic(root)

        assertEquals(12345, tag.id)
        assertEquals(4, tag.parts)
        assertEquals(42, tag.classicTagNumber)
        assertEquals(4.5, tag.rating!!, 0.001)
        assertEquals(1234, tag.downloadCount)
    }

    @Test
    fun parseFromXml_parses_date_field() {
        val root = XmlElement().apply { name = "tag" }
        // Use millisecond timestamp
        root.elements.add(prop("Posted", "1577836800000"))

        val tag = Tag()
        tag.parseFromXmlPublic(root)

        assertNotNull(tag.posted)
        assertEquals(1577836800000L, tag.posted!!.time)
    }

    @Test
    fun parseFromXml_parses_remote_location_fields() {
        val root = XmlElement().apply { name = "tag" }
        root.elements.add(prop("SheetMusic", "https://example.com/sheet.pdf", "pdf"))
        root.elements.add(prop("Notation", "https://example.com/notation.xml", "xml"))
        root.elements.add(prop("AllParts", "https://example.com/all.mp3", "mp3"))
        root.elements.add(prop("Bass", "https://example.com/bass.mp3", "mp3"))
        root.elements.add(prop("Bari", "https://example.com/bari.mp3", "mp3"))
        root.elements.add(prop("Lead", "https://example.com/lead.mp3", "mp3"))
        root.elements.add(prop("Tenor", "https://example.com/tenor.mp3", "mp3"))
        root.elements.add(prop("Other1", "https://example.com/other1.mp3", "mp3"))
        root.elements.add(prop("Other2", "https://example.com/other2.mp3", "mp3"))
        root.elements.add(prop("Other3", "https://example.com/other3.mp3", "mp3"))
        root.elements.add(prop("Other4", "https://example.com/other4.mp3", "mp3"))

        val tag = Tag()
        tag.parseFromXmlPublic(root)

        assertNotNull(tag.sheetMusicUri)
        assertEquals("https://example.com/sheet.pdf", tag.sheetMusicUri?.uri)
        assertEquals("pdf", tag.sheetMusicUri?.type)

        assertNotNull(tag.notationUri)
        assertEquals("https://example.com/notation.xml", tag.notationUri?.uri)

        assertNotNull(tag.allPartsTrackUri)
        assertEquals("https://example.com/all.mp3", tag.allPartsTrackUri?.uri)

        assertNotNull(tag.bassTrackUri)
        assertEquals("https://example.com/bass.mp3", tag.bassTrackUri?.uri)

        assertNotNull(tag.baritoneTrackUri)
        assertEquals("https://example.com/bari.mp3", tag.baritoneTrackUri?.uri)

        assertNotNull(tag.leadTrackUri)
        assertEquals("https://example.com/lead.mp3", tag.leadTrackUri?.uri)

        assertNotNull(tag.tenorTrackUri)
        assertEquals("https://example.com/tenor.mp3", tag.tenorTrackUri?.uri)

        assertNotNull(tag.other1TrackUri)
        assertEquals("https://example.com/other1.mp3", tag.other1TrackUri?.uri)

        assertNotNull(tag.other2TrackUri)
        assertEquals("https://example.com/other2.mp3", tag.other2TrackUri?.uri)

        assertNotNull(tag.other3TrackUri)
        assertEquals("https://example.com/other3.mp3", tag.other3TrackUri?.uri)

        assertNotNull(tag.other4TrackUri)
        assertEquals("https://example.com/other4.mp3", tag.other4TrackUri?.uri)
    }

    @Test
    fun parseFromXml_ignores_empty_remote_locations() {
        val root = XmlElement().apply { name = "tag" }
        root.elements.add(prop("SheetMusic", "", "pdf"))
        root.elements.add(prop("Bass", null, "mp3"))

        val tag = Tag()
        tag.parseFromXmlPublic(root)

        assertNull(tag.sheetMusicUri)
        assertNull(tag.bassTrackUri)
    }

    @Test
    fun parseFromXml_parses_videos() {
        val root = XmlElement().apply { name = "tag" }
        
        val videosElem = XmlElement().apply { name = "videos" }
        
        val video1 = XmlElement().apply { name = "video" }
        video1.elements.add(prop("id", "1"))
        video1.elements.add(prop("Desc", "First video"))
        video1.elements.add(prop("Code", "ABC123"))
        
        val video2 = XmlElement().apply { name = "video" }
        video2.elements.add(prop("id", "2"))
        video2.elements.add(prop("Desc", "Second video"))
        video2.elements.add(prop("Code", "XYZ789"))
        
        videosElem.elements.add(video1)
        videosElem.elements.add(video2)
        root.elements.add(videosElem)

        val tag = Tag()
        tag.parseFromXmlPublic(root)

        assertNotNull(tag.videos)
        assertEquals(2, tag.videos?.size)
        assertEquals(1, tag.videos?.get(0)?.id)
        assertEquals("First video", tag.videos?.get(0)?.description)
        assertEquals("ABC123", tag.videos?.get(0)?.youTubeCode)
        assertEquals(2, tag.videos?.get(1)?.id)
    }

    @Test
    fun parseFromXml_handles_null_and_empty_values() {
        val root = XmlElement().apply { name = "tag" }
        root.elements.add(prop("Title", null))
        root.elements.add(prop("AltTitle", ""))
        root.elements.add(prop("Version", "   "))  // whitespace only

        val tag = Tag()
        tag.parseFromXmlPublic(root)

        assertNull(tag.title)
        assertNull(tag.alternativeTitle)
        assertNull(tag.version)
    }

    @Test
    fun parseFromXml_updates_lastRefreshed() {
        val root = XmlElement().apply { name = "tag" }
        
        val tag = Tag()
        val beforeParse = System.currentTimeMillis()
        tag.parseFromXmlPublic(root)
        val afterParse = System.currentTimeMillis()

        assertTrue(tag.lastRefreshed.time >= beforeParse)
        assertTrue(tag.lastRefreshed.time <= afterParse)
    }

    @Test
    fun parseFromXml_handles_year_fields() {
        val root = XmlElement().apply { name = "tag" }
        root.elements.add(prop("Arranged", "2020"))
        root.elements.add(prop("SungYear", "2021"))

        val tag = Tag()
        tag.parseFromXmlPublic(root)

        assertEquals("2020", tag.yearArranged)
        assertEquals("2021", tag.sungYear)
    }

    @Test
    fun tag_equals_compares_by_id() {
        val tag1 = Tag()
        tag1.id = 777

        val tag2 = Tag()
        tag2.id = 777

        assertTrue(tag1 == tag2)
    }
}

// Extension to expose protected parseFromXml for testing
private fun Tag.parseFromXmlPublic(element: XmlElement) {
    val method = Tag::class.java.getDeclaredMethod("parseFromXml", XmlElement::class.java)
    method.isAccessible = true
    method.invoke(this, element)
}
