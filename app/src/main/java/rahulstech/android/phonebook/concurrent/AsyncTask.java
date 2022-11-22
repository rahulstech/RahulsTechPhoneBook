package rahulstech.android.phonebook.concurrent;

import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;
import androidx.annotation.NonNull;
import rahulstech.android.phonebook.util.Check;

public final class AsyncTask {

    private static final String TAG = "AsyncTask";

    private Throwable error = null;
    private Object result = null;

    private AsyncTask(Callable executable, AsyncTaskCallback cb) {
        AppExecutors.getBackgroundExecutor().execute(()->{
            final WeakReference<AsyncTaskCallback> callbackRef = new WeakReference<>(cb);
            try {
                Object result = executable.call();
                setResult(result);
            }
            catch (Exception ex) {
                setError(ex);
            }
            finally {
                AsyncTaskCallback _callback = callbackRef.get();
                if (null != _callback) {
                    AppExecutors.getMainExecutor().execute(()->{
                        if (null != error) _callback.onError(AsyncTask.this);
                        else _callback.onResult(AsyncTask.this);
                    });
                }
            }
        });
    }

    public static AsyncTask execute(@NonNull final Runnable execute, AsyncTaskCallback callback) {
        Check.isNonNull(execute,"null == Runnable");
        return new AsyncTask(()->{
            execute.run();
            return null;
        },callback);
    }

    public static AsyncTask execute(@NonNull final Callable execute, AsyncTaskCallback callback) {
        Check.isNonNull(execute,"null == Callable");
        return new AsyncTask(execute,callback);
    }

    public void setError(Throwable error) {
        this.error = error;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public Throwable getError() {
        return error;
    }

    @SuppressWarnings("unchecked")
    public <R> R getResult() {
        return (R) result;
    }

    public static class AsyncTaskCallback {

        public void onError(AsyncTask task) {}

        public void onResult(AsyncTask task) {}
    }
}
