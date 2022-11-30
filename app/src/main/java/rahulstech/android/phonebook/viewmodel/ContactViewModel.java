package rahulstech.android.phonebook.viewmodel;

import android.accounts.Account;
import android.app.Application;
import android.net.Uri;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import rahulstech.android.phonebook.concurrent.AsyncTask;
import rahulstech.android.phonebook.model.Contact;
import rahulstech.android.phonebook.model.ContactAccount;
import rahulstech.android.phonebook.model.ContactDetails;
import rahulstech.android.phonebook.model.ContactDisplay;
import rahulstech.android.phonebook.model.Email;
import rahulstech.android.phonebook.model.Event;
import rahulstech.android.phonebook.model.Name;
import rahulstech.android.phonebook.model.Note;
import rahulstech.android.phonebook.model.Organization;
import rahulstech.android.phonebook.model.PhoneNumber;
import rahulstech.android.phonebook.model.PostalAddress;
import rahulstech.android.phonebook.model.RawContact;
import rahulstech.android.phonebook.model.Relation;
import rahulstech.android.phonebook.model.Website;
import rahulstech.android.phonebook.repository.ContactRepository;
import rahulstech.android.phonebook.util.Check;

import static rahulstech.android.phonebook.util.Helpers.logDebug;

public class ContactViewModel extends AndroidViewModel {

    private static final String TAG = "ContactViewModel";

    private ContactRepository repository;

    public ContactViewModel(@NonNull Application application) {
        super(application);
        repository = ContactRepository.get(application.getApplicationContext());
    }

    //////////////////////////////////////////////////////////////////////////////////
    ///                      Contact Display Methods                              ///
    ////////////////////////////////////////////////////////////////////////////////

    private List<ContactDisplay> contactDisplayList = null;

    public void setContactDisplayList(List<ContactDisplay> contactDisplayList) {
        this.contactDisplayList = contactDisplayList;
    }

    public List<ContactDisplay> getContactDisplayList() {
        return contactDisplayList;
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

    private List<ContactAccount> accounts = null;

    public void getAccounts(@Nullable AsyncTask.AsyncTaskCallback callback) {
        AsyncTaskCallbackWrapper wrapper = new AsyncTaskCallbackWrapper(callback) {

            @Override
            public void onResult(AsyncTask task) {
                ContactViewModel.this.accounts = task.getResult();
                super.onResult(task);
            }
        };
        AsyncTask.execute(()->{
            if (null != ContactViewModel.this.accounts) return accounts;
            return repository.getAccounts();
        },wrapper);

    }

    //////////////////////////////////////////////////////////////////////////////////
    ///                      Contact Details Methods                              ///
    ////////////////////////////////////////////////////////////////////////////////

    private ContactDetails contactDetails = null;

    public ContactDetails getContactDetails() {
        return contactDetails;
    }

    public boolean hasContactDetails() {
        return null != contactDetails;
    }

    public void loadContactDetails(@NonNull Uri uri, @Nullable AsyncTask.AsyncTaskCallback callback) {
        Check.isNonNull(uri,"null == uri");
        AsyncTask.execute(()->repository.getContactDetails(uri),new AsyncTaskCallbackWrapper(callback){
            @Override
            public void onResult(AsyncTask task) {
                ContactViewModel.this.contactDetails = task.getResult();
                super.onResult(task);
            }
        });
    }

    public void insertContactDetails(@NonNull ContactDetails details, @Nullable AsyncTask.AsyncTaskCallback callback) {
        Check.isNonNull(details,"null == details");
        AsyncTask.execute(()->repository.insertContactDetails(details), callback);
    }

    public void updateContactDetails(@NonNull ContactDetails oldDetails, @NonNull ContactDetails newDetails, @Nullable AsyncTask.AsyncTaskCallback callback) {
        AsyncTask.execute(()->{
            // TODO: shift this code into ContactRepository class
            //  to do checking and creating operations together
            Contact contact = oldDetails.getContact();
            RawContact rawContact = oldDetails.getRawContact();
            List<PhoneNumber> oNumbers = oldDetails.getPhoneNumbers();
            List<Email> oEmails = oldDetails.getEmails();
            List<Event> oEvents = oldDetails.getEvents();
            List<Relation> oRelations = oldDetails.getRelations();
            List<PostalAddress> oAddresses = oldDetails.getAddresses();
            Organization oOrganization = oldDetails.getOrganization();
            List<Website> oWebsites = oldDetails.getWebsites();
            Note oNote = oldDetails.getNote();

            Name nName = newDetails.getName();
            List<PhoneNumber> nNumbers = newDetails.getPhoneNumbers();
            List<Email> nEmails = newDetails.getEmails();
            List<Event> nEvents = newDetails.getEvents();
            List<Relation> nRelations = newDetails.getRelations();
            List<PostalAddress> nAddresses = newDetails.getAddresses();
            Organization nOrganization = newDetails.getOrganization();
            List<Website> nWebsites = newDetails.getWebsites();
            Note nNote = newDetails.getNote();

            HashMap<Long,PhoneNumber> mNumber = new HashMap<>();
            HashMap<Long,Email> mEmail = new HashMap<>();
            HashMap<Long,Event> mEvent = new HashMap<>();
            HashMap<Long,Relation> mRelation = new HashMap<>();
            HashMap<Long,PostalAddress> mAddress = new HashMap<>();
            HashMap<Long,Website> mWebsite = new HashMap<>();

            ContactDetails toInsert = new ContactDetails(contact);
            toInsert.setRawContact(rawContact);
            toInsert.setPhoneNumbers(new ArrayList<>());
            toInsert.setEmails(new ArrayList<>());
            toInsert.setEvents(new ArrayList<>());
            toInsert.setRelations(new ArrayList<>());
            toInsert.setAddresses(new ArrayList<>());
            toInsert.setWebsites(new ArrayList<>());

            ContactDetails toUpdate = new ContactDetails(contact);
            toUpdate.setRawContact(rawContact);
            toUpdate.setPhoneNumbers(new ArrayList<>());
            toUpdate.setEmails(new ArrayList<>());
            toUpdate.setEvents(new ArrayList<>());
            toUpdate.setRelations(new ArrayList<>());
            toUpdate.setAddresses(new ArrayList<>());
            toUpdate.setWebsites(new ArrayList<>());

            ContactDetails toDelete = new ContactDetails(contact);
            toDelete.setRawContact(rawContact);

            if (null != oNumbers) {
                for (PhoneNumber number : oNumbers) {
                    mNumber.put(number.getId(),number);
                }
            }
            if (null != oEmails) {
                for (Email email : oEmails) {
                    mEmail.put(email.getId(),email);
                }
            }
            if (null != oEvents) {
                for (Event event : oEvents) {
                    mEvent.put(event.getId(),event);
                }
            }
            if (null != oRelations) {
                for (Relation relation : oRelations) {
                    mRelation.put(relation.getId(),relation);
                }
            }
            if (null != oAddresses) {
                for (PostalAddress address : oAddresses) {
                    mAddress.put(address.getId(),address);
                }
            }
            if (null != oWebsites) {
                for (Website website : oWebsites) {
                    mWebsite.put(website.getId(),website);
                }
            }
            if (null != nNumbers) {
                for (PhoneNumber data : nNumbers) {
                    long id = data.getId();
                    if (0==id) toInsert.getPhoneNumbers().add(data);
                    else {
                        PhoneNumber old = mNumber.get(id);
                        if (null != old) {
                            mNumber.remove(id);
                            if (!old.equals(data) && !Check.isEmptyString(data.getNumber()))
                                toUpdate.getPhoneNumbers().add(data);
                        }
                    }
                }
            }
            if (null != nEmails) {
                for (Email data : nEmails) {
                    long id = data.getId();
                    if (0==id) toInsert.getEmails().add(data);
                    else {
                        Email old = mEmail.get(id);
                        if (null != old) {
                            mEmail.remove(id);
                            if (!old.equals(data) && !Check.isEmptyString(data.getAddress()))
                                toUpdate.getEmails().add(data);
                        }
                    }
                }
            }
            if (null != nEvents) {
                for (Event data : nEvents) {
                    long id = data.getId();
                    if (0==id) toInsert.getEvents().add(data);
                    else {
                        Event old = mEvent.get(id);
                        if (null != old ) {
                            mEvent.remove(id);
                            if (!old.equals(data) && !Check.isEmptyString(data.getStartDate()))
                                toUpdate.getEvents().add(data);
                        }
                    }
                }
            }
            if (null != nRelations) {
                for (Relation data : nRelations) {
                    long id = data.getId();
                    if (0==id) toInsert.getRelations().add(data);
                    else {
                        Relation old = mRelation.get(id);
                        if (null != old) {
                            mRelation.remove(id);
                            if (!old.equals(data) && !Check.isEmptyString(data.getDisplayName()))
                                toUpdate.getRelations().add(data);
                        }
                    }
                }
            }
            if (null != nAddresses) {
                for (PostalAddress data : nAddresses) {
                    long id = data.getId();
                    if (0==id) toInsert.getAddresses().add(data);
                    else {
                        PostalAddress old = mAddress.get(id);
                        if (null != old) {
                            mAddress.remove(id);
                            if (!old.equals(data) && !Check.isEmptyString(data.getFormattedAddress()))
                                toUpdate.getAddresses().add(data);
                        }
                    }
                }
            }
            if (null != nWebsites) {
                for (Website data : nWebsites) {
                    long id = data.getId();
                    if (0==id) toInsert.getWebsites().add(data);
                    else {
                        Website old = mWebsite.get(id);
                        if (null != old) {
                            mWebsite.remove(id);
                            if (!old.equals(data) && !Check.isEmptyString(data.getUrl()))
                                toUpdate.getWebsites().add(data);
                        }
                    }
                }
            }

            if (!mNumber.isEmpty()) toDelete.setPhoneNumbers(new ArrayList<>(mNumber.values()));
            if (!mEmail.isEmpty()) toDelete.setEmails(new ArrayList<>(mEmail.values()));
            if (!mRelation.isEmpty()) toDelete.setRelations(new ArrayList<>(mRelation.values()));
            if (!mEvent.isEmpty()) toDelete.setEvents(new ArrayList<>(mEvent.values()));
            if (!mAddress.isEmpty()) toDelete.setAddresses(new ArrayList<>(mAddress.values()));
            if (!mWebsite.isEmpty()) toDelete.setWebsites(new ArrayList<>(mWebsite.values()));
            toDelete.setOrganization(oOrganization);
            toDelete.setNote(oNote);

            if (null != nOrganization && nOrganization.hasValues()) toInsert.setOrganization(nOrganization);
            if (null != nNote && !Check.isEmptyString(nNote.getNote())) toInsert.setNote(nNote);

            /*if (0==nName.getId()) toInsert.setName(nName);
            else toUpdate.setName(nName);*/
            toInsert.setName(nName);

            return repository.updateContactDetails(oldDetails.getContact(),oldDetails.getRawContact(),
                    toDelete,toUpdate,toInsert);
        },callback);
    }

    public void removeContact(@NonNull ContactDetails details, @Nullable AsyncTask.AsyncTaskCallback callback) {
        Check.isNonNull(details,"null == details");
        AsyncTask.execute(()->repository.deleteContact(details.getContact()),callback);
    }

    //////////////////////////////////////////////////////////////////////////////////
    ///                          Other Methods                                    ///
    ////////////////////////////////////////////////////////////////////////////////

    public void togglePhoneNumberPrimary(@NonNull PhoneNumber number, @Nullable AsyncTask.AsyncTaskCallback callback) {
        AsyncTask.execute(()->repository.changePhoneNumberPrimary(number,!number.isPrimary()),callback);
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
