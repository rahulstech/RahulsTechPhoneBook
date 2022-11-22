package rahulstech.android.phonebook.viewmodel;

import android.app.Application;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import rahulstech.android.phonebook.model.ContactDetails;
import rahulstech.android.phonebook.model.ContactDisplay;
import rahulstech.android.phonebook.repository.ContactRepository;
import rahulstech.android.phonebook.repository.ContactRepositoryOperation;

public class ContactViewModel extends AndroidViewModel {

    private static final String TAG = "ContactViewModel";

    private ContactRepository repository;
    private ContactRepositoryOperation operation;

    private Runnable haltedTask = null;

    private List<ContactDisplay> contactDisplayList = null;

    private ContactDetails contactDetails = null;

    public ContactViewModel(@NonNull Application application) {
        super(application);
        repository = ContactRepository.get(application.getApplicationContext());
        operation = repository.getContactRepositoryOperation();
    }

    public ContactRepositoryOperation getOperation() {
        return operation;
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

    public void setContactDisplayList(List<ContactDisplay> contactDisplayList) {
        this.contactDisplayList = contactDisplayList;
    }

    public List<ContactDisplay> getContactDisplayList() {
        return contactDisplayList;
    }

    public void setContactDetails(ContactDetails contactDetails) {
        this.contactDetails = contactDetails;
    }

    public ContactDetails getContactDetails() {
        return contactDetails;
    }

    public boolean hasContactDetails() {
        return null != contactDetails;
    }

}
