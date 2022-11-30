package rahulstech.android.phonebook.model;

import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import rahulstech.android.phonebook.util.Check;

public class RawContact {

    // TODO: compute the type name for account
    // for example type=com.google typename = Google

    private String lookupKey;

    private long id;

    private String name;

    private String type;

    private Drawable logo;

    public RawContact(String lookupKey, long id, String name, String type) {
        this.lookupKey = lookupKey;
        this.id = id;
        this.name = name;
        this.type = type;
    }

    public RawContact(String type, String name) {
        this.type = type;
        this.name = name;
    }

    public String getLookupKey() {
        return lookupKey;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public Drawable getLogo(@NonNull Context context) {
        Check.isNonNull(context,"null == context");
        if (null == logo) {
            AccountManager am = ContextCompat.getSystemService(context,AccountManager.class);
            if (null != am) {
                AuthenticatorDescription[] descriptions = am.getAuthenticatorTypes();
                if (null != descriptions) {
                    PackageManager pm = context.getPackageManager();
                    for (AuthenticatorDescription d : descriptions) {
                        if (d.type.equals(type)) {
                            try {
                                return pm.getApplicationLogo(d.packageName);
                            } catch (Exception ignore) {
                                logo = null;
                            }
                        }
                    }
                }
            }
        }
        return logo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RawContact)) return false;
        RawContact rawContact = (RawContact) o;
        return id == rawContact.id
                && Check.isEquals(lookupKey, rawContact.lookupKey)
                && Check.isEquals(name, rawContact.name)
                && Check.isEquals(type, rawContact.type);
    }
}
