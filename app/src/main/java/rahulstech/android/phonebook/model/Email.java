package rahulstech.android.phonebook.model;

import android.database.Cursor;
import android.provider.ContactsContract;

public class Email {

    private long contactId;

    private long rawContactId;

    private long id;

    private String address;

    private int type;

    public Email(long contactId, long rawContactId, long id, String address, int type) {
        this.contactId = contactId;
        this.rawContactId = rawContactId;
        this.id = id;
        this.address = address;
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

    public String getAddress() {
        return address;
    }

    public int getType() {
        return type;
    }

    public static Email create(Cursor c) {
        try {
            int _iContactId = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.CONTACT_ID);
            int _iRawContactId = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.RAW_CONTACT_ID);
            int _iId = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email._ID);
            int _iAddress = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS);
            int _iType = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.TYPE);

            long contactId = c.getLong(_iContactId);
            long rawContactId = c.getLong(_iRawContactId);
            long id = c.getLong(_iId);
            String address = c.getString(_iAddress);
            int type = c.getInt(_iType);

            return new Email(contactId,rawContactId,id,address,type);
        }
        catch (Exception ex) {
            throw new ModelException("can not create Email",ex);
        }
    }
}
