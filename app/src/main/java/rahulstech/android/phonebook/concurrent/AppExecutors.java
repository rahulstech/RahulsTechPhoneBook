package rahulstech.android.phonebook.concurrent;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AppExecutors {

    private static class MainExecutor implements Executor {

        final Handler handler;

        MainExecutor() {
            this.handler = new Handler(Looper.getMainLooper());
        }

        @Override
        public void execute(Runnable command) {
            final Handler handler = this.handler;
            handler.post(command);
        }
    }

    private static final Object lock = new Object();

    private static Executor backgroundExecutor = null;

    private static MainExecutor mainExecutor = null;

    public static final Executor getMainExecutor() {
        synchronized (lock) {
            if (null == mainExecutor) {
                mainExecutor = new MainExecutor();
            }
            return mainExecutor;
        }
    }

    public static final Executor getBackgroundExecutor() {
        synchronized (lock) {
            if (null == backgroundExecutor) {
                backgroundExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            }
            return backgroundExecutor;
        }
    }
}
