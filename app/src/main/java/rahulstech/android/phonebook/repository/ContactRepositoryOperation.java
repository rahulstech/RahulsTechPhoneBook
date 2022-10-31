package rahulstech.android.phonebook.repository;

import android.content.ContentValues;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import rahulstech.android.phonebook.model.Account;
import rahulstech.android.phonebook.model.Contact;
import rahulstech.android.phonebook.model.ContactDetails;
import rahulstech.android.phonebook.model.ContactDisplay;
import rahulstech.android.phonebook.model.Email;
import rahulstech.android.phonebook.model.Event;
import rahulstech.android.phonebook.model.Note;
import rahulstech.android.phonebook.model.PhoneNumber;
import rahulstech.android.phonebook.model.PostalAddress;
import rahulstech.android.phonebook.model.Relation;

public class ContactRepositoryOperation {

    private static final String TAG = "ContactRepoOps";

    private final Uri[] NOTIFY_CONTENT_DATA = new Uri[]{ContactsContract.Data.CONTENT_URI};

    ContactRepository repository;

    ContactRepositoryOperation(ContactRepository repository) {
        this.repository = repository;
    }


    
    public boolean addPhoneNumber(@NonNull Account account, @NonNull PhoneNumber number) {
        ContentValues values = new ContentValues();
        values.put(ContactsContract.Data.RAW_CONTACT_ID,account.getId());
        values.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
        values.put(ContactsContract.CommonDataKinds.Phone.NUMBER,number.getNumber());
        values.put(ContactsContract.CommonDataKinds.Phone.TYPE,number.getType());
        if (number.getType() == ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM) {
            values.put(ContactsContract.CommonDataKinds.Phone.LABEL,String.valueOf(number.getTypeLabel()));
        }
        long newId = repository.addContactData(values);
        Log.d(TAG,"newPhoneNumberId="+newId);
        if (newId > 0) {
            repository.getInvalidationTracker().notifyRefresh(NOTIFY_CONTENT_DATA);
            return true;
        }
        return false;
    }

    public boolean addEmail(@NonNull Account account, @NonNull Email email) {
        ContentValues values = new ContentValues();
        values.put(ContactsContract.Data.RAW_CONTACT_ID,account.getId());
        values.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE);
        values.put(ContactsContract.CommonDataKinds.Email.ADDRESS,email.getAddress());
        values.put(ContactsContract.CommonDataKinds.Email.TYPE,email.getType());
        if (email.getType() == ContactsContract.CommonDataKinds.Email.TYPE_CUSTOM) {
            values.put(ContactsContract.CommonDataKinds.Email.LABEL,String.valueOf(email.getTypeLabel()));
        }
        long newId = repository.addContactData(values);
        Log.d(TAG,"newEmailId="+newId);
        if (newId > 0) {
            repository.getInvalidationTracker().notifyRefresh(NOTIFY_CONTENT_DATA);
            return true;
        }
        return false;
    }

    public boolean addRelation(@NonNull Account account, @NonNull Relation relation) {
        ContentValues values = new ContentValues();
        values.put(ContactsContract.Data.RAW_CONTACT_ID,account.getId());
        values.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Relation.CONTENT_ITEM_TYPE);
        values.put(ContactsContract.CommonDataKinds.Relation.NAME,relation.getDisplayName());
        values.put(ContactsContract.CommonDataKinds.Relation.TYPE,relation.getType());
        if (relation.getType() == ContactsContract.CommonDataKinds.Relation.TYPE_CUSTOM)
            values.put(ContactsContract.CommonDataKinds.Relation.LABEL,
                    relation.getTypeLabel().toString());

        long newId = repository.addContactData(values);
        Log.d(TAG,"newRelationId="+newId);
        if (newId > 0) {
            repository.getInvalidationTracker().notifyRefresh(NOTIFY_CONTENT_DATA);
            return true;
        }
        return false;
    }

    public boolean addEvent(@NonNull Account account, @NonNull Event event) {
        ContentValues values = new ContentValues();
        values.put(ContactsContract.Data.RAW_CONTACT_ID,account.getId());
        values.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE);
        values.put(ContactsContract.CommonDataKinds.Event.START_DATE, event.getStartDate());
        values.put(ContactsContract.CommonDataKinds.Event.TYPE,event.getType());
        if (event.getType() == ContactsContract.CommonDataKinds.Event.TYPE_CUSTOM)
            values.put(ContactsContract.CommonDataKinds.Event.LABEL,
                    event.getTypeLabel().toString());

        long newId = repository.addContactData(values);
        Log.d(TAG,"newEventId="+newId);
        if (newId > 0) {
            repository.getInvalidationTracker().notifyRefresh(NOTIFY_CONTENT_DATA);
            return true;
        }
        return false;
    }

    public List<Account> getContactAccounts() {
        // TODO: add phone and sim cards as account list
        try {
            List<android.accounts.Account> list = repository.getContactAccounts();
            Log.i(TAG,"found accounts: "+(null == list ? null : list.size()) );
            List<Account> accounts = new ArrayList<>();
            for (android.accounts.Account acc : list) {
                accounts.add(new Account(null,0,acc.name, acc.type));
            }
            return accounts;
        }
        catch (Exception ex) {
            Log.e(TAG,null,ex);
        }
        return Collections.EMPTY_LIST;
    }

    public LiveData<List<ContactDisplay>> findAllContacts() {
        return repository.getInvalidationTracker()
                .createLiveData(repository.getContentUrisForModelClass(ContactDisplay.class),
                        ()-> {
                            try {
                                return repository.loadContactDisplays();
                            }
                            catch (Exception ex) {
                                Log.e(TAG,null,ex);
                            }
                            return null;
                        });
    }

    public LiveData<ContactDetails> findContactDetails(String lookupKey) {
        return repository.getInvalidationTracker()
                .createLiveData(repository.getContentUrisForModelClass(ContactDetails.class),
                        ()->{
                            try {
                                return repository.loadContactDetailsByLookupKey(lookupKey);
                            }
                            catch (Exception ex) {
                                Log.e(TAG,null,ex);
                            }
                            return null;
                    });
    }

    public LiveData<List<Event>> findContactEvents(Contact contact) {
        return repository.getInvalidationTracker()
                .createLiveData(
                        repository.getContentUrisForModelClass(Event.class),
                        ()->{
                            try {
                                return repository.loadContactEventsByLookupKey(contact.getLookupKey());
                            }
                            catch (Exception ex) {
                                Log.e(TAG,null,ex);
                            }
                            return null;
                        });
    }

    public LiveData<List<Relation>> findContactRelations(Contact contact) {
        return repository.getInvalidationTracker()
                .createLiveData(
                        repository.getContentUrisForModelClass(Relation.class),
                        ()->{
                            try {
                                return repository.loadContactRelationsByLookupKey(contact.getLookupKey());
                            }
                            catch (Exception ex) {
                                Log.e(TAG,null,ex);
                            }
                            return null;
                        });
    }

    public LiveData<List<PostalAddress>> findContactPostalAddress(Contact contact) {
        return repository.getInvalidationTracker()
                .createLiveData(repository.getContentUrisForModelClass(PostalAddress.class),
                        ()->{
                            try {
                                return repository.loadContactPostalAddressesByLookupKey(contact.getLookupKey());
                            }
                            catch (Exception ex) {
                                Log.e(TAG,null,ex);
                            }
                            return null;
                        });
    }

    public LiveData<Note> findContactNote(Contact contact) {
        return repository.getInvalidationTracker()
                .createLiveData(repository.getContentUrisForModelClass(Note.class),
                        ()->{
                            try {
                                return repository.loadContactNoteByLookupKey(contact.getLookupKey());
                            }
                            catch (Exception ex) {
                                Log.e(TAG,null,ex);
                            }
                            return null;
                        });
    }

    public boolean updatePhoneNumber(PhoneNumber number) {
        ContentValues values = new ContentValues();
        values.put(ContactsContract.CommonDataKinds.Phone.NUMBER,number.getNumber());
        values.put(ContactsContract.CommonDataKinds.Phone.TYPE,number.getType());
        values.put(ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY,number.isPrimary() ? 1 : 0);
        if (ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM == number.getType()) {
            values.put(ContactsContract.CommonDataKinds.Phone.LABEL,String.valueOf(number.getTypeLabel()));
        }
        else {
            values.putNull(ContactsContract.CommonDataKinds.Phone.LABEL);
        }
        int changes = repository.updateContactData(number.getId(),values);
        Log.d(TAG,"updatePhoneNumber changes="+changes);
        if (1 == changes) {
            repository.getInvalidationTracker().notifyRefresh(NOTIFY_CONTENT_DATA);
            return true;
        }
        return false;
    }

    public boolean updateEmail(Email email) {
        ContentValues values = new ContentValues();
        values.put(ContactsContract.CommonDataKinds.Email.ADDRESS,email.getAddress());
        values.put(ContactsContract.CommonDataKinds.Email.TYPE,email.getType());
        values.put(ContactsContract.CommonDataKinds.Email.IS_SUPER_PRIMARY,email.isPrimary() ? 1 : 0);
        if (ContactsContract.CommonDataKinds.Email.TYPE_CUSTOM == email.getType()) {
            values.put(ContactsContract.CommonDataKinds.Email.LABEL,String.valueOf(email.getTypeLabel()));
        }
        else {
            values.putNull(ContactsContract.CommonDataKinds.Phone.LABEL);
        }
        int changes = repository.updateContactData(email.getId(),values);
        Log.d(TAG,"updateEmail changes="+changes);
        if (1 == changes) {
            repository.getInvalidationTracker().notifyRefresh(NOTIFY_CONTENT_DATA);
            return true;
        }
        return false;
    }

    public boolean removePhoneNumber(long id) {
        int changes = repository.removeContactData(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,new long[]{id});
        Log.d(TAG,"removePhoneNumber changes="+changes);
        if (changes == 1) {
            repository.getInvalidationTracker().notifyRefresh(repository.getContentUrisForModelClass(PhoneNumber.class));
            return true;
        }
        return false;
    }

    public boolean removeEmail(long id) {
        int changes = repository.removeContactData(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE,new long[]{id});
        Log.d(TAG,"removeEmail changes="+changes);
        if (changes == 1) {
            repository.getInvalidationTracker().notifyRefresh(repository.getContentUrisForModelClass(Email.class));
            return true;
        }
        return false;
    }

    public boolean removePostalAddress(long id) {
        int changes = repository.removeContactData(ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE,new long[]{id});
        Log.d(TAG,"removePostalAddress changes="+changes);
        if (changes == 1) {
            repository.getInvalidationTracker().notifyRefresh(repository.getContentUrisForModelClass(PostalAddress.class));
            return true;
        }
        return false;
    }

    public boolean removeRelation(long id) {
        int changes = repository.removeContactData(ContactsContract.CommonDataKinds.Relation.CONTENT_ITEM_TYPE,new long[]{id});
        Log.d(TAG,"removeRelation changes="+changes);
        if (changes == 1) {
            repository.getInvalidationTracker().notifyRefresh(repository.getContentUrisForModelClass(Relation.class));
            return true;
        }
        return false;
    }

    public boolean removeEvent(long id) {
        int changes = repository.removeContactData(ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE,new long[]{id});
        Log.d(TAG,"removeEvent changes="+changes);
        if (changes == 1) {
            repository.getInvalidationTracker().notifyRefresh(repository.getContentUrisForModelClass(Event.class));
            return true;
        }
        return false;
    }

    public boolean removeNote(long id) {
        int changes = repository.removeContactData(ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE,new long[]{id});
        Log.d(TAG,"removeNote changes="+changes);
        if (changes == 1) {
            repository.getInvalidationTracker().notifyRefresh(repository.getContentUrisForModelClass(Note.class));
            return true;
        }
        return false;
    }
}
