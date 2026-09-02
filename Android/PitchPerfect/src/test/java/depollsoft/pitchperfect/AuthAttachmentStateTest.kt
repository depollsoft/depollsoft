package depollsoft.pitchperfect

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
    fun clear_forcesNextAttachment() {
        val state = AuthAttachmentState()
        state.connect("user-a")
        state.clear()

        assertFalse(state.isConnectedTo("user-a", hasLiveListener = true))
    }
}
