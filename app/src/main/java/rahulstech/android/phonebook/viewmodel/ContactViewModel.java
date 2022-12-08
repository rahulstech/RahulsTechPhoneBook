package rahulstech.android.phonebook.viewmodel;

import android.app.Application;
import android.net.Uri;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import rahulstech.android.phonebook.concurrent.AsyncTask;
import rahulstech.android.phonebook.model.ContactDetails;
import rahulstech.android.phonebook.model.PhoneNumber;
import rahulstech.android.phonebook.model.RawContact;
import rahulstech.android.phonebook.repository.ContactRepository;
import rahulstech.android.phonebook.util.Check;

public class ContactViewModel extends AndroidViewModel {

    private static final String TAG = "ContactViewModel";

    private ContactRepository repository;

    public ContactViewModel(@NonNull Application application) {
        super(application);
        repository = ContactRepository.get(application.getApplicationContext());
    }

    //////////////////////////////////////////////////////////////////////////////////
    ///                          Halted Task Methods                              ///
    ////////////////////////////////////////////////////////////////////////////////

    private Runnable haltedTask = null;

    public ContactRepository getRepository() {
        return repository;
    }

    public boolean hasAnyHaltedTask() { return null != haltedTask; }

    public Runnable getHaltedTask() {
        return haltedTask;
    }

    public void addHaltedTask(Runnable haltedTask) {
        this.haltedTask = haltedTask;
    }

    public void removeHaltedTask() {
        this.haltedTask = null;
    }

    //////////////////////////////////////////////////////////////////////////////////
    ///                      Contact Details Methods                              ///
    ////////////////////////////////////////////////////////////////////////////////

    private ContactDetails contactDetails = null;

    private List<ContactDetails> contacts = null;

    private RawContact editContactSource = null;

    public void setContacts(List<ContactDetails> contacts) {
        this.contacts = contacts;
    }

    public List<ContactDetails> getContacts() {
        return contacts;
    }

    public ContactDetails getContactDetails() {
        return contactDetails;
    }

    public boolean hasContactDetails() {
        return null != contactDetails;
    }

    public void setContactDetails(ContactDetails contactDetails) {
        this.contactDetails = contactDetails;
    }

    public RawContact getEditContactSource() {
        return editContactSource;
    }

    public void setEditContactSource(RawContact source) {
        this.editContactSource = source;
    }

    public void insertContactDetails(@NonNull ContactDetails details, @Nullable AsyncTask.AsyncTaskCallback callback) {
        Check.isNonNull(details,"null == details");
        AsyncTask.execute(()->repository.insertContactDetails(details), callback);
    }

    public void updateContactDetails(@NonNull ContactDetails oldDetails, @NonNull ContactDetails newDetails,
                                     @Nullable AsyncTask.AsyncTaskCallback callback) {
        Check.isNonNull(oldDetails,"null == oldDetails");
        Check.isNonNull(newDetails,"null == newDetails");
        AsyncTask.execute(()->repository.updateContactDetails(oldDetails, newDetails),callback);
    }

    public void removeContact(@NonNull ContactDetails details, @Nullable AsyncTask.AsyncTaskCallback callback) {
        Check.isNonNull(details,"null == details");
        AsyncTask.execute(()->repository.deleteContact(details.getContact()),callback);
    }

    //////////////////////////////////////////////////////////////////////////////////
    ///                              Utility                                      ///
    ////////////////////////////////////////////////////////////////////////////////

    private static class AsyncTaskCallbackWrapper extends AsyncTask.AsyncTaskCallback {

        @Nullable
        private AsyncTask.AsyncTaskCallback wrapped;

        public AsyncTaskCallbackWrapper() {}

        public AsyncTaskCallbackWrapper(@Nullable AsyncTask.AsyncTaskCallback callback) {
            wrap(callback);
        }

        public void wrap(@Nullable AsyncTask.AsyncTaskCallback callback) {
            this.wrapped = callback;
        }

        public boolean hasWrappedCallback() {
            return null != wrapped;
        }

        @Override
        public void onError(AsyncTask task) {
            if (hasWrappedCallback()) wrapped.onError(task);
        }

        @Override
        public void onResult(AsyncTask task) {
            if (hasWrappedCallback()) wrapped.onResult(task);
        }
    }
}
