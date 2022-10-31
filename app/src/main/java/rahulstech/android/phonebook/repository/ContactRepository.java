package rahulstech.android.phonebook.repository;

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
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.provider.ContactsContract;
import android.util.Log;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import rahulstech.android.phonebook.concurrent.AppExecutors;
import rahulstech.android.phonebook.model.Account;
import rahulstech.android.phonebook.model.Contact;
import rahulstech.android.phonebook.model.ContactDetails;
import rahulstech.android.phonebook.model.ContactDisplay;
import rahulstech.android.phonebook.model.Email;
import rahulstech.android.phonebook.model.Event;
import rahulstech.android.phonebook.model.Note;
import rahulstech.android.phonebook.model.Organization;
import rahulstech.android.phonebook.model.PhoneNumber;
import rahulstech.android.phonebook.model.PostalAddress;
import rahulstech.android.phonebook.model.Relation;
import rahulstech.android.phonebook.model.Website;
import rahulstech.android.phonebook.util.Check;
import rahulstech.android.phonebook.util.DateTimeUtil;

public class ContactRepository {

    private static final String TAG = "ContactsRepository";

    private static final Object lock = new Object();

    private Context appContext;
    private ContentResolver contactsProvider;
    private Executor backgroundExecutor;
    private InvalidationTracker mInvalidationTracker;

    private Map<Class<?>, Uri[]> mModelClassContentUrisMap;

    private ContactRepositoryOperation mContactRepoOperation;

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

    public Executor getBackgroundExecutor() {
        return backgroundExecutor;
    }

    public InvalidationTracker getInvalidationTracker() {
        return mInvalidationTracker;
    }

    private ContactRepository(Context context) {
        if (null == context) throw new NullPointerException("null == context");
        this.appContext = context.getApplicationContext();
        this.contactsProvider = this.appContext.getContentResolver();
        this.backgroundExecutor = AppExecutors.getBackgroundExecutor();
        this.mInvalidationTracker = new InvalidationTracker(this,getConcernedContentUris());
        prepareModelClassContentUrisMap();

        this.mContactRepoOperation = new ContactRepositoryOperation(this);
    }

    private Uri[] getConcernedContentUris() {
        return new Uri[]{
                ContactsContract.Contacts.CONTENT_URI,
                ContactsContract.RawContacts.CONTENT_URI,
                ContactsContract.Data.CONTENT_URI
        };
    }

    private void prepareModelClassContentUrisMap() {
        mModelClassContentUrisMap = new HashMap<>();
        mModelClassContentUrisMap.put(Contact.class,new Uri[]{ContactsContract.Contacts.CONTENT_URI});
        mModelClassContentUrisMap.put(ContactDisplay.class,new Uri[]{ContactsContract.Contacts.CONTENT_URI, ContactsContract.Data.CONTENT_URI});
        mModelClassContentUrisMap.put(Account.class,new Uri[]{ContactsContract.RawContacts.CONTENT_URI});
        mModelClassContentUrisMap.put(ContactDetails.class,new Uri[]{ContactsContract.Contacts.CONTENT_URI,ContactsContract.RawContacts.CONTENT_URI,ContactsContract.Data.CONTENT_URI});
        mModelClassContentUrisMap.put(PhoneNumber.class,new Uri[]{ContactsContract.Data.CONTENT_URI});
        mModelClassContentUrisMap.put(Email.class,new Uri[]{ContactsContract.Data.CONTENT_URI});
        mModelClassContentUrisMap.put(Event.class,new Uri[]{ContactsContract.Data.CONTENT_URI});
        mModelClassContentUrisMap.put(PostalAddress.class,new Uri[]{ContactsContract.Data.CONTENT_URI});
        mModelClassContentUrisMap.put(Organization.class,new Uri[]{ContactsContract.Data.CONTENT_URI});
        mModelClassContentUrisMap.put(Relation.class,new Uri[]{ContactsContract.Data.CONTENT_URI});
        mModelClassContentUrisMap.put(Website.class,new Uri[]{ContactsContract.Data.CONTENT_URI});
        mModelClassContentUrisMap.put(Note.class,new Uri[]{ContactsContract.Data.CONTENT_URI});
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///                                          API Methods                                              ///
    ////////////////////////////////////////////////////////////////////////////////////////////////////////

    public Uri[] getContentUrisForModelClass(Class<?> clazz) {
        final Uri[] uris = mModelClassContentUrisMap.get(clazz);
        if (null == uris) {
            throw new IllegalArgumentException("no content uris map found for model class "+clazz);
        }
        return uris;
    }

    public ContactRepositoryOperation getContactRepositoryOperation() {
        return mContactRepoOperation;
    }

    public List<android.accounts.Account> getContactAccounts() {
        // TODO: list of accounts for adding contact not loaded
        // for example there may be whatsapp and google account,
        // but this method must return google accounts only
        Set<android.accounts.Account> accounts = new HashSet<>();
        AccountManager am = AccountManager.get(appContext);
        HandlerThread thread = new HandlerThread("load_contact_account_thread");
        thread.start();
        Handler handler = new Handler(thread.getLooper());
        try {
            SyncAdapterType[] types = ContentResolver.getSyncAdapterTypes();
            for (SyncAdapterType t : types) {
                android.accounts.Account[] found = am.getAccountsByType(t.accountType);
                for (android.accounts.Account account : found) {
                    accounts.add(account);
                }
                /**am.getAccountsByTypeAndFeatures(t.accountType,new String[]{"service_contacts"},f->{
                    try {
                        android.accounts.Account[] featured_accounts = f.getResult();
                        for (android.accounts.Account account : featured_accounts) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                if (AccountManager.VISIBILITY_VISIBLE
                                        == am.getAccountVisibility(account,appContext.getPackageName())) {
                                    accounts.add(account);
                                }
                            }
                            else {
                                accounts.add(account);
                            }
                            accounts.add(account);
                        }
                    }
                    catch (Exception ex) {
                        Log.e(TAG,null,ex);
                    }
                },handler);*/
            }
        }
        catch (Exception ex) {
            Log.e(TAG,null,ex);
            accounts.clear();
        }
        List<android.accounts.Account> list = new ArrayList<>();
        list.addAll(accounts);
        return list;
    }

    public long addContactData(ContentValues values) {
        try {
            ContentResolver resolver = this.contactsProvider;
            Uri inserted = resolver.insert(ContactsContract.Data.CONTENT_URI,values);
            if (null == inserted) return 0;
            return Long.parseLong(inserted.getLastPathSegment());
        }
        catch (Exception ex) {
            throw new RepositoryException("can not add content data", ex);
        }
    }

    public int updateContactData(long dataId, ContentValues values) {
        try {
            ContentResolver resolver = this.contactsProvider;
            return resolver.update(ContactsContract.Data.CONTENT_URI,values,
                    ContactsContract.Data._ID+" = "+dataId,null);
        }
        catch (Exception ex) {
            throw new RepositoryException("can not update content data", ex);
        }
    }

    public List<Contact> loadContacts() {
        Cursor c = null;
        try {
            ContentResolver resolver = this.contactsProvider;
            c = resolver.query(ContactsContract.Contacts.CONTENT_URI,
                    new String[]{
                            ContactsContract.Contacts._ID,
                            ContactsContract.Contacts.LOOKUP_KEY,
                            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                            ContactsContract.Contacts.PHOTO_URI
                    },
                    null, null,
                    ContactsContract.Contacts.SORT_KEY_PRIMARY + " ASC");

            List<Contact> contacts = new ArrayList<>();
            if (c != null) {
                while (c.moveToNext()) {
                    String photo = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI));
                    Uri photoUri = Check.isEmptyString(photo) ? null : Uri.parse(photo);
                    contacts.add(
                            new Contact(
                                    c.getLong(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID)),
                                    c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY)),
                                    c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)),
                                    photoUri
                            )
                    );
                }
            }
            return contacts;
        }
        catch (Exception ex) {
            throw new RepositoryException("can not load contacts",ex);
        }
        finally {
            if (null != c) c.close();
        }
    }

    public Contact loadContactByLookupKey(String lookupKey) {
        Cursor c = null;
        try {
            ContentResolver resolver = this.contactsProvider;
            c = resolver.query(Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI,lookupKey),
                    new String[]{
                            ContactsContract.Contacts._ID,
                            ContactsContract.Contacts.LOOKUP_KEY,
                            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                            ContactsContract.Contacts.PHOTO_URI
                    },
                    null, null,
                    ContactsContract.Contacts.SORT_KEY_PRIMARY + " ASC");

            if (c != null && c.moveToFirst()) {
                String photo = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI));
                Uri photoUri = Check.isEmptyString(photo) ? null : Uri.parse(photo);
                return new Contact(
                        c.getLong(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID)),
                        c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY)),
                        c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)),
                        photoUri
                );
            }
        }
        catch (Exception ex) {
            throw new RepositoryException("can not load contacts",ex);
        }
        finally {
            if (null != c) c.close();
        }
        return null;
    }

    public List<PhoneNumber> loadPhoneNumbersByLookupKey(String lookupKey) {
        Cursor c = null;
        try {
            ContentResolver resolver = this.contactsProvider;
            c = resolver.query(ContactsContract.Data.CONTENT_URI,null,
                    ContactsContract.Data.LOOKUP_KEY+" = ? AND "+ContactsContract.Data.MIMETYPE+" = ?",
                    new String[]{lookupKey,ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE},
                    ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY+" DESC");

            List<PhoneNumber> numbers = new ArrayList<>();
            if (c != null) {
                while (c.moveToNext()) {
                    numbers.add(new PhoneNumber(
                            c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY)),
                       c.getLong(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone._ID)),
                       c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)),
                       1 == c.getInt(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY)),
                       c.getInt(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE)),
                       c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LABEL))
                    ));
                }
            }
            return numbers;
        }
        catch (Exception ex) {
            throw new RepositoryException("can not load PhoneNumber for contact="+lookupKey,ex);
        }
        finally {
            if (null != c) c.close();
        }
    }

    public List<Email> loadEmailsByLookupKey(String lookupKey) {
        Cursor c = null;
        try {
            ContentResolver resolver = this.contactsProvider;
            c = resolver.query(ContactsContract.Data.CONTENT_URI,null,
                    ContactsContract.Data.LOOKUP_KEY+" = ? AND "+ContactsContract.Data.MIMETYPE+" = ?",
                    new String[]{lookupKey,ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE},
                    null);

            List<Email> emails = new ArrayList<>();
            if (c != null) {
                while (c.moveToNext()) {
                    emails.add(
                      new Email(
                              c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.LOOKUP_KEY)),
                              c.getLong(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email._ID)),
                              c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS)),
                              1 == c.getInt(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.IS_SUPER_PRIMARY)),
                              c.getInt(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.TYPE)),
                              c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.LABEL))
                      )
                    );
                }
            }
            return emails;
        }
        catch (Exception ex) {
            throw new RepositoryException("can not load Email for contact="+lookupKey,ex);
        }
        finally {
            if (null != c) c.close();
        }
    }

    public Account loadAccountByLookupKey(String lookupKey) {
        Cursor c = null;
        try {
            ContentResolver resolver = this.contactsProvider;

            c = resolver.query(Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI,lookupKey),
                    new String[]{ContactsContract.Contacts.NAME_RAW_CONTACT_ID},
                    null,null,null);

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
                return new Account(lookupKey,id,
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

    public List<ContactDisplay> loadContactDisplays() {
        try {
            List<ContactDisplay> displays = new ArrayList<>();
            List<Contact> contacts = loadContacts();
            for (Contact c : contacts) {
                displays.add(
                        new ContactDisplay(c, loadPhoneNumbersByLookupKey(c.getLookupKey()))
                );
            }
            return displays;
        }
        catch (Exception ex) {
            throw new RepositoryException("can not load ContactDisplay",ex);
        }
    }

    public ContactDetails loadContactDetailsByLookupKey(String lookupKey) {
        try {
            Contact contact = loadContactByLookupKey(lookupKey);
            if (null == contact) return null;

            return new ContactDetails(
                    loadAccountByLookupKey(contact.getLookupKey()),
                    contact,
                    loadPhoneNumbersByLookupKey(contact.getLookupKey()),
                    loadEmailsByLookupKey(contact.getLookupKey()));
        }
        catch (Exception ex) {
            throw new RepositoryException("can not load ContactDetails",ex);
        }
    }

    public List<Relation> loadContactRelationsByLookupKey(String lookupKey) {
        try {
            List<Relation> relations = new ArrayList<>();
            ContentResolver resolver = this.contactsProvider;
            try (Cursor c = resolver.query(ContactsContract.Data.CONTENT_URI,
                    null,
                    ContactsContract.Data.LOOKUP_KEY + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?",
                    new String[]{String.valueOf(lookupKey), ContactsContract.CommonDataKinds.Relation.CONTENT_ITEM_TYPE},
                    null)) {

                if (null != c) {
                    while (c.moveToNext()) {
                        relations.add(
                                new Relation(
                                        c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Relation.LOOKUP_KEY)),
                                        c.getLong(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Relation._ID)),
                                        c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Relation.NAME)),
                                        c.getInt(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Relation.TYPE)),
                                        c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Relation.LABEL))
                                )
                        );
                    }
                }
            }

            loadRelativesContact(relations);

            return relations;
        }
        catch (Exception ex) {
            throw new RepositoryException("can not load relation",ex);
        }
    }

    public List<Event> loadContactEventsByLookupKey(String lookupKey) {
        Cursor c = null;
        try {
            ContentResolver resolver = this.contactsProvider;
            c = resolver.query(ContactsContract.Data.CONTENT_URI,
                    null,
                    ContactsContract.Data.LOOKUP_KEY+" = ? AND "+ ContactsContract.Data.MIMETYPE+" = ?",
                    new String[]{lookupKey, ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE},
                    null);
            List<Event> events = new ArrayList<>();
            if (null != c) {
                while (c.moveToNext()) {
                    events.add(new Event(
                            c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Event.LOOKUP_KEY)),
                            c.getLong(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Event._ID)),
                            c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Event.START_DATE)),
                            c.getInt(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Event.TYPE)),
                            c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Event.LABEL))
                    ));
                }
            }
            return events;
        }
        catch (Exception ex) {
            throw new RepositoryException("can load Event",ex);
        }
        finally {
            if(null != c) c.close();
        }
    }

    public List<PostalAddress> loadContactPostalAddressesByLookupKey(String lookupKey) {
        Cursor c = null;
        try {
            ContentResolver resolver = this.contactsProvider;
            c = resolver.query(ContactsContract.Data.CONTENT_URI,
                    null,
                    ContactsContract.Data.LOOKUP_KEY+" = ? AND "+ ContactsContract.Data.MIMETYPE+" = ?",
                    new String[]{lookupKey, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE},
                    ContactsContract.Data.IS_SUPER_PRIMARY+" DESC");
            List<PostalAddress> addresses = new ArrayList<>();
            if (null != c) {
                while (c.moveToNext()) {
                    addresses.add(new PostalAddress(
                            c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal.LOOKUP_KEY)),
                            c.getLong(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal._ID)),
                            c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS)),
                            c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal.STREET)),
                            c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal.POBOX)),
                            c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal.NEIGHBORHOOD)),
                            c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal.CITY)),
                            c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal.REGION)),
                            c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE)),
                            c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY)),
                            c.getInt(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal.TYPE)),
                            c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal.LABEL))
                    ));
                }
            }
            return addresses;
        }
        catch (Exception ex) {
            throw new RepositoryException("can load PostalAddress",ex);
        }
        finally {
            if(null != c) c.close();
        }
    }

    public Note loadContactNoteByLookupKey(String lookupKey) {
        Cursor c = null;
        try {
            ContentResolver resolver = this.contactsProvider;
            c = resolver.query(ContactsContract.Data.CONTENT_URI,
                    null,
                    ContactsContract.Data.LOOKUP_KEY+" = ? AND "+ ContactsContract.Data.MIMETYPE+" = ?",
                    new String[]{lookupKey, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE},
                    ContactsContract.Data.IS_SUPER_PRIMARY+" DESC");
            if (null != c && c.moveToFirst()) {
                return new Note(
                        c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Note.LOOKUP_KEY)),
                        c.getLong(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Note._ID)),
                        c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Note.NOTE))
                );
            }
        }
        catch (Exception ex) {
            throw new RepositoryException("can load Note",ex);
        }
        finally {
            if(null != c) c.close();
        }
        return null;
    }

    public int removeContactData(String mimetype, long[] ids) {
        try {
            ContentResolver resolver = this.contactsProvider;
            int length = ids.length;
            StringBuilder selection = new StringBuilder(ContactsContract.Data.MIMETYPE+" = ? AND "+ContactsContract.Data._ID+" IN(");
            String[] selectionArgs = new String[]{mimetype};
            for (int i=0; i<length; i++) {
                if (i>0) selection.append(",");
                selection.append(ids[i]);
            }
            selection.append(")");

            Log.d(TAG,"selection: "+selection+" mimetype: "+ mimetype);

            ArrayList<ContentProviderOperation> operations = new ArrayList<>();
            operations.add(
                    ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                            .withExpectedCount(length)
                            .withSelection(selection.toString(),selectionArgs).build()
            );
            ContentProviderResult[] results = resolver.applyBatch(ContactsContract.Data.CONTENT_URI.getAuthority(), operations);
            return results[0].count;
        }
        catch (Exception ex) {
            throw new RepositoryException("can not remove contact data",ex);
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///                                     Private Methods                                               ///
    ////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void loadRelativesContact(List<Relation> relations) {
        Cursor c = null;
        try {
            int count = relations.size();
            if (0 == count) return;

            HashMap<String,Relation> map = new HashMap<>();
            StringBuilder selection = new StringBuilder(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY+" IN(");
            String[] selectionArgs = new String[count];
            for (int i=0; i<count; i++) {
                if (i>0) selection.append(",");
                selection.append("?");
                Relation r = relations.get(i);
                String name = r.getDisplayName();
                selectionArgs[i] = name;
                map.put(name,r);
            }
            selection.append(")");

            ContentResolver resolver = this.contactsProvider;
            c = resolver.query(ContactsContract.Contacts.CONTENT_URI,new String[]{
                    ContactsContract.Contacts.LOOKUP_KEY,
                    ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,
                    ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
            },selection.toString(),selectionArgs,null);

            if (null != c) {
                while (c.moveToNext()) {
                    String lookupKey = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY));
                    String name = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY));
                    String thumbnail = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI));
                    Uri thumbnailUri = null == thumbnail ? null : Uri.parse(thumbnail);

                    Relation r = map.get(name);
                    r.setRelativeContactLookupKey(lookupKey);
                    r.setPhotoUri(thumbnailUri);
                }
            }
        }
        catch (Exception ex) {
            throw new RepositoryException("fail to load relation contacts",ex);
        }
        finally {
            if (null != c) c.close();
        }
    }
}
