package depollsoft.tagmaster

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UtilitiesTest {

    @Test
    fun asList_returns_typed_list_when_input_is_list() {
        val input: List<Any> = listOf(1, 2, 3)
        val typed: List<Int>? = input.asList()
        requireNotNull(typed)
        assertEquals(listOf(1, 2, 3), typed)
    }

    @Test
    fun asList_returns_null_when_input_is_not_list() {
        val input: List<*>? = null
        val typed: List<Int>? = input.asList()
        assertNull(typed)
    }
}
