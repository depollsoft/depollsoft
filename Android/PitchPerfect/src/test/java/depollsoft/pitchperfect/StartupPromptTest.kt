package depollsoft.pitchperfect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Which prompt the main screen opens as it starts, and that an open one survives recreation. */
class StartupPromptTest {
    private val asked = mutableListOf<String>()

    private fun due(
        name: String,
        answer: Boolean,
    ): () -> Boolean =
        {
            asked += name
            answer
        }

    @Test
    fun aLoginPromptRestoredAcrossRecreationStaysOpen() {
        // Sign-in was opened from the prompt, then the screen rotated. The changelog is also due
        // this process, but must neither replace the prompt (whose launcher waits for FirebaseUI's
        // result) nor be recorded as shown.
        val prompt = startupPromptFor(StartupPrompt.LOGIN, due("login", false), due("changelog", true))
        assertEquals(StartupPrompt.LOGIN, prompt)
        assertEquals("neither one-time check runs", emptyList<String>(), asked)
    }

    @Test
    fun theLoginPromptComesBeforeTheChangelog() {
        assertEquals(StartupPrompt.LOGIN, startupPromptFor(null, due("login", true), due("changelog", true)))
        assertEquals("the changelog stays due for later", listOf("login"), asked)
    }

    @Test
    fun theChangelogShowsWhenNoLoginIsDue() {
        assertEquals(StartupPrompt.CHANGELOG, startupPromptFor(null, due("login", false), due("changelog", true)))
        assertNull(startupPromptFor(null, { false }, { false }))
    }
}
