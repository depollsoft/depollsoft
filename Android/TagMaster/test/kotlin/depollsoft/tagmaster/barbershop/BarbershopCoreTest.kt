package depollsoft.tagmaster.barbershop

import org.junit.Assert.*
import org.junit.Test
class BarbershopCoreTest {

    @Test
    fun enums_cover_values() {
        // Exercise enum values and valueOf
        assertEquals(TagCollection.ClassicTags, TagCollection.valueOf("ClassicTags"))
        assertTrue(TagCollection.values().isNotEmpty())

        assertEquals(TagSortOptions.Title, TagSortOptions.valueOf("Title"))
        assertTrue(TagSortOptions.values().contains(TagSortOptions.Rating))
    }

    @Test
    fun remoteLocation_properties_roundTrip() {
        val rl = RemoteLocation()
        rl.uri = "https://example.com"
        rl.type = "video/mp4"
        assertEquals("https://example.com", rl.uri)
        assertEquals("video/mp4", rl.type)
    }

    @Test
    fun tagQueryResult_fields_roundTrip() {
        val tqr = TagQueryResult()
        tqr.start = 10
        tqr.count = 25
        tqr.available = 100
        assertEquals(10, tqr.start)
        assertEquals(25, tqr.count)
        assertEquals(100, tqr.available)
    }
}
