package rahulstech.android.phonebook.repository;

import android.content.ContentValues;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rahulstech.android.phonebook.model.Name;
import rahulstech.android.phonebook.model.RawContact;
import rahulstech.android.phonebook.model.Contact;
import rahulstech.android.phonebook.model.ContactDetails;
import rahulstech.android.phonebook.model.ContactDisplay;
import rahulstech.android.phonebook.model.Email;
import rahulstech.android.phonebook.model.Event;
import rahulstech.android.phonebook.model.Note;
import rahulstech.android.phonebook.model.Organization;
import rahulstech.android.phonebook.model.PhoneNumber;
import rahulstech.android.phonebook.model.PostalAddress;
import rahulstech.android.phonebook.model.Relation;
import rahulstech.android.phonebook.model.Website;

public class ContactRepositoryOperation {

    private static final String TAG = "ContactRepoOps";

    ContactRepository repository;

    ContactRepositoryOperation(ContactRepository repository) {
        this.repository = repository;
    }

    public boolean setContactStarred(Contact contact, boolean checked) {
        ContentValues values = new ContentValues();
        values.put(ContactsContract.Contacts.STARRED,checked ? 1 : 0);
        return 1 == repository.updateContact(contact.getId(),values);
    }

    public List<RawContact> getContactAccounts() {
        // TODO: add phone and sim cards as account list
        try {
            List<android.accounts.Account> list = repository.getContactAccounts();
            Log.i(TAG,"found accounts: "+(null == list ? null : list.size()) );
            List<RawContact> rawContacts = new ArrayList<>();
            for (android.accounts.Account acc : list) {
                rawContacts.add(new RawContact(null,0,acc.name, acc.type));
            }
            return rawContacts;
        }
        catch (Exception ex) {
            Log.e(TAG,null,ex);
        }
        return Collections.EMPTY_LIST;
    }

    public List<ContactDisplay> findAllContacts() {
        return repository.loadContactDisplays();
    }

    public ContactDetails findContactDetails(Uri uri) {
        try {
            final Uri lookupUri = repository.getLookupUri(uri);
            if (null == lookupUri) return null;
            final String lookupKey = lookupUri.getLastPathSegment();

            RawContact rawContact = repository.loadRawContact(lookupKey);
            Contact contact = repository.loadContact(lookupKey);
            Name name = repository.loadName(lookupKey);
            List<PhoneNumber> phoneNumbers = repository.loadPhoneNumbers(lookupKey);
            List<Email> emails = repository.loadEmails(lookupKey);
            List<Event> events = repository.loadContactEvents(lookupKey);
            List<Relation> relatives = repository.loadContactRelations(lookupKey);
            List<PostalAddress> addresses = repository.loadContactPostalAddresses(lookupKey);
            List<Organization> organizations = repository.loadContactOrganizations(lookupKey);
            List<Website> websites = repository.loadContactWebsites(lookupKey);
            Note note = repository.loadContactNote(lookupKey);

            ContactDetails details = new ContactDetails(rawContact,contact);
            details.setName(name);
            details.setPhoneNumbers(phoneNumbers);
            details.setEmails(emails);
            details.setEvents(events);
            details.setRelatives(relatives);
            details.setAddresses(addresses);
            details.setOrganizations(organizations);
            details.setWebsites(websites);
            details.setNote(note);

            return details;
        }
        catch (Exception ex) {
            throw new RepositoryException("exception thrown while fetching contact details for uri="+uri,ex);
        }
    }
}
