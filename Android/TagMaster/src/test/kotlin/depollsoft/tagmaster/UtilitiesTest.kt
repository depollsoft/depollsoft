package depollsoft.tagmaster

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.appcompat.widget.AppCompatDrawableManager
import depollsoft.lib.activity.BrowserActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.util.*

class UtilitiesTest {
    @Test
    fun asList_returns_typed_list_when_input_is_list() {
        val input: List<Any> = listOf(1, 2, 3)
        val typed: List<Int>? = input.asList()
        assertEquals(listOf(1, 2, 3), requireNotNull(typed))
    }

    @Test
    fun asList_returns_null_when_input_is_not_list() {
        val input: List<*>? = null
        assertNull(input.asList<Int>())
    }

    @Test
    fun parseDate_returns_null_for_absent_or_invalid_input() {
        for (input in listOf(null, "", "   ", "not-a-date")) assertNull(parseDate(input))
    }

    @Test
    fun parseDate_parses_millisecond_timestamp() {
        assertEquals(1577836800000L, requireNotNull(parseDate("1577836800000")).time)
    }

    @Test
    fun parseDate_handles_negative_timestamp() {
        assertEquals(-1000L, requireNotNull(parseDate("-1000")).time)
    }

    @Test
    fun parseDate_parses_iso_format() = assertDate("2020-01-15", 2020, Calendar.JANUARY, 15)

    @Test
    fun parseDate_parses_us_format() = assertDate("Jan 15, 2020", 2020, Calendar.JANUARY, 15)

    @Test
    fun parseDate_parses_feed_format() = assertDate("Tue, 25 Jan 2011", 2011, Calendar.JANUARY, 25)

    @Test
    fun parseDate_parses_single_digit_feed_day() = assertDate("Sun, 3 Aug 2025", 2025, Calendar.AUGUST, 3)

    @Test
    fun parseDate_rejects_impossible_calendar_values() {
        for (input in listOf(
            "Sun, 31 Feb 2025", "2025-02-29", "Feb 29, 2025",
            "2025-13-01", "2025-00-01", "2025-01-00", "Apr 31, 2025",
            "Mon, 3 Aug 2025", "0000-01-01",
        )) assertNull(input, parseDate(input))
    }

    @Test
    fun parseDate_requires_the_entire_input() {
        for (input in listOf(
            "Sun, 3 Aug 2025junk", "2025-08-03junk", "Aug 3, 2025junk",
            "2025-08-03 12:00", "2025-08-03/extra", "1577836800000ms",
            "9223372036854775808", "-9223372036854775809",
        )) assertNull(input, parseDate(input))
    }

    @Test
    fun parseDate_accepts_valid_leap_days_in_all_formats() {
        for (input in listOf("Thu, 29 Feb 2024", "2024-02-29", "Feb 29, 2024")) {
            assertDate(input, 2024, Calendar.FEBRUARY, 29)
        }
    }

    @Test
    fun parseDate_preserves_timestamp_boundaries() {
        for (timestamp in listOf(Long.MIN_VALUE, -1L, 0L, Long.MAX_VALUE)) {
            assertEquals(timestamp, requireNotNull(parseDate(timestamp.toString())).time)
        }
    }

    @Test
    fun formatDate_hides_absent_values() {
        assertEquals("", depollsoft.tagmaster.ui.formatDate(" %tD", null))
    }

    private fun assertDate(
        input: String,
        year: Int,
        month: Int,
        day: Int,
    ) {
        val calendar = Calendar.getInstance().apply { time = requireNotNull(parseDate(input)) }
        assertEquals(year, calendar.get(Calendar.YEAR))
        assertEquals(month, calendar.get(Calendar.MONTH))
        assertEquals(day, calendar.get(Calendar.DAY_OF_MONTH))
    }
}

/** Regression coverage for link dispatch. */
@RunWith(RobolectricTestRunner::class)
@Config(
    application = Application::class,
    manifest = Config.NONE,
    sdk = [28],
)
class CorrectnessInteractionTest {
    @Test
    fun deep_links_route_both_schemes_and_finish() {
        for (scheme in listOf("http", "https")) {
            for (url in listOf(
                "$scheme://tags.depoll.com/tag.php?id=1809",
                "$scheme://www.barbershoptags.com/dbpage.php?pg=view&dbase=tags&id=1809",
                "$scheme://barbershoptags.com/dbpage.php?pg=view&dbase=tags&id=1809",
            )) {
                withLink(url) { activity ->
                    val target =
                        Shadows
                            .shadowOf(activity)
                            .nextStartedActivity
                    assertEquals(TagDetailActivity::class.java.name, target.component?.className)
                    assertEquals(1809, target.getIntExtra(TagDetailActivity.TAG_ID_EXTRA, -1))
                    assertTrue(activity.isFinishing)
                }
            }
        }
    }

    @Test
    fun malformed_links_finish_without_starting_an_activity() {
        for (url in listOf(
            null,
            "mailto:someone@example.com",
            "https://tags.depoll.com/tag.php",
            "https://tags.depoll.com/tag.php?id=oops",
            "https://tags.depoll.com/tag.php?id=-1",
            "https://tags.depoll.com/tag.php?id=0",
            "https://tags.depoll.com/tag.php?id=2147483648",
        )) {
            withLink(url) { activity ->
                assertNull(
                    Shadows
                        .shadowOf(activity)
                        .nextStartedActivity,
                )
                assertTrue(activity.isFinishing)
            }
        }
    }

    @Test
    fun other_web_urls_keep_browser_fallback() {
        val url = "https://www.barbershoptags.com/dbpage.php?pg=other"
        withLink(url) { activity ->
            val target =
                Shadows
                    .shadowOf(activity)
                    .nextStartedActivity
            assertEquals(TagMasterBrowserActivity::class.java.name, target.component?.className)
            assertEquals(url, target.getStringExtra(BrowserActivity.URL_EXTRA))
            assertTrue(activity.isFinishing)
        }
    }

    @Test
    fun shared_browser_predicate_rejects_incomplete_or_untrusted_links_without_throwing() {
        for (url in listOf(null, "", "/dbpage.php?pg=view&dbase=tags&id=1",
            "mailto:someone@example.com", "ftp://barbershoptags.com/dbpage.php?pg=view&dbase=tags&id=1",
            "https://evilbarbershoptags.com/dbpage.php?pg=view&dbase=tags&id=1",
            "https://barbershoptags.com.evil.com/dbpage.php?pg=view&dbase=tags&id=1",
            "https://barbershoptags.com", "https://barbershoptags.com/dbpage.php",
            "https://barbershoptags.com/dbpage.php?dbase=tags&id=1",
            "https://barbershoptags.com/dbpage.php?pg=view&id=1",
            "https://barbershoptags.com/dbpage.php?pg=&dbase=&id=1")) {
            org.junit.Assert.assertFalse(url, UrlHandlerActivity.canHandleUri(url?.let(Uri::parse)))
        }
        for (host in listOf("barbershoptags.com", "www.barbershoptags.com", "tags.depoll.com")) {
            val path = if (host == "tags.depoll.com") "tag.php?" else "dbpage.php?pg=view&dbase=tags&"
            for (scheme in listOf("http", "https")) {
                assertTrue(UrlHandlerActivity.canHandleUri(Uri.parse("$scheme://$host/${path}id=1809")))
                for (id in listOf("", "-1", "0", "2147483648", "99999999999999999999", "abc")) {
                    org.junit.Assert.assertFalse(UrlHandlerActivity.canHandleUri(Uri.parse("$scheme://$host/${path}id=$id")))
                }
            }
        }
    }

    private fun withLink(
        url: String?,
        check: (UrlHandlerActivity) -> Unit,
    ) {
        val intent = Intent(Intent.ACTION_VIEW, url?.let(Uri::parse))
        val controller = Robolectric.buildActivity(UrlHandlerActivity::class.java, intent)
        // Unit tests omit app resources. Stub only AppCompat's window drawable lookup,
        // leaving the real trampoline lifecycle and intent dispatch under test.
        Mockito.mockStatic(AppCompatDrawableManager::class.java).use { drawables ->
            drawables
                .`when`<AppCompatDrawableManager> {
                    AppCompatDrawableManager
                        .get()
                }.thenReturn(Mockito.mock(AppCompatDrawableManager::class.java))
            controller.create()
            try {
                check(controller.get())
            } finally {
                controller.destroy()
            }
        }
    }

}
