package rahulstech.android.phonebook.util;

public enum ContactSorting {
    FIRSTNAME_FIRST,
    LASTNAME_FIRST,
    FIRSTNAME_FIRST_DESC,
    LASTNAME_FIRST_DESC;

    public boolean isDisplayFirstNameFirst() {
        return this == FIRSTNAME_FIRST || this == FIRSTNAME_FIRST_DESC;
    }

    public boolean isDisplayLastNameFirst() {
        return this == LASTNAME_FIRST || this == LASTNAME_FIRST_DESC;
    }

    public boolean isAscending() {
        return this == FIRSTNAME_FIRST || this == LASTNAME_FIRST;
    }

    public boolean isDescending() {
        return this == FIRSTNAME_FIRST_DESC || this == LASTNAME_FIRST_DESC;
    }
}