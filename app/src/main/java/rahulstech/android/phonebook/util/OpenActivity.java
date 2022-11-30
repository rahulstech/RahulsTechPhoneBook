package rahulstech.android.phonebook.util;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.ContactsContract;

import androidx.annotation.NonNull;
import rahulstech.android.phonebook.ContactDetailsActivity;
import rahulstech.android.phonebook.ContactInputActivity;
import rahulstech.android.phonebook.R;

public final class OpenActivity {

    public static void viewContactDetails(@NonNull Activity activity, @NonNull Uri data) {
        Check.isNonNull(activity,"null == activity");
        Check.isNonNull(data,"null == data");
        Intent i = new Intent(activity, ContactDetailsActivity.class);
        i.setAction(Intent.ACTION_VIEW);
        i.setDataAndType(data,ContactsContract.Contacts.CONTENT_TYPE);
        activity.startActivity(i);
    }

    public static void addContact(@NonNull Activity activity) {
        Check.isNonNull(activity,"null == activity");
        Intent i = new Intent(activity, ContactInputActivity.class);
        i.setAction(Intent.ACTION_INSERT);
        i.setType(ContactsContract.Contacts.CONTENT_TYPE);
        activity.startActivity(i);
    }

    public static void editContact(@NonNull Activity activity, @NonNull Uri data) {
        Check.isNonNull(activity,"null == activity");
        Check.isNonNull(data,"null == data");
        Intent i = new Intent(activity, ContactInputActivity.class);
        i.setAction(Intent.ACTION_EDIT);
        i.setDataAndType(data,ContactsContract.Contacts.CONTENT_ITEM_TYPE);
        activity.startActivity(i);
    }

    public static void makeVoiceCall(@NonNull Activity activity, String number) {
        if (Check.isEmptyString(number)) return;
        Intent i = new Intent(Intent.ACTION_CALL,Uri.parse("tel:" + number));
        startActivityWithChooser(activity,i);
    }

    public static void sendSms(@NonNull Activity activity, String number) {
        if (Check.isEmptyString(number)) return;
        Intent i = new Intent(Intent.ACTION_VIEW,Uri.parse("sms:"+number));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivityWithChooser(activity,i);
    }

    public static void sendEmail(@NonNull Activity activity, String address) {
        if (Check.isEmptyString(address)) return;
        Intent i = new Intent(Intent.ACTION_SENDTO,Uri.parse("mailto:"+address));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivityWithChooser(activity,i);
    }

    public static void viewCalender(@NonNull Activity activity, long time) {
        Intent i = new Intent(Intent.ACTION_VIEW,CalendarContract.CONTENT_URI.buildUpon()
                .appendPath("time").appendPath(String.valueOf(time)).build());
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivityWithChooser(activity,i);
    }

    public static void viewWebsite(@NonNull Activity activity, String url) {
        if (Check.isEmptyString(url)) return;
        Intent i = new Intent(Intent.ACTION_VIEW,Uri.parse(url));
        startActivityWithChooser(activity,i);
    }

    public static void openMap(@NonNull Activity activity, String address) {

    }

    public static void searchWeb(@NonNull Activity activity, String query) {
        Intent i = new Intent(Intent.ACTION_WEB_SEARCH);
        i.putExtra(SearchManager.QUERY,query);
        startActivityWithChooser(activity,i);
    }

    public static void startActivityWithChooser(@NonNull Activity activity, @NonNull Intent intent) {
        Check.isNonNull(activity,"null == activity");
        Check.isNonNull(intent,"null == intent");
        activity.startActivity(Intent.createChooser(intent,activity.getString(R.string.label_intent_chooser)));
    }


}
