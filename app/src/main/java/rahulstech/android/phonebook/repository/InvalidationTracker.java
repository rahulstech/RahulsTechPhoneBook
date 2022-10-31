package rahulstech.android.phonebook.repository;


import android.annotation.SuppressLint;
import android.net.Uri;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.arch.core.internal.SafeIterableMap;
import androidx.lifecycle.LiveData;

/**
 * InvalidationTracker keeps a list of tables modified by queries and notifies its callbacks about
 * these tables.
 */
// Some details on how the InvalidationTracker works:
// * An in memory table is created with (table_id, invalidated) table_id is a hardcoded int from
// initialization, while invalidated is a boolean bit to indicate if the table has been invalidated.
// * ObservedTableTracker tracks list of tables we should be watching (e.g. adding triggers for).
// * Before each beginTransaction, RoomDatabase invokes InvalidationTracker to sync trigger states.
// * After each endTransaction, RoomDatabase invokes InvalidationTracker to refresh invalidated
// tables.
// * Each update (write operation) on one of the observed tables triggers an update into the
// memory table table, flipping the invalidated flag ON.
// * When multi-instance invalidation is turned on, MultiInstanceInvalidationClient will be created.
// It works as an Observer, and notifies other instances of table invalidation.
public class InvalidationTracker {

    @NonNull
    //final Uri[] mContentUris;
    final Set<Uri> mContentUris;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final ContactRepository repository;

    private final InvalidationLiveDataContainer mInvalidationLiveDataContainer;

    // should be accessed with synchronization only.
    @VisibleForTesting
    @SuppressLint("RestrictedApi")
    final SafeIterableMap<Observer, ObserverWrapper> mObserverMap = new SafeIterableMap<>();

    private final Object mSyncTriggersLock = new Object();

    /**
     * Used by the generated code.
     *
     * @hide
     */
    @SuppressWarnings("WeakerAccess")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public InvalidationTracker(ContactRepository repository, Uri... uris) {
        this.repository = repository;
        mInvalidationLiveDataContainer = new InvalidationLiveDataContainer(repository);
        int size = uris.length;
        if (0 == size) {
            throw new IllegalArgumentException("no content uri provided to observe");
        }
        mContentUris = new HashSet<>(size);
        for (Uri u : uris) {
            mContentUris.add(u);
        }
    }

    /**
     * Adds the given observer to the observers list and it will be notified if any table it
     * observes changes.
     * <p>
     * Database changes are pulled on another thread so in some race conditions, the observer might
     * be invoked for changes that were done before it is added.
     * <p>
     * If the observer already exists, this is a no-op call.
     * <p>
     * If one of the tables in the Observer does not exist in the database, this method throws an
     * {@link IllegalArgumentException}.
     * <p>
     * This method should be called on a background/worker thread as it performs database
     * operations.
     *
     * @param observer The observer which listens the database for changes.
     */
    @SuppressLint("RestrictedApi")
    @WorkerThread
    public void addObserver(@NonNull Observer observer) {
        final Uri[] uris = observer.mContentUris;
        ObserverWrapper wrapper = new ObserverWrapper(observer, uris);
        mObserverMap.putIfAbsent(observer, wrapper);
    }

    private Uri[] validateAgainstObservingUris(Uri[] uris) {
        Set<Uri> set = new HashSet<>();
        for (Uri uri : uris) {
            if (!mContentUris.contains(uri)) {
                throw new IllegalArgumentException("uri "+uri+" is not set to invalidation observe");
            }
            set.add(uri);
        }
        return set.toArray(new Uri[0]);
    }

    /**
     * Adds an observer but keeps a weak reference back to it.
     * <p>
     * Note that you cannot remove this observer once added. It will be automatically removed
     * when the observer is garbage collected.
     *
     * @param observer The observer to which InvalidationTracker will keep a weak reference.
     * @hide
     */
    @SuppressWarnings("unused")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public void addWeakObserver(Observer observer) {
        addObserver(new WeakObserver(this, observer));
    }

    /**
     * Removes the observer from the observers list.
     * <p>
     * This method should be called on a background/worker thread as it performs database
     * operations.
     *
     * @param observer The observer to remove.
     */
    @SuppressLint("RestrictedApi")
    @SuppressWarnings("WeakerAccess")
    @WorkerThread
    public void removeObserver(@NonNull final Observer observer) {
        mObserverMap.remove(observer);
    }


    public void notifyRefresh(final Uri[] uris) {
        repository.getBackgroundExecutor().execute(() -> {
            synchronized (mObserverMap) {
                for (Map.Entry<Observer, ObserverWrapper> e : mObserverMap) {
                    e.getValue().notifyInvalidated(uris);
                }
            }
        });
    }

    /**
     * Creates a LiveData that computes the given function once and for every other invalidation
     * of the database.
     * <p>
     * Holds a strong reference to the created LiveData as long as it is active.
     *
     * @param uris      The list of content uris to observe
     * @param computeFunction The function that calculates the value
     * @param <T>             The return type
     * @return A new LiveData that computes the given function when the given list of tables
     * invalidates.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public <T> LiveData<T> createLiveData(Uri[] uris,
                                          Callable<T> computeFunction) {
        return mInvalidationLiveDataContainer.create(
                validateAgainstObservingUris(uris), computeFunction);
    }

    /**
     * Wraps an observer and keeps the table information.
     * <p>
     * Internally table ids are used which may change from database to database so the table
     * related information is kept here rather than in the Observer.
     */
    @SuppressWarnings("WeakerAccess")
    static class ObserverWrapper {
        final Uri[] mContentUris;
        final Observer mObserver;

        ObserverWrapper(Observer observer, Uri[] uris) {
            mObserver = observer;
            mContentUris = uris;
        }

        /**
         * Notifies the underlying {@link #mObserver} if it observes any of the specified
         * {@code tables}.
         *
         * @param uris The invalidated content uris.
         */
        void notifyInvalidated(Uri[] uris) {
            Set<Uri> invalidated = new HashSet<>();
            for (Uri uri : uris) {
                for (Uri ourUri : mContentUris) {
                    if (ourUri.equals(uri)) {
                        invalidated.add(ourUri);
                        break;
                    }
                }
            }
            if (!invalidated.isEmpty()) {
                mObserver.onInvalidated(invalidated);
            }
        }
    }

    /**
     * An observer that can listen for changes in the database.
     */
    public abstract static class Observer {
        final Uri[] mContentUris;

        /**
         * Observes the given list of tables and views.
         *
         * @param first The name of the table or view.
         * @param rest       More names of tables or views.
         */
        protected Observer(@NotNull Uri first, Uri...rest) {
            mContentUris = Arrays.copyOf(rest,rest.length+1);
            mContentUris[rest.length] = first;
        }

        /**
         * Observes the given list of tables and views.
         *
         * @param uris The list of tables or views to observe for changes.
         */
        public Observer(@NonNull Uri[] uris) {
            // copy tables in case user modifies them afterwards
            mContentUris = Arrays.copyOf(uris, uris.length);
        }

        /**
         * Called when one of the observed tables is invalidated in the database.
         *
         * @param uris A set of invalidated tables. This is useful when the observer targets
         *               multiple tables and you want to know which table is invalidated. This will
         *               be names of underlying tables when you are observing views.
         */
        public abstract void onInvalidated(@NonNull Set<Uri> uris);

        boolean isRemote() {
            return false;
        }
    }

    /**
     * An Observer wrapper that keeps a weak reference to the given object.
     * <p>
     * This class will automatically unsubscribe when the wrapped observer goes out of memory.
     */
    static class WeakObserver extends Observer {
        final InvalidationTracker mTracker;
        final WeakReference<Observer> mDelegateRef;

        WeakObserver(InvalidationTracker tracker, Observer delegate) {
            super(delegate.mContentUris);
            mTracker = tracker;
            mDelegateRef = new WeakReference<>(delegate);
        }

        @Override
        public void onInvalidated(@NonNull Set<Uri> uris) {
            final Observer observer = mDelegateRef.get();
            if (observer == null) {
                mTracker.removeObserver(this);
            } else {
                observer.onInvalidated(uris);
            }
        }
    }
}

