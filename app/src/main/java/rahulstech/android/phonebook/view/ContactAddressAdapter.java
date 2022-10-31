package rahulstech.android.phonebook.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.ImageViewCompat;
import rahulstech.android.phonebook.R;
import rahulstech.android.phonebook.model.PostalAddress;

public class ContactAddressAdapter extends ClickableItemAdapter<PostalAddress, ContactAddressAdapter.AddressViewHolder> {

    public ContactAddressAdapter(@NonNull Context context) {
        super(context);
    }

    @NonNull
    @Override
    public AddressViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new AddressViewHolder(this,
                getLayoutInflater().inflate(R.layout.contact_detials_gird_item_image_text_cancel,parent,false));
    }

    public static class AddressViewHolder extends ClickableItemAdapter.ClickableItemViewHolder<PostalAddress> implements View.OnClickListener {

        View btnActionRemove;
        ImageView icon;
        TextView type;

        public AddressViewHolder(@NonNull ClickableItemAdapter<?, ?> adapter, @NonNull View itemView) {
            super(adapter, itemView);
            btnActionRemove = findViewById(R.id.action_remove);
            icon = findViewById(R.id.imageview);
            type = findViewById(R.id.text_primary);

            icon.setImageResource(R.drawable.ic_baseline_location);
            ImageViewCompat.setImageTintList(icon, ColorStateList.valueOf(Color.parseColor("#FD5D5D")));
            btnActionRemove.setOnClickListener(this);
            itemView.setOnClickListener(this);
        }

        @Override
        public void bind(@Nullable PostalAddress item) {
            if (null != item) {
                type.setText(item.getTypeLabel(type.getResources()));
            }
        }

        @Override
        public void onClick(View v) {
            dispatchItemClick(v);
        }
    }
}
