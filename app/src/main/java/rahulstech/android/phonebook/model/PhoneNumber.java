package rahulstech.android.phonebook.model;

import android.content.res.Resources;
import android.provider.ContactsContract;

import androidx.annotation.NonNull;
import rahulstech.android.phonebook.util.Check;

public class PhoneNumber {

    private String lookupKey;

    private long id;

    private String number;

    private boolean primary;

    private int type;

    private CharSequence typeLable;

    public PhoneNumber(String lookupKey, long id, String number, boolean primary, int type, CharSequence typeLable) {
        this.lookupKey = lookupKey;
        this.id = id;
        this.number = number;
        this.primary = primary;
        this.type = type;
        this.typeLable = typeLable;
    }

    public PhoneNumber() {}

    public PhoneNumber(@NonNull PhoneNumber copy) {
        this.lookupKey = copy.lookupKey;
        this.id = copy.id;
        this.number = copy.number;
        this.primary = copy.primary;
        this.type = copy.type;
        this.typeLable = copy.typeLable;
    }

    public String getLookupKey() {
        return lookupKey;
    }

    public void setLookupKey(String lookupKey) {
        this.lookupKey = lookupKey;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
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
        return typeLable;
    }

    public void setTypeLable(CharSequence typeLable) {
        this.typeLable = typeLable;
    }

    public CharSequence getTypeLabel(Resources res) {
        return ContactsContract.CommonDataKinds.Phone.getTypeLabel(res,type,typeLable);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PhoneNumber)) return false;
        PhoneNumber number1 = (PhoneNumber) o;
        return id == number1.id
                && primary == number1.primary
                && type == number1.type
                && Check.isEquals(lookupKey, number1.lookupKey)
                && Check.isEquals(number, number1.number)
                && Check.isEquals(typeLable, number1.typeLable);
    }

    @Override
    public String toString() {
        return "PhoneNumber{" +
                "lookupKey='" + lookupKey + '\'' +
                ", id=" + id +
                ", number='" + number + '\'' +
                ", primary=" + primary +
                ", type=" + type +
                ", typeLable=" + typeLable +
                '}';
    }
}
