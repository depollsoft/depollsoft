package depollsoft.tagmaster

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.*

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

    // parseDate tests
    
    @Test
    fun parseDate_returns_epoch_for_null_input() {
        val result = parseDate(null)
        assertEquals(0L, result.time)
    }

    @Test
    fun parseDate_returns_epoch_for_blank_input() {
        assertEquals(0L, parseDate("").time)
        assertEquals(0L, parseDate("   ").time)
    }

    @Test
    fun parseDate_parses_millisecond_timestamp() {
        // January 1, 2020 00:00:00 UTC = 1577836800000ms
        val result = parseDate("1577836800000")
        assertEquals(1577836800000L, result.time)
    }

    @Test
    fun parseDate_parses_iso_format() {
        val result = parseDate("2020-01-15")
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US)
        cal.time = result
        assertEquals(2020, cal.get(Calendar.YEAR))
        assertEquals(Calendar.JANUARY, cal.get(Calendar.MONTH))
        assertEquals(15, cal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun parseDate_parses_us_format() {
        val result = parseDate("Jan 15, 2020")
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US)
        cal.time = result
        assertEquals(2020, cal.get(Calendar.YEAR))
        assertEquals(Calendar.JANUARY, cal.get(Calendar.MONTH))
        assertEquals(15, cal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun parseDate_returns_epoch_for_invalid_format() {
        val result = parseDate("not-a-date")
        assertEquals(0L, result.time)
    }

    @Test
    fun parseDate_handles_negative_timestamp() {
        // Timestamps before epoch are valid
        val result = parseDate("-1000")
        assertEquals(-1000L, result.time)
    }
}
