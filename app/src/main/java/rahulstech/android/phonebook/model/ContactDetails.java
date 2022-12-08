package rahulstech.android.phonebook.model;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.ContactsContract;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import rahulstech.android.phonebook.util.Check;
import rahulstech.android.phonebook.util.ContactSorting;
import rahulstech.android.phonebook.util.DrawableUtil;
import rahulstech.android.phonebook.util.Helpers;

import static rahulstech.android.phonebook.util.Check.isAlphaString;
import static rahulstech.android.phonebook.util.Check.isEmptyString;
import static rahulstech.android.phonebook.util.Helpers.createContactPhotoPlaceholder;
import static rahulstech.android.phonebook.util.Helpers.firstNonEmptyString;

public class ContactDetails {

    private RawContact rawContact;

    private Contact contact;

    private long mainRawContactId;

    private List<RawContact> linkedRawContacts;

    @Nullable
    private String displayNameFirstNameFirst = null;

    @Nullable
    private String displayNameLastNameFirst = null;

    @Nullable
    private String sortKeyFirstNameFirst = null;

    @Nullable
    private String sortKeyLastNameFirst = null;

    @Nullable
    private String displayLabelFirstNameFirst = null;

    @Nullable
    private String displayLabelLastNameFirst = null;

    @Nullable
    private Name name;

    @Nullable
    private PhoneNumber phoneNumberPrimary;

    @Nullable
    private List<PhoneNumber> phoneNumbers;

    @Nullable
    private List<Email> emails;

    @Nullable
    private List<Event> events;

    @Nullable
    private List<Relation> relations;

    @Nullable
    private List<PostalAddress> addresses;

    @Nullable
    private Organization organization;

    @Nullable
    private List<Website> websites;

    @Nullable
    private Note note;

    @Deprecated
    public ContactDetails(RawContact rawContact, Contact contact) {
        this.rawContact = rawContact;
        this.contact = contact;
    }

    public ContactDetails(Contact contact) {
        this.contact = contact;
    }

    public void setRawContact(RawContact rawContact) {
        this.rawContact = rawContact;
    }

    public RawContact getRawContact() {
        return rawContact;
    }

    public List<RawContact> getLinkedRawContacts() {
        return linkedRawContacts;
    }

    public void setLinkedRawContacts(List<RawContact> linkedRawContacts) {
        this.linkedRawContacts = linkedRawContacts;
    }

    public Contact getContact() {
        return contact;
    }

    public long getContactId() { return null == contact ? 0 : contact.getId(); }

    @Nullable
    public Name getName() {
        return name;
    }

    public void setName(@Nullable Name name) {
        this.name = name;
    }

    public void buildNameDependentValues() {
        if (null != name) {
            displayNameFirstNameFirst = name.buildDisplayName(ContactSorting.FIRSTNAME_FIRST);
            displayNameLastNameFirst = name.buildDisplayName(ContactSorting.LASTNAME_FIRST);
            sortKeyFirstNameFirst = buildSortKey(name,ContactSorting.FIRSTNAME_FIRST);
            sortKeyLastNameFirst = buildSortKey(name,ContactSorting.LASTNAME_FIRST);
            displayLabelFirstNameFirst = buildDisplayLabel(name,ContactSorting.FIRSTNAME_FIRST);
            displayLabelLastNameFirst = buildDisplayLabel(name,ContactSorting.LASTNAME_FIRST);
        }
    }

    static String buildSortKey(@Nullable Name name, ContactSorting sorting) {
        if (null == name) return null;
        String sortKey;
        if (sorting.isDisplayFirstNameFirst()) {
            sortKey = firstNonEmptyString(name.getGivenName(),name.getFamilyName(),
                    name.getMiddleName(),name.getSuffix(),name.getPrefix());
        }
        else {
            sortKey = firstNonEmptyString(name.getFamilyName(),name.getGivenName(),
                    name.getMiddleName(),name.getSuffix(),name.getPrefix());
        }
        if (isEmptyString(sortKey)) return null;
        if (!isAlphaString(sortKey.substring(0,1))) return null;
        return sortKey;
    }

    static String buildDisplayLabel(@Nullable Name name, ContactSorting sorting) {
        if (null == name) return null;
        String displayName = name.buildDisplayName(sorting);
        if (isEmptyString(displayName)) return null;
        String label = displayName.substring(0,1);
        if (!isAlphaString(label)) return null;
        label = label.toUpperCase();
        return label;
    }

    /**
     * Returns names to be display. The returned value must be used for display
     * purpose only not any kind of computation purpose like sorting or creating
     * display label.
     * @return contact display name
     * @see Name#getDisplayName()
     * @see Name#UNKNOWN_NAME
     */
    @NonNull
    public String getDisplayName(@NonNull ContactSorting sorting) {
        final String displayName = sorting.isDisplayFirstNameFirst() ? displayNameFirstNameFirst : displayNameLastNameFirst;
        return isEmptyString(displayName) ? Name.UNKNOWN_NAME.getDisplayName() : displayName;
    }

    /**
     * Returns the string to use sorting contacts. Depending upon the
     * sorting it decides the best suitable sorting key.
     * It may return null if no alternative found.
     *
     * @param sorting the {@link ContactSorting} to decide the key
     * @return
     */
    @Nullable
    public String getSortingKey(@NonNull ContactSorting sorting) {
        return sorting.isDisplayFirstNameFirst() ? sortKeyFirstNameFirst : sortKeyLastNameFirst;
    }

    /**
     *
     * @return
     */
    @Nullable
    public String getDisplayLabel(@NonNull ContactSorting sorting) {
        final String label = sorting.isDisplayFirstNameFirst() ? displayLabelFirstNameFirst : displayLabelLastNameFirst;
        return label;
    }

    public Uri getPhotoUri() {
        return contact.getPhotoUri();
    }

    @Nullable
    public List<PhoneNumber> getPhoneNumbers() {
        return phoneNumbers;
    }

    public boolean hasPhoneNumberPrimary() {
        return null!=phoneNumberPrimary || (null!=phoneNumbers && 1==phoneNumbers.size());
    }

    @Nullable
    public PhoneNumber getPhoneNumberPrimary() {
        if (null != phoneNumberPrimary) return phoneNumberPrimary;
        if (hasPhoneNumberPrimary()) return phoneNumbers.get(0);
        return null;
    }

    public void setPhoneNumberPrimary(@Nullable PhoneNumber number) {
        this.phoneNumberPrimary = number;
    }

    public void setPhoneNumbers(@Nullable List<PhoneNumber> phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
    }

    @Nullable
    public List<Email> getEmails() {
        return emails;
    }

    public void setEmails(@Nullable List<Email> emails) {
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
    public List<Relation> getRelations() {
        return relations;
    }

    public void setRelations(List<Relation> relations) {
        this.relations = relations;
    }

    public void setAddresses(@Nullable List<PostalAddress> addresses) {
        this.addresses = addresses;
    }

    @Nullable
    public List<PostalAddress> getAddresses() {
        return addresses;
    }

    @Nullable
    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(@Nullable Organization organization) {
        this.organization = organization;
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
        return ContactsContract.Contacts.CONTENT_URI.buildUpon()
                .appendPath(String.valueOf(getContactId())).build();
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
                && Check.isEquals(relations,details.relations)
                && Check.isEquals(addresses,details.addresses)
                && Check.isEquals(organization,details.organization)
                && Check.isEquals(websites,details.websites)
                && Check.isEquals(note,note);
    }
}
