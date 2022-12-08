package rahulstech.android.phonebook.view;

import android.net.Uri;
import android.text.TextUtils;
import android.widget.Filter;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import rahulstech.android.phonebook.model.ContactDetails;
import rahulstech.android.phonebook.model.Name;
import rahulstech.android.phonebook.model.RawContact;
import rahulstech.android.phonebook.util.Check;

import static rahulstech.android.phonebook.util.Check.isEmptyString;
import static rahulstech.android.phonebook.util.Helpers.logDebug;

public class ContactFilter extends Filter {

    private static final String TAG = "ContactFilter";

    public enum FilterType {
        NAME,
        NUMBER,
        NAME_NUMBER
    }

    private List<ContactDetails> contacts;
    private FilterType filterType;

    private ArrayList<WeakReference<PublishResultCallback>> callbacks;

    public ContactFilter(@NonNull PublishResultCallback callback) {
        Check.isNonNull(callback,"null == callback");
        callbacks = new ArrayList<>();
        addPublishResultCallback(callback);
    }

    public void addPublishResultCallback(PublishResultCallback callback) {
        if (null == callback) return;
        callbacks.add(new WeakReference<>(callback));
    }

    public void filter(@Nullable CharSequence constraint, @Nullable List<ContactDetails> contacts,
                       @NonNull FilterType filterType) {
        Check.isNonNull(filterType,"null == filterType");
        this.contacts = contacts;
        this.filterType = filterType;
        super.filter(constraint);
    }

    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
        final List<ContactDetails> current = this.contacts;
        final int count = null == current ? 0 : current.size();
        final FilterType filterType = this.filterType;
        FilterResults output = new FilterResults();
        if (0==count) {
            output.values = new Result(constraint,null,filterType);
            output.count = 0;
        }
        else if (null == constraint || constraint.length() == 0) {
            output.values = new Result(null,current,filterType);
            output.count = count;
        }
        else {
            ArrayList<ContactDetails> filtered = new ArrayList<>();
            String phrase = constraint.toString().toLowerCase();
            for (ContactDetails contact : current) {
                Name name = contact.getName();
                if (null == name) continue;
                String displayName = name.getDisplayName();
                boolean matched = matchByName(phrase,displayName);
                if (matched) filtered.add(contact);
            }
            output.values = new Result(constraint,filtered,filterType);
            output.count = filtered.size();
        }
        return output;
    }

    private boolean matchByName(String phrase, String name) {
        if (isEmptyString(name)) return false;
        return TextUtils.indexOf(name.toLowerCase(),phrase) >= 0;
    }

    @Override
    protected void publishResults(CharSequence constraint, FilterResults results) {
        for (WeakReference<PublishResultCallback> ref : callbacks) {
            PublishResultCallback callback = ref.get();
            if (null != callback) {
                Result values = (Result) results.values;
                callback.publish(values);
            }
        }
    }

    public interface PublishResultCallback {
        void publish(@NonNull Result result);
    }

    public static class Result {
        @Nullable
        final public CharSequence constraint;
        @Nullable
        final public List<ContactDetails> contacts;
        @NonNull
        final public FilterType filterType;
        Result(@Nullable CharSequence constraint, @Nullable List<ContactDetails> contacts, @NonNull FilterType filterType) {
            this.constraint = constraint;
            this.contacts = contacts;
            this.filterType = filterType;
        }
    }
}
