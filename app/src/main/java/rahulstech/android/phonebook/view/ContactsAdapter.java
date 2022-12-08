package rahulstech.android.phonebook.view;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.AbstractList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import me.samlss.broccoli.Broccoli;
import me.samlss.broccoli.BroccoliGradientDrawable;
import me.samlss.broccoli.PlaceholderParameter;
import rahulstech.android.phonebook.R;
import rahulstech.android.phonebook.model.ContactDetails;
import rahulstech.android.phonebook.util.Check;
import rahulstech.android.phonebook.util.ContactSorting;

import static rahulstech.android.phonebook.util.DrawableUtil.vectorDrawable;
import static rahulstech.android.phonebook.util.Helpers.createContactPhotoPlaceholder;

public class ContactsAdapter extends ClickableItemAdapter<ContactDetails, ClickableItemAdapter.ClickableItemViewHolder<ContactDetails>> implements ContactFilter.PublishResultCallback {

    // TODO: highlight matched phrase

    private static final String TAG = "ContactsAdapter";

    private static final List<ContactDetails> LOADING_ITEMS = new AbstractList<ContactDetails>() {

        private static final int COUNT = 20;

        @Override
        public ContactDetails get(int index) {
            if (index < 0 || index >= COUNT) throw new IndexOutOfBoundsException("valid range 0-"+(COUNT -1));
            return null;
        }

        @Override
        public int size() {
            return COUNT;
        }
    };

    public static final int ITEM_BLANK = 0;
    public static final int ITEM_CONTACT = 1;

    public static final ContactDetails BLANK_CONTACT = new ContactDetails(null);

    private ContactFilter filter;
    private ContactSorting oldSorting = null,newSorting = null;
    private AsyncListDiffer<ContactDetails> mDiffer;

    private boolean showingItemLoading = true;

    private DiffUtil.ItemCallback<ContactDetails> mDiffItemCallback = new DiffUtil.ItemCallback<ContactDetails>() {
        @Override
        public boolean areItemsTheSame(@NonNull ContactDetails oldItem, @NonNull ContactDetails newItem) {
            long oldId = BLANK_CONTACT == oldItem ? 0 : oldItem.getContactId();
            long newId = BLANK_CONTACT == newItem ? 0 : newItem.getContactId();
            return oldId == newId;
        }

        @Override
        public boolean areContentsTheSame(@NonNull ContactDetails oldItem, @NonNull ContactDetails newItem) {
            return oldSorting != newSorting || oldItem.equals(newItem);
        }
    };

    public ContactsAdapter(@NonNull Context context) {
        super(context);
        filter = new ContactFilter(this);
        mDiffer = new AsyncListDiffer<>(this, mDiffItemCallback);
    }

    @Nullable
    public ContactSorting getSorting() {
        return newSorting;
    }

    public void setSorting(@NonNull ContactSorting sorting) {
        Check.isNonNull(sorting,"null == sorting");
        oldSorting = newSorting;
        newSorting = sorting;
    }

    public void showLoading() {
        mDiffer.submitList(LOADING_ITEMS);
        showingItemLoading = true;
    }

    public void changeItems(@NonNull ContactSorting sorting, @Nullable List<ContactDetails> newItems) {
        setSorting(sorting);
        if (null == newItems || newItems.isEmpty()) {
            mDiffer.submitList(null);
        }
        else {
            mDiffer.submitList(new AbstractList<ContactDetails>() {
                final List<ContactDetails> items = newItems;

                @Override
                public ContactDetails get(int index) {
                    if (index == items.size()) return BLANK_CONTACT;
                    return items.get(index);
                }

                @Override
                public int size() {
                    return items.size()+1;
                }
            });
        }
        showingItemLoading = false;
    }

    public List<ContactDetails> getItems() {
        return mDiffer.getCurrentList();
    }

    @Override
    public int getItemCount() {
        return getItems().size();
    }

    @Override
    public ContactDetails getItem(int position) {
        return getItems().get(position);
    }

    @Override
    public int getItemViewType(int position) {
        if (showingItemLoading) return ITEM_CONTACT;
        ContactDetails item = getItem(position);
        if (item == BLANK_CONTACT) return ITEM_BLANK;
        return ITEM_CONTACT;
    }

    @NonNull
    @Override
    public ClickableItemViewHolder<ContactDetails> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (ITEM_CONTACT == viewType) {
            return new ContactListItemViewHolder(this,
                    getLayoutInflater().inflate(R.layout.contact_list_item_contact, parent, false));
        }
        else {
            return new ClickableItemViewHolder<ContactDetails>(this,
                    getLayoutInflater().inflate(R.layout.blank,parent,false)) {
                @Override
                public void bind(@Nullable ContactDetails item) {}
            };
        }
    }

    @Override
    public void publish(@NonNull ContactFilter.Result result) {
        changeItems(getSorting(),result.contacts);
    }

    public ContactFilter getFilter() {
        return filter;
    }

    public static class ContactListItemViewHolder extends ClickableItemAdapter.ClickableItemViewHolder<ContactDetails> {

        private static final int PLACEHOLDER_ANIMATION_DURATION = 700;

        private TextView contactName;
        private ImageView thumbnail;
        private View btnVoiceCall, btnSms;
        private Broccoli broccoli;

        public ContactListItemViewHolder(ContactsAdapter adapter, @NonNull View v) {
            super( adapter,v);
            contactName = v.findViewById(R.id.contact_name);
            thumbnail = v.findViewById(R.id.contact_thumbnail);
            broccoli = new Broccoli();
            btnVoiceCall = findViewById(R.id.action_voice_call);
            btnSms = findViewById(R.id.action_sms);
        }

        private void attachClickListener() {
            itemView.setOnClickListener(this);
            btnVoiceCall.setOnClickListener(this);
            btnSms.setOnClickListener(this);
        }

        private void detachClickListeners() {
            itemView.setOnClickListener(null);
            btnVoiceCall.setOnClickListener(null);
            btnSms.setOnClickListener(null);
        }

        private ContactSorting getSorting() {
            return ((ContactsAdapter) getAdapter()).getSorting();
        }

        @Override
        public void bind(@Nullable ContactDetails item) {
            broccoli.removeAllPlaceholders();
            if (null != item) {
                ContactSorting sorting = getSorting();
                String displayName = item.getDisplayName(sorting);
                contactName.setText(displayName);
                Glide.with(thumbnail)
                        .load(item.getPhotoUri())
                        .placeholder(createContactPhotoPlaceholder(item,sorting,thumbnail,
                                vectorDrawable(itemView.getContext(),R.drawable.placeholder_contact_photo)))
                        .into(thumbnail);
                attachClickListener();
            }
            else {
                detachClickListeners();
                broccoli.addPlaceholder(newPlaceholderParameter(thumbnail));
                broccoli.addPlaceholder(newPlaceholderParameter(contactName));
                broccoli.addPlaceholder(newPlaceholderParameter(btnVoiceCall));
                broccoli.addPlaceholder(newPlaceholderParameter(btnSms));
                broccoli.show();
            }
        }

        private PlaceholderParameter newPlaceholderParameter(View view) {
            return new PlaceholderParameter.Builder()
                    .setView(view)
                    .setDrawable(new BroccoliGradientDrawable(Color.parseColor("#DDDDDD"),
                            Color.parseColor("#CCCCCC"), 0, PLACEHOLDER_ANIMATION_DURATION, new LinearInterpolator()))
                    .build();
        }
    }
}
