package rahulstech.android.phonebook.repository;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncAdapterType;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import rahulstech.android.phonebook.model.Contact;
import rahulstech.android.phonebook.model.ContactAccount;
import rahulstech.android.phonebook.model.ContactDetails;
import rahulstech.android.phonebook.model.ContactDisplay;
import rahulstech.android.phonebook.model.Email;
import rahulstech.android.phonebook.model.Event;
import rahulstech.android.phonebook.model.Name;
import rahulstech.android.phonebook.model.Note;
import rahulstech.android.phonebook.model.Organization;
import rahulstech.android.phonebook.model.PhoneNumber;
import rahulstech.android.phonebook.model.PostalAddress;
import rahulstech.android.phonebook.model.RawContact;
import rahulstech.android.phonebook.model.Relation;
import rahulstech.android.phonebook.model.Website;
import rahulstech.android.phonebook.util.Check;
import rahulstech.android.phonebook.util.ContactSorting;

import static rahulstech.android.phonebook.util.Helpers.logDebug;

public class ContactRepository {

    private static final String TAG = "ContactRepository";

    private static final Object lock = new Object();

    private static final String[][] ALLOWED_CONTACT_ACCOUNT_TYPE = new String[][] {
      new String[]{"com.google","Google"}
    };

    private Context appContext;
    private ContentResolver contactsProvider;

    private static ContactRepository instance = null;

    public static ContactRepository get(Context context) {
        synchronized (lock) {
            if (null == instance) {
                instance = new ContactRepository(context);
            }
            return instance;
        }
    }

    public Context getContext() {
        return appContext;
    }

    private ContactRepository(@NonNull Context context) {
        Check.isNonNull(context,"null == context");
        this.appContext = context.getApplicationContext();
        this.contactsProvider = this.appContext.getContentResolver();
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///                                          API Methods                                              ///
    ////////////////////////////////////////////////////////////////////////////////////////////////////////

    public List<ContactAccount> getAccounts() {
        // TODO: list of accounts for adding contact not loaded
        // for example there may be whatsapp and google account,
        // but this method must return google accounts only
        Set<Account> accounts = new HashSet<>();
        AccountManager am = AccountManager.get(appContext);
        try {
            SyncAdapterType[] types = ContentResolver.getSyncAdapterTypes();
            for (SyncAdapterType t : types) {
                if (!ContactsContract.AUTHORITY.equals(t.authority) || !t.supportsUploading()) continue;
                Account[] found = am.getAccountsByType(t.accountType);
                for (Account account : found) {
                    accounts.add(account);
                }
            }
        }
        catch (Exception ex) {
            Log.e(TAG,null,ex);
            accounts.clear();
        }

        ArrayList<ContactAccount> contactAccounts = new ArrayList<>();
        // TODO: add device as contact account
        for (Account account : accounts) {
            String type = account.type;
            for (String[] allowed : ALLOWED_CONTACT_ACCOUNT_TYPE) {
                String allowedType = allowed[0];
                String displayName = allowed[1];
                if (0==allowedType.compareTo(type)) {
                    contactAccounts.add(new ContactAccount(displayName,account.name,account.type));
                }
            }
        }

        return contactAccounts;
    }

    @Nullable
    public ContactDetails getContactDetails(@NonNull Uri uri) {
        Cursor c = null;
        try {
            final Uri contactUri = getLookupUri(uri);
            if (null == contactUri) return null;
            final String key = contactUri.getLastPathSegment();

            ContentResolver resolver = this.contactsProvider;
            c = resolver.query(ContactsContract.Data.CONTENT_URI,
                    null,
                    ContactsContract.Data.CONTACT_ID+" = ? OR "+ContactsContract.Data.LOOKUP_KEY+" = ?",
                    new String[]{key,key},
                    null);
            if (null != c && c.moveToFirst()) {
                ContactDetails details = new ContactDetails(newContact(c));
                String nickname = null;
                ArrayList<PhoneNumber> numbers = new ArrayList<>();
                ArrayList<Email> emails = new ArrayList<>();
                ArrayList<Event> events = new ArrayList<>();
                ArrayList<Relation> relations = new ArrayList<>();
                ArrayList<PostalAddress> addresses = new ArrayList<>();
                ArrayList<Website> websites = new ArrayList<>();

                do {
                    String mime = c.getString(c.getColumnIndexOrThrow(ContactsContract.Data.MIMETYPE));
                    switch (mime) {
                        case ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE: {
                            Name name = newName(c);
                            name.setNickname(nickname);
                            details.setName(name);
                        }
                        break;
                        case ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE: {
                            nickname = c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Nickname.NAME));
                            Name name = details.getName();
                            if (null != name) name.setNickname(nickname);
                        }
                        break;
                        case ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE: {
                            numbers.add(newPhoneNumber(c));
                        }
                        break;
                        case ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE: {
                            emails.add(newEmail(c));
                        }
                        break;
                        case ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE: {
                            events.add(newEvent(c));
                        }
                        break;
                        case ContactsContract.CommonDataKinds.Relation.CONTENT_ITEM_TYPE: {
                            relations.add(newRelation(c));
                        }
                        break;
                        case ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE: {
                            details.setNote(newNote(c));
                        }
                        break;
                        case ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE: {
                            details.setOrganization(newOrganization(c));
                        }
                        break;
                        case ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE: {
                            addresses.add(newPostalAddress(c));
                        }
                        break;
                        case ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE: {
                            websites.add(newWebsite(c));
                        }
                    }
                }
                while (c.moveToNext());
                details.setPhoneNumbers(numbers);
                details.setEmails(emails);
                details.setEvents(events);
                details.setRelations(relations);
                details.setAddresses(addresses);
                details.setWebsites(websites);

                closeCursorSilently(c);
                c = null;

                details.setRawContact(loadRawContact(key));

                return details;
            }
        }
        catch (Exception ex) {
            throw new RepositoryException("can not load contact details",ex);
        }
        finally {
            closeCursorSilently(c);
        }
        return null;
    }

    public boolean setContactStarred(@NonNull Contact contact, boolean starred) {
        ContentValues values = new ContentValues();
        values.put(ContactsContract.Contacts.STARRED,starred ? 1 : 0);
        return 1 == updateContact(contact.getId(),values);
    }

    public int updateContact(long id, ContentValues values) {
        try {
            ContentResolver resolver = this.contactsProvider;
            return resolver.update(ContactsContract.Contacts.CONTENT_URI,values,ContactsContract.Contacts._ID+" = "+id,null);
        }
        catch (Exception ex) {
            throw new RepositoryException("contact not updated",ex);
        }
    }

    public boolean changePhoneNumberPrimary(@NonNull PhoneNumber number, boolean setPrimary) {
        Check.isNonNull(number,"null == number");
        if (number.isPrimary() == setPrimary) return true;
        ContentValues cvs = new ContentValues();
        cvs.put(ContactsContract.Data.IS_SUPER_PRIMARY, setPrimary ? 1 : 0);
        return 1==updateContactData(number.getId(),cvs);
    }

    public int updateContactData(long id, ContentValues values) {
        try {
            ContentResolver resolver = this.contactsProvider;
            return resolver.update(ContactsContract.Data.CONTENT_URI,values,ContactsContract.Data._ID+" = "+id,null);
        }
        catch (Exception ex) {
            throw new RepositoryException("contact data not updated",ex);
        }
    }

    public boolean updateContactDetails(@NonNull Contact contact, @NonNull RawContact rawContact,
                                        @NonNull ContactDetails toDelete, @NonNull ContactDetails toUpdate, @NonNull ContactDetails toInsert) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        int[] deleteIdx = new int[2];
        int[] updateIdx = new int[2];
        int[] insertIdx = new int[2];

        deleteIdx[0] = 0;
        if (null != toDelete.getPhoneNumbers()) {
            for (PhoneNumber data : toDelete.getPhoneNumbers()) {
                ops.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                                .withSelection(ContactsContract.Data._ID+" = "+data.getId(),null)
                        .build());
            }
        }
         if (null != toDelete.getEmails()) {
            for (Email data : toDelete.getEmails()) {
                ops.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection(ContactsContract.Data._ID+" = "+data.getId(),null)
                        .build());
            }
        }
        if (null != toDelete.getEvents()) {
            for (Event data : toDelete.getEvents()) {
                ops.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection(ContactsContract.Data._ID+" = "+data.getId(),null)
                        .build());
            }
        }
        if (null != toDelete.getRelations()) {
            for (Relation data : toDelete.getRelations()) {
                ops.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection(ContactsContract.Data._ID+" = "+data.getId(),null)
                        .build());
            }
        }
        if (null != toDelete.getAddresses()) {
            for (PostalAddress data : toDelete.getAddresses()) {
                ops.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection(ContactsContract.Data._ID+" = "+data.getId(),null)
                        .build());
            }
        }
        if (null != toDelete.getWebsites()) {
            for (Website data : toDelete.getWebsites()) {
                ops.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection(ContactsContract.Data._ID+" = "+data.getId(),null)
                        .build());
            }
        }
        if (null != toDelete.getOrganization()) {
            ops.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                    .withSelection(ContactsContract.Data._ID+" = "+toDelete.getOrganization().getId(),null)
                    .build());
        }
        if (null != toDelete.getNote()) {
            ops.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                    .withSelection(ContactsContract.Data._ID+" = "+toDelete.getNote().getId(),null)
                    .build());
        }
        deleteIdx[1] = ops.size()-1;

        updateIdx[0] = ops.size();
        if (null != toUpdate.getName()) {
            Name name = toUpdate.getName();
            ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name.getDisplayName())
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.PREFIX, name.getPrefix())
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, name.getGivenName())
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME, name.getMiddleName())
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, name.getFamilyName())
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.SUFFIX, name.getSuffix())
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_GIVEN_NAME, name.getPhoneticGivenName())
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_MIDDLE_NAME, name.getPhoneticMiddleName())
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_FAMILY_NAME, name.getPhoneticFamilyName())
                    .withSelection(ContactsContract.Data._ID+" = "+name.getId(),null)
                    .build());
        }
        if (null != toUpdate.getPhoneNumbers()) {
            for (PhoneNumber data : toUpdate.getPhoneNumbers()) {
                ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, data.getNumber())
                        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, data.getType())
                        .withValue(ContactsContract.CommonDataKinds.Phone.LABEL, data.getTypeLabel())
                                .withSelection(ContactsContract.Data._ID+" = "+data.getId(),null)
                        .build());
            }
        }
        if (null != toUpdate.getEmails()) {
            for (Email data : toUpdate.getEmails()) {
                ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, data.getAddress())
                        .withValue(ContactsContract.CommonDataKinds.Email.TYPE, data.getType())
                        .withValue(ContactsContract.CommonDataKinds.Email.LABEL, data.getTypeLabel())
                        .withSelection(ContactsContract.Data._ID+" = "+data.getId(),null)
                        .build());
            }
        }
        if (null != toUpdate.getRelations()) {
            for (Relation data : toUpdate.getRelations()) {
                ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.CommonDataKinds.Relation.NAME, data.getDisplayName())
                        .withValue(ContactsContract.CommonDataKinds.Relation.TYPE, data.getType())
                        .withValue(ContactsContract.CommonDataKinds.Relation.LABEL, data.getTypeLabel())
                        .withSelection(ContactsContract.Data._ID+" = "+data.getId(),null)
                        .build());
            }
        }if (null != toUpdate.getEvents()) {
            for (Event data : toUpdate.getEvents()) {
                ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.CommonDataKinds.Event.START_DATE, data.getStartDate())
                        .withValue(ContactsContract.CommonDataKinds.Event.TYPE, data.getType())
                        .withValue(ContactsContract.CommonDataKinds.Event.LABEL, data.getTypeLabel())
                        .withSelection(ContactsContract.Data._ID+" = "+data.getId(),null)
                        .build());
            }
        }
        if (null != toUpdate.getAddresses()) {
            for (PostalAddress data : toUpdate.getAddresses()) {
                ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, data.getFormattedAddress())
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, data.getType())
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.LABEL, data.getTypeLabel())
                        .withSelection(ContactsContract.Data._ID+" = "+data.getId(),null)
                        .build());
            }
        }
        if (null != toUpdate.getWebsites()) {
            for (Website data : toUpdate.getWebsites()) {
                ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.CommonDataKinds.Website.URL, data.getUrl())
                        .withSelection(ContactsContract.Data._ID+" = "+data.getId(),null)
                        .build());
            }
        }
        if (null != toUpdate.getOrganization()) {
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValue(ContactsContract.CommonDataKinds.Organization.COMPANY,toUpdate.getOrganization().getCompany())
                    .withValue(ContactsContract.CommonDataKinds.Organization.TITLE,toUpdate.getOrganization().getTitle())
                    .withValue(ContactsContract.CommonDataKinds.Organization.DEPARTMENT,toUpdate.getOrganization().getDepartment())
                    .build());
        }
        updateIdx[1] = ops.size()-1;

        long rawContactId = rawContact.getId();
        insertIdx[0] = ops.size();
        /*if (null != toInsert.getName()) {
            Name name = toInsert.getName();
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.RAW_CONTACT_ID, rawContactId)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name.getDisplayName())
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.PREFIX, name.getPrefix())
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, name.getGivenName())
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME, name.getMiddleName())
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, name.getFamilyName())
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.SUFFIX, name.getSuffix())
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_GIVEN_NAME, name.getPhoneticGivenName())
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_MIDDLE_NAME, name.getPhoneticMiddleName())
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_FAMILY_NAME, name.getPhoneticFamilyName())
                    .build());
        }*/
        if (null != toInsert.getPhoneNumbers()) {
            for (PhoneNumber data : toInsert.getPhoneNumbers()) {
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, data.getNumber())
                        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, data.getType())
                        .withValue(ContactsContract.CommonDataKinds.Phone.LABEL, data.getTypeLabel())
                        .build());
            }
        }
        if (null != toInsert.getEmails()) {
            for (Email email : toInsert.getEmails()) {
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email.getAddress())
                        .withValue(ContactsContract.CommonDataKinds.Email.TYPE, email.getType())
                        .withValue(ContactsContract.CommonDataKinds.Email.LABEL, email.getTypeLabel())
                        .build());
            }
        }
        if (null != toInsert.getEvents()) {
            for (Event event : toInsert.getEvents()) {
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Event.START_DATE, event.getStartDate())
                        .withValue(ContactsContract.CommonDataKinds.Event.TYPE, event.getType())
                        .withValue(ContactsContract.CommonDataKinds.Event.LABEL, event.getTypeLabel())
                        .build());
            }
        }
        if (null != toInsert.getAddresses()) {
            for (PostalAddress address : toInsert.getAddresses()) {
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, address.getFormattedAddress())
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, address.getType())
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.LABEL, address.getTypeLabel())
                        .build());
            }
        }
        if (null != toInsert.getRelations()) {
            for (Relation relation : toInsert.getRelations()) {
                logDebug(TAG,"updateContactDetails: insert relation="+relation);
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Relation.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Relation.NAME, relation.getDisplayName())
                        .withValue(ContactsContract.CommonDataKinds.Relation.TYPE, relation.getType())
                        .withValue(ContactsContract.CommonDataKinds.Relation.LABEL, relation.getTypeLabel())
                        .build());
            }
        }
        if (null != toInsert.getOrganization()) {
            Organization organization = toInsert.getOrganization();
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValue(ContactsContract.Data.RAW_CONTACT_ID,rawContactId)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Organization.COMPANY,organization.getCompany())
                    .withValue(ContactsContract.CommonDataKinds.Organization.TITLE,organization.getTitle())
                    .withValue(ContactsContract.CommonDataKinds.Organization.DEPARTMENT,organization.getDepartment())
                    .withValue(ContactsContract.CommonDataKinds.Organization.JOB_DESCRIPTION,null)
                    .withValue(ContactsContract.CommonDataKinds.Organization.OFFICE_LOCATION,null)
                    .withValue(ContactsContract.CommonDataKinds.Organization.PHONETIC_NAME,null)
                    .withValue(ContactsContract.CommonDataKinds.Organization.PHONETIC_NAME_STYLE,null)
                    .withValue(ContactsContract.CommonDataKinds.Organization.SYMBOL,null)
                    .withValue(ContactsContract.CommonDataKinds.Organization.TYPE, ContactsContract.CommonDataKinds.StructuredPostal.TYPE_OTHER)
                    .withValue(ContactsContract.CommonDataKinds.Organization.LABEL,null)
                    .build());
        }
        if (null != toInsert.getWebsites()) {
            for (Website website : toInsert.getWebsites()) {
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Website.URL, website.getUrl())
                        .withValue(ContactsContract.CommonDataKinds.Website.TYPE, ContactsContract.CommonDataKinds.Website.TYPE_OTHER)
                        .withValue(ContactsContract.CommonDataKinds.Website.LABEL, null)
                        .build());
            }
        }
        if (null != toInsert.getNote()) {
            Note note = toInsert.getNote();
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValue(ContactsContract.Data.RAW_CONTACT_ID,rawContactId)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Note.NOTE,note.getNote())
                    .build());
        }
        insertIdx[1] = ops.size()-1;

        try {
            ContentResolver resolver = this.contactsProvider;
            logDebug(TAG,"total operations: "+ops.size());
            ContentProviderResult[] results = resolver.applyBatch(ContactsContract.AUTHORITY,ops);
            for (int i=0; i<results.length; i++) {
                ContentProviderResult result = results[i];
                if (i>=deleteIdx[0] && i<=deleteIdx[1]) {
                    if (result.count != 1) throw new RepositoryException("a delete operation @ "+i+" not executed successfully");
                }
                else if (i>=updateIdx[0] && i<=updateIdx[1]) {
                    if (result.count != 1) throw new RepositoryException("a update operation @ "+i+" not executed successfully");
                }
                else if (i>=insertIdx[0] && i<=insertIdx[1]) {
                    if (result.uri == null) throw new RepositoryException("a insert operation @ "+i+" not executed successfully");
                }
            }
            return true;
        }
        catch (RepositoryException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new RepositoryException("can not update contact details",ex);
        }
    }

    public boolean insertContactDetails(@NonNull ContactDetails details) {
        Check.isNonNull(details,"null == details");

        RawContact rawContact = details.getRawContact();

        Check.isNonNull(rawContact,"no account selected to insert new account");

        Name name = details.getName();
        List<PhoneNumber> numbers = details.getPhoneNumbers();
        List<Email> emails = details.getEmails();
        List<Event> events = details.getEvents();
        List<Relation> relations = details.getRelations();
        List<PostalAddress> addresses = details.getAddresses();
        Organization organization = details.getOrganization();
        List<Website> websites = details.getWebsites();
        Note note = details.getNote();

        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME,rawContact.getName())
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE,rawContact.getType())
                .build());
        if (null != name) {
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.CommonDataKinds.StructuredName.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name.getDisplayName())
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.PREFIX, name.getPrefix())
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, name.getGivenName())
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME, name.getMiddleName())
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, name.getFamilyName())
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.SUFFIX, name.getSuffix())
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_GIVEN_NAME, name.getPhoneticGivenName())
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_MIDDLE_NAME, name.getPhoneticMiddleName())
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_FAMILY_NAME, name.getPhoneticFamilyName())
                    .build());
        }
        if (null != numbers) {
            for (PhoneNumber number : numbers) {
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, number.getNumber())
                        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, number.getType())
                        .withValue(ContactsContract.CommonDataKinds.Phone.LABEL, number.getTypeLabel())
                        .build());
            }
        }
        if (null != emails) {
            for (Email email : emails) {
                ops.add(ContentProviderOperation.newInsert(ContactsContract.CommonDataKinds.Email.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email.getAddress())
                        .withValue(ContactsContract.CommonDataKinds.Email.TYPE, email.getType())
                        .withValue(ContactsContract.CommonDataKinds.Email.LABEL, email.getTypeLabel())
                        .build());
            }
        }
        if (null != events) {
            for (Event event : events) {
                logDebug(TAG,"insertContactDetails: event="+event);
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Event.START_DATE, event.getStartDate())
                        .withValue(ContactsContract.CommonDataKinds.Event.TYPE, event.getType())
                        .withValue(ContactsContract.CommonDataKinds.Event.LABEL, event.getTypeLabel())
                        .build());
            }
        }
        if (null != addresses) {
            for (PostalAddress address : addresses) {
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, address.getFormattedAddress())
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, address.getType())
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.LABEL, address.getTypeLabel())
                        .build());
            }
        }
        if (null != relations) {
            for (Relation relation : relations) {
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Relation.NAME, relation.getDisplayName())
                        .withValue(ContactsContract.CommonDataKinds.Relation.TYPE, relation.getType())
                        .withValue(ContactsContract.CommonDataKinds.Relation.LABEL, relation.getTypeLabel())
                        .build());
            }
        }
        if (null != organization) {
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID,0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Organization.COMPANY,organization.getCompany())
                    .withValue(ContactsContract.CommonDataKinds.Organization.TITLE,organization.getTitle())
                    .withValue(ContactsContract.CommonDataKinds.Organization.DEPARTMENT,organization.getDepartment())
                    .withValue(ContactsContract.CommonDataKinds.Organization.JOB_DESCRIPTION,null)
                    .withValue(ContactsContract.CommonDataKinds.Organization.OFFICE_LOCATION,null)
                    .withValue(ContactsContract.CommonDataKinds.Organization.PHONETIC_NAME,null)
                    .withValue(ContactsContract.CommonDataKinds.Organization.PHONETIC_NAME_STYLE,null)
                    .withValue(ContactsContract.CommonDataKinds.Organization.SYMBOL,null)
                    .withValue(ContactsContract.CommonDataKinds.Organization.TYPE, ContactsContract.CommonDataKinds.StructuredPostal.TYPE_OTHER)
                    .withValue(ContactsContract.CommonDataKinds.Organization.LABEL,null)
                    .build());
        }
        if (null != websites) {
            for (Website website : websites) {
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Website.URL, website.getUrl())
                        .withValue(ContactsContract.CommonDataKinds.Website.TYPE, ContactsContract.CommonDataKinds.Website.TYPE_OTHER)
                        .withValue(ContactsContract.CommonDataKinds.Website.LABEL, null)
                        .build());
            }
        }
        if (null != note && Check.isEmptyString(note.getNote())) {
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID,0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Note.NOTE,note.getNote())
                    .build());
        }

        try {
            ContentResolver resolver = this.contactsProvider;
            ContentProviderResult[] results = resolver.applyBatch(ContactsContract.AUTHORITY,ops);
            for (ContentProviderResult result : results) {
                if (null == result.uri) throw new RepositoryException("one of the operation not executed");
            }
            return true;
        }
        catch (RepositoryException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new RepositoryException("can not insert new contact",ex);
        }
    }

    public boolean deleteContact(@NonNull Contact contact) {
        Check.isNonNull(contact,"null == contact");
        try {
            ContentResolver resolver = this.contactsProvider;
            Uri uri = ContactsContract.Contacts.CONTENT_LOOKUP_URI.buildUpon().appendPath(contact.getLookupKey()).build();
            return 1==resolver.delete(uri,null,null);
        }
        catch (Exception ex) {
            throw new RepositoryException("can not delete contact",ex);
        }
    }

    public RawContact loadRawContact(String key) {
        Cursor c = null;
        try {
            ContentResolver resolver = this.contactsProvider;

            c = resolver.query(ContactsContract.Contacts.CONTENT_URI,
                    new String[]{ContactsContract.Contacts.NAME_RAW_CONTACT_ID},
                    "("+ContactsContract.Contacts.LOOKUP_KEY+" = ? OR "+ContactsContract.Contacts._ID+" = ?)",
                    new String[]{key,key},null);

            long id = -1;
            if (null != c) {
                if (c.moveToFirst()) {
                    id = c.getLong(0);
                }
                c.close();
                c = null;
            }

            if (id == -1) return null;

            c = resolver.query(ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI,id),
                    new String[]{
                            ContactsContract.RawContacts._ID,
                            ContactsContract.RawContacts.ACCOUNT_NAME,
                            ContactsContract.RawContacts.ACCOUNT_TYPE
                    },
                    null,null,null);
            if (null != c && c.moveToFirst()) {
                return new RawContact(key,id,
                        c.getString(c.getColumnIndexOrThrow(ContactsContract.RawContacts.ACCOUNT_NAME)),
                        c.getString(c.getColumnIndexOrThrow(ContactsContract.RawContacts.ACCOUNT_TYPE)));
            }
        }
        catch (Exception ex) {
            throw new RepositoryException("can not load Account",ex);
        }
        finally {
            if (null != c) c.close();
        }
        return null;
    }

    @Nullable
    public List<ContactDisplay> loadContactDisplay(@NonNull ContactAccount account, @NonNull ContactSorting sorting) {
        Check.isNonNull(account,"null == account");
        Check.isNonNull(sorting,"null == sorting");

        ArrayList<ContactDisplay> displays = new ArrayList<>();
        Cursor c = null;
        try {
            String selectionContacts, selectionData;
            if (account == ContactAccount.ALL) {
                selectionContacts = null;
                selectionData = "("+ContactsContract.Data.MIMETYPE+" = \""+ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE+"\" OR "+
                        ContactsContract.Data.MIMETYPE+" = \""+ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE+"\") AND "+
                        ContactsContract.Data.RAW_CONTACT_ID+" = "+ContactsContract.Data.NAME_RAW_CONTACT_ID;
            }
            else {
                long rawContactId = getRawContactId(account.type,account.name);
                selectionContacts = ContactsContract.Contacts.NAME_RAW_CONTACT_ID+" = "+rawContactId;
                selectionData = "("+ContactsContract.Data.MIMETYPE+" = \""+ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE+"\" OR "+
                        ContactsContract.Data.MIMETYPE+" = \""+ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE+"\") AND "+
                        ContactsContract.Data.RAW_CONTACT_ID+" = "+rawContactId;
            }

            ContentResolver resolver = this.contactsProvider;

            Map<Long,ContactDisplay> map = new HashMap<>();
            try {
                c = resolver.query(ContactsContract.Contacts.CONTENT_URI,
                        null, selectionContacts, null, getContactDisplayOrdering(sorting));
                if (null != c) {
                    while (c.moveToNext()) {
                        ContactDisplay display = new ContactDisplay(newContact(c));
                        displays.add(display);
                        map.put(display.getContactId(),display);
                    }
                }
                if (map.isEmpty()) return null;
            }
            finally {
                closeCursorSilently(c);
            }

            try {
                c = resolver.query(ContactsContract.Data.CONTENT_URI, null,
                        selectionData,null, ContactsContract.Data.CONTACT_ID+" ASC");
                if (null != c) {
                    while (c.moveToNext()) {
                        long contactId = c.getLong(c.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID));
                        String mime = c.getString(c.getColumnIndexOrThrow(ContactsContract.Data.MIMETYPE));
                        ContactDisplay display = map.get(contactId);
                        if (null == display) continue;
                        if (ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE.equals(mime)) {
                            PhoneNumber number = newPhoneNumber(c);
                            display.addPhoneNumber(number);
                            if (number.isPrimary()) display.setPhoneNumberPrimary(number);
                        }
                        else {
                            display.setName(newName(c));
                        }
                    }
                }
            }
            finally {
                closeCursorSilently(c);
            }
        }
        catch (Exception ex) {
            throw new RepositoryException("can not load contact display for account="+account,ex);
        }
        return displays;
    }

    private long getRawContactId(String accountType, String accountName) {
        Cursor c = null;
        try {
            ContentResolver resolver = this.contactsProvider;
            c = resolver.query(ContactsContract.RawContacts.CONTENT_URI, new String[]{ContactsContract.RawContacts._ID},
                    ContactsContract.RawContacts.ACCOUNT_TYPE + " = ? AND " + ContactsContract.RawContacts.ACCOUNT_NAME + " = ?",
                    new String[]{accountType, accountName},
                    null);
            if (null != c && c.moveToFirst()) {
                return c.getLong(0);
            }
        }
        finally {
            closeCursorSilently(c);
        }
        return 0;
    }

    private String getContactDisplayOrdering(@NonNull ContactSorting sorting) {
        Check.isNonNull(sorting,"null == sorting");
        String ordering = ContactsContract.Contacts.DISPLAY_NAME+" IS NULL,"; // this will sort all the "null" display_name always last
        if (ContactSorting.FIRSTNAME_FIRST == sorting) ordering += ContactsContract.Contacts.SORT_KEY_PRIMARY+" COLLATE NOCASE ASC";
        else if (ContactSorting.FIRSTNAME_FIRST_DESC == sorting) ordering += ContactsContract.Contacts.SORT_KEY_PRIMARY+" COLLATE NOCASE DESC";
        else if (ContactSorting.LASTNAME_FIRST == sorting) ordering += ContactsContract.Contacts.SORT_KEY_ALTERNATIVE+" COLLATE NOCASE ASC";
        else  ordering += ContactsContract.Contacts.SORT_KEY_ALTERNATIVE+" COLLATE NOCASE DESC";
        return ordering;
    }

    private Contact newContact(Cursor c) {
        String photo = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI));
        Uri photoUri = Check.isEmptyString(photo) ? null : Uri.parse(photo);
        return new Contact(
                c.getLong(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID)),
                c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY)),
                c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)),
                c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_ALTERNATIVE)),
                photoUri,
                1 == c.getInt(c.getColumnIndexOrThrow(ContactsContract.Contacts.STARRED))
        );
    }

    private Name newName(Cursor c) {
        return new Name(
                c.getLong(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.CONTACT_ID)),
                c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME)),
                c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME)),
                c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME)),
                c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.PREFIX)),
                c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME)),
                c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.SUFFIX)),
                c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_GIVEN_NAME)),
                c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_MIDDLE_NAME)),
                c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_FAMILY_NAME))
        );
    }

    private PhoneNumber newPhoneNumber(Cursor c) {
        return new PhoneNumber(
                c.getLong(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone._ID)),
                c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)),
                1 == c.getInt(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY)),
                c.getInt(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE)),
                c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LABEL))
        );
    }

    private Email newEmail(Cursor c) {
        return new Email(
                c.getLong(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email._ID)),
                c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS)),
                1 == c.getInt(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.IS_SUPER_PRIMARY)),
                c.getInt(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.TYPE)),
                c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.LABEL))
        );
    }

    private Relation newRelation(Cursor c) {
        return new Relation(
                c.getLong(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Relation._ID)),
                c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Relation.NAME)),
                c.getInt(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Relation.TYPE)),
                c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Relation.LABEL))
        );
    }

    private Event newEvent(Cursor c) {
        return new Event(
                c.getLong(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Event._ID)),
                c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Event.START_DATE)),
                c.getInt(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Event.TYPE)),
                c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Event.LABEL))
        );
    }

    private PostalAddress newPostalAddress(Cursor c) {
        return new PostalAddress(
                c.getLong(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal._ID)),
                c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS)),
                c.getInt(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal.TYPE)),
                c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal.LABEL))
        );
    }

    private Organization newOrganization(Cursor c) {
        return new Organization(
                c.getLong(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Organization._ID)),
                c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Organization.COMPANY)),
                c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Organization.TITLE)),
                c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Organization.DEPARTMENT))
        );
    }

    private Website newWebsite(Cursor c) {
        return new Website(
                c.getLong(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal._ID)),
                c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Website.URL))
        );
    }

    private Note newNote(Cursor c) {
        return new Note(
                c.getLong(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Note._ID)),
                c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Note.NOTE))
        );
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///                                     Private Methods                                               ///
    ////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Uri getLookupUri(Uri uri) {
        return ContactsContract.Contacts.getLookupUri(contactsProvider,uri);
    }

    private void closeCursorSilently(@Nullable Cursor c) {
        if (null == c) return;
        try {
            c.close();
        }
        catch (Exception ex) {
            Log.e(TAG,null,ex);
        }
    }
}
