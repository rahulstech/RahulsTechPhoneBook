package rahulstech.android.phonebook.model;

import android.provider.ContactsContract;

import rahulstech.android.phonebook.util.Check;

public class Name {

    private String lookupKey;

    private long id;

    private String displayName;

    private String givenName;

    private String familyName;

    private String prefix;

    private String middleName;

    private String suffix;

    private String phoneticGivenName;

    private String phoneticMiddleName;

    private String phoneticFamilyName;

    public Name(String lookupKey, long id, String displayName, String givenName, String familyName, String prefix, String middleName, String suffix, String phoneticGivenName, String phoneticMiddleName, String phoneticFamilyName) {
        this.lookupKey = lookupKey;
        this.id = id;
        this.displayName = displayName;
        this.givenName = givenName;
        this.familyName = familyName;
        this.prefix = prefix;
        this.middleName = middleName;
        this.suffix = suffix;
        this.phoneticGivenName = phoneticGivenName;
        this.phoneticMiddleName = phoneticMiddleName;
        this.phoneticFamilyName = phoneticFamilyName;
    }

    public Name(String displayName) {
        this.displayName = displayName;
    }

    public Name(String givenName, String familyName, String prefix, String middleName, String suffix) {
        this.givenName = givenName;
        this.familyName = familyName;
        this.prefix = prefix;
        this.middleName = middleName;
        this.suffix = suffix;
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

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public String getPhoneticGivenName() {
        return phoneticGivenName;
    }

    public void setPhoneticGivenName(String phoneticGivenName) {
        this.phoneticGivenName = phoneticGivenName;
    }

    public String getPhoneticMiddleName() {
        return phoneticMiddleName;
    }

    public void setPhoneticMiddleName(String phoneticMiddleName) {
        this.phoneticMiddleName = phoneticMiddleName;
    }

    public String getPhoneticFamilyName() {
        return phoneticFamilyName;
    }

    public void setPhoneticFamilyName(String phoneticFamilyName) {
        this.phoneticFamilyName = phoneticFamilyName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Name)) return false;
        Name name = (Name) o;
        return id == name.id
                && Check.isEquals(lookupKey, name.lookupKey)
                && Check.isEquals(displayName, name.displayName)
                && Check.isEquals(givenName, name.givenName)
                && Check.isEquals(familyName, name.familyName)
                && Check.isEquals(prefix, name.prefix)
                && Check.isEquals(middleName, name.middleName)
                && Check.isEquals(suffix, name.suffix)
                && Check.isEquals(phoneticGivenName, name.phoneticGivenName)
                && Check.isEquals(phoneticMiddleName, name.phoneticMiddleName)
                && Check.isEquals(phoneticFamilyName, name.phoneticFamilyName);
    }
}
