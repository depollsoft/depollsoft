package depollsoft.tagmaster

import depollsoft.lib.privacy.PrivacyChoices
import depollsoft.lib.privacy.TelemetryConsent
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import depollsoft.lib.activity.RichApplication
import depollsoft.lib.analytics.UsageAnalytics
import depollsoft.lib.json.JsonSerializer
import depollsoft.lib.review.ReviewPrompt
import depollsoft.lib.state.StateList
import depollsoft.lib.util.Preferences

class TagMasterApplication : RichApplication() {
    override fun onCreate() {
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
            if (analytics) UsageAnalytics.signedIn(Firebase.auth.currentUser != null)
        }
        UsageAnalytics.sink = firebaseSink(FirebaseAnalytics.getInstance(this))
        ReviewPrompt.install(this)
        ReviewPrompt.isBusy = { Sounding.any }
        TelemetryConsent.applyChoices(choices.analytics, choices.crashes)
        registerStorageAliases()
        Firebase.auth.addAuthStateListener {
            AuthState.notifyChanged()
            ListModel.connectToFirestore()
            UsageAnalytics.signedIn(it.currentUser != null)
        }
        AppCompatDelegate.setDefaultNightMode(themeMode)
    }

    /** Sends [UsageAnalytics] to Firebase, while the person allows usage analytics. */
    private fun firebaseSink(sdk: FirebaseAnalytics) =
        object : UsageAnalytics.Sink {
            override fun logEvent(
                name: String,
                params: Map<String, String>,
            ) {
                if (!PrivacyChoices(this@TagMasterApplication).analytics) return
                sdk.logEvent(name, Bundle().apply { params.forEach { (key, value) -> putString(key, value) } })
            }

            override fun setUserProperty(
                name: String,
                value: String,
            ) {
                if (!PrivacyChoices(this@TagMasterApplication).analytics) return
                sdk.setUserProperty(name, value)
            }
        }

    companion object {
        /**
         * Lists are stored under the name the original binding library's collection had, so every
         * list saved by an earlier version still loads, and lists saved now still load in one.
         */
        fun registerStorageAliases() {
            JsonSerializer.registerAlias(StateList::class.java, "depollsoft.lib.binding.ObservableCollection")
        }
        public var themeMode: Int
            get() = Preferences.get("tagmaster.theme") ?: AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            set(value) {
                AppCompatDelegate.setDefaultNightMode(value)
                Preferences.set("tagmaster.theme", value)
            }
    }
}
