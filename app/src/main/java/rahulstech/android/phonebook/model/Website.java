package rahulstech.android.phonebook.model;

import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;


import rahulstech.android.phonebook.repository.ModelException;
import rahulstech.android.phonebook.util.Check;

public class Website {
    
    private Account account;
    
    private long id;
    
    private Uri url;
    
    private int type;
    
    private CharSequence typeLabel;

    public Website(Account account, long id, Uri url, int type, CharSequence typeLabel) {
        this.account = account;
        this.id = id;
        this.url = url;
        this.type = type;
        this.typeLabel = typeLabel;
    }

    public Account getAccount() {
        return account;
    }

    public long getId() {
        return id;
    }

    public Uri getUrl() {
        return url;
    }

    public int getType() {
        return type;
    }

    public CharSequence getTypeLabel() {
        return typeLabel;
    }
    
    public CharSequence getTypeLabel(Resources res) {
        return typeLabel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Website)) return false;
        Website website = (Website) o;
        return id == website.id && type == website.type
                && Check.isEquals(account, website.account)
                && Check.isEquals(url, website.url)
                && Check.isEquals(typeLabel, website.typeLabel);
    }

    public static Website create(Account account, Cursor c) {
        try {
            int _iId = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Website._ID);
            int _iUrl = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Website.URL);
            int _iType = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Website.TYPE);
            int _iLabel = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Website.LABEL);

            long id = c.getLong(_iId);
            Uri url = Uri.parse(c.getString(_iUrl));
            int type = c.getInt(_iType);
            CharSequence typeLabel = c.getString(_iLabel);

            return new Website(account,id,url,type,typeLabel);
        }
        catch (Exception ex) {
            throw new ModelException("can not create Website",ex);
        }
    }
}
