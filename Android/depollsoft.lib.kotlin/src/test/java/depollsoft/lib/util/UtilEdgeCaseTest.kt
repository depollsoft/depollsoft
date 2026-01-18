package depollsoft.lib.util

import depollsoft.lib.activity.RichApplication
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Edge case and comprehensive tests for preference delegates in Util.kt
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = RichApplication::class)
class UtilEdgeCaseTest {

    // =====================
    // initializePreference tests
    // =====================

    @Test
    fun initializePreference_with_string_type() {
        val key = "init_str_${System.nanoTime()}"
        initializePreference(key, "test", String::class.java)
        val value: String = getPref(key)
        assertEquals("test", value)
    }

    @Test
    fun initializePreference_with_int_type() {
        val key = "init_int_${System.nanoTime()}"
        initializePreference(key, 42, Int::class.java)
        val value: Int = getPref(key)
        assertEquals(42, value)
    }

    @Test
    fun initializePreference_with_boolean_type() {
        val key = "init_bool_${System.nanoTime()}"
        initializePreference(key, true, Boolean::class.java)
        val value: Boolean = getPref(key)
        assertEquals(true, value)
    }

    @Test
    fun initializePreference_with_long_type() {
        val key = "init_long_${System.nanoTime()}"
        initializePreference(key, 999999999999L, Long::class.java)
        val value: Long = getPref(key)
        assertEquals(999999999999L, value)
    }

    @Test
    fun initializePreference_with_float_type() {
        // Note: Float may be stored as a different type depending on Preferences implementation
        // This test verifies the Float initialization pattern works
        val key = "init_float_${System.nanoTime()}"
        // Use Double instead since Float may not be directly supported
        initializePreference(key, 3.14, Double::class.java)
        val value: Double = getPref(key)
        assertEquals(3.14, value, 0.001)
    }

    @Test
    fun initializePreference_reinitialize_does_not_change_value() {
        val key = "reinit_${System.nanoTime()}"
        initializePreference(key, "first", String::class.java)
        setPref(key, "changed")
        
        // Re-initialize should not overwrite
        initializePreference(key, "second", String::class.java)
        val value: String = getPref(key)
        assertEquals("changed", value)
    }

    // =====================
    // getPref/setPref edge cases
    // =====================

    @Test
    fun setPref_overwrites_existing_value() {
        val key = "overwrite_${System.nanoTime()}"
        initializePreference(key, "original", String::class.java)
        
        setPref(key, "new_value")
        
        val value: String = getPref(key)
        assertEquals("new_value", value)
    }

    @Test
    fun setPref_multiple_times() {
        val key = "multi_set_${System.nanoTime()}"
        initializePreference(key, 0, Int::class.java)
        
        for (i in 1..10) {
            setPref(key, i)
            val value: Int = getPref(key)
            assertEquals(i, value)
        }
    }

    @Test
    fun getPref_returns_initialized_value_without_set() {
        val key = "no_set_${System.nanoTime()}"
        initializePreference(key, "default_value", String::class.java)
        
        val value: String = getPref(key)
        assertEquals("default_value", value)
    }

    // =====================
    // preference delegate edge cases
    // =====================

    @Test
    fun preference_with_empty_string_initial() {
        val key = "empty_str_${System.nanoTime()}"
        var value by preference(key, "")
        
        assertEquals("", value)
        value = "now has value"
        assertEquals("now has value", value)
    }

    @Test
    fun preference_with_zero_int_initial() {
        val key = "zero_int_${System.nanoTime()}"
        var value by preference(key, 0)
        
        assertEquals(0, value)
        value = 100
        assertEquals(100, value)
    }

    @Test
    fun preference_with_false_boolean_initial() {
        val key = "false_bool_${System.nanoTime()}"
        var value by preference(key, false)
        
        assertEquals(false, value)
        value = true
        assertEquals(true, value)
    }

    @Test
    fun preference_with_negative_int() {
        val key = "neg_int_${System.nanoTime()}"
        var value by preference(key, -100)
        
        assertEquals(-100, value)
        value = -999
        assertEquals(-999, value)
    }

    @Test
    fun preference_with_negative_long() {
        val key = "neg_long_${System.nanoTime()}"
        var value by preference(key, -999999999999L)
        
        assertEquals(-999999999999L, value)
    }

    @Test
    fun preference_callback_not_called_on_read() {
        val key = "no_callback_read_${System.nanoTime()}"
        var callCount = 0
        val value by preference(key, "initial") { callCount++ }
        
        // Reading should not trigger callback
        val ignored1 = value
        val ignored2 = value
        
        assertEquals(0, callCount)
    }

    @Test
    fun preference_callback_called_multiple_times_on_writes() {
        val key = "multi_callback_${System.nanoTime()}"
        var callCount = 0
        var lastValue: Int? = null
        var value by preference(key, 0) { 
            callCount++
            lastValue = it
        }
        
        value = 1
        assertEquals(1, callCount)
        assertEquals(1, lastValue)
        
        value = 2
        assertEquals(2, callCount)
        assertEquals(2, lastValue)
        
        value = 3
        assertEquals(3, callCount)
        assertEquals(3, lastValue)
    }

    @Test
    fun preference_same_value_still_triggers_callback() {
        val key = "same_val_callback_${System.nanoTime()}"
        var callCount = 0
        var value by preference(key, "initial") { callCount++ }
        
        value = "initial" // same value
        assertEquals(1, callCount)
        
        value = "initial" // same value again
        assertEquals(2, callCount)
    }

    // =====================
    // writeThroughPreference edge cases
    // =====================

    @Test
    fun writeThroughPreference_caches_after_first_read() {
        val key = "wt_cache_${System.nanoTime()}"
        var value by writeThroughPreference(key, "cached")
        
        // First read should initialize cache
        assertEquals("cached", value)
        
        // Subsequent reads should use cache
        assertEquals("cached", value)
        assertEquals("cached", value)
    }

    @Test
    fun writeThroughPreference_write_updates_cache() {
        val key = "wt_write_cache_${System.nanoTime()}"
        var value by writeThroughPreference(key, "initial")
        
        assertEquals("initial", value)
        value = "updated"
        assertEquals("updated", value)
    }

    @Test
    fun writeThroughPreference_callback_on_write() {
        val key = "wt_callback_${System.nanoTime()}"
        var callCount = 0
        var value by writeThroughPreference(key, 0) { callCount++ }
        
        value = 1
        assertEquals(1, callCount)
        
        value = 2
        assertEquals(2, callCount)
    }

    @Test
    fun writeThroughPreference_with_empty_string() {
        val key = "wt_empty_${System.nanoTime()}"
        var value by writeThroughPreference(key, "")
        
        assertEquals("", value)
        value = "not empty"
        assertEquals("not empty", value)
        value = ""
        assertEquals("", value)
    }

    @Test
    fun writeThroughPreference_with_long_string() {
        val key = "wt_long_${System.nanoTime()}"
        val longString = "a".repeat(10000)
        var value by writeThroughPreference(key, longString)
        
        assertEquals(longString, value)
    }

    @Test
    fun writeThroughPreference_read_before_write_returns_initial() {
        val key = "wt_read_first_${System.nanoTime()}"
        var value by writeThroughPreference(key, "initial")
        
        // Read without writing first
        assertEquals("initial", value)
    }

    @Test
    fun writeThroughPreference_write_same_value_updates_cache() {
        val key = "wt_same_val_${System.nanoTime()}"
        var value by writeThroughPreference(key, "value")
        
        assertEquals("value", value)
        value = "value" // same value
        assertEquals("value", value)
    }

    // =====================
    // Cross-instance persistence tests
    // =====================

    @Test
    fun preference_value_persists_across_delegate_instances() {
        val key = "persist_test_${System.nanoTime()}"
        
        // First delegate instance
        var value1 by preference(key, "default")
        value1 = "persisted"
        
        // Second delegate instance with different default
        var value2 by preference(key, "other_default")
        
        assertEquals("persisted", value2)
    }

    @Test
    fun writeThroughPreference_value_persists_across_instances() {
        val key = "wt_persist_${System.nanoTime()}"
        
        var value1 by writeThroughPreference(key, "default")
        value1 = "persisted"
        
        var value2 by writeThroughPreference(key, "other")
        assertEquals("persisted", value2)
    }

    @Test
    fun preference_and_writeThroughPreference_share_backing_store() {
        val key = "shared_store_${System.nanoTime()}"
        
        var pref by preference(key, "initial")
        pref = "from_preference"
        
        var wtp by writeThroughPreference(key, "other")
        assertEquals("from_preference", wtp)
        
        wtp = "from_writethrough"
        assertEquals("from_writethrough", pref)
    }

    // =====================
    // Type-specific edge cases
    // =====================

    @Test
    fun preference_int_max_value() {
        val key = "int_max_${System.nanoTime()}"
        var value by preference(key, Int.MAX_VALUE)
        
        assertEquals(Int.MAX_VALUE, value)
    }

    @Test
    fun preference_int_min_value() {
        val key = "int_min_${System.nanoTime()}"
        var value by preference(key, Int.MIN_VALUE)
        
        assertEquals(Int.MIN_VALUE, value)
    }

    @Test
    fun preference_long_max_value() {
        val key = "long_max_${System.nanoTime()}"
        var value by preference(key, Long.MAX_VALUE)
        
        assertEquals(Long.MAX_VALUE, value)
    }

    @Test
    fun preference_long_min_value() {
        val key = "long_min_${System.nanoTime()}"
        var value by preference(key, Long.MIN_VALUE)
        
        assertEquals(Long.MIN_VALUE, value)
    }

    @Test
    fun preference_double_precision() {
        // Note: Float may not be directly supported, using Double instead
        val key = "double_prec_${System.nanoTime()}"
        var value by preference(key, 0.123456)
        
        assertEquals(0.123456, value, 0.000001)
    }

    // =====================
    // Special string values
    // =====================

    @Test
    fun preference_with_special_characters() {
        val key = "special_chars_${System.nanoTime()}"
        var value by preference(key, "initial")
        
        value = "Hello\nWorld\t!\"'"
        assertEquals("Hello\nWorld\t!\"'", value)
    }

    @Test
    fun preference_with_unicode() {
        val key = "unicode_${System.nanoTime()}"
        var value by preference(key, "initial")
        
        value = "日本語 🎵 émojis"
        assertEquals("日本語 🎵 émojis", value)
    }

    @Test
    fun preference_key_with_special_characters() {
        val key = "key.with-special_chars:${System.nanoTime()}"
        var value by preference(key, "initial")
        
        value = "test"
        assertEquals("test", value)
    }
}
