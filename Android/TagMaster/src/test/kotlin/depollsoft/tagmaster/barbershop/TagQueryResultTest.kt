package depollsoft.tagmaster.barbershop

import org.junit.Assert.assertEquals
import org.junit.Test

class TagQueryResultTest {
    @Test
    fun defaults_and_setters() {
        val r = TagQueryResult()
        assertEquals(0, r.available)
        assertEquals(0, r.count)
        assertEquals(0, r.start)
        r.available = 3
        r.count = 2
        r.start = 1
        assertEquals(3, r.available)
        assertEquals(2, r.count)
        assertEquals(1, r.start)
    }
}

