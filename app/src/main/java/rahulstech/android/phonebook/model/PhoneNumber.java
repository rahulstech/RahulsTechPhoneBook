package rahulstech.android.phonebook.model;

import android.database.Cursor;
import android.provider.ContactsContract;


public class PhoneNumber {

    private long contactId;

    private long rawContactId;

    private long id;

    private String phoneNumber;

    private int type;

    public PhoneNumber(long contactId, long rawContactId, long id, String phoneNumber, int type) {
        this.contactId = contactId;
        this.rawContactId = rawContactId;
        this.id = id;
        this.phoneNumber = phoneNumber;
        this.type = type;
    }

    public long getContactId() {
        return contactId;
    }

    public long getRawContactId() {
        return rawContactId;
    }

    public long getId() {
        return id;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public int getType() {
        return type;
    }

    public static PhoneNumber create(Cursor c) {
        try {
            int _iContactId = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID);
            int _iRawContactId = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID);
            int _iId = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone._ID);
            int _iNumber = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER);
            int _iType = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE);

            long contactId = c.getLong(_iContactId);
            long rawContactId = c.getLong(_iRawContactId);
            long id = c.getLong(_iId);
            String number = c.getString(_iNumber);
            int type = c.getInt(_iType);

            return new PhoneNumber(contactId,rawContactId,id,number,type);
        }
        catch (Exception ex) {
            throw new ModelException("can not create PhoneNumber",ex);
        }
    }
}
