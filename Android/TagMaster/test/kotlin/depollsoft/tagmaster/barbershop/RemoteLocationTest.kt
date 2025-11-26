package depollsoft.tagmaster.barbershop

import org.junit.Assert.assertEquals
import org.junit.Test

class RemoteLocationTest {
    @Test
    fun get_set_fields() {
        val rl = RemoteLocation()
        rl.uri = "http://example.com/a.mp3"
        rl.type = "audio/mpeg"
        assertEquals("http://example.com/a.mp3", rl.uri)
        assertEquals("audio/mpeg", rl.type)
    }
}

