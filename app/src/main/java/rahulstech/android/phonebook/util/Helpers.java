package rahulstech.android.phonebook.util;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import rahulstech.android.phonebook.R;
import rahulstech.android.phonebook.model.ContactDetails;
import rahulstech.android.phonebook.model.ContactDisplay;
import rahulstech.android.phonebook.model.Name;
import rahulstech.android.phonebook.view.ContactListAdapter;

import static rahulstech.android.phonebook.BuildConfig.DEBUG;
import static rahulstech.android.phonebook.util.DrawableUtil.getMinimumDimension;
import static rahulstech.android.phonebook.util.DrawableUtil.roundedTextDrawable;
import static rahulstech.android.phonebook.util.DrawableUtil.vectorDrawable;

public class Helpers {

    public static String joinNonEmpty(String glue, String... parts) {
        String output = "";
        for (int i=0; i<parts.length; i++) {
            String s = parts[i];
            if (Check.isEmptyString(s)) continue;
            if (Check.isEmptyString(output)) output += s;
            else output += glue+s;
        }
        return output;
    }

    public static boolean areAllNonEmpty(String... parts) {
        for (String s : parts) {
            if (Check.isEmptyString(s)) return false;
        }
        return true;
    }

    public static boolean anyNonEmpty(String... parts) {
        for (String s : parts) {
            if (!Check.isEmptyString(s)) return true;
        }
        return false;
    }

    public static  String firstNonEmptyString(String... strings) {
        for (String s : strings) {
            if (!Check.isEmptyString(s)) return s;
        }
        return null;
    }

    @Nullable
    public static String getDisplayLabel(@NonNull ContactSorting sorting, @Nullable Name name, @Nullable String alternative){
        Check.isNonNull(sorting,"null == sorting");
        String text;
        if (null == name || Check.isEmptyString(name.getDisplayName())) {
            // case 1: no structured name available
            //          then use system provided alternative display name
            text = alternative;
        }
        else if (sorting.isDisplayFirstNameFirst()) {
            text = firstNonEmptyString(
                    name.getGivenName(), name.getFamilyName(),
                    name.getMiddleName(),
                    name.getSuffix(), name.getPrefix()
            );
        }
        else {
            text = firstNonEmptyString(
                    name.getFamilyName(), name.getGivenName(),
                    name.getMiddleName(),
                    name.getSuffix(), name.getPrefix()
            );
        }
        if (Check.isEmptyString(text)) return null;
        return text.substring(0,1).toUpperCase();
    }

    @NonNull
    public static Drawable createContactPhotoPlaceholder( @NonNull ContactDetails details, @NonNull ContactSorting sorting,
                                                          @NonNull View view, @Nullable Drawable ifNull) {
        Check.isNonNull(details,"null == details");
        Check.isNonNull(sorting,"null == sorting");
        Check.isNonNull(view,"null == view");
        Name name = details.getName();
        String alternative = details.getContact().getDisplayName(sorting.isDisplayFirstNameFirst());
        String label = getDisplayLabel(sorting,name,alternative);
        int radius = getMinimumDimension(view)/2;
        return roundedTextDrawable(label,0,1,radius,ifNull);
    }

    public static void logDebug(String tag, String message) {
        if (DEBUG) Log.d(tag,message);
    }
}
