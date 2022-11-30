package rahulstech.android.phonebook.model;

import android.content.res.Resources;
import android.net.Uri;
import android.provider.ContactsContract;

import java.util.Objects;

import androidx.annotation.Nullable;
import rahulstech.android.phonebook.util.Check;

public class Relation {

    private long id;

    private String displayName;
    
    private int type;
    
    private CharSequence typeLabel;

    public Relation(long id, String displayName, int type, CharSequence typeLabel) {
        this.id = id;
        this.displayName = displayName;
        this.type = type;
        this.typeLabel = typeLabel;
    }

    public Relation() {}

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
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
        return ContactsContract.CommonDataKinds.Relation.getTypeLabel(res,type,typeLabel);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Relation)) return false;
        Relation relation = (Relation) o;
        return id == relation.id && type == relation.type
                && Objects.equals(displayName, relation.displayName)
                && Objects.equals(typeLabel, relation.typeLabel);
    }

    @Override
    public String toString() {
        return "Relation{" +
                "id=" + id +
                ", displayName='" + displayName + '\'' +
                ", type=" + type +
                ", typeLabel=" + typeLabel +
                '}';
    }
}