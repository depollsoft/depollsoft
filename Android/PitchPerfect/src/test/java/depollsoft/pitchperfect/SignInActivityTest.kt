package depollsoft.pitchperfect

import depollsoft.lib.activity.RichApplication
import depollsoft.testing.SignInHostContract
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = RichApplication::class, sdk = [35])
class SignInActivityTest : SignInHostContract<SignInActivity>() {
    override val host = SignInActivity::class.java
    override val uploadsSongsForNewAccount = true

    override fun prepareSignedOutApp() {
        ScreenTestSupport.startFromFirstLaunch()
        ScreenTestSupport.ensureFirebaseApp()
        ScreenTestSupport.ensureFacebookSdk()
    }
}
