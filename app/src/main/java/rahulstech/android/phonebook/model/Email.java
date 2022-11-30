package rahulstech.android.phonebook.model;

import android.content.res.Resources;
import android.provider.ContactsContract;

import androidx.annotation.Nullable;
import rahulstech.android.phonebook.util.Check;

public class Email {

    private long id;

    private String address;

    private boolean primary;

    private int type;

    private CharSequence typeLabel;

    public Email(long id, String address, boolean primary, int type, CharSequence typeLabel) {
        this.id = id;
        this.address = address;
        this.primary = primary;
        this.type = type;
        this.typeLabel = typeLabel;
    }

    public Email() {}

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public CharSequence getTypeLabel() {
        return typeLabel;
    }

    public void setTypeLabel(CharSequence typeLabel) {
        this.typeLabel = typeLabel;
    }

    public CharSequence getTypeLabel(Resources res) {
        return ContactsContract.CommonDataKinds.Email.getTypeLabel(res,type,typeLabel);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Email)) return false;
        Email email = (Email) o;
        return id == email.id
                && primary == email.primary
                && type == email.type
                && Check.isEquals(address, email.address)
                && Check.isEquals(typeLabel, email.typeLabel);
    }
}
