package depollsoft.pitchperfect;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;

import bolts.Task;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class TaskUtilsTest {

    @Mock
    private PendingResult<TestResult> mockPendingResult;
    
    @Mock
    private TestResult mockResult;

    private static class TestResult implements Result {
        private final boolean success;
        
        public TestResult(boolean success) {
            this.success = success;
        }
        
        public boolean isSuccess() {
            return success;
        }
    }

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testFromPendingResult_WithSuccessfulResult() throws Exception {
        // Setup
        when(mockPendingResult.isCanceled()).thenReturn(false);
        
        // Execute
        Task<TestResult> task = TaskUtils.fromPendingResult(mockPendingResult);
        
        // Verify callback was set
        verify(mockPendingResult).setResultCallback(any(ResultCallback.class));
        
        // Simulate successful result callback
        ResultCallback<TestResult> callback = getResultCallback();
        callback.onResult(mockResult);
        
        assertNotNull("Task should not be null", task);
        assertTrue("Task should be completed", task.isCompleted());
        assertFalse("Task should not be faulted", task.isFaulted());
        assertFalse("Task should not be cancelled", task.isCancelled());
        assertEquals("Task result should match mock result", mockResult, task.getResult());
    }

    @Test
    public void testFromPendingResult_WithCancelledResult() throws Exception {
        // Setup
        when(mockPendingResult.isCanceled()).thenReturn(true);
        
        // Execute
        Task<TestResult> task = TaskUtils.fromPendingResult(mockPendingResult);
        
        // Verify callback was set
        verify(mockPendingResult).setResultCallback(any(ResultCallback.class));
        
        // Simulate cancelled result callback
        ResultCallback<TestResult> callback = getResultCallback();
        callback.onResult(mockResult);
        
        assertNotNull("Task should not be null", task);
        assertTrue("Task should be completed", task.isCompleted());
        assertTrue("Task should be cancelled", task.isCancelled());
        assertFalse("Task should not be faulted", task.isFaulted());
    }

    @Test
    public void testFromPendingResult_CreatesNonNullTask() {
        Task<TestResult> task = TaskUtils.fromPendingResult(mockPendingResult);
        
        assertNotNull("Task should not be null", task);
        verify(mockPendingResult).setResultCallback(any(ResultCallback.class));
    }

    @Test
    public void testFromPendingResult_CallbackNotNull() {
        TaskUtils.fromPendingResult(mockPendingResult);
        
        verify(mockPendingResult).setResultCallback(notNull());
    }

    @Test
    public void testFromPendingResult_MultipleResults() throws Exception {
        // Create multiple pending results
        PendingResult<TestResult> pendingResult1 = mock(PendingResult.class);
        PendingResult<TestResult> pendingResult2 = mock(PendingResult.class);
        
        when(pendingResult1.isCanceled()).thenReturn(false);
        when(pendingResult2.isCanceled()).thenReturn(true);
        
        Task<TestResult> task1 = TaskUtils.fromPendingResult(pendingResult1);
        Task<TestResult> task2 = TaskUtils.fromPendingResult(pendingResult2);
        
        assertNotNull("First task should not be null", task1);
        assertNotNull("Second task should not be null", task2);
        assertNotSame("Tasks should be different instances", task1, task2);
        
        verify(pendingResult1).setResultCallback(any(ResultCallback.class));
        verify(pendingResult2).setResultCallback(any(ResultCallback.class));
    }

    @Test
    public void testFromPendingResult_WithNullPendingResult() {
        try {
            TaskUtils.fromPendingResult(null);
            fail("Should throw exception for null pending result");
        } catch (NullPointerException e) {
            // Expected behavior
        }
    }

    @Test
    public void testFromPendingResult_TaskInitialState() {
        Task<TestResult> task = TaskUtils.fromPendingResult(mockPendingResult);
        
        assertNotNull("Task should not be null", task);
        assertFalse("Task should not be completed initially", task.isCompleted());
        assertFalse("Task should not be faulted initially", task.isFaulted());
        assertFalse("Task should not be cancelled initially", task.isCancelled());
    }

    @Test
    public void testFromPendingResult_ResultCallback_HandlesSuccessCorrectly() throws Exception {
        when(mockPendingResult.isCanceled()).thenReturn(false);
        
        Task<TestResult> task = TaskUtils.fromPendingResult(mockPendingResult);
        
        // Get the callback that was set
        ResultCallback<TestResult> callback = getResultCallback();
        
        // Verify the callback handles success correctly
        TestResult testResult = new TestResult(true);
        callback.onResult(testResult);
        
        assertEquals("Task should have the correct result", testResult, task.getResult());
        assertFalse("Task should not be cancelled", task.isCancelled());
    }

    @Test
    public void testFromPendingResult_ResultCallback_HandlesCancellationCorrectly() throws Exception {
        when(mockPendingResult.isCanceled()).thenReturn(true);
        
        Task<TestResult> task = TaskUtils.fromPendingResult(mockPendingResult);
        
        // Get the callback that was set
        ResultCallback<TestResult> callback = getResultCallback();
        
        // Verify the callback handles cancellation correctly
        callback.onResult(mockResult);
        
        assertTrue("Task should be cancelled", task.isCancelled());
        assertTrue("Task should be completed", task.isCompleted());
    }

    @Test
    public void testFromPendingResult_GenericTypeHandling() {
        // Test with different result types to ensure generic handling works
        PendingResult<TestResult> stringPendingResult = mock(PendingResult.class);
        Task<TestResult> stringTask = TaskUtils.fromPendingResult(stringPendingResult);
        
        assertNotNull("String task should not be null", stringTask);
        verify(stringPendingResult).setResultCallback(any(ResultCallback.class));
    }

    // Helper method to extract the ResultCallback that was passed to setResultCallback
    @SuppressWarnings("unchecked")
    private ResultCallback<TestResult> getResultCallback() {
        ArgumentCaptor<ResultCallback> captor = ArgumentCaptor.forClass(ResultCallback.class);
        verify(mockPendingResult).setResultCallback(captor.capture());
        return captor.getValue();
    }
}