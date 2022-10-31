package rahulstech.android.phonebook.repository;


import android.net.Uri;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.Callable;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;

/**
 * A helper class that maintains {@link RepositoryTrackingLiveData} instances for an
 * {@link InvalidationTracker}.
 * <p>
 * We keep a strong reference to active LiveData instances to avoid garbage collection in case
 * developer does not hold onto the returned LiveData.
 */
class InvalidationLiveDataContainer {
    @SuppressWarnings("WeakerAccess")
    @VisibleForTesting
    final Set<LiveData> mLiveDataSet = Collections.newSetFromMap(
            new IdentityHashMap<>()
    );
    private final ContactRepository repository;

    InvalidationLiveDataContainer(ContactRepository repository) {
        this.repository = repository;
    }

    <T> LiveData<T> create(Uri[] uris,
                           Callable<T> computeFunction) {
        return new RepositoryTrackingLiveData<>(repository, this, computeFunction,
                uris);
    }

    void onActive(LiveData liveData) {
        mLiveDataSet.add(liveData);
    }

    void onInactive(LiveData liveData) {
        mLiveDataSet.remove(liveData);
    }
}
