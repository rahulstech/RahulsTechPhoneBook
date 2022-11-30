package rahulstech.android.phonebook.util;

public enum ContactSorting {
    FIRSTNAME_FIRST,
    LASTNAME_FIRST,
    FIRSTNAME_FIRST_DESC,
    LASTNAME_FIRST_DESC;

    public boolean isDisplayFirstNameFirst() {
        return this == FIRSTNAME_FIRST || this == FIRSTNAME_FIRST_DESC;
    }
}