package depollsoft.pitchperfect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LoginPromptTest {
    @Test
    fun providerConfiguration_bypassesFirebaseUiForFacebook() {
        assertFalse(LoginPrompt.CREDENTIAL_MANAGER_ENABLED)
        assertFalse(LoginPrompt.ALWAYS_SHOW_PROVIDER_CHOICE)
        assertEquals(
            setOf("password", "google.com", "facebook.com"),
            LoginPrompt.PROVIDER_IDS,
        )
        assertEquals(
            setOf("password", "google.com"),
            LoginPrompt.FIREBASE_UI_PROVIDER_IDS,
        )
    }
}
