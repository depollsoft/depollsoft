package depollsoft.pitchperfect;

import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;

import bolts.Task;

public class TaskUtils {
    public static <T extends Result> Task<T> fromPendingResult(final PendingResult<T> pendingResult) {
        final Task<T>.TaskCompletionSource tcs = Task.create();
        pendingResult.setResultCallback(new ResultCallback<T>() {
            @Override
            public void onResult(T t) {
                if (pendingResult.isCanceled()) {
                    tcs.trySetCancelled();
                } else {
                    tcs.trySetResult(t);
                }
            }
        });
        return tcs.getTask();
    }
}
