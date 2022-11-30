package rahulstech.android.phonebook.model;

import android.content.res.Resources;
import android.provider.ContactsContract;

import androidx.annotation.Nullable;
import rahulstech.android.phonebook.util.Check;

import static rahulstech.android.phonebook.util.Helpers.anyNonEmpty;
import static rahulstech.android.phonebook.util.Helpers.joinNonEmpty;

public class PostalAddress {

    private long id;

    private String formattedAddress;
    
    private int type;
    
    private CharSequence typeLabel;

    public PostalAddress(long id, String formattedAddress, int type, CharSequence typeLabel) {
        this.id = id;
        this.formattedAddress = formattedAddress;
        this.type = type;
        this.typeLabel = typeLabel;
    }

    public PostalAddress() {}

    public long getId() {
        return id;
    }

    public String getFormattedAddress() {
        return formattedAddress;
    }

    public int getType() {
        return type;
    }

    public CharSequence getTypeLabel() {
        return typeLabel;
    }

    public CharSequence getTypeLabel(Resources res) {
        return ContactsContract.CommonDataKinds.StructuredPostal.getTypeLabel(res,type,typeLabel);
    }

    public void setFormattedAddress(String formattedAddress) {
        this.formattedAddress = formattedAddress;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setTypeLabel(CharSequence typeLabel) {
        this.typeLabel = typeLabel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PostalAddress)) return false;
        PostalAddress that = (PostalAddress) o;
        return type == that.type
                && id == that.id
                && Check.isEquals(formattedAddress, that.formattedAddress)
                && Check.isEquals(typeLabel, that.typeLabel);
    }

    @Override
    public String toString() {
        return "PostalAddress{" +
                "id=" + id +
                ", formattedAddress='" + formattedAddress + '\'' +
                ", type=" + type +
                ", typeLabel=" + typeLabel +
                '}';
    }
}
