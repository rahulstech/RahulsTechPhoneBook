package rahulstech.android.phonebook.model;

import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;

public class ContactAccount {

    public static final ContactAccount ALL = new ContactAccount("ALL",null,null);

    public final String displayName;
    public final String name;
    public final String type;

    @Nullable
    private Drawable icon;

    public ContactAccount(String displayName, String name, String type) {
        this.displayName = displayName;
        this.name = name;
        this.type = type;
    }

    public void setIcon(@Nullable Drawable icon) {
        this.icon = icon;
    }

    @Nullable
    public Drawable getIcon() {
        return icon;
    }
}
