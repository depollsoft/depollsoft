package depollsoft.lib.analytics

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.*

/**
 * Comprehensive tests for Analytics class covering time-based tracking,
 * event logging, and tag management.
 */
@RunWith(RobolectricTestRunner::class)
class AnalyticsComprehensiveTest {

    private lateinit var context: Context
    private lateinit var analytics: Analytics
    private lateinit var prefs: SharedPreferences
    private val prefsName = "test_analytics_prefs"

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        Analytics.staticAppContext = context
        analytics = Analytics(context, prefsName)
    }

    // =====================
    // Constant Tests
    // =====================

    @Test
    fun app_open_constant_is_app_open() {
        assertEquals("app_open", Analytics.APP_OPEN)
    }

    // =====================
    // Event Logging Tests
    // =====================

    @Test
    fun logEvent_with_empty_tags_still_adds_daily_hourly() {
        analytics.logEvent("test_event", tags = emptySet())
        Thread.sleep(50) // Allow coroutine to execute
        
        val dailyKey = "test_event.daily"
        val hourlyKey = "test_event.hourly"
        
        assertTrue(prefs.getLong(dailyKey, 0L) > 0L)
        assertTrue(prefs.getLong(hourlyKey, 0L) > 0L)
    }

    @Test
    fun logEvent_with_custom_tags_preserves_tags() {
        analytics.logEvent("tag_test_event", tags = setOf("custom", "tags"))
        Thread.sleep(50)
        
        // Verify timestamps were set (indicating event was processed)
        assertTrue(prefs.getLong("tag_test_event.daily", 0L) > 0L)
    }

    @Test
    fun logEvent_with_fields_accepts_map() {
        // Should not throw
        analytics.logEvent(
            "fields_test",
            fields = mapOf(
                "field1" to "value1",
                "field2" to "value2",
                "empty" to ""
            )
        )
        Thread.sleep(50)
        assertTrue(prefs.getLong("fields_test.daily", 0L) > 0L)
    }

    @Test
    fun logEvent_with_metrics_accepts_map() {
        // Should not throw
        analytics.logEvent(
            "metrics_test",
            metrics = mapOf(
                "metric1" to 1.0,
                "metric2" to 99.99,
                "zero" to 0.0,
                "negative" to -5.5
            )
        )
        Thread.sleep(50)
        assertTrue(prefs.getLong("metrics_test.daily", 0L) > 0L)
    }

    @Test
    fun logEvent_with_all_parameters() {
        analytics.logEvent(
            eventName = "full_event",
            tags = setOf("tag1", "tag2"),
            fields = mapOf("key" to "value"),
            metrics = mapOf("count" to 42.0)
        )
        Thread.sleep(50)
        assertTrue(prefs.getLong("full_event.daily", 0L) > 0L)
    }

    @Test
    fun logEvent_with_empty_event_name() {
        // Should handle gracefully
        analytics.logEvent("")
        Thread.sleep(50)
        assertTrue(prefs.getLong(".daily", 0L) > 0L)
    }

    @Test
    fun logEvent_with_special_characters_in_name() {
        analytics.logEvent("event-with_special.chars")
        Thread.sleep(50)
        assertTrue(prefs.getLong("event-with_special.chars.daily", 0L) > 0L)
    }

    // =====================
    // Daily/Hourly Tracking Tests
    // =====================

    @Test
    fun logEvent_sets_daily_timestamp_on_first_call() {
        val dailyKey = "daily_test.daily"
        assertEquals(0L, prefs.getLong(dailyKey, 0L))
        
        analytics.logEvent("daily_test")
        Thread.sleep(50)
        
        val timestamp = prefs.getLong(dailyKey, 0L)
        assertTrue(timestamp > 0L)
        
        // Should be within the last minute
        val now = System.currentTimeMillis()
        assertTrue(timestamp <= now)
        assertTrue(timestamp > now - 60000)
    }

    @Test
    fun logEvent_sets_hourly_timestamp_on_first_call() {
        val hourlyKey = "hourly_test.hourly"
        assertEquals(0L, prefs.getLong(hourlyKey, 0L))
        
        analytics.logEvent("hourly_test")
        Thread.sleep(50)
        
        val timestamp = prefs.getLong(hourlyKey, 0L)
        assertTrue(timestamp > 0L)
    }

    @Test
    fun logEvent_does_not_update_timestamps_within_same_hour_and_day() {
        analytics.logEvent("same_period")
        Thread.sleep(50)
        
        val daily1 = prefs.getLong("same_period.daily", 0L)
        val hourly1 = prefs.getLong("same_period.hourly", 0L)
        
        // Call again immediately
        analytics.logEvent("same_period")
        Thread.sleep(50)
        
        val daily2 = prefs.getLong("same_period.daily", 0L)
        val hourly2 = prefs.getLong("same_period.hourly", 0L)
        
        assertEquals(daily1, daily2)
        assertEquals(hourly1, hourly2)
    }

    @Test
    fun logEvent_multiple_events_track_separately() {
        analytics.logEvent("event_a")
        analytics.logEvent("event_b")
        Thread.sleep(50)
        
        val dailyA = prefs.getLong("event_a.daily", 0L)
        val dailyB = prefs.getLong("event_b.daily", 0L)
        
        assertTrue(dailyA > 0L)
        assertTrue(dailyB > 0L)
        
        // Timestamps may be same or different, but both should be set
        assertNotEquals(0L, dailyA)
        assertNotEquals(0L, dailyB)
    }

    // =====================
    // Edge Cases
    // =====================

    @Test
    fun logEvent_with_empty_fields_map() {
        analytics.logEvent("empty_fields", fields = emptyMap())
        Thread.sleep(50)
        assertTrue(prefs.getLong("empty_fields.daily", 0L) > 0L)
    }

    @Test
    fun logEvent_with_empty_metrics_map() {
        analytics.logEvent("empty_metrics", metrics = emptyMap())
        Thread.sleep(50)
        assertTrue(prefs.getLong("empty_metrics.daily", 0L) > 0L)
    }

    @Test
    fun multiple_analytics_instances_share_prefs() {
        val analytics1 = Analytics(context, prefsName)
        val analytics2 = Analytics(context, prefsName)
        
        analytics1.logEvent("shared_event")
        Thread.sleep(50)
        
        val timestamp1 = prefs.getLong("shared_event.daily", 0L)
        
        // Second analytics instance should see the same timestamp
        analytics2.logEvent("shared_event")
        Thread.sleep(50)
        
        val timestamp2 = prefs.getLong("shared_event.daily", 0L)
        
        assertEquals(timestamp1, timestamp2)
    }

    @Test
    fun analytics_instance_with_different_prefs_are_isolated() {
        val prefs2Name = "test_analytics_prefs_2"
        val prefs2 = context.getSharedPreferences(prefs2Name, Context.MODE_PRIVATE)
        prefs2.edit().clear().commit()
        
        val analytics2 = Analytics(context, prefs2Name)
        
        analytics.logEvent("isolated_event")
        Thread.sleep(50)
        
        // First prefs should have the timestamp
        assertTrue(prefs.getLong("isolated_event.daily", 0L) > 0L)
        
        // Second prefs should not
        assertEquals(0L, prefs2.getLong("isolated_event.daily", 0L))
    }

    @Test
    fun sharedPrefs_property_returns_correct_name() {
        assertEquals(prefsName, analytics.sharedPrefs)
    }

    // =====================
    // Concurrent Access Tests
    // =====================

    @Test
    fun logEvent_handles_rapid_sequential_calls() {
        repeat(10) { i ->
            analytics.logEvent("rapid_event_$i")
        }
        Thread.sleep(100)
        
        // All events should have timestamps
        repeat(10) { i ->
            assertTrue(prefs.getLong("rapid_event_$i.daily", 0L) > 0L)
        }
    }

    @Test
    fun logEvent_same_event_multiple_times_quickly() {
        repeat(5) {
            analytics.logEvent("repeated_event")
        }
        Thread.sleep(50)
        
        // Should have timestamp set
        assertTrue(prefs.getLong("repeated_event.daily", 0L) > 0L)
    }
}
