package depollsoft.lib.review

import android.content.SharedPreferences
import java.util.TimeZone

/**
 * When an app may ask the store for a review. iOS applies the same rules (iOS/shared/ReviewPrompt.swift);
 * docs/analytics.md explains them.
 *
 * The counts stay on the device and are never sent anywhere. An app records [recordUse] each time
 * someone does the app's main job (plays a pitch, opens a tag) and [recordSound] each time it makes a
 * sound for them (a pitch, a learning track). Asking is deliberately rare: someone has to have used the
 * app on [MIN_ACTIVE_DAYS] different days across at least [MIN_DAYS_SINCE_FIRST_USE] days, and after an
 * ask the app waits [MIN_DAYS_BETWEEN_ASKS] days and a new version, and the active days start over.
 * Nothing is asked within [QUIET_MILLIS_AFTER_SOUND] of a sound, because someone may be rehearsing or
 * singing with others. The store may still decide not to show anything.
 */
class ReviewPolicy(
    private val prefs: SharedPreferences,
    private val now: () -> Long = System::currentTimeMillis,
    private val timeZone: () -> TimeZone = TimeZone::getDefault,
) {
    /** Today's date in the device's time zone, as days since 1970-01-01. */
    private fun today(): Long {
        val millis = now()
        return (millis + timeZone().getOffset(millis)) / MILLIS_PER_DAY
    }

    private fun long(key: String): Long? = if (prefs.contains(key)) prefs.getLong(key, 0) else null

    val activeDays: Int get() = prefs.getInt(ACTIVE_DAYS, 0)

    fun recordUse() {
        val today = today()
        val editor = prefs.edit()
        if (long(FIRST_USE_DAY) == null) editor.putLong(FIRST_USE_DAY, today)
        if (long(LAST_ACTIVE_DAY) != today) {
            editor.putLong(LAST_ACTIVE_DAY, today)
            editor.putInt(ACTIVE_DAYS, activeDays + 1)
        }
        editor.apply()
    }

    fun recordSound() {
        prefs.edit().putLong(LAST_SOUND_AT, now()).apply()
    }

    fun shouldAsk(version: String): Boolean {
        val today = today()
        val firstUse = long(FIRST_USE_DAY) ?: return false
        if (today - firstUse < MIN_DAYS_SINCE_FIRST_USE) return false
        if (activeDays < MIN_ACTIVE_DAYS) return false
        if (prefs.getString(LAST_ASK_VERSION, null) == version) return false
        val lastAsk = long(LAST_ASK_DAY)
        if (lastAsk != null && today - lastAsk < MIN_DAYS_BETWEEN_ASKS) return false
        val lastSound = long(LAST_SOUND_AT) ?: return true
        // A clock set back leaves the sound ahead of now: recent until the clock catches up, or the
        // next sound records the time afresh.
        return now() - lastSound >= QUIET_MILLIS_AFTER_SOUND
    }

    /** Records an ask; the next one needs a new version, the wait, and fresh active days. */
    fun recordAsk(version: String) {
        prefs.edit()
            .putLong(LAST_ASK_DAY, today())
            .putString(LAST_ASK_VERSION, version)
            .putInt(ACTIVE_DAYS, 0)
            .remove(LAST_ACTIVE_DAY)
            .apply()
    }

    companion object {
        const val MIN_ACTIVE_DAYS = 5
        const val MIN_DAYS_SINCE_FIRST_USE = 14
        const val MIN_DAYS_BETWEEN_ASKS = 180
        const val QUIET_MILLIS_AFTER_SOUND = 5L * 60 * 1000

        const val PREFS_NAME = "review_prompt"
        private const val FIRST_USE_DAY = "first_use_day"
        private const val LAST_ACTIVE_DAY = "last_active_day"
        private const val ACTIVE_DAYS = "active_days"
        private const val LAST_ASK_DAY = "last_ask_day"
        private const val LAST_ASK_VERSION = "last_ask_version"
        private const val LAST_SOUND_AT = "last_sound_at"
        private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
    }
}
