package rahulstech.android.phonebook.model;

import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import java.util.Objects;

public class ContactDisplay {

    private long contactId;

    private String displayName;

    private Uri thumbnailUri;

    private boolean hasPhoneNumber;

    public ContactDisplay(long contactId, String displayName, Uri thumbnailUri, boolean hasPhoneNumber) {
        this.contactId = contactId;
        this.displayName = displayName;
        this.thumbnailUri = thumbnailUri;
        this.hasPhoneNumber = hasPhoneNumber;
    }

    public long getContactId() {
        return contactId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Uri getThumbnailUri() {
        return thumbnailUri;
    }

    public boolean hasPhoneNumber() {
        return hasPhoneNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContactDisplay)) return false;
        ContactDisplay that = (ContactDisplay) o;
        return contactId == that.contactId && hasPhoneNumber == that.hasPhoneNumber && Objects.equals(displayName, that.displayName) && Objects.equals(thumbnailUri, that.thumbnailUri);
    }

    public static ContactDisplay create(Cursor c) {
        try {
            int _iID = c.getColumnIndexOrThrow(ContactsContract.Contacts._ID);
            int _iDisplayNamePrimary = c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY);
            int _iPhotoThumbUri = c.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI);
            int _iHasPhoneNumber = c.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER);

            long contactId = c.getLong(_iID);
            String displayName = c.getString(_iDisplayNamePrimary);
            Uri thumbnailUri = c.isNull(_iPhotoThumbUri) ? null : Uri.parse(c.getString(_iPhotoThumbUri));
            boolean hasPhoneNumber = 1 == c.getInt(_iHasPhoneNumber);

            ContactDisplay contact = new ContactDisplay(
                    contactId,
                    displayName,
                    thumbnailUri,
                    hasPhoneNumber
            );

            return contact;
        }
        catch (Exception ex) {
            throw new ModelException("can not create mode",ex);
        }
    }
}
