package rahulstech.android.phonebook.util;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import rahulstech.android.phonebook.R;
import rahulstech.android.phonebook.model.ContactDetails;
import rahulstech.android.phonebook.model.Name;

import static rahulstech.android.phonebook.BuildConfig.DEBUG;
import static rahulstech.android.phonebook.util.DrawableUtil.getMinimumDimension;
import static rahulstech.android.phonebook.util.DrawableUtil.roundedTextDrawable;

public class Helpers {

    public static float dpToPx(@NonNull Context context, float dp) {
        Check.isNonNull(context,"null == context");
        return context.getResources().getDisplayMetrics().density*dp;
    }

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

    @NonNull
    public static Drawable createContactPhotoPlaceholder( @NonNull ContactDetails details, @NonNull ContactSorting sorting,
                                                          @NonNull View view, @Nullable Drawable ifNull) {
        Check.isNonNull(details,"null == details");
        Check.isNonNull(sorting,"null == sorting");
        Check.isNonNull(view,"null == view");
        String label = details.getDisplayLabel(sorting);
        int radius = getMinimumDimension(view)/2;
        return roundedTextDrawable(label,0,1,radius,ifNull);
    }

    @NonNull
    public static AlertDialog createMessage(@NonNull Context context, @StringRes int message) {
        Check.isNonNull(context,"null == context");
        return createMessage(context,context.getString(message));
    }

    @NonNull
    public static AlertDialog createMessage(@NonNull Context context, @NonNull String message) {
        Check.isNonNull(context,"null == context");
        Check.isNonEmptyString(message,"empty message");
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setPositiveButton(R.string.label_ok,null)
                .setMessage(message).create();
        return dialog;
    }

    public static void logDebug(String tag, String message) {
        if (DEBUG) Log.d(tag,message);
    }
}
