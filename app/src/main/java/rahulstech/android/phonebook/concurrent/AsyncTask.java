package rahulstech.android.phonebook.concurrent;

import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.concurrent.Executor;

import androidx.annotation.NonNull;

public abstract class AsyncTask<P,R> {

    private static final String TAG = "AsyncTask";

    private final Object lock = new Object();

    private Executor backgroundExecutor = AppExecutors.getBackgroundExecutor();
    private Executor mainExecutor = AppExecutors.getMainExecutor();

    private boolean canceled = false;

    private R result = null;

    private WeakReference<AsyncTaskCallback> callbackRef = new WeakReference<>(null);

    public AsyncTask() {}

    public void setAsyncTaskCallback(AsyncTaskCallback callback) {
        this.callbackRef = new WeakReference<>(callback);
    }

    public void cancel() {
        synchronized (lock) {
            canceled = true;
        }
    }

    public boolean isCanceled() {
        synchronized (lock) {
            return canceled;
        }
    }

    public R getResult() {
        return result;
    }

    public final void execute(P arg, @NonNull Executor executor) {
        if (null == executor) throw new NullPointerException("null == executor");
        executor.execute(() -> execute_task(arg));
    }

    public final void execute(P arg) {
        execute(arg,backgroundExecutor);
    }

    protected abstract R onExecuteTask(P args) throws Exception;

    private void execute_task(P arg) {
        try {
            if (isCanceled()) onCanceled();
            Log.d(TAG,"will execute task");
            R result = onExecuteTask(arg);
            Log.d(TAG,"task execution done");
            if (isCanceled()) onCanceled();
            onResult(result);
        }
        catch (Exception ex) {
            Log.e(TAG,"error on task execution",ex);
            onError(ex);
        }
    }

    private void onError(Throwable error) {
        synchronized (lock) {
            final AsyncTaskCallback<P,R> callback = callbackRef.get();
            if (null != callback) mainExecutor.execute(() -> callback.onError(error));
        }
    }

    private void onResult(R result) {
        synchronized (lock) {
            this.result = result;
            final AsyncTaskCallback<P,R> callback = callbackRef.get();
            if (null != callback) mainExecutor.execute(() -> callback.onResult(result));
        }
    }

    private void onCanceled() {
        synchronized (lock) {
            Log.i(TAG,"task canceled");
            final AsyncTaskCallback<P,R> callback = callbackRef.get();
            if (null != callback) mainExecutor.execute(() -> callback.onCanceled());
        }
    }


    public static class AsyncTaskCallback<P,R> {

        public void onError(Throwable error) {}

        public void onResult(R result) {}

        public void onCanceled() {}
    }
}
