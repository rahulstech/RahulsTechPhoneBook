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
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.ContactsContract;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

import rahulstech.android.phonebook.concurrent.AppExecutors;
import rahulstech.android.phonebook.model.RawContact;
import rahulstech.android.phonebook.model.Contact;
import rahulstech.android.phonebook.model.ContactDisplay;
import rahulstech.android.phonebook.model.Email;
import rahulstech.android.phonebook.model.Event;
import rahulstech.android.phonebook.model.Name;
import rahulstech.android.phonebook.model.Note;
import rahulstech.android.phonebook.model.Organization;
import rahulstech.android.phonebook.model.PhoneNumber;
import rahulstech.android.phonebook.model.PostalAddress;
import rahulstech.android.phonebook.model.Relation;
import rahulstech.android.phonebook.model.Website;
import rahulstech.android.phonebook.util.Check;

public class ContactRepository {

    private static final String TAG = "ContactsRepository";

    private static final Object lock = new Object();

    private Context appContext;
    private ContentResolver contactsProvider;
    private Executor backgroundExecutor;

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

    private ContactRepository(Context context) {
        if (null == context) throw new NullPointerException("null == context");
        this.appContext = context.getApplicationContext();
        this.contactsProvider = this.appContext.getContentResolver();
        this.backgroundExecutor = AppExecutors.getBackgroundExecutor();
        this.mContactRepoOperation = new ContactRepositoryOperation(this);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///                                          API Methods                                              ///
    ////////////////////////////////////////////////////////////////////////////////////////////////////////

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
        return new ArrayList<>(accounts);
    }

    public Uri getLookupUri(Uri uri) {
        return ContactsContract.Contacts.getLookupUri(contactsProvider,uri);
    }

    public List<Contact> loadContacts() {
        Cursor c = null;
        try {
            ContentResolver resolver = this.contactsProvider;
            c = resolver.query(ContactsContract.Contacts.CONTENT_URI,
                    null,
                    null, null,
                    ContactsContract.Contacts.SORT_KEY_PRIMARY + " ASC");

            List<Contact> contacts = new ArrayList<>();
            if (c != null) {
                while (c.moveToNext()) {
                    String photo = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI));
                    Uri photoUri = Check.isEmptyString(photo) ? null : Uri.parse(photo);
                    contacts.add(newContact(c));
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

    public Contact loadContact(String key) {
        Cursor c = null;
        try {
            ContentResolver resolver = this.contactsProvider;
            c = resolver.query(ContactsContract.Contacts.CONTENT_URI,
                    null,
                    ContactsContract.Contacts.LOOKUP_KEY+" = ? OR "+ContactsContract.Contacts._ID+" = ?",
                    new String[]{key,key},
                    null);

            if (c != null && c.moveToFirst()) {
                return newContact(c);
            }
        }
        catch (Exception ex) {
            throw new RepositoryException("can not load contact with key="+key,ex);
        }
        finally {
            if (null != c) c.close();
        }
        return null;
    }

    public int updateContact(long id, ContentValues values) {
        try {
            ContentResolver resolver = this.contactsProvider;
            return resolver.update(ContactsContract.Contacts.CONTENT_URI,values,ContactsContract.Contacts._ID+" = "+id,null);
        }
        catch (Exception ex) {
            throw new RepositoryException("contact with id="+id+" not updated",ex);
        }
    }

    private Contact newContact(Cursor c) {
        String photo = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI));
        Uri photoUri = Check.isEmptyString(photo) ? null : Uri.parse(photo);
        return new Contact(
                c.getLong(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID)),
                c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY)),
                c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)),
                photoUri,
                1 == c.getInt(c.getColumnIndexOrThrow(ContactsContract.Contacts.STARRED))
        );
    }

    public Name loadName(String key) {
        Cursor c = null;
        try {
            ContentResolver resolver = this.contactsProvider;
            c = resolver.query(ContactsContract.Data.CONTENT_URI,null,
                    "("+ContactsContract.Data.LOOKUP_KEY+" = ? OR "+ ContactsContract.Data.CONTACT_ID +" = ?) AND "
                            +ContactsContract.Data.MIMETYPE+" = ?",
                    new String[]{key,key,ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE},
                    null);

            if (c != null) {
                if (c.moveToFirst()) {
                    return new Name(
                            c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.LOOKUP_KEY)),
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
            }
        }
        catch (Exception ex) {
            throw new RepositoryException("can not load Name for contact="+key,ex);
        }
        finally {
            if (null != c) c.close();
        }
        return null;
    }

    public List<PhoneNumber> loadPhoneNumbers(String key) {
        Cursor c = null;
        try {
            ContentResolver resolver = this.contactsProvider;
            c = resolver.query(ContactsContract.Data.CONTENT_URI,null,
                    "("+ContactsContract.Data.LOOKUP_KEY+" = ? OR "+ ContactsContract.Data.CONTACT_ID +" = ?) AND "
                            +ContactsContract.Data.MIMETYPE+" = ?",
                    new String[]{key,key,ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE},
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
            throw new RepositoryException("can not load PhoneNumber for contact="+key,ex);
        }
        finally {
            if (null != c) c.close();
        }
    }

    public List<Email> loadEmails(String key) {
        Cursor c = null;
        try {
            ContentResolver resolver = this.contactsProvider;
            c = resolver.query(ContactsContract.Data.CONTENT_URI,null,
                    "("+ContactsContract.Data.LOOKUP_KEY+" = ? OR "+ContactsContract.Data.CONTACT_ID+" = ?) AND "+
                            ContactsContract.Data.MIMETYPE+" = ?",
                    new String[]{key,key,ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE},
                    ContactsContract.CommonDataKinds.Email.IS_SUPER_PRIMARY+" DESC");

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
            throw new RepositoryException("can not load Email for contact="+key,ex);
        }
        finally {
            if (null != c) c.close();
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

    public List<ContactDisplay> loadContactDisplays() {
        try {
            List<ContactDisplay> displays = new ArrayList<>();
            List<Contact> contacts = loadContacts();
            for (Contact c : contacts) {
                displays.add(
                        new ContactDisplay(c, loadPhoneNumbers(c.getLookupKey()))
                );
            }
            return displays;
        }
        catch (Exception ex) {
            throw new RepositoryException("can not load ContactDisplay",ex);
        }
    }

    public List<Relation> loadContactRelations(String key) {
        try {
            List<Relation> relations = new ArrayList<>();
            ContentResolver resolver = this.contactsProvider;
            try (Cursor c = resolver.query(ContactsContract.Data.CONTENT_URI,
                    null,
                    "("+ContactsContract.Data.LOOKUP_KEY + " = ? OR "+ContactsContract.Data.CONTACT_ID+" = ? ) AND "
                            + ContactsContract.Data.MIMETYPE + " = ?",
                    new String[]{key,key,ContactsContract.CommonDataKinds.Relation.CONTENT_ITEM_TYPE},
                    ContactsContract.CommonDataKinds.Relation.NAME+" DESC")) {

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

    public List<Event> loadContactEvents(String key) {
        Cursor c = null;
        try {
            ContentResolver resolver = this.contactsProvider;
            c = resolver.query(ContactsContract.Data.CONTENT_URI,
                    null,
                    "("+ContactsContract.Data.LOOKUP_KEY+" = ? OR "+ContactsContract.Data.CONTACT_ID+" = ?) AND "
                            + ContactsContract.Data.MIMETYPE+" = ?",
                    new String[]{key,key,ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE},
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

    public List<PostalAddress> loadContactPostalAddresses(String key) {
        Cursor c = null;
        try {
            ContentResolver resolver = this.contactsProvider;
            c = resolver.query(ContactsContract.Data.CONTENT_URI,
                    null,
                    "("+ContactsContract.Data.LOOKUP_KEY+" = ? OR "+ContactsContract.Data.CONTACT_ID+" = ?) AND "+
                            ContactsContract.Data.MIMETYPE+" = ?",
                    new String[]{key, key, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE},
                    null);
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

    public List<Organization> loadContactOrganizations(String key) {
        Cursor c = null;
        try {
            ContentResolver resolver = this.contactsProvider;
            c = resolver.query(ContactsContract.Data.CONTENT_URI,
                    null,
                    "("+ContactsContract.Data.LOOKUP_KEY+" = ? OR "+ContactsContract.Data.CONTACT_ID+" = ?) AND "+
                            ContactsContract.Data.MIMETYPE+" = ?",
                    new String[]{key, key, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE},
                    null);
            List<Organization> addresses = new ArrayList<>();
            if (null != c) {
                while (c.moveToNext()) {
                    addresses.add(new Organization(
                            c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Organization.LOOKUP_KEY)),
                            c.getLong(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Organization._ID)),
                            c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Organization.COMPANY)),
                            c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Organization.TITLE)),
                            c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Organization.DEPARTMENT)),
                            c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Organization.JOB_DESCRIPTION)),
                            c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Organization.OFFICE_LOCATION)),
                            c.getInt(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Organization.TYPE)),
                            c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Organization.LABEL))
                    ));
                }
            }
            return addresses;
        }
        catch (Exception ex) {
            throw new RepositoryException("can load Organization",ex);
        }
        finally {
            if(null != c) c.close();
        }
    }

    public List<Website> loadContactWebsites(String key) {
        Cursor c = null;
        try {
            ContentResolver resolver = this.contactsProvider;
            c = resolver.query(ContactsContract.Data.CONTENT_URI,
                    null,
                    "("+ContactsContract.Data.LOOKUP_KEY+" = ? OR "+ContactsContract.Data.CONTACT_ID+" = ?) AND "+
                            ContactsContract.Data.MIMETYPE+" = ?",
                    new String[]{key, key, ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE},
                    null);
            List<Website> addresses = new ArrayList<>();
            if (null != c) {
                while (c.moveToNext()) {
                    addresses.add(new Website(
                            c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal.LOOKUP_KEY)),
                            c.getLong(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal._ID)),
                            c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Website.URL)))
                    );
                }
            }
            return addresses;
        }
        catch (Exception ex) {
            throw new RepositoryException("can load Website",ex);
        }
        finally {
            if(null != c) c.close();
        }
    }

    public Note loadContactNote(String key) {
        Cursor c = null;
        try {
            ContentResolver resolver = this.contactsProvider;
            c = resolver.query(ContactsContract.Data.CONTENT_URI,
                    null,
                    "("+ContactsContract.Data.LOOKUP_KEY+" = ? OR "+ContactsContract.Data.CONTACT_ID+" = ?) AND "+
                            ContactsContract.Data.MIMETYPE+" = ?",
                    new String[]{key, key,ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE},
                    null);
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

    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///                                     Private Methods                                               ///
    ////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void loadRelativesContact(List<Relation> relations) {
        // TODO: create a map between relation data id and relative contact lookup, if any
        // this method much fetch contact details using the map
    }
}
