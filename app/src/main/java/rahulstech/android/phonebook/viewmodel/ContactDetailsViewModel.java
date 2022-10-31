package rahulstech.android.phonebook.viewmodel;

import android.app.Application;
import android.util.Log;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import rahulstech.android.phonebook.concurrent.AsyncTask;
import rahulstech.android.phonebook.model.Account;
import rahulstech.android.phonebook.model.ContactDetails;
import rahulstech.android.phonebook.model.Email;
import rahulstech.android.phonebook.model.Event;
import rahulstech.android.phonebook.model.Note;
import rahulstech.android.phonebook.model.PhoneNumber;
import rahulstech.android.phonebook.model.Relation;
import rahulstech.android.phonebook.repository.ContactRepository;
import rahulstech.android.phonebook.repository.ContactRepositoryOperation;
import rahulstech.android.phonebook.util.Check;

public class ContactDetailsViewModel extends AndroidViewModel {

    private static final String TAG = "ContactDetailsVM";

    private ContactRepository repo;
    private ContactRepositoryOperation operation;
    private LiveData<ContactDetails> contactDetails = null;
    private LiveData<List<Event>> events = null;
    private LiveData<List<Relation>> relations = null;
    private LiveData<Note> note = null;

    private Runnable haltedTask = null;

    public ContactDetailsViewModel(@NonNull Application application) {
        super(application);
        repo = ContactRepository.get(application.getApplicationContext());
        operation = repo.getContactRepositoryOperation();
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

    public void loadContactAccounts(AsyncTask.AsyncTaskCallback<List<Account>> callback) {
        AsyncTask.execute(()->operation.getContactAccounts(),callback);
    }

    public void addContactDetails(@NonNull ContactDetails details) {

    }

    public void addPhoneNumber(@NonNull ContactDetails details,@NonNull PhoneNumber number) {
        addPhoneNumber(details,number,null);
    }

    public void addPhoneNumber(@NonNull ContactDetails details, @NonNull PhoneNumber number, AsyncTask.AsyncTaskCallback<Boolean> callback) {
        if (null == details || null == number) {
            Log.i(TAG,"null == ContactDetails || null == PhoneNumber");
            return;
        }
        AsyncTask.execute(()->operation.addPhoneNumber(details.getAccount(),number),callback);
    }

    public void setPrimary(@NonNull PhoneNumber number) {
        if (null == number) {
            Log.i(TAG,"null == PhoneNumber");
            return;
        }
        if (!number.isPrimary()) {
            number.setPrimary(true);
            AsyncTask.execute(() -> operation.updatePhoneNumber(number), new AsyncTask.AsyncTaskCallback());
        }
    }

    public void unsetPrimary(@NonNull PhoneNumber number) {
        if (null == number) {
            Log.i(TAG,"null == PhoneNumber");
            return;
        }
        if (number.isPrimary()) {
            number.setPrimary(false);
            AsyncTask.execute(() -> operation.updatePhoneNumber(number), new AsyncTask.AsyncTaskCallback());
        }
    }

    public void setPrimary(@NonNull Email email) {
        if (null == email) {
            Log.i(TAG,"null == PhoneNumber");
            return;
        }
        if (!email.isPrimary()) {
            email.setPrimary(true);
            AsyncTask.execute(() -> operation.updateEmail(email), new AsyncTask.AsyncTaskCallback());
        }
    }

    public void unsetPrimary(@NonNull Email email) {
        if (null == email) {
            Log.i(TAG,"null == PhoneNumber");
            return;
        }
        if (email.isPrimary()) {
            email.setPrimary(false);
            AsyncTask.execute(() -> operation.updateEmail(email), new AsyncTask.AsyncTaskCallback());
        }
    }

    public void addEmail(@NonNull ContactDetails details, @NonNull Email email) {
        addEmail(details,email,null);
    }

    public void addEmail(@NonNull ContactDetails details, @NonNull Email email, AsyncTask.AsyncTaskCallback<Boolean> callback) {
        if (null == details || null == email) {
            Log.i(TAG,"null == ContactDetails || null == Email");
            return;
        }
        AsyncTask.execute(()->operation.addEmail(details.getAccount(),email),callback);
    }

    public void addRelation(@NonNull ContactDetails details, @NonNull Relation relation) {
        addRelation(details,relation,null);
    }

    public void addRelation(@NonNull ContactDetails details, @NonNull Relation relation, AsyncTask.AsyncTaskCallback<Boolean> callback) {
        if (null == details || null == relation) {
            Log.i(TAG,"null == ContactDetails || null == Relation");
            return;
        }
        AsyncTask.execute(()->operation.addRelation(details.getAccount(),relation),callback);
    }

    public void addEvent(@NonNull ContactDetails details, @NonNull Event event) {
        if (null == details || null == event) {
            Log.i(TAG,"null == ContactDetails || null == Event");
            return;
        }
        AsyncTask.execute(()->operation.addEvent(details.getAccount(),event),new AsyncTask.AsyncTaskCallback());
    }

    public LiveData<ContactDetails> findContactDetailsByLookupKey(String lookupKey) {
        if (null == contactDetails) {
            contactDetails = operation.findContactDetails(lookupKey);
        }
        return contactDetails;
    }

    @Nullable
    public ContactDetails getLoadedContactDetails() {
        if (null == contactDetails) return null;
        return contactDetails.getValue();
    }

    public LiveData<List<Event>> getContactEvents(ContactDetails details) {
        if (null == events) {
            events = operation.findContactEvents(details.getContact());
        }
        return events;
    }

    public LiveData<List<Relation>> getContactRelations(ContactDetails details) {
        if (null == relations) {
            relations = operation.findContactRelations(details.getContact());
        }
        return relations;
    }

    public LiveData<Note> getContactNote(ContactDetails details) {
        if (null == note) {
            note = operation.findContactNote(details.getContact());
        }
        return note;
    }

    public void removePhoneNumber(@NonNull PhoneNumber pn) {
        Check.isNonNull(pn,"null == phone number");
        AsyncTask.execute(()->operation.removePhoneNumber(pn.getId()),null);
    }

    public void removeEmail(@NonNull Email email) {
        Check.isNonNull(email,"null == email");
        AsyncTask.execute(()->operation.removeEmail(email.getId()),null);
    }

    public void removeRelation(@NonNull Relation relation) {
        Check.isNonNull(relation,"null == relation");
        AsyncTask.execute(()->operation.removeRelation(relation.getId()),null);
    }

    public void removeEvent(@NonNull Event event) {
        Check.isNonNull(event,"null == event");
        AsyncTask.execute(()->operation.removeEvent(event.getId()),null);
    }

    public void removeNote(@NonNull Note note) {
        Check.isNonNull(note,"null == note");
        AsyncTask.execute(()->operation.removeNote(note.getId()),null);
    }


}
