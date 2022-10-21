package rahulstech.android.phonebook.model;

import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import java.util.Collections;
import java.util.List;

public class ContactDetails {

    private long contactId;

    private String displayName;

    private Uri photoUri;

    private List<PhoneNumber> phoneNumbers = Collections.emptyList();

    private List<Email> emails = Collections.emptyList();

    public ContactDetails(long contactId, String displayName, Uri photoUri) {
        this.contactId = contactId;
        this.displayName = displayName;
        this.photoUri = photoUri;
    }

    public long getContactId() {
        return contactId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Uri getPhotoUri() {
        return photoUri;
    }

    public List<PhoneNumber> getPhoneNumbers() {
        return phoneNumbers;
    }

    public void setPhoneNumbers(List<PhoneNumber> phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
    }

    public List<Email> getEmails() {
        return emails;
    }

    public void setEmails(List<Email> emails) {
        this.emails = emails;
    }

    public static ContactDetails create(Cursor c) {
        try {
            int _iContactId = c.getColumnIndexOrThrow(ContactsContract.Contacts._ID);
            int _iDisplayName = c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY);
            int _iPhotoUri = c.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI);

            long contactId = c.getLong(_iContactId);
            String displayName = c.getString(_iDisplayName);
            Uri photoUri = c.isNull(_iPhotoUri) ? null : Uri.parse(c.getString(_iPhotoUri));

            return new ContactDetails(contactId,displayName,photoUri);
        }
        catch (Exception ex) {
            throw new ModelException("can not create ContactDetails",ex);
        }
    }
}
