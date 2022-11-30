package rahulstech.android.phonebook.model;

import android.net.Uri;
import android.provider.ContactsContract;

import rahulstech.android.phonebook.util.Check;

public class Contact {
    
    private long id;
    
    private String lookupKey;

    private String displayNamePrimary;

    private String displayNameAlternative;

    private Uri photoUri;

    private boolean starred;

    public Contact(long id, String lookupKey, String displayNamePrimary, String displayNameAlternative, Uri photoUri, boolean starred) {
        this.id = id;
        this.lookupKey = lookupKey;
        this.displayNamePrimary = Check.isEmptyString(displayNamePrimary) ? Name.UNKNOWN_NAME.getDisplayName() :  displayNamePrimary;
        this.displayNameAlternative = Check.isEmptyString(displayNameAlternative) ? Name.UNKNOWN_NAME.getDisplayName() :  displayNameAlternative;
        this.photoUri = photoUri;
        this.starred = starred;
    }

    public long getId() {
        return id;
    }

    public String getLookupKey() {
        return lookupKey;
    }

    public String getDisplayNamePrimary() {
        return displayNamePrimary;
    }

    public String getDisplayNameAlternative() {
        return displayNameAlternative;
    }

    public String getDisplayName(boolean primary) {
         return primary ? displayNamePrimary : displayNameAlternative;
    }

    public Uri getPhotoUri() {
        return photoUri;
    }

    public boolean isStarred() {
        return starred;
    }

    public Uri getContentUri() {
        return ContactsContract.Contacts.getLookupUri(id,lookupKey);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Contact)) return false;
        Contact contact = (Contact) o;
        return id == contact.id
                && Check.isEquals(lookupKey, contact.lookupKey)
                && Check.isEquals(displayNamePrimary, contact.displayNamePrimary)
                && Check.isEquals(displayNameAlternative, contact.displayNameAlternative)
                && Check.isEquals(photoUri, contact.photoUri)
                && starred == contact.starred;
    }
}
