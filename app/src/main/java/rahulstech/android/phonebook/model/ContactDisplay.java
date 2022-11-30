package rahulstech.android.phonebook.model;

import android.net.Uri;
import android.provider.ContactsContract;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import rahulstech.android.phonebook.util.Check;

public class ContactDisplay {
    private Contact contact;

    private Name name;

    private PhoneNumber phoneNumberPrimary;

    @NonNull
    private List<PhoneNumber> phoneNumbers;

    public ContactDisplay(Contact contact) {
        this.contact = contact;
        this.phoneNumbers = new ArrayList<>();
    }

    public void setName(Name name) {
        this.name = name;
    }

    public Contact getContact() {
        return contact;
    }

    public long getContactId() {
        return contact.getId();
    }

    public String getDisplayName(boolean primary) {
        return primary ? contact.getDisplayNamePrimary() : contact.getDisplayNameAlternative();
    }

    public Name getName() {
        return name;
    }

    public Uri getThumbnailUri() {
        return contact.getPhotoUri();
    }

    public void addPhoneNumber(PhoneNumber number) {
        if (null != number) phoneNumbers.add(number);
    }

    public List<PhoneNumber> getPhoneNumbers() {
        return phoneNumbers;
    }

    public void setPhoneNumberPrimary(PhoneNumber number) {
        this.phoneNumberPrimary = number;
    }

    public boolean hasPhoneNumberPrimary() {
        return null != phoneNumberPrimary || 1==phoneNumbers.size();
    }

    public PhoneNumber getPhoneNumberPrimary() {
        if (null == phoneNumberPrimary && 1==phoneNumbers.size()) return phoneNumbers.get(0);
        return phoneNumberPrimary;
    }

    public Uri getContentUri() {
        return contact.getContentUri();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContactDisplay)) return false;
        ContactDisplay that = (ContactDisplay) o;
        return Check.isEquals(contact,that.contact)
                && Check.isEquals(name,that.name)
                && Check.isEquals(phoneNumberPrimary,that.phoneNumberPrimary)
                && Check.isEquals(phoneNumbers,that.phoneNumbers);
    }
}
