package rahulstech.android.phonebook.concurrent;

import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import rahulstech.android.phonebook.util.Check;

public abstract class AsyncTask<P,R> {

    private static final String TAG = "AsyncTask";

    private final Object lock = new Object();

    private Executor backgroundExecutor = AppExecutors.getBackgroundExecutor();
    private Executor mainExecutor = AppExecutors.getMainExecutor();

    private int version = 0;
    private boolean canceled = false;
    private P parameter = null;

    private WeakReference<AsyncTaskCallback> callbackRef = new WeakReference<>(null);

    public AsyncTask() {}

    public static AsyncTask execute(@NonNull final Runnable execute, AsyncTaskCallback callback) {
        Check.isNonNull(execute,"null == Runnable");
        AsyncTask task = new AsyncTask() {
            @Override
            protected Object onExecuteTask(Object args) throws Exception {
                execute.run();
                return null;
            }
        };
        task.setAsyncTaskCallback(callback);
        task.execute(null);
        return task;
    }

    public static <R> AsyncTask<Object,R> execute(@NonNull final Callable<R> execute, AsyncTaskCallback<R> callback) {
        Check.isNonNull(execute,"null == Callable");
        AsyncTask task = new AsyncTask() {
            @Override
            protected R onExecuteTask(Object args) throws Exception {
                return execute.call();
            }
        };
        task.setAsyncTaskCallback(callback);
        task.execute(null);
        return task;
    }

    public void setAsyncTaskCallback(AsyncTaskCallback<R> callback) {
        this.callbackRef = new WeakReference<>(callback);
    }

    public void cancel() {
        synchronized (lock) {
            this.canceled = true;
        }
    }

    public boolean isCanceled() {
        synchronized (lock) {
            return this.canceled;
        }
    }

    public P getParameter() {
        return parameter;
    }

    public final void execute(P arg, @NonNull Executor executor) {
        if (null == executor) throw new NullPointerException("null == executor");
        this.parameter = arg;
        increaseVersion();
        executor.execute(() -> execute_task(arg));
    }

    public final void execute(P arg) {
        execute(arg,backgroundExecutor);
    }

    public void onError(Throwable error) {}

    public void onResult(@Nullable R result) {}

    public void onCanceled() {}

    protected abstract R onExecuteTask(P args) throws Exception;

    private void execute_task(P arg) {
        try {
            if (isCanceled()) handleCancellation();
            final int version_start = getVersion();
            Log.d(TAG,"start: version="+version_start);
            R result = onExecuteTask(arg);
            final int version_end = getVersion();
            Log.d(TAG,"end: version="+version_end);
            if (version_start == version_end) {
                if (isCanceled()) {
                    handleCancellation();
                }
                else {
                    handleResult(result);
                }
            }
        }
        catch (Exception ex) {
            Log.e(TAG,null,ex);
            handleError(ex);
        }
    }

    private void increaseVersion() {
        synchronized (lock ) {
            this.version++;
        }
    }

    private int getVersion() {
        synchronized (lock) {
            return this.version;
        }
    }

    private void handleError(Throwable error) {
        mainExecutor.execute(() -> {
            onError(error);
            AsyncTaskCallback<R> callback = callbackRef.get();
            if (null != callback) callback.onError(error);
        });
    }

    private void handleResult(R result) {
        mainExecutor.execute(() -> {
            onResult(result);
            AsyncTaskCallback<R> callback = callbackRef.get();
            if (null != callback) callback.onResult(result);
        });
    }

    private void handleCancellation() {
        mainExecutor.execute(() -> {
            onCanceled();
            AsyncTaskCallback<R> callback = callbackRef.get();
            if (null != callback) callback.onCanceled();
        });
    }

    /**
     *
     * @param <R>
     */
    public static class AsyncTaskCallback<R> {

        public void onError(Throwable error) {}

        public void onResult(R result) {}

        public void onCanceled() {}
    }
}
