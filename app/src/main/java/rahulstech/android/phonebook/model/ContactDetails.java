package rahulstech.android.phonebook.model;

import android.net.Uri;

import java.util.List;

import androidx.annotation.Nullable;
import rahulstech.android.phonebook.util.Check;

public class ContactDetails {

    private RawContact rawContact;

    private Contact contact;

    private Name name;

    @Nullable
    private List<PhoneNumber> phoneNumbers;

    @Nullable
    private List<Email> emails;

    @Nullable
    private List<Event> events;

    @Nullable
    private List<Relation> relatives;

    @Nullable
    private List<PostalAddress> addresses;

    @Nullable
    private List<Organization> organizations;

    @Nullable
    private List<Website> websites;

    @Nullable
    private Note note;

    public ContactDetails(RawContact rawContact, Contact contact) {
        this.rawContact = rawContact;
        this.contact = contact;
    }

    public RawContact getAccount() {
        return rawContact;
    }

    public Contact getContact() {
        return contact;
    }

    public Name getName() {
        return name;
    }

    public void setName(Name name) {
        this.name = name;
    }

    public String getDisplayName() {
        return contact.getDisplayName();
    }

    public Uri getPhotoUri() {
        return contact.getPhotoUri();
    }

    @Nullable
    public List<PhoneNumber> getPhoneNumbers() {
        return phoneNumbers;
    }

    public void setPhoneNumbers(List<PhoneNumber> phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
    }

    @Nullable
    public List<Email> getEmails() {
        return emails;
    }

    public void setEmails(List<Email> emails) {
        this.emails = emails;
    }

    @Nullable
    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }

    @Nullable
    public List<Relation> getRelatives() {
        return relatives;
    }

    public void setRelatives(List<Relation> relatives) {
        this.relatives = relatives;
    }

    public void setAddresses(@Nullable List<PostalAddress> addresses) {
        this.addresses = addresses;
    }

    @Nullable
    public List<PostalAddress> getAddresses() {
        return addresses;
    }

    @Nullable
    public List<Organization> getOrganizations() {
        return organizations;
    }

    public void setOrganizations(@Nullable List<Organization> organizations) {
        this.organizations = organizations;
    }

    @Nullable
    public List<Website> getWebsites() {
        return websites;
    }

    public void setWebsites(@Nullable List<Website> websites) {
        this.websites = websites;
    }

    public void setNote(Note note) {
        this.note = note;
    }

    @Nullable
    public Note getNote() {
        return note;
    }

    public Uri getContentUri() {
        return contact.getContentUri();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContactDetails)) return false;
        ContactDetails details = (ContactDetails) o;
        return Check.isEquals(rawContact,details.rawContact)
                && Check.isEquals(contact, details.contact)
                && Check.isEquals(name,details.name)
                && Check.isEquals(phoneNumbers, details.phoneNumbers)
                && Check.isEquals(emails, details.emails)
                && Check.isEquals(events,details.events)
                && Check.isEquals(relatives,details.relatives)
                && Check.isEquals(addresses,details.addresses)
                && Check.isEquals(organizations,details.organizations)
                && Check.isEquals(websites,details.websites)
                && Check.isEquals(note,note);
    }
}
