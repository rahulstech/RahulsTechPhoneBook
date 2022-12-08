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
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import rahulstech.android.phonebook.model.Contact;
import rahulstech.android.phonebook.model.ContactDetails;
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
import rahulstech.android.phonebook.util.Settings;

import static rahulstech.android.phonebook.util.Check.isEmptyString;
import static rahulstech.android.phonebook.util.Check.isNonNull;
import static rahulstech.android.phonebook.util.Helpers.firstNonEmptyString;
import static rahulstech.android.phonebook.util.Helpers.logDebug;

public class ContactRepository {

    private static final String TAG = "ContactRepository";

    private static final Object lock = new Object();

    private static final String LOCAL_CONTACT_SOURCE_DISPLAY_NAME = "Phone";

    private static final HashMap<String,String> ACCOUNT_TYPE_DISPLAY_NAME = new HashMap<>();
    static {
        ACCOUNT_TYPE_DISPLAY_NAME.put("com.google","Google");
        ACCOUNT_TYPE_DISPLAY_NAME.put("com.osp.app.signin","Samsung");
    }

    private static final HashSet<String> EXCLUDED_ACCOUNT_TYPES = new HashSet<>();
    static {
        EXCLUDED_ACCOUNT_TYPES.add("com.whatsapp"); // Whatsapp
        EXCLUDED_ACCOUNT_TYPES.add("com.whatsapp.w4b"); // Whatsapp Business
        EXCLUDED_ACCOUNT_TYPES.add("org.telegram.messenger"); // Telegram
        EXCLUDED_ACCOUNT_TYPES.add("org.thoughtcrime.securesms"); // Signal
        EXCLUDED_ACCOUNT_TYPES.add("com.viber.voip"); // Viber
    }

    private static ContactRepository instance = null;

    private Context appContext;
    private ContentResolver contactsProvider;
    private Settings settings;

    private List<RawContact> allContactSources = null;


    private ContactRepository(@NonNull Context context) {
        isNonNull(context,"null == context");
        this.appContext = context.getApplicationContext();
        this.contactsProvider = this.appContext.getContentResolver();
        this.settings = new Settings(appContext);
        /*RawContact localSource = getLocalContactSource();
        if (null != localSource)
            ACCOUNT_TYPE_DISPLAY_NAME.put(localSource.getType(),localSource.getDisplayName());*/
    }

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

    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///                                          API Methods                                              ///
    ////////////////////////////////////////////////////////////////////////////////////////////////////////

    public List<RawContact> getAllContactSources() {
        if (null != allContactSources) return allContactSources;

        List<RawContact> sources = new ArrayList<>();
        RawContact localSource = getLocalContactSource();
        sources.add(localSource);

        HashSet<Account> accounts = new HashSet<>();
        AccountManager am = AccountManager.get(appContext);
        try {
            SyncAdapterType[] types = ContentResolver.getSyncAdapterTypes();
            for (SyncAdapterType t : types) {
                if (!ContactsContract.AUTHORITY.equals(t.authority) || !t.supportsUploading()) continue;
                Account[] found = am.getAccountsByType(t.accountType);
                accounts.addAll(Arrays.asList(found));
            }
        }
        catch (Exception ex) {
            Log.e(TAG,null,ex);
            accounts.clear();
        }
        for (Account account : accounts) {
            String type = account.type;
            String name = account.name;
            if (EXCLUDED_ACCOUNT_TYPES.contains(type)) continue;
            String displayName = getContactSourceDisplayName(type,name);
            sources.add(new RawContact(type,name,displayName));
        }
        this.allContactSources = sources;
        return sources;
    }

    public String getContactSourceDisplayName(String type, String alternative) {
        String displayName = ACCOUNT_TYPE_DISPLAY_NAME.get(type);
        return firstNonEmptyString(displayName,alternative);
    }

    private RawContact getLocalContactSource() {
        String name = settings.getLocalContactAccountName();
        String type = settings.getLocalContactAccountType();
        if (!isEmptyString(name) && !isEmptyString(type)) {
            return new RawContact(type,name,LOCAL_CONTACT_SOURCE_DISPLAY_NAME);
        }
        try {
            ContentResolver resolver = this.contactsProvider;
            // first insert a new raw contact locally
            ContentValues cvs = new ContentValues();
            cvs.put(ContactsContract.RawContacts.ACCOUNT_NAME,(String) null);
            cvs.put(ContactsContract.RawContacts.ACCOUNT_TYPE,(String) null);
            Uri uri = resolver.insert(ContactsContract.RawContacts.CONTENT_URI,cvs);
            // now fetch the inserted raw contact and get the account name and type
            long rawContactId = ContentUris.parseId(uri);
            Cursor c = null;
            try {
                c = resolver.query(ContactsContract.RawContacts.CONTENT_URI,
                        new String[]{ContactsContract.RawContacts.ACCOUNT_NAME,ContactsContract.RawContacts.ACCOUNT_TYPE},
                        ContactsContract.RawContacts._ID+" = "+rawContactId,null,null);
                if (null != c && c.moveToFirst()) {
                    name = c.getString(0);
                    type = c.getString(1);
                    ACCOUNT_TYPE_DISPLAY_NAME.put(type,LOCAL_CONTACT_SOURCE_DISPLAY_NAME);
                    settings.setLocalContactAccount(name,type).save();
                }
            }
            finally {
                closeCursorSilently(c);
                // finally remove the raw contact
                resolver.delete(uri,null,null);
            }
            return new RawContact(type,name,LOCAL_CONTACT_SOURCE_DISPLAY_NAME);
        }
        catch (Exception ex) {
            throw new RepositoryException("can not get local contact account",ex);
        }
    }

    private Long[] getExcludeRawContactIds() {
        Cursor c = null;
        try {
            ContentResolver resolver = this.contactsProvider;
            String[] placeholders = new String[EXCLUDED_ACCOUNT_TYPES.size()];
            Arrays.fill(placeholders,"?");
            c = resolver.query(ContactsContract.RawContacts.CONTENT_URI,new String[]{ContactsContract.RawContacts._ID},
                    ContactsContract.RawContacts.ACCOUNT_TYPE+" IN("+TextUtils.join(",",placeholders)+")",
                    EXCLUDED_ACCOUNT_TYPES.toArray(new String[0]), null);
            if (null != c) {
                Long[] ids = new Long[c.getCount()];
                int idx = 0;
                while (c.moveToNext()) {
                    ids[idx++] = c.getLong(0);
                }
                return ids;
            }
        }
        catch (Exception ex) {
            throw new RepositoryException("can not get excluded raw contact ids",ex);
        }
        finally {closeCursorSilently(c);}
        return new Long[0];
    }

    public List<ContactDetails> getAllContacts() {
        Cursor c = null;
        try {
            HashMap<Long,ContactDetails> map = new HashMap<>();
            HashMap<Long,Long> rawContactId_contactId_map = new HashMap<>();
            ContentResolver resolver = this.contactsProvider;
            try {
                c = resolver.query(ContactsContract.Data.CONTENT_URI, null,
                        ContactsContract.Data.MIMETYPE + " IN(?,?) AND " +
                                ContactsContract.Data.RAW_CONTACT_ID + " = " + ContactsContract.Data.NAME_RAW_CONTACT_ID,
                        new String[]{ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE,
                                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE},
                        ContactsContract.Data.CONTACT_ID);
                if (c != null) {
                    while (c.moveToNext()) {
                        long contactId = c.getLong(c.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID));
                        long nameRawContactId = c.getLong(c.getColumnIndexOrThrow(ContactsContract.Data.NAME_RAW_CONTACT_ID));
                        ContactDetails details = map.get(contactId);
                        if (null == details) {
                            details = new ContactDetails(newContact(c));
                            rawContactId_contactId_map.put(nameRawContactId,contactId);
                            map.put(contactId, details);
                        }
                        String mime = c.getString(c.getColumnIndexOrThrow(ContactsContract.Data.MIMETYPE));
                        switch (mime) {
                            case ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE: {
                                long rawContactId = c.getLong(c.getColumnIndexOrThrow(ContactsContract.Data.RAW_CONTACT_ID));
                                if (rawContactId == rawContactId) {
                                    details.setName(newName(c));
                                    details.buildNameDependentValues();
                                }
                            }
                            break;
                            case ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE: {
                                List<PhoneNumber> numbers = details.getPhoneNumbers();
                                if (null == numbers) {
                                    numbers = new ArrayList<>();
                                    details.setPhoneNumbers(numbers);
                                }
                                PhoneNumber number = newPhoneNumber(c);
                                if (number.isPrimary()) details.setPhoneNumberPrimary(number);
                                numbers.add(number);
                            }
                        }
                    }
                }
            }
            finally {closeCursorSilently(c);}
            if (!map.isEmpty()) {
                try {
                    c = resolver.query(ContactsContract.RawContacts.CONTENT_URI,
                            new String[]{ContactsContract.RawContacts.CONTACT_ID, ContactsContract.RawContacts._ID,
                                    ContactsContract.RawContacts.ACCOUNT_NAME, ContactsContract.RawContacts.ACCOUNT_TYPE},
                            ContactsContract.RawContacts._ID + " IN(" + TextUtils.join(",",rawContactId_contactId_map.keySet()) + ")",
                            null, null);
                    if (null != c) {
                        while (c.moveToNext()) {
                            long rawContactId = c.getLong(1);
                            long contactId = rawContactId_contactId_map.get(rawContactId);
                            ContactDetails details = map.get(contactId);
                            details.setRawContact(newRawContact(c));
                        }
                    }
                } finally {closeCursorSilently(c);}
            }

            return new ArrayList<>(map.values());
        }
        catch (Exception ex) {
            throw new RepositoryException("can not load contacts",ex);
        }
        finally {closeCursorSilently(c);}
    }

    @Nullable
    public ContactDetails getContactDetails(@NonNull Uri uri, @Nullable RawContact source) {
        Cursor c = null;
        try {
            final String key = uri.getLastPathSegment();
            ContentResolver resolver = this.contactsProvider;
            String selection;
            String[] selectionArgs;
            if (null==source) {
                Long[] excludedRawContactIds = getExcludeRawContactIds();
                selection = "(" + ContactsContract.Data.CONTACT_ID + " = ? OR " + ContactsContract.Data.LOOKUP_KEY + " = ?) AND "
                        + ContactsContract.Data.RAW_CONTACT_ID + " NOT IN(" + TextUtils.join(",", excludedRawContactIds) + ")";
                selectionArgs = new String[]{key, key};
            }
            else {
                selection = ContactsContract.Data.RAW_CONTACT_ID+" = "+source.getId();
                selectionArgs = null;
            }
            ContactDetails details = null;
            try {
                c = resolver.query(ContactsContract.Data.CONTENT_URI, null, selection, selectionArgs, null);
                if (null != c && c.moveToFirst()) {
                    details = new ContactDetails(newContact(c));
                    long nameRawContactId = c.getLong(c.getColumnIndexOrThrow(ContactsContract.Data.NAME_RAW_CONTACT_ID));
                    String nickname = null;
                    do {
                        String mime = c.getString(c.getColumnIndexOrThrow(ContactsContract.Data.MIMETYPE));
                        switch (mime) {
                            case ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE: {
                                // if source is not given then in case of merged contact multiple name data may available
                                // so pick the name data where raw_data_id and name_raw_data_id is same
                                // if source is given then only contact data with given raw_contact_id is filtered
                                // so pick what ever the name data available.
                                long rawContactId = null != source ? nameRawContactId :
                                        c.getLong(c.getColumnIndexOrThrow(ContactsContract.Data.RAW_CONTACT_ID));
                                if (rawContactId == nameRawContactId) {
                                    details.setName(newName(c));
                                    details.buildNameDependentValues();
                                    if (null != nickname) {
                                        details.getName().setNickname(nickname);
                                    }
                                }
                            }
                            break;
                            case ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE: {
                                nickname = c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Nickname.NAME));
                                if (null != details.getName()) {
                                    details.getName().setNickname(nickname);
                                }
                            }
                            break;
                            case ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE: {
                                List<PhoneNumber> numbers = details.getPhoneNumbers();
                                if (null == numbers) {
                                    numbers = new ArrayList<>();
                                    details.setPhoneNumbers(numbers);
                                }
                                PhoneNumber number = newPhoneNumber(c);
                                if (number.isPrimary()) details.setPhoneNumberPrimary(number);
                                numbers.add(number);
                            }
                            break;
                            case ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE: {
                                List<Email> emails = details.getEmails();
                                if (null == emails) {
                                    emails = new ArrayList<>();
                                    details.setEmails(emails);
                                }
                                emails.add(newEmail(c));
                            }
                            break;
                            case ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE: {
                                List<Event> events = details.getEvents();
                                if (null == events) {
                                    events = new ArrayList<>();
                                    details.setEvents(events);
                                }
                                events.add(newEvent(c));
                            }
                            break;
                            case ContactsContract.CommonDataKinds.Relation.CONTENT_ITEM_TYPE: {
                                List<Relation> relations = details.getRelations();
                                if (null == relations) {
                                    relations = new ArrayList<>();
                                    details.setRelations(relations);
                                }
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
                                List<PostalAddress> addresses = details.getAddresses();
                                if (null == addresses) {
                                    addresses = new ArrayList<>();
                                    details.setAddresses(addresses);
                                }
                                addresses.add(newPostalAddress(c));
                            }
                            break;
                            case ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE: {
                                List<Website> websites = details.getWebsites();
                                if (null == websites) {
                                    websites = new ArrayList<>();
                                    details.setWebsites(websites);
                                }
                                websites.add(newWebsite(c));
                            }
                        }
                    }
                    while (c.moveToNext());
                }
            }
            finally {closeCursorSilently(c);}
            if (null != details) {
                // TODO: set raw contacts
                details.setRawContact(source);
            }

            return details;
        }
        catch (Exception ex) {
            throw new RepositoryException("can not load contact details",ex);
        }
        finally {
            closeCursorSilently(c);
        }
    }

    public List<RawContact> getAllRawContactsForContact(@NonNull Uri uri) {
        isNonNull(uri,"null == uri");
        Cursor c = null;
        ArrayList<RawContact> rawContacts = new ArrayList<>();
        try {
            long contactId = ContentUris.parseId(uri);
            ContentResolver resolver = this.contactsProvider;
            c = resolver.query(ContactsContract.RawContacts.CONTENT_URI,new String[]{
                    ContactsContract.RawContacts.CONTACT_ID,ContactsContract.RawContacts._ID,
                    ContactsContract.RawContacts.ACCOUNT_NAME,ContactsContract.RawContacts.ACCOUNT_TYPE
            },ContactsContract.RawContacts.CONTACT_ID+" = "+contactId,null,null);
            if (c != null) {
                while (c.moveToNext()) {
                    RawContact rawContact = newRawContact(c);
                    String displayName = getContactSourceDisplayName(rawContact.getType(),rawContact.getName());
                    rawContact.setDisplayName(displayName);
                    rawContacts.add(rawContact);
                }
            }
            return rawContacts;
        }
        catch (Exception ex) {
            throw new RepositoryException("can not load all raw contacts for uri="+uri,ex);
        }
        finally {closeCursorSilently(c);}
    }

    public List<RawContact> getRawContactsForContactEditing(@NonNull Uri uri) {
        List<RawContact> rawContacts = getAllRawContactsForContact(uri);
        ArrayList<RawContact> filtered = new ArrayList<>();
        for (RawContact rc : rawContacts) {
            if (!EXCLUDED_ACCOUNT_TYPES.contains(rc.getType()))
                filtered.add(rc);
        }
        return filtered;
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

    public boolean changePhoneNumberPrimary(@NonNull PhoneNumber number, boolean primary) {
        isNonNull(number,"null == number");
        if (number.isPrimary() == primary) return true;
        ContentValues cvs = new ContentValues();
        cvs.put(ContactsContract.Data.IS_SUPER_PRIMARY, primary ? 1 : 0);
        cvs.put(ContactsContract.Data.IS_PRIMARY, primary ? 1 : 0);
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

    public boolean updateContactDetails(@NonNull ContactDetails oldDetails, @NonNull ContactDetails newDetails) {
        // TODO: implement update contact photo
        final int TYPE_CUSTOM = ContactsContract.CommonDataKinds.BaseTypes.TYPE_CUSTOM;

        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        RawContact rawContact = oldDetails.getRawContact();
        long rawContactId = rawContact.getId();
        Name nName = newDetails.getName();
        List<PhoneNumber> nNumbers = newDetails.getPhoneNumbers();
        List<Email> nEmails = newDetails.getEmails();
        List<Event> nEvents = newDetails.getEvents();
        List<Relation> nRelations = newDetails.getRelations();
        List<PostalAddress> nAddresses = newDetails.getAddresses();
        Organization nOrg = newDetails.getOrganization();
        List<Website> nWebsites = newDetails.getWebsites();
        Note nNote = newDetails.getNote();

        // delete all the old contact data, which are handled by this app, for the given raw_contact_id
        ops.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                .withSelection(ContactsContract.Data.RAW_CONTACT_ID+" = "+rawContactId+" AND "+ContactsContract.Data.MIMETYPE+" IN(?,?,?,?,?,?,?,?,?,?)",
                        new String[]{
                                ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE,
                                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
                                ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE,
                                ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE,
                                ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE,
                                ContactsContract.CommonDataKinds.Relation.CONTENT_ITEM_TYPE,
                                ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE,
                                ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE,
                                ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE,
                                ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE
                        })
                .build());

        // insert contact data with new values
        if (null != nName) {
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, nName.getDisplayName())
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.PREFIX, nName.getPrefix())
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, nName.getGivenName())
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME, nName.getMiddleName())
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, nName.getFamilyName())
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.SUFFIX, nName.getSuffix())
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_GIVEN_NAME, nName.getPhoneticGivenName())
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_MIDDLE_NAME, nName.getPhoneticMiddleName())
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.PHONETIC_FAMILY_NAME, nName.getPhoneticFamilyName())
                    .build());
            String nickname = nName.getNickname();
            if (!isEmptyString(nickname)) {
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID,rawContactId)
                        .withValue(ContactsContract.Data.MIMETYPE,ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Nickname.NAME,nickname)
                        .withValue(ContactsContract.CommonDataKinds.Nickname.TYPE,ContactsContract.CommonDataKinds.Nickname.TYPE_DEFAULT)
                        .withValue(ContactsContract.CommonDataKinds.Nickname.LABEL,null)
                        .build());
            }
        }
        if (null != nNumbers) {
            for (PhoneNumber data : nNumbers) {
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, data.getNumber())
                        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, data.getType())
                        .withValue(ContactsContract.CommonDataKinds.Phone.LABEL, TYPE_CUSTOM==data.getType() ? data.getTypeLabel() : null)
                        .build());
            }
        }
        if (null != nEmails) {
            for (Email data : nEmails) {
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, data.getAddress())
                        .withValue(ContactsContract.CommonDataKinds.Email.TYPE, data.getType())
                        .withValue(ContactsContract.CommonDataKinds.Email.LABEL, TYPE_CUSTOM==data.getType() ? data.getTypeLabel() : null)
                        .build());
            }
        }
        if (null != nEvents) {
            for (Event data : nEvents) {
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Event.START_DATE, data.getStartDate())
                        .withValue(ContactsContract.CommonDataKinds.Event.TYPE, data.getType())
                        .withValue(ContactsContract.CommonDataKinds.Event.LABEL,TYPE_CUSTOM==data.getType() ? data.getTypeLabel() : null)
                        .build());
            }
        }
        if (null != nRelations) {
            for (Relation data : nRelations) {
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Relation.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Relation.NAME, data.getDisplayName())
                        .withValue(ContactsContract.CommonDataKinds.Relation.TYPE, data.getType())
                        .withValue(ContactsContract.CommonDataKinds.Relation.LABEL, TYPE_CUSTOM==data.getType() ? data.getTypeLabel() : null)
                        .build());
            }
        }
        if (null != nAddresses) {
            for (PostalAddress data : nAddresses) {
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, data.getFormattedAddress())
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, data.getType())
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.LABEL, TYPE_CUSTOM==data.getType() ? data.getTypeLabel() : null)
                        .build());
            }
        }
        if (null != nWebsites) {
            for (Website data : nWebsites) {
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Website.URL, data.getUrl())
                        .withValue(ContactsContract.CommonDataKinds.Website.TYPE, ContactsContract.CommonDataKinds.Website.TYPE_OTHER)
                        .withValue(ContactsContract.CommonDataKinds.Website.LABEL, null)
                        .build());
            }
        }
        if (null != nOrg) {
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValue(ContactsContract.Data.RAW_CONTACT_ID,rawContactId)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Organization.COMPANY,nOrg.getCompany())
                    .withValue(ContactsContract.CommonDataKinds.Organization.TITLE,nOrg.getTitle())
                    .withValue(ContactsContract.CommonDataKinds.Organization.DEPARTMENT,nOrg.getDepartment())
                    .withValue(ContactsContract.CommonDataKinds.Organization.JOB_DESCRIPTION,null)
                    .withValue(ContactsContract.CommonDataKinds.Organization.OFFICE_LOCATION,null)
                    .withValue(ContactsContract.CommonDataKinds.Organization.PHONETIC_NAME,null)
                    .withValue(ContactsContract.CommonDataKinds.Organization.PHONETIC_NAME_STYLE,null)
                    .withValue(ContactsContract.CommonDataKinds.Organization.SYMBOL,null)
                    .withValue(ContactsContract.CommonDataKinds.Organization.TYPE, ContactsContract.CommonDataKinds.StructuredPostal.TYPE_OTHER)
                    .withValue(ContactsContract.CommonDataKinds.Organization.LABEL,null)
                    .build());
        }
        if (null != nNote) {
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValue(ContactsContract.Data.RAW_CONTACT_ID,rawContactId)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Note.NOTE,nNote.getNote())
                    .build());
        }

        try {
            ContentResolver resolver = this.contactsProvider;
            resolver.applyBatch(ContactsContract.AUTHORITY,ops);
            return true;
        }
        catch (Exception ex) {
            throw new RepositoryException("can not update contact details",ex);
        }
    }

    public boolean insertContactDetails(@NonNull ContactDetails details) {
        // TODO: implement insert contact photo
        isNonNull(details,"null == details");

        RawContact rawContact = details.getRawContact();
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
            String nickname = name.getNickname();
            if (!isEmptyString(nickname)) {
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID,0)
                        .withValue(ContactsContract.Data.MIMETYPE,ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Nickname.NAME,nickname)
                        .withValue(ContactsContract.CommonDataKinds.Nickname.TYPE,ContactsContract.CommonDataKinds.Nickname.TYPE_DEFAULT)
                        .withValue(ContactsContract.CommonDataKinds.Nickname.LABEL,null)
                        .build());
            }
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
        if (null != note && isEmptyString(note.getNote())) {
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
        isNonNull(contact,"null == contact");
        try {
            ContentResolver resolver = this.contactsProvider;
            Uri uri = ContactsContract.Contacts.CONTENT_LOOKUP_URI.buildUpon().appendPath(contact.getLookupKey()).build();
            return 1==resolver.delete(uri,null,null);
        }
        catch (Exception ex) {
            throw new RepositoryException("can not delete contact",ex);
        }
    }

    private Contact newContact(Cursor c) {
        String photo = c.getString(c.getColumnIndexOrThrow(ContactsContract.Data.PHOTO_URI));
        Uri photoUri = isEmptyString(photo) ? null : Uri.parse(photo);
        return new Contact(
                c.getString(c.getColumnIndexOrThrow(ContactsContract.Data.LOOKUP_KEY)),
                c.getLong(c.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID)),
                photoUri,
                1 == c.getInt(c.getColumnIndexOrThrow(ContactsContract.Data.STARRED))
        );
    }

    private RawContact newRawContact(Cursor c) {
        return new RawContact(
                c.getLong(c.getColumnIndexOrThrow(ContactsContract.RawContacts.CONTACT_ID)),
                c.getLong(c.getColumnIndexOrThrow(ContactsContract.RawContacts._ID)),
                c.getString(c.getColumnIndexOrThrow(ContactsContract.RawContacts.ACCOUNT_NAME)),
                c.getString(c.getColumnIndexOrThrow(ContactsContract.RawContacts.ACCOUNT_TYPE))
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
