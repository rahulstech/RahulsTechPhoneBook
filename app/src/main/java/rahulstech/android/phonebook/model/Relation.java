package rahulstech.android.phonebook.model;

import android.content.res.Resources;
import android.net.Uri;
import android.provider.ContactsContract;

import rahulstech.android.phonebook.util.Check;

public class Relation {
    // TODO: relation contact photo not properly found
    // solution: add custom contact data mapping relation data and contact lookup key
    private String lookupKey;

    private long id;

    private String displayName;

    private String relativeContactLookupKey; // causing problem for contacts with same name

    private Uri photoUri;
    
    private int type;
    
    private CharSequence typeLabel;

    public Relation(String lookupKey, long id, String displayName, int type, CharSequence typeLabel) {
        this.lookupKey = lookupKey;
        this.id = id;
        this.displayName = displayName;
        this.type = type;
        this.typeLabel = typeLabel;
    }

    public Relation() {}

    public String getLookupKey() {
        return lookupKey;
    }

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

    public String getRelativeContactLookupKey() {
        return relativeContactLookupKey;
    }

    public void setRelativeContactLookupKey(String relativeContactLookupKey) {
        this.relativeContactLookupKey = relativeContactLookupKey;
    }

    public Uri getPhotoUri() {
        return photoUri;
    }

    public void setPhotoUri(Uri photoUri) {
        this.photoUri = photoUri;
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

    public Uri getRelationContactUri() {
        if (null == relativeContactLookupKey) return null;
        return ContactsContract.Contacts.CONTENT_LOOKUP_URI.buildUpon().appendPath(relativeContactLookupKey).build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Relation)) return false;
        Relation relation = (Relation) o;
        return id == relation.id
                && Check.isEquals(displayName,relation.displayName)
                && Check.isEquals(typeLabel, relation.typeLabel);
    }
}