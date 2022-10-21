package rahulstech.android.phonebook.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Queue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import rahulstech.android.phonebook.R;
import rahulstech.android.phonebook.concurrent.AsyncTask;
import rahulstech.android.phonebook.model.ContactDisplay;

public class ContactListAdapter extends RecyclerView.Adapter<ContactListAdapter.ContactListItemViewHolder> {

    private DiffUtil.ItemCallback<ListItem> DIFF_ITEM_CALLBACK = new DiffUtil.ItemCallback<ListItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull ListItem oldItem, @NonNull ListItem newItem) {
            return oldItem.itemType == newItem.itemType;
        }

        @Override
        public boolean areContentsTheSame(@NonNull ListItem oldItem, @NonNull ListItem newItem) {
            return oldItem.equals(newItem);
        }
    };

    private AsyncListDiffer<ListItem> mDiffer;
    private LayoutInflater inflater;
    private List<ContactDisplay> original = Collections.EMPTY_LIST;

    private AsyncTask asyncTask = new AsyncTask();
    private AsyncTask.AsyncTaskCallback callback = new AsyncTask.AsyncTaskCallback() {
        @Override
        public void onError(AsyncTask asyncTask, AsyncTask.Task task, Throwable error) {}

        @Override
        public void onResult(AsyncTask asyncTask, AsyncTask.Task task) {
            if (1 == task.getTaskId()) {
                changeListItems(changeItemTask.getResult());
            }
            else if (2 == task.getTaskId()) {
                runChangeItemsTask(filterItemTask.getResult());
            }
        }

        @Override
        public void onCanceled(AsyncTask asyncTask, AsyncTask.Task task) {}

        @Override
        public void onShutdown(AsyncTask asyncTask, Queue<AsyncTask.Task> notExecutedTasks) {}
    };

    private ChangeItemTask changeItemTask = null;
    private FilterItemTask filterItemTask = null;

    public ContactListAdapter(@NonNull Context context) {
        inflater = LayoutInflater.from(context);
        mDiffer = new AsyncListDiffer<>(this,DIFF_ITEM_CALLBACK);
        asyncTask.setAsyncTaskCallback(callback);
    }

    public void changeContacts(List<ContactDisplay> contacts) {
        runChangeItemsTask(contacts);
        changeOriginalContacts(contacts);
    }

    @NonNull
    @Override
    public ContactListItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ContactListItemViewHolder(inflater.inflate(R.layout.contact_list_item,parent,false));
    }

    @Override
    public void onBindViewHolder(@NonNull ContactListItemViewHolder holder, int position) {
        holder.bind(mDiffer.getCurrentList().get(position));
    }

    @Override
    public int getItemCount() {
        return mDiffer.getCurrentList().size();
    }

    public void filter(String phrase) {
        if (null == phrase || "".equals(phrase)) {
            runChangeItemsTask(original);
        }
        else {
            runFilterItemsTask(phrase,original);
        }
    }

    private void runChangeItemsTask(List<ContactDisplay> contacts) {
        if (null != changeItemTask) {
            changeItemTask.cancel();
        }
        changeItemTask = new ChangeItemTask(contacts);
        asyncTask.enqueue(changeItemTask);
    }

    private void runFilterItemsTask(String phrase, List<ContactDisplay> original) {
        if (null != filterItemTask) {
            filterItemTask.cancel();
        }
        filterItemTask = new FilterItemTask(phrase,original);
        asyncTask.enqueue(filterItemTask);
    }

    private void changeOriginalContacts(List<ContactDisplay> contacts) {
        this.original = contacts;
    }

    private void changeListItems(List<ListItem> newItems) {
        mDiffer.submitList(newItems);
    }

    public static class ListItem {

        public static final int TYPE_HEADER = 1;

        public static final int TYPE_CONTACT = 2;

        String header;
        ContactDisplay contact;

        int itemType;

        public ListItem() {}

        ListItem setHeader(String header) {
            this.header = header;
            this.contact = null;
            this.itemType = TYPE_HEADER;
            return this;
        }

        ListItem setContact(ContactDisplay contact) {
            this.header = null;
            this.contact = contact;
            this.itemType = TYPE_CONTACT;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ListItem)) return false;
            ListItem that = (ListItem) o;
            if (this.itemType != that.itemType) return false;
            if (TYPE_HEADER == this.itemType) return Objects.equals(this.header,that.header);
            else return Objects.equals(this.contact,that.contact);
        }

        @Override
        public int hashCode() {
            return Objects.hash(header, contact, itemType);
        }
    }

    public static class ContactListItemViewHolder extends RecyclerView.ViewHolder {

        View itemHeader;
        View itemContact;

        TextView header;

        TextView contactName;
        ImageView thumbnail;

        public ContactListItemViewHolder(@NonNull View v) {
            super(v);
            itemHeader = v.findViewById(R.id.item_header);
            itemContact = v.findViewById(R.id.item_contact);
            header = v.findViewById(R.id.header);
            contactName = v.findViewById(R.id.contact_name);
            thumbnail = v.findViewById(R.id.contact_thumbnail);
        }

        public void bind(@NonNull ListItem item) {
            int itemType = item.itemType;
            if (ListItem.TYPE_CONTACT == itemType) {
                itemHeader.setVisibility(View.GONE);
                itemContact.setVisibility(View.VISIBLE);
                bindContact(item.contact);
            }
            else {
                itemContact.setVisibility(View.GONE);
                itemHeader.setVisibility(View.VISIBLE);
                bindHeader(item.header);
            }
        }

        void bindContact(@Nullable ContactDisplay display) {
            contactName.setText(display.getDisplayName());
        }

        void bindHeader(String headerText) {
            header.setText(headerText);
        }
    }

    private static class ChangeItemTask extends AsyncTask.Task {

        final List<ContactDisplay> newContacts;

        public ChangeItemTask(List<ContactDisplay> newContacts) {
            super(1);
            this.newContacts = newContacts;
        }

        public List<ContactDisplay> getNewContacts() {
            return newContacts;
        }

        @Override
        public void execute() {
            if (isCanceled()) return;
            List<ListItem> items = buildListItems(newContacts);
            setResult(items);
        }

        private List<ListItem> buildListItems(List<ContactDisplay> contacts) {
            List<ListItem> items = new ArrayList<>();
            Iterator<ContactDisplay> it = contacts.iterator();
            String lastHeader = null;
            while (it.hasNext()) {
                ContactDisplay contact = it.next();
                String header = createSectionHeader(contact);
                if (!Objects.equals(lastHeader,header)) {
                    ListItem headerItem = new ListItem().setHeader(header);
                    items.add(headerItem);
                    lastHeader = header;
                }
                ListItem realItem = new ListItem().setContact(contact);
                items.add(realItem);
            }
            return items;
        }

        private String createSectionHeader(ContactDisplay realItem) {
            String displayName = realItem.getDisplayName();
            String firstLetter = displayName.substring(0,1);
            String headerText = firstLetter.toUpperCase()+firstLetter.toLowerCase();
            return headerText;
        }
    }

    private static class FilterItemTask extends AsyncTask.Task {

        final String phrase;
        final List<ContactDisplay> original;

        public FilterItemTask(String phrase, List<ContactDisplay> original) {
            super(2);
            this.phrase = phrase;
            this.original = original;
        }

        public String getPhrase() {
            return phrase;
        }

        public List<ContactDisplay> getOriginal() {
            return original;
        }

        @Override
        public void execute() {
            List<ContactDisplay> result = new ArrayList<>();
            Iterator<ContactDisplay> it = original.iterator();
            while (it.hasNext()) {
                if (isCanceled()) return;
                ContactDisplay contact = it.next();
                if (isAllowed(contact,phrase)) {
                    result.add(contact);
                }
            }
            setResult(result);
        }

        boolean isAllowed(ContactDisplay contact, String phrase) {
            String displayName = contact.getDisplayName();
            return displayName.contains(phrase);
        }
    }
}
