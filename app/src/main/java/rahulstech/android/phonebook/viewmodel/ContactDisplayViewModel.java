package rahulstech.android.phonebook.viewmodel;

import android.app.Application;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import rahulstech.android.phonebook.model.ContactDisplay;
import rahulstech.android.phonebook.repository.ContactRepository;
import rahulstech.android.phonebook.repository.ContactRepositoryOperation;

public class ContactDisplayViewModel extends AndroidViewModel {

    private ContactRepository repo;
    private ContactRepositoryOperation operation;
    private LiveData<List<ContactDisplay>> contacts = null;

    private Runnable haltedTask = null;

    public ContactDisplayViewModel(@NonNull Application application) {
        super(application);
        repo = ContactRepository.get(application.getApplicationContext());
        operation = repo.getContactRepositoryOperation();
    }

    public LiveData<List<ContactDisplay>> getAllContacts() {
        if (null == contacts) {
            contacts = operation.findAllContacts();
        }
        return contacts;
    }

    public Runnable getHaltedTask() {
        return haltedTask;
    }

    public boolean hasAnyHaltedTask() {
        return null != haltedTask;
    }

    public void addHaltedTask(Runnable haltedTask) {
        this.haltedTask = haltedTask;
    }

    public void removeHaltedTask() {
        this.haltedTask = null;
    }


}
