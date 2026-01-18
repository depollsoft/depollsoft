package depollsoft.tagmaster.barbershop

import depollsoft.lib.xml.XmlElement
import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, manifest = Config.NONE)
class VideoParseFromXmlTest {

    private fun prop(name: String, value: String?): XmlElement {
        val e = XmlElement()
        e.name = name
        e.value = value
        return e
    }

    @Test
    fun parseFromXml_parses_fields_and_trims_values() {
        val root = XmlElement().apply { name = "video" }
        root.elements.add(prop("id", "7"))
        root.elements.add(prop("Desc", "  Demo  "))
        root.elements.add(prop("SungKey", " C# "))
        root.elements.add(prop("Multitrack", "No"))
        root.elements.add(prop("Code", "XYZ"))
        root.elements.add(prop("SungBy", "Choir"))
        root.elements.add(prop("SungWebsite", "https://example.com"))
        // Use millisecond timestamp (Jan 1, 2018 midnight UTC = 1514764800000)
        root.elements.add(prop("Posted", "1514764800000"))

        val video = Video()
        video.parseFromXml(root)

        assertEquals(7, video.id)
        assertEquals("Demo", video.description)
        assertEquals("C#", video.sungKey)
        assertFalse(video.isMultitrack)
        assertEquals("XYZ", video.youTubeCode)
        assertEquals("Choir", video.sungBy)
        assertEquals("https://example.com", video.sungWebsite)
        assertNotNull(video.posted)
        assertTrue(video.posted.time > 0)
    }
}
