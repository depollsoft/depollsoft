package depollsoft.tagmaster

import depollsoft.lib.privacy.PrivacyChoices
import depollsoft.lib.privacy.TelemetryConsent
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import depollsoft.lib.activity.RichApplication
import depollsoft.lib.analytics.Analytics
import depollsoft.lib.json.JsonSerializer
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
        }
        TelemetryConsent.applyChoices(choices.analytics, choices.crashes)
        registerStorageAliases()
        Firebase.auth.addAuthStateListener {
            AuthState.notifyChanged()
            ListModel.connectToFirestore()
        }
        AppCompatDelegate.setDefaultNightMode(themeMode)

        if (choices.analytics) Analytics.default.logEvent(
            Analytics.APP_OPEN,
            tags = setOfNotNull(if (Firebase.auth.currentUser != null) "logged_in" else null),
        )
    }

    companion object {
        private const val DEBUG_SIGNATURE =
            "308201e53082014ea00302010202044f337962300d06092a864886f70d01010505003037310b30090603550406130255533110300e060355040a1307416e64726f6964311630140603550403130d416e64726f6964204465627567301e170d3132303230393037343433345a170d3432303230313037343433345a3037310b30090603550406130255533110300e060355040a1307416e64726f6964311630140603550403130d416e64726f696420446562756730819f300d06092a864886f70d010101050003818d0030818902818100b4d99aa8cff1bddc23c6313aebcfc93a869b8f918807e70e8b6ef03d722813b9090044ba2da31b53ae908393ee5fee0e2f6e582f9018bd6013dfef51bf073e049b42c8ba72387ca82687185603696c31c754669b0b3a84452190e87aac5800d7b07dce4475073f7a24cb0da8313d449d29803d3010296c588f3728033b3ddd1f0203010001300d06092a864886f70d0101050500038181002d0527ee1bb08ae465cc273b1b6fdafa4b1476d587712a8cdd47a7f64f3a790f2f7327c3c656da30af86a4c1febda7258a9226edb429365287c3b489dc74fe75160151543790903e3b5c0faac5bdb70192bbce9d41337426038d0061256b7e59abb9120291865ecc4adbc2b16428b0042120a937f5ef6744b2d4cba08e37b660"
        private const val FACEBOOK_DEBUG = "403828359632347"
        private const val FACEBOOK_PRODUCTION = "311400242255131"
        const val LOG_TAG = "depollsoft.tagmaster"

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
