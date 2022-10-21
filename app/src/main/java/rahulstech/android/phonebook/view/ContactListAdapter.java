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

    private OnListItemClickListener itemClickListener = null;

    private ChangeItemTask changeItemTask = null;
    private FilterItemTask filterItemTask = null;

    public ContactListAdapter(@NonNull Context context) {
        inflater = LayoutInflater.from(context);
        mDiffer = new AsyncListDiffer<>(this,DIFF_ITEM_CALLBACK);
    }

    public void setOnItemClickListener(OnListItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    public void changeContacts(List<ContactDisplay> contacts) {
        runChangeItemsTask(contacts);
        changeOriginalContacts(contacts);
    }

    public ListItem getItem(int position) {
        return mDiffer.getCurrentList().get(position);
    }

    @NonNull
    @Override
    public ContactListItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ContactListItemViewHolder vh = new ContactListItemViewHolder(this,
                inflater.inflate(R.layout.contact_list_item,parent,false));
        vh.setOnListItemClickListener(itemClickListener);
        return vh;
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
        changeItemTask = new ChangeItemTask(contacts, new AsyncTask.AsyncTaskCallback<Void, List<ListItem>>(){

            @Override
            public void onResult(List<ListItem> result) {
                changeListItems(result);
            }
        });
        changeItemTask.execute(null);
    }

    private void runFilterItemsTask(String phrase, List<ContactDisplay> original) {
        if (null != filterItemTask) {
            filterItemTask.cancel();
        }
        filterItemTask = new FilterItemTask(phrase,original, new AsyncTask.AsyncTaskCallback<Void, List<ContactDisplay>>(){
            @Override
            public void onResult(List<ContactDisplay> result) {
                runChangeItemsTask(result);
            }
        });
        filterItemTask.execute(null);
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

        public String header;
        public ContactDisplay contact;

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

    public interface OnListItemClickListener {

        void onClickListItem(RecyclerView.Adapter<?> adapter, int position);
    }

    public static class ContactListItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        View itemHeader;
        View itemContact;

        TextView header;

        TextView contactName;
        ImageView thumbnail;

        ContactListAdapter adapter;
        OnListItemClickListener itemClickListener;

        public ContactListItemViewHolder(ContactListAdapter adapter, @NonNull View v) {
            super(v);
            itemHeader = v.findViewById(R.id.item_header);
            itemContact = v.findViewById(R.id.item_contact);
            header = v.findViewById(R.id.header);
            contactName = v.findViewById(R.id.contact_name);
            thumbnail = v.findViewById(R.id.contact_thumbnail);
            this.adapter = adapter;
            itemContact.setOnClickListener(this);
        }

        public void setOnListItemClickListener(OnListItemClickListener itemClickListener) {
            this.itemClickListener = itemClickListener;
        }

        @Override
        public void onClick(View v) {
            if (v == itemContact) {
                if (null != itemClickListener)
                    itemClickListener.onClickListItem(adapter,getAdapterPosition());
            }
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

    private static class ChangeItemTask extends AsyncTask<Void,List<ListItem>> {

        final List<ContactDisplay> newContacts;

        public ChangeItemTask(List<ContactDisplay> newContacts,AsyncTaskCallback<Void,List<ListItem>> callback) {
            this.newContacts = newContacts;
            setAsyncTaskCallback(callback);
        }

        public List<ContactDisplay> getNewContacts() {
            return newContacts;
        }

        @Override
        protected List<ListItem> onExecuteTask(Void args) throws Exception {
            List<ListItem> items = buildListItems(newContacts);
            return items;
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

    private static class FilterItemTask extends AsyncTask<Void,List<ContactDisplay>> {

        final String phrase;
        final List<ContactDisplay> original;

        public FilterItemTask(String phrase, List<ContactDisplay> original, AsyncTaskCallback<Void,List<ContactDisplay>> callback) {
            this.phrase = phrase;
            this.original = original;
            setAsyncTaskCallback(callback);
        }

        public String getPhrase() {
            return phrase;
        }

        public List<ContactDisplay> getOriginal() {
            return original;
        }

        @Override
        protected List<ContactDisplay> onExecuteTask(Void args) throws Exception {
            List<ContactDisplay> result = new ArrayList<>();
            Iterator<ContactDisplay> it = original.iterator();
            while (it.hasNext()) {
                ContactDisplay contact = it.next();
                if (isAllowed(contact,phrase)) {
                    result.add(contact);
                }
            }
            return result;
        }

        boolean isAllowed(ContactDisplay contact, String phrase) {
            String displayName = contact.getDisplayName();
            return displayName.contains(phrase);
        }
    }
}
