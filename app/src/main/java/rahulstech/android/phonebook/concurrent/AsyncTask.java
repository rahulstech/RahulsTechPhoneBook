package rahulstech.android.phonebook.concurrent;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class AsyncTask {

    private final Object lock = new Object();

    private Executor backgroundExecutor = AppExecutors.getBackgroundExecutor();
    private Executor mainExecutor = AppExecutors.getMainExecutor();

    private Lock mTaskLock;
    private Condition mTaskCond;
    private Queue<Task> mTasks;

    private boolean shutdown = false;

    private WeakReference<AsyncTaskCallback> callbackRef = new WeakReference<>(null);

    public AsyncTask() {
        mTaskLock = new ReentrantLock();
        mTaskCond = mTaskLock.newCondition();
        mTasks = new ArrayDeque<>();
        backgroundExecutor.execute(new ExecuteTask(this));
    }

    public void setAsyncTaskCallback(AsyncTaskCallback callback) {
        this.callbackRef = new WeakReference<>(callback);
    }

    public void shutdown() {
        synchronized (lock) {
            shutdown = true;
            cancelAllTasks();
            mTaskCond.signal();
            onShutdown();
        }
    }

    public boolean isShutdown() {
        synchronized (lock) {
            return shutdown;
        }
    }

    public void enqueue(Task task) {
        if (isShutdown()) throw new AsyncTaskException("async task is shutdown");
        mTaskLock.lock();
        try {
            mTasks.add(task);
            mTaskCond.signalAll();
        }
        finally {
            mTaskLock.unlock();
        }
    }

    public void cancelAllTasks() {
        for (Task t : mTasks) {
            t.cancel();
        }
    }

    protected void onShutdown() {
        synchronized (lock) {
            final AsyncTaskCallback callback = this.callbackRef.get();
            if (null != callback) mainExecutor.execute(() -> callback.onShutdown(this, mTasks));
        }
    }

    protected void onTaskError(Task task, Throwable error) {
        synchronized (lock) {
            final AsyncTaskCallback callback = this.callbackRef.get();
            if (null != callback) mainExecutor.execute(() -> callback.onError(this,task,error));
        }
    }

    protected void onTaskComplete(Task task) {
        synchronized (lock) {
            final AsyncTaskCallback callback = this.callbackRef.get();
            if (null != callback) mainExecutor.execute(() -> callback.onResult(this,task));
        }
    }

    protected void onTaskCanceled(Task task) {
        synchronized (lock) {
            final AsyncTaskCallback callback = this.callbackRef.get();
            if (null != callback) mainExecutor.execute(() -> callback.onCanceled(this,task));
        }
    }

    public static abstract class Task {

        private final Object lock = new Object();

        private final int taskId;
        private boolean canceled = false;
        private Object result = null;

        protected Task(int taskId) {
            this.taskId = taskId;
        }

        public final <R> void setResult(R result) {
            synchronized (lock) {
                this.result = result;
            }
        }

        @SuppressWarnings("unchecked")
        public final <R> R getResult() {
            synchronized (lock) {
                return (R) result;
            }
        }

        public final void cancel() {
            synchronized (lock) {
                this.canceled = canceled;
            }
        }

        public final boolean isCanceled() {
            synchronized (lock) {
                return this.canceled;
            }
        }

        public int getTaskId() {
            return taskId;
        }

        public abstract void execute();
    }

    public interface AsyncTaskCallback {

        void onError(AsyncTask asyncTask, Task task, Throwable error);

        void onResult(AsyncTask asyncTask, Task task);

        void onCanceled(AsyncTask asyncTask, Task task);

        void onShutdown(AsyncTask asyncTask, Queue<Task> notExecutedTasks);
    }

    private static class ExecuteTask implements Runnable {

        final AsyncTask asyncTask;

        public ExecuteTask(AsyncTask asyncTask) {
            this.asyncTask = asyncTask;
        }

        @Override
        public void run() {
            Lock lock = asyncTask.mTaskLock;
            Condition cond = asyncTask.mTaskCond;
            Queue<Task> mTasks = asyncTask.mTasks;
            Task running = null;
            while (true) {
                lock.lock();
                try {
                    cond.await();
                    if (asyncTask.isShutdown()) {
                        if (null != running) running.cancel();
                        break;
                    }
                    running = mTasks.poll();
                    try {
                        if (running.isCanceled()) throw new AsyncTaskException("trying to execute canceled task");
                        running.execute();
                        if (running.isCanceled())
                            asyncTask.onTaskCanceled(running);
                        else if (!asyncTask.isShutdown())
                            asyncTask.onTaskComplete(running);
                    }
                    catch (Throwable taskError) {
                        asyncTask.onTaskError(running,taskError);
                    }
                } catch (InterruptedException e) {
                    asyncTask.shutdown();
                    break;
                }
                finally {
                    lock.unlock();
                }
            }
        }
    }
}
