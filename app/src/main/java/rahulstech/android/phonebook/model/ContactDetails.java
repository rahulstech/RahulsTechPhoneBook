package rahulstech.android.phonebook.model;

import android.net.Uri;

import java.util.Collections;
import java.util.List;

import rahulstech.android.phonebook.util.Check;

public class ContactDetails {

    private Account account;

    private Contact contact;

    private PhoneNumber phoneNumberPrimary;

    private Email emailPrimary;

    private List<PhoneNumber> phoneNumbers = Collections.EMPTY_LIST;
    
    private List<Email> emails = Collections.EMPTY_LIST;


    public ContactDetails(Account account, Contact contact, List<PhoneNumber> phoneNumbers, List<Email> emails) {
        this.account = account;
        this.contact = contact;
        setPhoneNumbers(phoneNumbers);
        setEmails(emails);
    }

    public Account getAccount() {
        return account;
    }

    public Contact getContact() {
        return contact;
    }

    public String getDisplayName() {
        return contact.getDisplayName();
    }

    public Uri getPhotoUri() {
        return contact.getPhotoUri();
    }

    public List<PhoneNumber> getPhoneNumbers() {
        return phoneNumbers;
    }

    public void setPhoneNumbers(List<PhoneNumber> phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
        if (null == phoneNumbers) this.phoneNumbers = Collections.EMPTY_LIST;
        if (!this.phoneNumbers.isEmpty()) {
            this.phoneNumberPrimary = phoneNumbers.get(0);
            for (PhoneNumber pn : this.phoneNumbers) {
                if (pn.isPrimary()) {
                    this.phoneNumberPrimary = pn;
                    break;
                }
            }
        }
        else {
            this.phoneNumberPrimary = null;
        }
    }

    public boolean hasPhoneNumberPrimary() {
        return null != phoneNumberPrimary;
    }

    public PhoneNumber getPhoneNumberPrimary() {
        return phoneNumberPrimary;
    }

    public List<Email> getEmails() {
        return emails;
    }

    public void setEmails(List<Email> emails) {
        this.emails = emails;
        if (null == emails) this.emails = Collections.EMPTY_LIST;
        if (!this.emails.isEmpty()) {
            this.emailPrimary = this.emails.get(0);
            for (Email e : this.emails) {
                if (e.isPrimary()) {
                    this.emailPrimary = e;
                    break;
                }
            }
        }
        else {
            this.emailPrimary = null;
        }
    }

    public boolean hasEmailPrimary() {
        return this.emailPrimary != null;
    }

    public Email getEmailPrimary() {
        return emailPrimary;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContactDetails)) return false;
        ContactDetails details = (ContactDetails) o;
        return Check.isEquals(account,details.account)
                && Check.isEquals(contact, details.contact)
                && Check.isEquals(phoneNumbers, details.phoneNumbers)
                && Check.isEquals(emails, details.emails);
    }
}
