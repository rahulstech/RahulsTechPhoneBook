package rahulstech.android.phonebook.model;

import androidx.annotation.NonNull;
import rahulstech.android.phonebook.util.Check;
import rahulstech.android.phonebook.util.ContactSorting;

import static rahulstech.android.phonebook.util.Check.isEmptyString;
import static rahulstech.android.phonebook.util.Helpers.anyNonEmpty;
import static rahulstech.android.phonebook.util.Helpers.joinNonEmpty;
import static rahulstech.android.phonebook.util.Helpers.logDebug;

public class Name {

    public static final Name UNKNOWN_NAME = new Name(0,"Unknown","Unknown",
            null,null,null,null,null,null,null);

    public static final String[] PREFIXES = new String[] {
            "MR","MR.","MRS","MRS.","MISS","MISS.","DR","DR."
    };

    public static final String[] SUFFIXES = new String[] {
            "JR","JR."
    };

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

    private String phoneticName;

    private String nickname;

    public Name(long id, String displayName, String givenName, String familyName, String prefix, String middleName, String suffix, String phoneticGivenName, String phoneticMiddleName, String phoneticFamilyName) {
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

    public Name() {}

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

    public String getPhoneticName() {
        return phoneticName;
    }

    public void setPhoneticName(String phoneticName) {
        this.phoneticName = phoneticName;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Name)) return false;
        Name name = (Name) o;
        return id == name.id
                && Check.isEquals(displayName, name.displayName)
                && Check.isEquals(givenName, name.givenName)
                && Check.isEquals(familyName, name.familyName)
                && Check.isEquals(prefix, name.prefix)
                && Check.isEquals(middleName, name.middleName)
                && Check.isEquals(suffix, name.suffix)
                && Check.isEquals(phoneticGivenName, name.phoneticGivenName)
                && Check.isEquals(phoneticMiddleName, name.phoneticMiddleName)
                && Check.isEquals(phoneticFamilyName, name.phoneticFamilyName)
                && Check.isEquals(nickname, name.nickname);
    }

    @Override
    public String toString() {
        return "Name{" +
                "id=" + id +
                ", displayName='" + displayName + '\'' +
                ", givenName='" + givenName + '\'' +
                ", familyName='" + familyName + '\'' +
                ", prefix='" + prefix + '\'' +
                ", middleName='" + middleName + '\'' +
                ", suffix='" + suffix + '\'' +
                ", phoneticGivenName='" + phoneticGivenName + '\'' +
                ", phoneticMiddleName='" + phoneticMiddleName + '\'' +
                ", phoneticFamilyName='" + phoneticFamilyName + '\'' +
                ", nickname='" + nickname + '\'' +
                '}';
    }

    public void splitDisplayName() {
        final String displayName = this.displayName;
        if (isEmptyString(displayName)) {
            this.prefix = this.givenName = this.middleName =this.familyName = this.suffix = null;
            return;
        }
        String[] parts = displayName.split("\\s+");
        String prefix,givenName,middleName,familyName,suffix;
        if (null==parts || 0==parts.length) {
            this.prefix = this.givenName = this.middleName =this.familyName = this.suffix = null;
            return;
        }
        int idxGivenName, idxFamilyName, idxMiddleNameStart, idxMiddleNameEnd, max = parts.length - 1;
        if (!isPrefix(parts[0])) {
            prefix = null;
            idxGivenName = 0;
        } else {
            prefix = parts[0];
            idxGivenName = 1;
        }
        if (!isSuffix(parts[max])) {
            suffix = null;
            idxFamilyName = max;
        } else {
            suffix = parts[max];
            idxFamilyName = max - 1;
        }
        givenName = parts[idxGivenName];
        if (idxFamilyName > idxGivenName) familyName = parts[idxFamilyName];
        else familyName = null;
        idxMiddleNameStart = idxGivenName + 1;
        idxMiddleNameEnd = idxFamilyName - 1;
        if (idxMiddleNameEnd > idxGivenName && idxMiddleNameStart < idxMiddleNameEnd) {
            StringBuilder builder = new StringBuilder();
            for (int i = idxMiddleNameStart; i <= idxMiddleNameEnd; i++) {
                if (i > idxMiddleNameStart) builder.append(" ");
                builder.append(parts[i]);
            }
            middleName = builder.toString();
        } else {
            middleName = null;
        }
        this.prefix = prefix;
        this.givenName = givenName;
        this.middleName = middleName;
        this.familyName = familyName;
        this.suffix = suffix;
    }

    public void splitPhoneticName(final String phoneticName) {
        if (isEmptyString(phoneticName)) {
            this.phoneticGivenName = this.phoneticMiddleName =this.phoneticFamilyName = null;
            return;
        }
        String[] parts = phoneticName.split("\\s+");
        String givenName,middleName,familyName;
        if (null==parts || 0==parts.length) {
            this.phoneticGivenName = this.phoneticMiddleName =this.phoneticFamilyName = null;
            return;
        }
        int max = parts.length - 1, idxGivenName = 0, idxFamilyName = max, idxMiddleNameStart = idxGivenName+1, idxMiddleNameEnd = max-1;
        givenName = parts[idxGivenName];
        if (idxFamilyName > idxGivenName) familyName = parts[idxFamilyName];
        else familyName = null;
        if (idxMiddleNameEnd > idxGivenName && idxMiddleNameStart < idxMiddleNameEnd) {
            StringBuilder builder = new StringBuilder();
            for (int i = idxMiddleNameStart; i <= idxMiddleNameEnd; i++) {
                if (i > idxMiddleNameStart) builder.append(" ");
                builder.append(parts[i]);
            }
            middleName = builder.toString();
        } else {
            middleName = null;
        }
        this.givenName = givenName;
        this.middleName = middleName;
        this.familyName = familyName;
    }

    public String buildDisplayName(@NonNull ContactSorting sorting) {
        String prefix = this.prefix;
        String givenName = this.givenName ;
        String middleName = this.middleName;
        String familyName = this.familyName;
        String suffix = this.suffix;
        if (sorting.isDisplayFirstNameFirst()) {
            return joinNonEmpty(" ",prefix,givenName,middleName,familyName,suffix);
        }
        else {
            String leading = joinNonEmpty(prefix,familyName);
            String trailing = joinNonEmpty(middleName,givenName,suffix);
            return joinNonEmpty(", ",leading,trailing);
        }
    }

    public void buildDisplayNameFirstNameFirst() {
        setDisplayName(buildDisplayName(ContactSorting.FIRSTNAME_FIRST));
    }

    public String buildPhoneticName() {
        return joinNonEmpty(" ",phoneticGivenName,phoneticMiddleName,phoneticFamilyName);
    }

    private boolean isPrefix(String check) {
        for (String prefix : PREFIXES) {
            if (prefix.compareToIgnoreCase(check)==0) return true;
        }
        return false;
    }

    private boolean isSuffix(String check) {
        for (String suffix : SUFFIXES) {
            if (suffix.compareToIgnoreCase(check)==0) return true;
        }
        return false;
    }


}
