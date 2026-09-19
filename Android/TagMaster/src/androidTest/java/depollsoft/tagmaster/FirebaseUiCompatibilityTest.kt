package depollsoft.tagmaster

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.firebase.ui.auth.IdpResponse
import com.firebase.ui.auth.data.model.FlowParameters
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies the buildSrc bytecode patch reached the packaged app: FirebaseUI's credential-save
 * intent must accept a null email, or a Facebook account without one crashes the process
 * right after Firebase has signed it in.
 */
@RunWith(AndroidJUnit4::class)
class FirebaseUiCompatibilityTest {
    @Test
    fun credentialSaveIntent_acceptsMissingEmailInPackagedApp() {
        val flowParameters =
            FlowParameters(
                "Tag Master",
                emptyList(),
                null,
                R.style.AppTheme_ActionBar,
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
