package rahulstech.android.phonebook.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.ImageViewCompat;
import rahulstech.android.phonebook.R;
import rahulstech.android.phonebook.model.Event;
import rahulstech.android.phonebook.util.DateTimeUtil;

public class ContactEventAdapter extends ClickableItemAdapter<Event, ContactEventAdapter.EventItemViewHolder> {

    public ContactEventAdapter(@NonNull Context context) {
        super(context);
    }

    @NonNull
    @Override
    public EventItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new EventItemViewHolder(this,
                getLayoutInflater().inflate(R.layout.contact_detials_gird_item_image_text_cancel,parent,false));
    }

    public static class EventItemViewHolder extends ClickableItemAdapter.ClickableItemViewHolder<Event> {

        SimpleDateFormat format_no_year = new SimpleDateFormat("MMM dd");
        SimpleDateFormat format_full = new SimpleDateFormat("d-MMM-yy");
        View btnActionRemove;
        ImageView icon;
        TextView dateStart;
        TextView type;

        protected EventItemViewHolder(@NonNull ClickableItemAdapter<?, ?> adapter, @NonNull View itemView) {
            super(adapter, itemView);

            btnActionRemove = findViewById(R.id.action_remove);
            icon = findViewById(R.id.imageview);
            dateStart = findViewById(R.id.text_primary);
            type = findViewById(R.id.text_secondary);
            type.setVisibility(View.VISIBLE);

            icon.setImageResource(R.drawable.ic_baseline_calendar_month);
            ImageViewCompat.setImageTintList(icon, ColorStateList.valueOf(icon.getResources().getColor(R.color.color_dark_yellow_green)));
            itemView.setOnClickListener(this);
            btnActionRemove.setOnClickListener(this);
        }

        @Override
        public void bind(@Nullable Event item) {
            if (null != item) {
                dateStart.setText(DateTimeUtil.formatContactEventStartDate(item.getStartDate(),format_full,format_no_year));
                type.setText(item.getTypeLabel(type.getResources()));
            }
        }
    }
}
