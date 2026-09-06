package depollsoft.pitchperfect

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.firebase.ui.auth.IdpResponse
import com.firebase.ui.auth.data.model.FlowParameters
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FirebaseUiCompatibilityTest {
    @Test
    fun credentialSaveIntent_acceptsMissingEmailInPackagedApp() {
        val flowParameters =
            FlowParameters(
                "Pitch Perfect",
                emptyList(),
                null,
                R.style.AuthTheme,
                AuthUiNoLogo,
                null,
                null,
                false,
                false,
                true,
                false,
                null,
                null,
                null,
            )
        val response = IdpResponse.from(IllegalStateException("test response"))
        val ownerClass =
            Class.forName("com.firebase.ui.auth.ui.credentials.CredentialSaveActivity")
        val companion = ownerClass.getField("Companion").get(null)
        val method = companion.javaClass.declaredMethods.single { it.name == "createIntent" }

        val intent =
            method.invoke(
                companion,
                ApplicationProvider.getApplicationContext(),
                flowParameters,
                null,
                null,
                response,
            )

        assertNotNull(intent)
    }

    private companion object {
        const val AuthUiNoLogo = -1
    }
}
