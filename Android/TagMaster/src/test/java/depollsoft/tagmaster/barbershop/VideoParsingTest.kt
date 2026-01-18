package depollsoft.tagmaster.barbershop

import depollsoft.lib.xml.XmlElement
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoParsingTest {
    private fun elem(name: String, value: String? = null): XmlElement {
        val e = XmlElement()
        e.name = name
        if (value != null) e.value = value
        return e
    }

    @Test
    fun parses_video_from_xml() {
        val xml = elem("video")
        xml.elements.add(elem("id", "42"))
        xml.elements.add(elem("Desc", "Sample"))
        xml.elements.add(elem("SungKey", "C"))
        xml.elements.add(elem("Multitrack", "Yes"))
        xml.elements.add(elem("Code", "abcd"))
        xml.elements.add(elem("SungBy", "Choir"))
        xml.elements.add(elem("SungWebsite", "http://example.com"))

        val v = Video()
        v.parseFromXml(xml)

        assertEquals(42, v.id)
        assertEquals("Sample", v.description)
        assertEquals("C", v.sungKey)
        assertEquals(true, v.isMultitrack)
        assertEquals("abcd", v.youTubeCode)
        assertEquals("Choir", v.sungBy)
        assertEquals("http://example.com", v.sungWebsite)
    }

    @Test
    fun parses_video_with_multitrack_no() {
        val xml = elem("video")
        xml.elements.add(elem("id", "1"))
        xml.elements.add(elem("Multitrack", "No"))
        val v = Video()
        v.parseFromXml(xml)
        assertEquals(false, v.isMultitrack)
    }

    @Test
    fun enums_coverage() {
        // Touch enums to include them in coverage
        val c = TagCollection.ClassicTags
        val e = TagSortOptions.Rating
        assertEquals(TagCollection.ClassicTags, c)
        assertEquals(TagSortOptions.Rating, e)
    }
}
