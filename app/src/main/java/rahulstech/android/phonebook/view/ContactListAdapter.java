package rahulstech.android.phonebook.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import rahulstech.android.phonebook.R;
import rahulstech.android.phonebook.model.ContactDisplay;

public class ContactListAdapter extends AbsSectionedRecyclerListViewAdapter<ContactDisplay, ContactListAdapter.ContactListItemViewHolder> {

    public ContactListAdapter(@NonNull Context context) {
        super(context);
    }

    @NonNull
    @Override
    public SectionedListItemViewHolder<ContactDisplay> onCreateSectionedListItemViewHolder(@NonNull ViewGroup parent, @NonNull LayoutInflater inflater) {
        return new ContactListItemViewHolder(this,
                inflater.inflate(R.layout.contact_list_item,parent,false));
    }

    @Override
    protected List<ListItem<ContactDisplay>> buildListItems(List<ContactDisplay> children) {
        List<ListItem<ContactDisplay>> items = new ArrayList<>();
        Iterator<ContactDisplay> it = children.iterator();
        String lastHeader = null;
        while (it.hasNext()) {
            ContactDisplay contact = it.next();
            String header = createSectionHeader(contact);
            if (!Objects.equals(lastHeader,header)) {
                ListItem<ContactDisplay> headerItem = new ListItem<ContactDisplay>().setHeader(header);
                items.add(headerItem);
                lastHeader = header;
            }
            ListItem<ContactDisplay> childItem = new ListItem<ContactDisplay>().setChild(contact);
            items.add(childItem);
        }
        return items;
    }

    private String createSectionHeader(ContactDisplay realItem) {
        String displayName = realItem.getDisplayName();
        String firstLetter = displayName.substring(0,1);
        String headerText = firstLetter.toUpperCase()+firstLetter.toLowerCase();
        return headerText;
    }

    @Override
    protected List<ContactDisplay> filterChildren(String phrase, List<ContactDisplay> original) {
        List<ContactDisplay> result = new ArrayList<>();
        Iterator<ContactDisplay> it = original.iterator();
        while (it.hasNext()) {
            ContactDisplay contact = it.next();
            if (isAllowed(phrase,contact)) {
                result.add(contact);
            }
        }
        return result;
    }

    private boolean isAllowed(String phrase, ContactDisplay contact) {
        String displayNameLower = contact.getDisplayName().toLowerCase();
        String phraseLower = phrase.toLowerCase();
        return displayNameLower.contains(phraseLower);
    }

    private static RequestOptions REQ_OPS = RequestOptions
                                            .diskCacheStrategyOf(DiskCacheStrategy.ALL)
                                            .useAnimationPool(true)
                                            .placeholder(R.mipmap.placeholder_contact_photo);

    public static class ContactListItemViewHolder extends ContactListAdapter.SectionedListItemViewHolder<ContactDisplay> implements View.OnClickListener {

        View itemHeader;
        View itemContact;

        TextView header;

        TextView contactName;
        ImageView thumbnail;

        public ContactListItemViewHolder(ContactListAdapter adapter, @NonNull View v) {
            super( adapter,v);
        }

        @Override
        protected void onInit(@NonNull View v) {
            itemHeader = v.findViewById(R.id.item_header);
            itemContact = v.findViewById(R.id.item_contact);
            header = v.findViewById(R.id.header);
            contactName = v.findViewById(R.id.contact_name);
            thumbnail = v.findViewById(R.id.contact_thumbnail);
            itemContact.setOnClickListener(this);
        }

        @Override
        public View getHeaderView() {
            return itemHeader;
        }

        @Override
        public View getChildView() {
            return itemContact;
        }

        @Override
        public void bindHeader(@Nullable String headerText) {
            header.setText(headerText);
        }

        @Override
        public void bindChild(@Nullable ContactDisplay child) {
            contactName.setText(child.getDisplayName());
            Glide.with(thumbnail)
                    .applyDefaultRequestOptions(REQ_OPS)
                    .load(child.getThumbnailUri())
                    .into(thumbnail);
        }

        @Override
        public void onClick(View v) {
            if (v == itemContact) {
                dispatchItemClick(itemContact);
            }
        }
    }
}
