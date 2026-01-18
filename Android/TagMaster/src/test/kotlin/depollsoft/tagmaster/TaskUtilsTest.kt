package depollsoft.tagmaster

import bolts.TaskCompletionSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class TaskUtilsTest {

    @Test
    fun await_returns_result_for_completed_task() {
        val tcs = TaskCompletionSource<Int>()
        tcs.setResult(42)

        val result = runBlocking { tcs.task.await() }
        assertEquals(42, result)
    }

    @Test
    fun await_throws_for_faulted_task() {
        val tcs = TaskCompletionSource<Int>()
        val ex = IllegalStateException("boom")
        tcs.setError(ex)

        try {
            runBlocking { tcs.task.await() }
            fail("Expected await() to throw")
        } catch (t: Throwable) {
            // Bolts wraps errors; ensure our original exception is surfaced.
            if (t !== ex && t.cause !== ex) {
                throw t
            }
        }
    }
}
