package rahulstech.android.phonebook.model;

import androidx.annotation.NonNull;
import rahulstech.android.phonebook.util.Check;

public class RawContact {

    public static final RawContact ALL_SOURCE = new RawContact(null,null,"All");

    private long contactId;

    private long id;

    private String name;

    private String type;

    private String displayName;

    public RawContact(long contactId, long id, String name, String type) {
        this.contactId = contactId;
        this.id = id;
        this.name = name;
        this.type = type;
    }

    public RawContact(@NonNull RawContact src) {
        Check.isNonNull(src,"null == source");
        this.contactId = src.contactId;
        this.id = src.id;
        this.name = src.name;
        this.type = src.type;
        this.displayName = src.displayName;
    }

    public RawContact(String type, String name, String displayName) {
        this.type = type;
        this.name = name;
        this.displayName = displayName;
    }

    public long getContactId() {
        return contactId;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RawContact)) return false;
        RawContact rawContact = (RawContact) o;
        return id == rawContact.id
                && Check.isEquals(contactId, rawContact.contactId)
                && Check.isEquals(name, rawContact.name)
                && Check.isEquals(type, rawContact.type);
    }

    @Override
    public String toString() {
        return "RawContact{" +
                "contactId=" + contactId +
                ", id=" + id +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", displayName='" + displayName + '\'' +
                '}';
    }
}
