package depollsoft.lib.util

import depollsoft.lib.activity.RichApplication
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.concurrent.thread

/**
 * Thread safety and concurrency tests for preference delegates
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = RichApplication::class)
class UtilConcurrencyTest {

    @Test
    fun preference_handles_concurrent_writes() {
        val key = "concurrent_write_${System.nanoTime()}"
        var value by preference(key, 0)
        
        val threads = (1..10).map { threadNum ->
            thread {
                repeat(10) { iteration ->
                    value = threadNum * 100 + iteration
                }
            }
        }
        
        threads.forEach { it.join() }
        
        // Value should be some valid number that was written
        val finalValue = value
        assertTrue(finalValue in 0..1009)
    }

    @Test
    fun preference_handles_concurrent_reads() {
        val key = "concurrent_read_${System.nanoTime()}"
        var value by preference(key, "constant")
        
        val results = mutableListOf<String>()
        val threads = (1..10).map {
            thread {
                repeat(10) {
                    synchronized(results) {
                        results.add(value)
                    }
                }
            }
        }
        
        threads.forEach { it.join() }
        
        // All reads should return "constant"
        assertEquals(100, results.size)
        results.forEach { assertEquals("constant", it) }
    }

    @Test
    fun writeThroughPreference_handles_concurrent_writes() {
        val key = "wt_concurrent_${System.nanoTime()}"
        var value by writeThroughPreference(key, 0)
        
        val threads = (1..5).map { threadNum ->
            thread {
                repeat(20) { iteration ->
                    value = threadNum * 1000 + iteration
                }
            }
        }
        
        threads.forEach { it.join() }
        
        // Value should be valid
        val finalValue = value
        assertTrue(finalValue >= 0)
    }

    @Test
    fun preference_read_after_concurrent_writes() {
        val key = "read_after_write_${System.nanoTime()}"
        var value by preference(key, -1)
        
        val writeThread = thread {
            repeat(100) { i ->
                value = i
            }
        }
        
        writeThread.join()
        
        // After all writes complete, should have a valid value
        val finalValue = value
        assertTrue(finalValue in 0..99)
    }

    @Test
    fun multiple_preference_delegates_same_key_concurrent() {
        val key = "multi_delegate_${System.nanoTime()}"
        
        val threads = (1..5).map { threadNum ->
            thread {
                var localValue by preference(key, 0)
                repeat(10) { i ->
                    localValue = threadNum * 100 + i
                    Thread.sleep(1)
                }
            }
        }
        
        threads.forEach { it.join() }
        
        var finalValue by preference(key, -999)
        assertTrue(finalValue != -999) // Should have been updated
    }

    @Test
    fun preference_callback_thread_safety() {
        val key = "callback_thread_${System.nanoTime()}"
        val callbackValues = mutableListOf<Int>()
        
        var value by preference(key, 0) { newValue ->
            synchronized(callbackValues) {
                callbackValues.add(newValue)
            }
        }
        
        val threads = (1..5).map { threadNum ->
            thread {
                repeat(10) { i ->
                    value = threadNum * 100 + i
                }
            }
        }
        
        threads.forEach { it.join() }
        
        // All callbacks should have been invoked
        assertEquals(50, callbackValues.size)
    }

    @Test
    fun writeThroughPreference_cache_consistency() {
        val key = "cache_consistency_${System.nanoTime()}"
        var value by writeThroughPreference(key, "initial")
        
        // Initial read to populate cache
        assertEquals("initial", value)
        
        val writeThread = thread {
            repeat(50) { i ->
                value = "value_$i"
            }
        }
        
        val readResults = mutableListOf<String>()
        val readThread = thread {
            repeat(50) {
                synchronized(readResults) {
                    readResults.add(value)
                }
                Thread.sleep(1)
            }
        }
        
        writeThread.join()
        readThread.join()
        
        // All reads should return valid values
        readResults.forEach { result ->
            assertTrue(result.startsWith("value_") || result == "initial")
        }
    }

    @Test
    fun different_keys_independent_under_concurrency() {
        val key1 = "key1_${System.nanoTime()}"
        val key2 = "key2_${System.nanoTime()}"
        
        var value1 by preference(key1, "a")
        var value2 by preference(key2, "b")
        
        val thread1 = thread {
            repeat(50) { i ->
                value1 = "value1_$i"
            }
        }
        
        val thread2 = thread {
            repeat(50) { i ->
                value2 = "value2_$i"
            }
        }
        
        thread1.join()
        thread2.join()
        
        // Each key should have its own independent value
        assertTrue(value1.startsWith("value1_"))
        assertTrue(value2.startsWith("value2_"))
    }

    @Test
    fun rapid_alternating_reads_writes() {
        val key = "alternating_${System.nanoTime()}"
        var value by preference(key, 0)
        
        val results = mutableListOf<Int>()
        
        repeat(100) { i ->
            value = i
            synchronized(results) {
                results.add(value)
            }
        }
        
        // Each read after write should return what was written
        results.forEachIndexed { index, result ->
            assertTrue(result in 0..index)
        }
    }
}
