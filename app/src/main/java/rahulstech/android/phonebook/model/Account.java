package rahulstech.android.phonebook.model;

import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import rahulstech.android.phonebook.util.Check;

public class Account {

    // TODO: compute the type name for account
    // for example type=com.google typename = Google

    private String lookupKey;

    private long id;

    private String name;

    private String type;

    private Drawable logo;

    public Account(String lookupKey, long id, String name, String type) {
        this.lookupKey = lookupKey;
        this.id = id;
        this.name = name;
        this.type = type;
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
        if (!(o instanceof Account)) return false;
        Account account = (Account) o;
        return id == account.id
                && Check.isEquals(lookupKey,account.lookupKey)
                && Check.isEquals(name, account.name)
                && Check.isEquals(type, account.type);
    }
}
