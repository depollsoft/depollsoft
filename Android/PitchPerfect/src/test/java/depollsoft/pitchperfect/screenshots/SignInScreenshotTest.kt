package depollsoft.pitchperfect.screenshots

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.firebase.ui.auth.ui.FirebaseAuthTestTags
import depollsoft.lib.activity.RichApplication
import depollsoft.pitchperfect.ScreenTestSupport
import depollsoft.pitchperfect.SignInActivity
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.captureScreen
import depollsoft.pitchperfect.screenshots.ScreenshotSupport.launch
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** FirebaseUI's sign-in screens, hosted on the plate by [SignInActivity]. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = RichApplication::class, qualifiers = ScreenshotSupport.PHONE)
class SignInScreenshotTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Before
    fun setUp() {
        ScreenshotSupport.setUp(compose)
        ScreenTestSupport.ensureFacebookSdk()
    }

    @After
    fun tearDown() = ScreenshotSupport.tearDown()

    @Test
    fun providerChoice() {
        launch(SignInActivity::class.java)
        compose.onNodeWithTag(FirebaseAuthTestTags.MethodPicker.PROVIDER_LIST).assertExists()
        captureScreen("sign_in_providers")
    }

    @Test
    @Config(qualifiers = ScreenshotSupport.PHONE_NIGHT)
    fun providerChoiceNight() {
        launch(SignInActivity::class.java)
        captureScreen("night_sign_in_providers")
    }

    @Test
    fun email() {
        launch(SignInActivity::class.java)
        compose.onNodeWithText("Sign in with email").performClick()
        ScreenshotSupport.settle()
        compose.onNodeWithTag(FirebaseAuthTestTags.SignIn.EMAIL_FIELD).assertExists()
        captureScreen("sign_in_email")
    }
}
