package rahulstech.android.phonebook.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.bumptech.glide.Glide;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import me.samlss.broccoli.Broccoli;
import me.samlss.broccoli.BroccoliGradientDrawable;
import me.samlss.broccoli.PlaceholderParameter;
import rahulstech.android.phonebook.R;
import rahulstech.android.phonebook.model.ContactDisplay;

public class ContactListAdapter extends ClickableItemAdapter<ContactDisplay, ContactListAdapter.ContactListItemViewHolder> {

    public ContactListAdapter(@NonNull Context context) {
        super(context);
    }

    @NonNull
    @Override
    public ContactListItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ContactListItemViewHolder(this,
                getLayoutInflater().inflate(R.layout.contact_list_item_contact,parent,false));
    }

    public static class ContactListItemViewHolder extends ClickableItemAdapter.ClickableItemViewHolder<ContactDisplay> {

        private static final int PLACEHOLDER_ANIMATION_DURATION = 700;

        TextView contactName;
        ImageView thumbnail;

        Broccoli broccoli;

        public ContactListItemViewHolder(ContactListAdapter adapter, @NonNull View v) {
            super( adapter,v);
            contactName = v.findViewById(R.id.contact_name);
            thumbnail = v.findViewById(R.id.contact_thumbnail);
            broccoli = new Broccoli();
            v.setOnClickListener(this);
            findViewById(R.id.action_voice_call).setOnClickListener(this);
            findViewById(R.id.action_sms).setOnClickListener(this);
        }

        @Override
        public void bind(@Nullable ContactDisplay item) {
            broccoli.removeAllPlaceholders();
            if (null != item) {
                String label = item.getDisplayName().substring(0,1);
                contactName.setText(item.getDisplayName());
                Glide.with(thumbnail)
                        .load(item.getThumbnailUri())
                        .placeholder(getContactPlaceholder(label))
                        .into(thumbnail);
            }
            else {
                broccoli.addPlaceholder(new PlaceholderParameter.Builder()
                        .setView(thumbnail)
                        .setDrawable(new BroccoliGradientDrawable(Color.parseColor("#DDDDDD"),
                                        Color.parseColor("#CCCCCC"), 0, PLACEHOLDER_ANIMATION_DURATION, new LinearInterpolator()))
                        .build());
                broccoli.addPlaceholder(new PlaceholderParameter.Builder()
                        .setView(contactName)
                        .setDrawable(new BroccoliGradientDrawable(Color.parseColor("#DDDDDD"),
                                Color.parseColor("#CCCCCC"), 0, PLACEHOLDER_ANIMATION_DURATION, new LinearInterpolator()))
                        .build());
                broccoli.show();
            }
        }

        private Drawable getContactPlaceholder(String label) {
            int radius = thumbnail.getMeasuredWidth()/2;
            int color = ColorGenerator.MATERIAL.getRandomColor();
            return TextDrawable.builder().buildRoundRect(label,color,radius);
        }
    }
}
