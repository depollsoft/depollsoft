package depollsoft.pitchperfect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginPromptTest {
    @Test
    fun providerConfiguration_disablesCrashingCredentialSavePath() {
        assertFalse(LoginPrompt.CREDENTIAL_MANAGER_ENABLED)
        assertTrue(LoginPrompt.ALWAYS_SHOW_PROVIDER_CHOICE)
        assertEquals(
            setOf("password", "google.com", "facebook.com"),
            LoginPrompt.PROVIDER_IDS,
        )
    }
}
