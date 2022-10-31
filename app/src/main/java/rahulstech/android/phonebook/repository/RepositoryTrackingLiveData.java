package rahulstech.android.phonebook.repository;


import android.annotation.SuppressLint;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;
import rahulstech.android.phonebook.concurrent.AppExecutors;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A LiveData implementation that closely works with {@link InvalidationTracker} to implement
 * database drive {@link androidx.lifecycle.LiveData} queries that are strongly hold as long
 * as they are active.
 * <p>
 * We need this extra handling for {@link androidx.lifecycle.LiveData} because when they are
 * observed forever, there is no {@link androidx.lifecycle.Lifecycle} that will keep them in
 * memory but they should stay. We cannot add-remove observer in {@link LiveData#onActive()},
 * {@link LiveData#onInactive()} because that would mean missing changes in between or doing an
 * extra query on every UI rotation.
 * <p>
 * This {@link LiveData} keeps a weak observer to the {@link InvalidationTracker} but it is hold
 * strongly by the {@link InvalidationTracker} as long as it is active.
 */
class RepositoryTrackingLiveData<T> extends LiveData<T> {
    @SuppressWarnings("WeakerAccess")
    final ContactRepository repository;

    @SuppressWarnings("WeakerAccess")
    final Callable<T> mComputeFunction;

    private final InvalidationLiveDataContainer mContainer;

    @SuppressWarnings("WeakerAccess")
    final InvalidationTracker.Observer mObserver;

    @SuppressWarnings("WeakerAccess")
    final AtomicBoolean mInvalid = new AtomicBoolean(true);

    @SuppressWarnings("WeakerAccess")
    final AtomicBoolean mComputing = new AtomicBoolean(false);

    @SuppressWarnings("WeakerAccess")
    final AtomicBoolean mRegisteredObserver = new AtomicBoolean(false);

    @SuppressWarnings("WeakerAccess")
    final Runnable mRefreshRunnable = new Runnable() {
        @WorkerThread
        @Override
        public void run() {
            if (mRegisteredObserver.compareAndSet(false, true)) {
                repository.getInvalidationTracker().addWeakObserver(mObserver);
            }
            boolean computed;
            do {
                computed = false;
                // compute can happen only in 1 thread but no reason to lock others.
                if (mComputing.compareAndSet(false, true)) {
                    // as long as it is invalid, keep computing.
                    try {
                        T value = null;
                        while (mInvalid.compareAndSet(true, false)) {
                            computed = true;
                            try {
                                value = mComputeFunction.call();
                            } catch (Exception e) {
                                throw new RuntimeException("Exception while computing live data.", e);
                            }
                        }
                        if (computed) {
                            postValue(value);
                        }
                    } finally {
                        // release compute lock
                        mComputing.set(false);
                    }
                }
                // check invalid after releasing compute lock to avoid the following scenario.
                // Thread A runs compute()
                // Thread A checks invalid, it is false
                // Main thread sets invalid to true
                // Thread B runs, fails to acquire compute lock and skips
                // Thread A releases compute lock
                // We've left invalid in set state. The check below recovers.
            } while (computed && mInvalid.get());
        }
    };

    @SuppressWarnings("WeakerAccess")
    final Runnable mInvalidationRunnable = () -> {
        boolean isActive = hasActiveObservers();
        if (mInvalid.compareAndSet(false, true)) {
            if (isActive) {
                getQueryExecutor().execute(mRefreshRunnable);
            }
        }
    };

    @SuppressLint("RestrictedApi")
    RepositoryTrackingLiveData(
            ContactRepository repository,
            InvalidationLiveDataContainer container,
            Callable<T> computeFunction,
            Uri[] uris) {
        this.repository = repository;
        mComputeFunction = computeFunction;
        mContainer = container;
        mObserver = new InvalidationTracker.Observer(uris) {
            @Override
            public void onInvalidated(@NonNull Set<Uri> uris) {
                AppExecutors.getMainExecutor().execute(mInvalidationRunnable);
            }
        };
    }

    @Override
    protected void onActive() {
        super.onActive();
        mContainer.onActive(this);
        getQueryExecutor().execute(mRefreshRunnable);
    }

    @Override
    protected void onInactive() {
        super.onInactive();
        mContainer.onInactive(this);
    }

    Executor getQueryExecutor() {
        return repository.getBackgroundExecutor();
    }
}
