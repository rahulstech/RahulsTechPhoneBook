package rahulstech.android.phonebook.view;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import rahulstech.android.phonebook.R;
import rahulstech.android.phonebook.model.Email;

public class ContactEmailAdapter extends ClickableItemAdapter<Email, ContactEmailAdapter.EmailViewHolder> {

    public ContactEmailAdapter(@NonNull Context context) {
        super(context);
    }

    @NonNull
    @Override
    public EmailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new EmailViewHolder(this,
                getLayoutInflater().inflate(R.layout.contact_details_email,parent,false));
    }

    public static class EmailViewHolder extends ClickableItemViewHolder<Email> {

        TextView email;
        TextView type;
        View btnActionRemove;

        public EmailViewHolder(@NonNull ClickableItemAdapter<?,?> adapter, @NonNull View v) {
            super(adapter, v);
            email = findViewById(R.id.text_primary);
            type = findViewById(R.id.text_secondary);
            btnActionRemove = findViewById(R.id.action_remove);
            v.setOnClickListener(this);
            btnActionRemove.setOnClickListener(this);
        }

        @Override
        public void bind(@Nullable Email item) {
            if (null != item) {
                email.setText(item.getAddress());
                type.setText(item.getTypeLabel(type.getResources()));
            }
        }
    }
}
