package rahulstech.android.phonebook.view;

import android.widget.Filter;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import rahulstech.android.phonebook.model.ContactDisplay;
import rahulstech.android.phonebook.util.Check;

public class ContactFilter extends Filter {

    public enum FilterType {
        NAME,
        NUMBER,
        NAME_NUMBER
    }

    private List<ContactDisplay> contacts;
    private FilterType filterType;

    private WeakReference<PublishResultCallback> callbackRef;

    public ContactFilter(@NonNull PublishResultCallback callback) {
        Check.isNonNull(callback,"null == callback");
        this.callbackRef = new WeakReference<>(callback);
    }

    public void filter(@Nullable CharSequence constraint, @Nullable List<ContactDisplay> current,
                       FilterType filterType) {
        this.contacts = current;
        this.filterType = filterType;
        super.filter(constraint);
    }

    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
        final List<ContactDisplay> current = this.contacts;
        final FilterType filterType = this.filterType;
        Result values = new Result();
        values.constraint = constraint;
        if (null == current || current.isEmpty()) {
            values.contacts = null;
        }
        else if (null == constraint || constraint.length() == 0) {
            values.contacts = current;
            values.constraint = null;
        }
        else {
            ArrayList<ContactDisplay> filtered = new ArrayList<>();
            String phrase = constraint.toString().toLowerCase();
            for (ContactDisplay display : current) {
                String name = display.getContact().getDisplayNamePrimary();
                if (matchByName(phrase,name)) filtered.add(display);
            }
            values.contacts = filtered.isEmpty() ? null : filtered;
        }
        FilterResults output = new FilterResults();
        output.values = values;
        output.count = null == values.contacts ? 0 : values.contacts.size();
        return output;
    }

    private boolean matchByName(String phrase, String name) {
        if (Check.isEmptyString(name)) return false;
        return name.toLowerCase().contains(phrase);
    }

    @Override
    protected void publishResults(CharSequence constraint, FilterResults results) {
        PublishResultCallback callback = callbackRef.get();
        if (null != callback) {
            Result values = (Result) results.values;
            callback.publish(values.constraint,values.contacts);
        }
    }

    public interface PublishResultCallback {
        void publish(@Nullable CharSequence constraint, @Nullable List<ContactDisplay> filtered);
    }

    public static class Result {
        @Nullable
        public CharSequence constraint;
        @Nullable
        public List<ContactDisplay> contacts;
    }
}
