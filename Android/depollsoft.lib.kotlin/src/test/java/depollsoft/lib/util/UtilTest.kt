package depollsoft.lib.util

import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import depollsoft.lib.activity.RichApplication

@RunWith(RobolectricTestRunner::class)
@Config(application = RichApplication::class)
class UtilTest {

    @Test
    fun initializePreference_initializes_correctly() {
        val key = "init_test_${System.nanoTime()}"
        initializePreference(key, "initial", String::class.java)

        // Should be able to get the initialized value
        val value: String = getPref(key)
        assertEquals("initial", value)
    }

    @Test
    fun getPref_and_setPref_work_together() {
        val key = "get_set_test_${System.nanoTime()}"
        initializePreference(key, "start", String::class.java)

        // Get initial value
        val initial: String = getPref(key)
        assertEquals("start", initial)

        // Set new value
        setPref(key, "changed")

        // Get updated value
        val updated: String = getPref(key)
        assertEquals("changed", updated)
    }

    @Test
    fun getPref_and_setPref_with_different_types() {
        val intKey = "int_test_${System.nanoTime()}"
        val boolKey = "bool_test_${System.nanoTime()}"
        val longKey = "long_test_${System.nanoTime()}"

        initializePreference(intKey, 10, Int::class.java)
        initializePreference(boolKey, false, Boolean::class.java)
        initializePreference(longKey, 100L, Long::class.java)

        // Test Int
        val intVal: Int = getPref(intKey)
        assertEquals(10, intVal)
        setPref(intKey, 20)
        val updatedInt: Int = getPref(intKey)
        assertEquals(20, updatedInt)

        // Test Boolean
        val boolVal: Boolean = getPref(boolKey)
        assertEquals(false, boolVal)
        setPref(boolKey, true)
        val updatedBool: Boolean = getPref(boolKey)
        assertEquals(true, updatedBool)

        // Test Long
        val longVal: Long = getPref(longKey)
        assertEquals(100L, longVal)
        setPref(longKey, 200L)
        val updatedLong: Long = getPref(longKey)
        assertEquals(200L, updatedLong)
    }

    @Test
    fun preference_delegate_reads_and_writes() {
        val key = "delegate_test_${System.nanoTime()}"

        class TestHolder {
            var value by preference(key, "initial")
        }

        val holder = TestHolder()
        assertEquals("initial", holder.value)

        holder.value = "changed"
        assertEquals("changed", holder.value)
    }

    @Test
    fun preference_delegate_with_callback() {
        val key = "callback_test_${System.nanoTime()}"
        var callbackInvoked = false
        var capturedValue: String? = null

        class TestHolder {
            var value by preference(key, "initial") { newValue ->
                callbackInvoked = true
                capturedValue = newValue
            }
        }

        val holder = TestHolder()
        assertFalse(callbackInvoked)

        holder.value = "updated"
        assertTrue(callbackInvoked)
        assertEquals("updated", capturedValue)
        assertEquals("updated", holder.value)
    }

    @Test
    fun preference_delegate_with_int() {
        val key = "int_delegate_test_${System.nanoTime()}"

        class TestHolder {
            var count by preference(key, 0)
        }

        val holder = TestHolder()
        assertEquals(0, holder.count)

        holder.count = 42
        assertEquals(42, holder.count)
    }

    @Test
    fun preference_delegate_with_boolean() {
        val key = "bool_delegate_test_${System.nanoTime()}"

        class TestHolder {
            var flag by preference(key, false)
        }

        val holder = TestHolder()
        assertFalse(holder.flag)

        holder.flag = true
        assertTrue(holder.flag)
    }

    @Test
    fun preference_delegate_with_long() {
        val key = "long_delegate_test_${System.nanoTime()}"

        class TestHolder {
            var timestamp by preference(key, 0L)
        }

        val holder = TestHolder()
        assertEquals(0L, holder.timestamp)

        holder.timestamp = 123456789L
        assertEquals(123456789L, holder.timestamp)
    }

    @Test
    fun preference_delegate_with_nullable_boolean() {
        val key = "nullable_bool_delegate_test_${System.nanoTime()}"

        class TestHolder {
            var value: Boolean? by preference(key, null)
        }

        val holder = TestHolder()
        assertNull(holder.value)

        holder.value = true
        assertEquals(true, holder.value)

        holder.value = null
        assertNull(holder.value)
    }

    @Test
    fun writeThroughPreference_caches_value() {
        val key = "writethrough_test_${System.nanoTime()}"

        class TestHolder {
            var value by writeThroughPreference(key, "initial")
        }

        val holder = TestHolder()

        // First read should hit storage
        val val1 = holder.value
        assertEquals("initial", val1)

        // Subsequent reads should use cached value
        val val2 = holder.value
        val val3 = holder.value
        assertEquals("initial", val2)
        assertEquals("initial", val3)
    }

    @Test
    fun writeThroughPreference_updates_cache_on_write() {
        val key = "writethrough_update_test_${System.nanoTime()}"

        class TestHolder {
            var value by writeThroughPreference(key, "initial")
        }

        val holder = TestHolder()
        assertEquals("initial", holder.value)

        holder.value = "updated"
        assertEquals("updated", holder.value)

        // Should still return cached value on subsequent reads
        assertEquals("updated", holder.value)
    }

    @Test
    fun writeThroughPreference_with_callback() {
        val key = "writethrough_callback_test_${System.nanoTime()}"
        var callbackCount = 0
        var lastValue: String? = null

        class TestHolder {
            var value by writeThroughPreference(key, "initial") { newValue ->
                callbackCount++
                lastValue = newValue
            }
        }

        val holder = TestHolder()
        assertEquals(0, callbackCount)

        holder.value = "first"
        assertEquals(1, callbackCount)
        assertEquals("first", lastValue)

        holder.value = "second"
        assertEquals(2, callbackCount)
        assertEquals("second", lastValue)
    }

    @Test
    fun writeThroughPreference_with_int() {
        val key = "writethrough_int_test_${System.nanoTime()}"

        class TestHolder {
            var count by writeThroughPreference(key, 0)
        }

        val holder = TestHolder()
        assertEquals(0, holder.count)

        // Multiple reads should return cached value
        assertEquals(0, holder.count)
        assertEquals(0, holder.count)

        holder.count = 10
        assertEquals(10, holder.count)
        assertEquals(10, holder.count)
    }

    @Test
    fun writeThroughPreference_lazy_initialization() {
        val key = "writethrough_lazy_test_${System.nanoTime()}"

        class TestHolder {
            var value by writeThroughPreference(key, "initial")
        }

        val holder = TestHolder()

        // First read triggers initialization
        val val1 = holder.value
        assertEquals("initial", val1)

        // Subsequent reads use cache
        val val2 = holder.value
        assertEquals("initial", val2)
    }

    @Test
    fun preference_persists_across_instances() {
        val key = "persist_test_${System.nanoTime()}"

        class TestHolder {
            var value by preference(key, "default")
        }

        val holder1 = TestHolder()
        holder1.value = "persisted"

        // Create new instance - should read persisted value
        val holder2 = TestHolder()
        assertEquals("persisted", holder2.value)
    }

    @Test
    fun writeThroughPreference_persists_across_instances() {
        val key = "writethrough_persist_test_${System.nanoTime()}"

        class TestHolder {
            var value by writeThroughPreference(key, "default")
        }

        val holder1 = TestHolder()
        holder1.value = "persisted"

        // Create new instance - should read persisted value
        val holder2 = TestHolder()
        assertEquals("persisted", holder2.value)
    }
}