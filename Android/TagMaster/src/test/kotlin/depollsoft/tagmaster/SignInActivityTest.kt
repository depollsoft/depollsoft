package depollsoft.tagmaster

import android.app.Application
import depollsoft.testing.SignInHostContract
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class SignInActivityTest : SignInHostContract<SignInActivity>() {
    override val host = SignInActivity::class.java
    override val uploadsSongsForNewAccount = false

    override fun prepareSignedOutApp() {
        ScreenTestSupport.blockNetwork()
        ScreenTestSupport.ensureSyntheticFirebaseApp()
        ScreenTestSupport.ensureFacebookSdk()
    }
}
