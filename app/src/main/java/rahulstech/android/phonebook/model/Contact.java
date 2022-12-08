package rahulstech.android.phonebook.model;

import android.net.Uri;
import android.provider.ContactsContract;

import androidx.annotation.Nullable;
import rahulstech.android.phonebook.util.Check;

public class Contact {
    
    private long id;
    
    private String lookupKey;

    @Nullable
    private Uri photoUri;

    private boolean starred;

    public Contact(String lookupKey, long id, @Nullable Uri photoUri, boolean starred) {
        this.lookupKey = lookupKey;
        this.id = id;
        this.photoUri = photoUri;
        this.starred = starred;
    }

    public long getId() {
        return id;
    }

    public String getLookupKey() {
        return lookupKey;
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
                && Check.isEquals(photoUri, contact.photoUri)
                && starred == contact.starred;
    }
}
