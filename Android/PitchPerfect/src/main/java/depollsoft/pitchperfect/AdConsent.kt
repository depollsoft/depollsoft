package depollsoft.pitchperfect

import android.app.Activity
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import depollsoft.lib.privacy.TelemetryConsent

object AdConsent {
    private var requestedThisSession = false
    private var gathering = false
    var revision = 0
        private set

    fun canRequestAds(activity: Activity): Boolean =
        requestedThisSession && UserMessagingPlatform.getConsentInformation(activity).canRequestAds()

    fun gather(activity: Activity, completed: () -> Unit) {
        val info = UserMessagingPlatform.getConsentInformation(activity)
        TelemetryConsent.adPrivacyRequired = {
            info.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        }
        TelemetryConsent.showAdPrivacy = { host ->
            UserMessagingPlatform.showPrivacyOptionsForm(host) { error ->
                revision++
                if (error != null) android.widget.Toast.makeText(host,
                    "Ad privacy choices are unavailable. Please try again.", android.widget.Toast.LENGTH_LONG).show()
            }
        }
        if (gathering) return
        if (requestedThisSession) {
            completed()
            return
        }
        gathering = true
        fun finish() {
            gathering = false
            requestedThisSession = true
            revision++
            if (!activity.isDestroyed && !activity.isFinishing) completed()
        }
        info.requestConsentInfoUpdate(activity, ConsentRequestParameters.Builder().build(), {
            if (!activity.isDestroyed && !activity.isFinishing) {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    finish()
                }
            } else finish()
        }, { finish() })
    }
}
