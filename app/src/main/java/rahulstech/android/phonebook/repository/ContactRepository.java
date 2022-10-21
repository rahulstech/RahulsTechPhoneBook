package rahulstech.android.phonebook.repository;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;

import java.util.ArrayList;
import java.util.List;

import rahulstech.android.phonebook.model.ContactDetails;
import rahulstech.android.phonebook.model.ContactDisplay;
import rahulstech.android.phonebook.model.PhoneNumber;

public class ContactRepository {

    private static final String TAG = "ContactsRepository";

    private static final Object lock = new Object();

    private Context appContext;
    private ContentResolver contactsProvider;

    private static ContactRepository instance = null;

    public static ContactRepository get(Context context) {
        synchronized (lock) {
            if (null == instance) {
                instance = new ContactRepository(context);
            }
            return instance;
        }
    }

    private ContactRepository(Context context) {
        if (null == context) throw new NullPointerException("null == context");
        this.appContext = context.getApplicationContext();
        this.contactsProvider = this.appContext.getContentResolver();
    }

    public List<ContactDisplay> loadContactDisplay() {
        List<ContactDisplay> contacts = new ArrayList<>();
        try {
            final ContentResolver contactsProvider = this.contactsProvider;
            Cursor c = contactsProvider.query(ContactsContract.Contacts.CONTENT_URI,
                    new String[]{
                            ContactsContract.Contacts._ID,
                            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                            ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,
                            ContactsContract.Contacts.HAS_PHONE_NUMBER
                    },
                    null, null,
                    ContactsContract.Contacts.SORT_KEY_PRIMARY+" ASC");
            if (null != c) {
                while (c.moveToNext()) {
                    ContactDisplay contact = ContactDisplay.create(c);
                    contacts.add(contact);
                }
                c.close();
            }
        }
        catch (Exception ex) {
            throw new RepositoryException("error in loading display contacts",ex);
        }
        return contacts;
    }

    public ContactDetails getContactDetails(long contactId) {
        try {
            ContentResolver resolver = this.contactsProvider;
            Cursor c = resolver.query(ContactsContract.Contacts.CONTENT_URI,
                    new String[]{
                            ContactsContract.Contacts._ID,
                            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                            ContactsContract.Contacts.PHOTO_URI
                    },
                    ContactsContract.Contacts._ID+" = "+contactId,null,
                    null);

            ContactDetails details = null;
            if (null != c) {
                if (c.moveToFirst()) details = ContactDetails.create(c);
                c.close();
            }

            if (null != details) {
                List<PhoneNumber> phoneNumbers = queryPhoneNumbers(contactId);

                details.setPhoneNumbers(phoneNumbers);
            }

            return details;
        }
        catch (Exception ex) {
            throw new RepositoryException("can not load contact details for contactId="+contactId,ex);
        }
    }

    private List<PhoneNumber> queryPhoneNumbers(long contactId) {
        try {
            ContentResolver resolver = this.contactsProvider;
            Cursor c = resolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                            ContactsContract.CommonDataKinds.Phone._ID,
                            ContactsContract.CommonDataKinds.Phone.NUMBER,
                            ContactsContract.CommonDataKinds.Phone.TYPE
                    },
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID+" = "+contactId,null,
                    null);

            List<PhoneNumber> phoneNumbers = new ArrayList<>();
            if (null != c) {
                while (c.moveToNext()) {
                    PhoneNumber phoneNumber = PhoneNumber.create(c);
                    phoneNumbers.add(phoneNumber);
                }
                c.close();
            }

            return phoneNumbers;
        }
        catch (Exception ex) {
            throw new RepositoryException("can not load PhoneNumber for contactId="+contactId,ex);
        }
    }
}
