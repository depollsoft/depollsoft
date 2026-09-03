package depollsoft.pitchperfect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthAttachmentStateTest {
    @Test
    fun sameUserWithLiveListener_isAlreadyConnected() {
        val state = AuthAttachmentState()
        state.connect("user-a")

        assertTrue(state.isConnectedTo("user-a", hasLiveListener = true))
        assertFalse(state.isConnectedTo("user-a", hasLiveListener = false))
        assertFalse(state.isConnectedTo("user-b", hasLiveListener = true))
    }

    @Test
    fun transitionTo_onlyChangesForDifferentAuthenticatedUser() {
        val state = AuthAttachmentState()

        assertFalse(state.transitionTo(null))
        assertTrue(state.transitionTo("user-a"))
        assertFalse(state.transitionTo("user-a"))
        assertTrue(state.transitionTo("user-b"))
        assertTrue(state.transitionTo(null))
        assertFalse(state.transitionTo(null))
    }

    @Test
    fun repeatedSameUserCallbacks_requireOneAttachmentTransition() {
        val state = AuthAttachmentState()
        var transitions = 0

        repeat(10_000) {
            if (state.transitionTo("user-a")) transitions++
        }

        assertEquals(1, transitions)
    }

    @Test
    fun clear_forcesNextAttachment() {
        val state = AuthAttachmentState()
        state.connect("user-a")
        state.clear()

        assertFalse(state.isConnectedTo("user-a", hasLiveListener = true))
    }
}
