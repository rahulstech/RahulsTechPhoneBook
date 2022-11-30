package rahulstech.android.phonebook.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import rahulstech.android.phonebook.ContactsListActivity;

public class Settings {

    private static final String NAME = "phonebook_settings";

    private static final String KEY_CONTACT_SORTING = "contact_sorting";

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
}
