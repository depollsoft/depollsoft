package depollsoft.pitchperfect;

import static org.junit.Assert.*;

import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import bolts.Task;

@RunWith(RobolectricTestRunner.class)
public class TaskUtilsTest {

    static class FakeResult implements Result {
        private final Status status;
        FakeResult(Status status) { this.status = status; }
        @Override public Status getStatus() { return status; }
    }

    static class FakePendingResult extends PendingResult<FakeResult> {
        private ResultCallback<? super FakeResult> callback;
        private boolean canceled;
        void setCanceled(boolean c) { this.canceled = c; }
        void emit(FakeResult r) { if (callback != null) callback.onResult(r); }

        @Override public void cancel() { this.canceled = true; }
        @Override public boolean isCanceled() { return this.canceled; }
        @Override public FakeResult await() { throw new UnsupportedOperationException(); }
        @Override public FakeResult await(long time, TimeUnit units) { throw new UnsupportedOperationException(); }
        @Override public void setResultCallback(ResultCallback<? super FakeResult> callback) { this.callback = callback; }
        @Override public void setResultCallback(ResultCallback<? super FakeResult> callback, long time, TimeUnit units) { this.callback = callback; }
    }

    @Test
    public void fromPendingResult_completes_on_success() throws Exception {
        FakePendingResult pr = new FakePendingResult();
        Task<FakeResult> task = TaskUtils.fromPendingResult(pr);
        pr.emit(new FakeResult(Status.RESULT_SUCCESS));
        task.waitForCompletion();
        FakeResult result = task.getResult();
        assertNotNull(result);
        assertEquals(Status.RESULT_SUCCESS.getStatusCode(), result.getStatus().getStatusCode());
    }

    @Test
    public void fromPendingResult_cancels_when_pendingResult_cancelled() throws Exception {
        FakePendingResult pr = new FakePendingResult();
        Task<FakeResult> task = TaskUtils.fromPendingResult(pr);
        pr.setCanceled(true);
        pr.emit(new FakeResult(Status.RESULT_CANCELED));
        // Bolts Task returns null on waitForCompletion when cancelled
        assertTrue(task.isCancelled());
    }
}
