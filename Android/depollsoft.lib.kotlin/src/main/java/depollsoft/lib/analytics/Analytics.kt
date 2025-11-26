package depollsoft.lib.analytics

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import depollsoft.lib.activity.RichApplication
import depollsoft.lib.toJsonElement
import depollsoft.lib.toJsonString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class Analytics(appContext: Context, val sharedPrefs: String) {
    private val prefs = appContext.getSharedPreferences(sharedPrefs, Context.MODE_PRIVATE)
    private fun startOfHour(date: Date): Date {
        val timestamp = date.time
        val startOfHour = timestamp - timestamp % MILLIS_PER_HOUR
        return Date(startOfHour)
    }

    private fun startOfDay(date: Date): Date {
        val timestamp = date.time
        val startOfDay = timestamp - timestamp % MILLIS_PER_DAY
        return Date(startOfDay)
    }

    private fun updateTags(tags: Set<String>, event: String): Set<String> {
        synchronized(prefs) {
            var newTags = tags.toMutableSet()
            val dailyKey = "${event}.daily"
            val hourlyKey = "${event}.hourly"
            val now = Date()
            val lastDaily = Date(prefs.getLong(dailyKey, 0L))
            val lastHourly = Date(prefs.getLong(hourlyKey, 0L))
            val editorLazy = lazy { prefs.edit() }
            val editor by editorLazy
            if (startOfDay(lastDaily) != startOfDay(now)) {
                newTags.add("daily")
                editor.putLong(dailyKey, now.time)
            }
            if (startOfHour(lastHourly) != startOfHour(now)) {
                newTags.add("hourly")
                editor.putLong(hourlyKey, now.time)
            }
            if (editorLazy.isInitialized()) {
                editor.apply()
            }
            return newTags
        }
    }

    private fun <T> transformDictionary(
        dict: Map<String, T>,
        keyKey: String,
        valueKey: String
    ): List<Map<String, Any?>> =
        dict.map { mapOf(keyKey to it.key, valueKey to it.value) }

    public fun logEvent(
        eventName: String,
        tags: Set<String> = emptySet(),
        fields: Map<String, String> = emptyMap(),
        metrics: Map<String, Double> = emptyMap()
    ) {
        val event = mapOf(
            "tags" to updateTags(tags, eventName),
            "app_id" to appContext.packageName,
            "app_version" to PackageInfoCompat.getLongVersionCode(
                appContext.packageManager.getPackageInfo(appContext.packageName, 0)
            ),
            "event_timestamp" to dateFormat.format(Date()),
            "event_timezone" to TimeZone.getDefault().id,
            "event_name" to eventName,
            "fields" to transformDictionary(fields, "key", "value"),
            "metrics" to transformDictionary(metrics, "name", "value"),
            "device_model" to Build.MODEL,
            "platform" to "android",
            "platform_version" to Build.VERSION.SDK_INT
        )

        CoroutineScope(Dispatchers.IO + Job()).launch {
            val eventString = event.toJsonString()

            val connection = endpoint.openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.addRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.outputStream.bufferedWriter().use {
                    it.write(eventString)
                }
                if (connection.responseCode < 200 || connection.responseCode >= 400) {
                    Log.e(
                        "depollsoft.lib",
                        "Sending analytics failed: ${connection.responseCode} ${connection.responseMessage}"
                    )
                }
            } catch(e: Exception) {
                // Definitely don't fail during analytics collection
            } finally {
                connection.disconnect()
            }
        }
    }

    companion object {
        const val APP_OPEN = "app_open"
        private const val MILLIS_PER_HOUR = 1000 * 60 * 60
        private const val MILLIS_PER_DAY = MILLIS_PER_HOUR * 24
        private val endpoint = URL("https://api.depollsoft.xyz/analytics")
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS", Locale.US)

        lateinit var appContext: Context
        private fun ctx(): Context = if (this::appContext.isInitialized) appContext else RichApplication.getAppContext()

        val default by lazy { Analytics(ctx(), "depollsoft.lib.analytics") }
    }
}