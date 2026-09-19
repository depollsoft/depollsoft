package depollsoft.tagmaster

import com.bindroid.trackable.track
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthStateTest {
    private var signedIn = false

    @After
    fun tearDown() {
        AuthState.setTestSource(null)
    }

    @Test
    fun isSignedIn_reflectsTheCurrentSource() {
        AuthState.setTestSource { signedIn }
        assertEquals(false, AuthState.isSignedIn)

        signedIn = true
        assertEquals(true, AuthState.isSignedIn)
    }

    @Test
    fun notifyChanged_reevaluatesEveryTrackedRead() {
        AuthState.setTestSource { signedIn }
        val observed = mutableListOf<Boolean>()
        track({ AuthState.isSignedIn }) {
            observed += it()
            keepTracking
        }
        assertEquals(listOf(false), observed)

        // A sign-in that Firebase reports through its auth-state listener, with no
        // activity result at all, still repaints anything bound to the auth state.
        signedIn = true
        AuthState.notifyChanged()
        assertEquals(listOf(false, true), observed)

        signedIn = false
        AuthState.notifyChanged()
        assertEquals(listOf(false, true, false), observed)
    }
}
