package depollsoft.pitchperfect

import depollsoft.lib.privacy.PrivacyChoices
import depollsoft.lib.privacy.TelemetryConsent
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

import android.content.res.Configuration
import android.os.SystemClock
import androidx.appcompat.app.AppCompatDelegate
import depollsoft.lib.state.StateList
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import depollsoft.lib.activity.RichApplication
import depollsoft.lib.analytics.Analytics
import depollsoft.lib.json.JsonSerializer
import depollsoft.lib.util.Preferences
import depollsoft.pitchperfect.lib.*

class PitchPerfectApplication : RichApplication() {
    override fun onCreate() {
        val startedAt = SystemClock.elapsedRealtime()
        super.onCreate()
        val choices = PrivacyChoices(this)
        TelemetryConsent.applyChoices = { analytics, crashes ->
            val sdk = FirebaseAnalytics.getInstance(this)
            sdk.setConsent(mapOf(
                FirebaseAnalytics.ConsentType.ANALYTICS_STORAGE to if (analytics) FirebaseAnalytics.ConsentStatus.GRANTED else FirebaseAnalytics.ConsentStatus.DENIED,
                FirebaseAnalytics.ConsentType.AD_STORAGE to FirebaseAnalytics.ConsentStatus.DENIED,
                FirebaseAnalytics.ConsentType.AD_USER_DATA to FirebaseAnalytics.ConsentStatus.DENIED,
                FirebaseAnalytics.ConsentType.AD_PERSONALIZATION to FirebaseAnalytics.ConsentStatus.DENIED,
            ))
            sdk.setAnalyticsCollectionEnabled(analytics)
            if (!analytics) sdk.resetAnalyticsData()
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(crashes)
            if (!crashes) FirebaseCrashlytics.getInstance().deleteUnsentReports()
        }
        TelemetryConsent.applyChoices(choices.analytics, choices.crashes)
        PerformanceDiagnostics.startMainThreadMonitor()
        Note.setPlayer(WidgetAwareNotePlayer(Note.DEFAULT_PLAYER) { PitchPipeAppWidget.updateWidgets() })
        registerStorageAliases()
        AppCompatDelegate.setDefaultNightMode(themeMode)
        extraInit()
        PerformanceDiagnostics.logDuration(
            "Application initialized",
            startedAt,
            "authenticated=${Firebase.auth.currentUser != null}",
        )
    }

    private val authAttachment = AuthAttachmentState()

    private fun extraInit() {
        Firebase.auth.addAuthStateListener { auth ->
            val user = auth.currentUser
            if (!authAttachment.transitionTo(user?.uid)) {
                return@addAuthStateListener
            }

            SongsModel.get().detachFromFirestore()
            SettingsModel.detachFromFirestore()
            if (user != null) {
                SongsModel.get().attachToFirestore()
                SettingsModel.attachToFirestore()
            }
        }
        val tags: MutableSet<String> = mutableSetOf()
        if (Firebase.auth.currentUser != null) {
            tags.add("logged_in")
        }
        if (PrivacyChoices(this).analytics) {
            Analytics.default.logEvent(Analytics.APP_OPEN, tags = tags)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Widget cells are pre-rendered bitmaps; redraw them in the new theme.
        PitchPipeAppWidget.updateWidgets()
    }

    override fun onTerminate() {
        super.onTerminate()
    }

    companion object {
        /**
         * The short names stored preferences use for the classes they hold. Changing one would strand
         * every list saved under the old name.
         */
        fun registerStorageAliases() {
            JsonSerializer.registerAlias(java.lang.Integer::class.java, "Integer")
            JsonSerializer.registerAlias(java.lang.Integer.TYPE, "int")
            JsonSerializer.registerAlias(Key::class.java, "Key")
            JsonSerializer.registerAlias(KeyType::class.java, "KeyType")
            JsonSerializer.registerAlias(Accidental::class.java, "Accidental")
            JsonSerializer.registerAlias(Note::class.java, "Note")
            JsonSerializer.registerAlias(PitchedSong::class.java, "PitchedSong")
            JsonSerializer.registerAlias(SongList::class.java, "SongList")
            JsonSerializer.registerAlias(String::class.java, "String")
            JsonSerializer.registerAlias(JsonSerializer.PRIMITIVE_CLASS, "Primitive")
            JsonSerializer.registerAlias(java.lang.Boolean::class.java, "Boolean")
            JsonSerializer.registerAlias(java.lang.Boolean.TYPE, "bool")
            JsonSerializer.registerAlias(java.lang.Double::class.java, "Double")
            JsonSerializer.registerAlias(java.lang.Double.TYPE, "double")
            // Bindroid-era versions stored every list as "List"; StateList takes that name over.
            JsonSerializer.registerAlias(StateList::class.java, "List")
        }

        var themeMode: Int
            get() =
                Preferences.get("pitchperfect.theme")
                    ?: AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            set(value) {
                AppCompatDelegate.setDefaultNightMode(value)
                Preferences.set("pitchperfect.theme", value)
                PitchPipeAppWidget.updateWidgets()
            }
    }
}
