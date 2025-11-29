package depollsoft.lib.analytics

import android.content.Context
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AnalyticsTest {
    
    @Test
    fun analytics_app_open_constant() {
        // Test the constant is accessible and has expected value
        assertEquals("app_open", Analytics.APP_OPEN)
    }

    @Test
    fun logEvent_updates_daily_and_hourly_prefs_and_handles_payloads() {
        val context: Context = RuntimeEnvironment.getApplication()
        val prefsName = "analytics_test_prefs"
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        Analytics.staticAppContext = context
        val analytics = Analytics(context, prefsName)

        // First call should set daily/hourly timestamps
        analytics.logEvent(
            eventName = "unit_test_event",
            tags = setOf("t1"),
            fields = mapOf("k" to "v"),
            metrics = mapOf("m" to 1.23)
        )

        Thread.sleep(10) // allow background coroutine to write

        val daily1 = prefs.getLong("unit_test_event.daily", 0L)
        val hourly1 = prefs.getLong("unit_test_event.hourly", 0L)
        assertTrue("daily timestamp should be set", daily1 > 0L)
        assertTrue("hourly timestamp should be set", hourly1 > 0L)

        // Second call within same hour/day should not change stored timestamps
        analytics.logEvent("unit_test_event")
        Thread.sleep(10)
        val daily2 = prefs.getLong("unit_test_event.daily", 0L)
        val hourly2 = prefs.getLong("unit_test_event.hourly", 0L)
        assertEquals(daily1, daily2)
        assertEquals(hourly1, hourly2)
    }
}
