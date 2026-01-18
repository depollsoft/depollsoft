package depollsoft.lib.util

import depollsoft.lib.activity.RichApplication
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = RichApplication::class)
class UtilBehaviorTest {

    @Test
    fun writeThroughPreference_caches_and_writes_backing_store() {
        val key = "wtp_key_${System.nanoTime()}"
        val holder = object {
            var value: String by writeThroughPreference(key, "default")
        }

        // Initial read should return the initial value
        assertEquals("default", holder.value)

        // Write updates memoized value and backing Preferences
        holder.value = "changed"
        assertEquals("changed", holder.value)

        // New delegate reading the same key should see the stored value
        val holder2 = object { var value: String by writeThroughPreference(key, "other") }
        assertEquals("changed", holder2.value)
    }

    @Test
    fun preference_delegate_reads_and_writes_various_types() {
        val intKey = "pref_int_${System.nanoTime()}"
        val boolKey = "pref_bool_${System.nanoTime()}"
        val strKey = "pref_str_${System.nanoTime()}"

        val holder = object {
            var i: Int by preference(intKey, 1)
            var b: Boolean by preference(boolKey, false)
            var s: String by preference(strKey, "a")
        }

        assertEquals(1, holder.i)
        assertEquals(false, holder.b)
        assertEquals("a", holder.s)

        holder.i = 5
        holder.b = true
        holder.s = "z"

        assertEquals(5, holder.i)
        assertEquals(true, holder.b)
        assertEquals("z", holder.s)
    }
}
