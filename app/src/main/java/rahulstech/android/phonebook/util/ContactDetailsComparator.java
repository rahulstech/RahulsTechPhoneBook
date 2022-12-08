package rahulstech.android.phonebook.util;

import java.util.Comparator;

import androidx.annotation.NonNull;
import rahulstech.android.phonebook.model.ContactDetails;

import static rahulstech.android.phonebook.util.Check.isEmptyString;

public class ContactDetailsComparator implements Comparator<ContactDetails> {

    private static final String TAG = "ContactDetailsComp";

    private final ContactSorting sorting;
    private final boolean ascending;

    public ContactDetailsComparator(@NonNull ContactSorting sorting) {
        this.sorting = sorting;
        this.ascending = sorting.isAscending();
    }

    @Override
    public int compare(ContactDetails left, ContactDetails right) {
        if (!ascending) {
            ContactDetails tmp = left;
            left = right;
            right = tmp;
        }
        String leftSortKey = left.getSortingKey(sorting);
        String rightSortKey = right.getSortingKey(sorting);
        String leftDisplayName = left.getDisplayName(sorting);
        String rightDisplayName = right.getDisplayName(sorting);

        if (isEmptyString(leftSortKey) && isEmptyString(rightSortKey)) return 0;
        if (ascending) {
            if (isEmptyString(leftSortKey)) return 1;
            if (isEmptyString(rightSortKey)) return -1;
        }
        else {
            if (isEmptyString(leftSortKey)) return -1;
            if (isEmptyString(rightSortKey)) return 1;
        }
        int diff = leftSortKey.compareToIgnoreCase(rightSortKey);
        if (0==diff) {
            diff = leftDisplayName.compareToIgnoreCase(rightDisplayName);
        }
        return diff;
    }
}
