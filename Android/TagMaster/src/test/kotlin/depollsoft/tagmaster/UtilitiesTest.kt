package depollsoft.tagmaster

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatDrawableManager
import depollsoft.lib.activity.BrowserActivity
import depollsoft.pitchperfect.lib.Accidental
import depollsoft.pitchperfect.lib.Note
import depollsoft.pitchperfect.lib.ui.PitchPipeButton
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
import java.time.Duration
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
    fun nullable_date_converter_hides_absent_values() {
        assertEquals("", NullableDateConverter(" %tD").convertToTarget(null, String::class.java))
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

/** Regression coverage for non-touch playback and link dispatch. */
@RunWith(RobolectricTestRunner::class)
@Config(
    application = Application::class,
    manifest = Config.NONE,
    sdk = [28],
)
class CorrectnessInteractionTest {
    @Test
    fun pitch_click_plays_once_and_stops_after_1500ms() {
        withPitchButton { button, note ->
            button.performClick()
            Mockito
                .verify(note)
                .play()
            Shadows
                .shadowOf(Looper.getMainLooper())
                .idleFor(Duration.ofMillis(1499))
            Mockito
                .verify(note, Mockito.never())
                .stop()
            Shadows
                .shadowOf(Looper.getMainLooper())
                .idleFor(Duration.ofMillis(1))
            Mockito
                .verify(note)
                .stop()
        }
    }

    @Test
    fun pitch_touch_release_does_not_start_a_second_note() {
        withPitchButton { button, note ->
            val now = SystemClock.uptimeMillis()
            for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
                val event = MotionEvent.obtain(now, now, action, 1f, 1f, 0)
                button.onTouchEvent(event)
                event.recycle()
            }
            Shadows
                .shadowOf(Looper.getMainLooper())
                .idle()
            Mockito
                .verify(note, Mockito.times(1))
                .play()
            Mockito
                .verify(note, Mockito.times(1))
                .stop()
        }
    }

    @Test
    fun pitch_toggle_accessibility_activates_and_stops_without_a_timer() {
        withPitchButton { button, note ->
            button.setIsToggle(true)
            button.performClick()
            Mockito.verify(note).setIsPlaying(true)
            Mockito.`when`(note.isPlaying).thenReturn(true)
            button.performClick()
            Mockito.verify(note).setIsPlaying(false)
            Shadows
                .shadowOf(Looper.getMainLooper())
                .idleFor(Duration.ofSeconds(2))
            Mockito
                .verify(note, Mockito.never())
                .stop()
        }
    }

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

    @Test
    fun pitch_cancel_stops_hold_without_a_click() {
        withPitchButton { button, note ->
            touch(button, MotionEvent.ACTION_DOWN)
            touch(button, MotionEvent.ACTION_CANCEL)
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
            Mockito.verify(note).play()
            Mockito.verify(note).stop()
        }
    }

    @Test
    fun pitch_replacement_stops_old_hold_and_does_not_stop_replacement_on_release() {
        withPitchButton { button, note ->
            touch(button, MotionEvent.ACTION_DOWN)
            val replacement = Mockito.mock(Note::class.java)
            Mockito.`when`(replacement.accidental).thenReturn(Accidental.Natural)
            button.note = replacement
            touch(button, MotionEvent.ACTION_UP)
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            Mockito.verify(note).stop()
            Mockito.verify(replacement, Mockito.never()).play()
            Mockito.verify(replacement, Mockito.never()).stop()
        }
    }

    @Test
    fun pitch_detach_stops_hold_and_timed_click() {
        for (hold in listOf(true, false)) withPitchButton { button, note ->
            if (hold) touch(button, MotionEvent.ACTION_DOWN) else button.performClick()
            (button.parent as android.view.ViewGroup).removeView(button)
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
            Mockito.verify(note).play()
            Mockito.verify(note).stop()
        }
    }

    @Test
    fun pitch_touch_toggle_retains_press_activation_without_timed_stop() {
        withPitchButton { button, note ->
            button.setIsToggle(true)
            touch(button, MotionEvent.ACTION_DOWN)
            touch(button, MotionEvent.ACTION_UP)
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
            Mockito.verify(note).setIsPlaying(true)
            Mockito.verify(note, Mockito.never()).stop()
        }
    }

    private fun touch(button: PitchPipeButton, action: Int) {
        val now = SystemClock.uptimeMillis()
        MotionEvent.obtain(now, now, action, 1f, 1f, 0).also {
            button.onTouchEvent(it)
            it.recycle()
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

    private fun withPitchButton(check: (PitchPipeButton, Note) -> Unit) {
        val controller =
            Robolectric
                .buildActivity(Activity::class.java)
                .setup()
        try {
            val activity = controller.get()
            val button =
                PitchPipeButton(activity)
            val note = Mockito.mock(Note::class.java)
            Mockito
                .`when`(note.accidental)
                .thenReturn(Accidental.Natural)
            Mockito
                .`when`(note.friendlyName)
                .thenReturn("C")
            button.note = note
            activity.setContentView(button)
            button.layout(0, 0, 100, 100)
            Mockito.clearInvocations(note)
            check(button, note)
        } finally {
            controller.pause().stop().destroy()
        }
    }
}
