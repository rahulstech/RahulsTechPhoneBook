package rahulstech.android.phonebook.model;

import android.net.Uri;
import android.provider.ContactsContract;

import java.util.Collections;
import java.util.List;

import rahulstech.android.phonebook.util.Check;

public class ContactDisplay {
    // TODO: creating section by display name has problem, display name label may be used instead
    private Contact contact;

    private PhoneNumber phoneNumberPrimary;

    private List<PhoneNumber> phoneNumbers = Collections.EMPTY_LIST;

    public ContactDisplay(Contact contact, List<PhoneNumber> numbers) {
        this.contact = contact;
        setPhoneNumbers(numbers);
    }

    public Contact getContact() {
        return contact;
    }

    public long getContactId() {
        return contact.getId();
    }

    public String getDisplayName() {
        return contact.getDisplayName();
    }

    public Uri getThumbnailUri() {
        return contact.getPhotoUri();
    }

    public boolean hasPhoneNumber() {
        return !phoneNumbers.isEmpty();
    }

    public List<PhoneNumber> getPhoneNumbers() {
        return phoneNumbers;
    }

    public void setPhoneNumbers(List<PhoneNumber> phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
        if (null == phoneNumbers) this.phoneNumbers = Collections.EMPTY_LIST;
        List<PhoneNumber> numbers = this.phoneNumbers;
        PhoneNumber primary = null;
        if (numbers.size()==1) primary = numbers.get(0);
        else {
            for (PhoneNumber pn : numbers) {
                if (pn.isPrimary()) {
                    primary = pn;
                    break;
                }
            }
        }
        this.phoneNumberPrimary = primary;
    }

    public boolean hasPhoneNumberPrimary() {
        return null != phoneNumberPrimary;
    }

    public PhoneNumber getPhoneNumberPrimary() {
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
                && Check.isEquals(phoneNumbers,that.phoneNumbers);
    }
}
