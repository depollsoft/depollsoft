package depollsoft.lib.util

import depollsoft.lib.activity.RichApplication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = RichApplication::class)
class UtilMoreTest {

    @Test
    fun preference_afterSet_is_invoked() {
        val key = "pref_after_${System.nanoTime()}"
        var called = false
        val holder = object {
            var v: String by preference(key, "a") { called = true }
        }
        holder.v = "b"
        assertTrue(called)
        assertEquals("b", holder.v)
    }

    @Test
    fun writeThroughPreference_supports_multiple_types() {
        val intKey = "wt_int_${System.nanoTime()}"
        val boolKey = "wt_bool_${System.nanoTime()}"

        val holder = object {
            var i: Int by writeThroughPreference(intKey, 10)
            var b: Boolean by writeThroughPreference(boolKey, false)
        }

        assertEquals(10, holder.i)
        assertEquals(false, holder.b)
        holder.i = 11
        holder.b = true
        assertEquals(11, holder.i)
        assertEquals(true, holder.b)
    }
}
