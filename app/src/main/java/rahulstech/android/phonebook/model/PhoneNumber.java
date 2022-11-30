package rahulstech.android.phonebook.model;

import android.content.res.Resources;
import android.provider.ContactsContract;

import androidx.annotation.NonNull;
import rahulstech.android.phonebook.util.Check;

public class PhoneNumber {

    private long id;

    private String number;

    private boolean primary;

    private int type;

    private CharSequence typeLabel;

    public PhoneNumber(long id, String number, boolean primary, int type, CharSequence typeLabel) {
        this.id = id;
        this.number = number;
        this.primary = primary;
        this.type = type;
        this.typeLabel = typeLabel;
    }

    public PhoneNumber() {}

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
        return typeLabel;
    }

    public void setTypeLabel(CharSequence typeLabel) {
        this.typeLabel = typeLabel;
    }

    public CharSequence getTypeLabel(Resources res) {
        return ContactsContract.CommonDataKinds.Phone.getTypeLabel(res,type, typeLabel);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PhoneNumber)) return false;
        PhoneNumber number1 = (PhoneNumber) o;
        return id == number1.id
                && primary == number1.primary
                && type == number1.type
                && Check.isEquals(number, number1.number)
                && Check.isEquals(typeLabel, number1.typeLabel);
    }

    @Override
    public String toString() {
        return "PhoneNumber{" +
                ", id=" + id +
                ", number='" + number + '\'' +
                ", primary=" + primary +
                ", type=" + type +
                ", typeLable=" + typeLabel +
                '}';
    }
}
