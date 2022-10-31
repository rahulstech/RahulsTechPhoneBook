package rahulstech.android.phonebook.model;

import android.net.Uri;

import rahulstech.android.phonebook.util.Check;

public class Contact {
    
    private long id;
    
    private String lookupKey;
    
    private String displayName;
    
    private Uri photoUri;

    public Contact(long id, String lookupKey, String displayName, Uri photoUri) {
        this.id = id;
        this.lookupKey = lookupKey;
        this.displayName = displayName;
        this.photoUri = photoUri;
    }

    public long getId() {
        return id;
    }

    public String getLookupKey() {
        return lookupKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Uri getPhotoUri() {
        return photoUri;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Contact)) return false;
        Contact contact = (Contact) o;
        return id == contact.id
                && Check.isEquals(lookupKey, contact.lookupKey)
                && Check.isEquals(displayName, contact.displayName)
                && Check.isEquals(photoUri, contact.photoUri);
    }
}
