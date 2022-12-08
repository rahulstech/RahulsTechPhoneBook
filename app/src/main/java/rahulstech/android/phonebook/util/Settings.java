package rahulstech.android.phonebook.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import rahulstech.android.phonebook.model.RawContact;

public class Settings {

    private static final String NAME = "phonebook_settings";

    private static final String KEY_CONTACT_SORTING = "contact_sorting";

    private static final String KEY_LOCAL_CONTACT_ACCOUNT_NAME = "local_contact_account_name";

    private static final String KEY_LOCAL_CONTACT_ACCOUNT_TYPE = "local_contact_account_type";

    private static final String KEY_DISPLAY_CONTACT_ACCOUNT_NAME = "display_contact_source_name";

    private static final String KEY_DISPLAY_CONTACT_ACCOUNT_TYPE = "display_contact_source_type";

    private Context context;

    private SharedPreferences.Editor editor = null;

    public Settings(@NonNull Context context) {
        Check.isNonNull(context,"null == context");
        this.context = context;
    }

    public static Settings getInstance(@NonNull Context context) {
        return new Settings(context);
    }

    private SharedPreferences getReader() {
        return context.getSharedPreferences(NAME,Context.MODE_PRIVATE);
    }

    private SharedPreferences.Editor getWriter() {
        if (null == editor) {
            editor = getReader().edit();
        }
        return editor;
    }

    public void save() {
        editor.commit();
        editor.apply();
        editor = null;
    }

    public Settings setContactSorting(ContactSorting sorting) {
        getWriter().putString(KEY_CONTACT_SORTING,sorting.name());
        return this;
    }

    public ContactSorting getContactSorting() {
        return ContactSorting.valueOf(getReader().getString(
                KEY_CONTACT_SORTING,ContactSorting.FIRSTNAME_FIRST.name()));
    }

    public Settings setDisplayContactSource(String name, String type) {
        SharedPreferences.Editor writer = getWriter();
        writer.putString(KEY_DISPLAY_CONTACT_ACCOUNT_TYPE,type);
        writer.putString(KEY_DISPLAY_CONTACT_ACCOUNT_NAME,name);
        return this;
    }

    public Settings setLocalContactAccount(String name, String type) {
        SharedPreferences.Editor writer = getWriter();
        writer.putString(KEY_LOCAL_CONTACT_ACCOUNT_NAME,name);
        writer.putString(KEY_LOCAL_CONTACT_ACCOUNT_TYPE,type);
        return this;
    }

    public String getLocalContactAccountName() {
        return getReader().getString(KEY_LOCAL_CONTACT_ACCOUNT_NAME,null);
    }

    public String getLocalContactAccountType() {
        return getReader().getString(KEY_LOCAL_CONTACT_ACCOUNT_TYPE,null);
    }

    public String getDisplayContactSourceName() {
        return getReader().getString(KEY_DISPLAY_CONTACT_ACCOUNT_NAME,null);
    }

    public String getDisplayContactSourceType() {
        return getReader().getString(KEY_DISPLAY_CONTACT_ACCOUNT_TYPE,null);
    }
}
