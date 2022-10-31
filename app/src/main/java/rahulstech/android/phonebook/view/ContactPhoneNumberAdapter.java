package rahulstech.android.phonebook.view;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import rahulstech.android.phonebook.R;
import rahulstech.android.phonebook.model.PhoneNumber;

public class ContactPhoneNumberAdapter extends ClickableItemAdapter<PhoneNumber,ContactPhoneNumberAdapter.PhoneNumberViewHolder>  {

    public ContactPhoneNumberAdapter(@NonNull Context context) {
        super(context);
    }

    @NonNull
    @Override
    public PhoneNumberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new PhoneNumberViewHolder(this,
                getLayoutInflater().inflate(R.layout.contact_details_phone_number,parent,false));
    }

    public static class PhoneNumberViewHolder extends ClickableItemAdapter.ClickableItemViewHolder<PhoneNumber> {

        TextView number;
        TextView type;
        View btnActionVoiceCall;
        View btnActionSms;
        View btnActionVideoCall;
        View btnActionRemove;

        public PhoneNumberViewHolder(@NonNull ClickableItemAdapter<?,?> adapter, @NonNull View itemView) {
            super(adapter, itemView);
            number = findViewById(R.id.text_primary);
            type = findViewById(R.id.text_secondary);
            btnActionVoiceCall = findViewById(R.id.action_voice_call);
            btnActionSms = findViewById(R.id.action_sms);
            btnActionVideoCall = findViewById(R.id.action_video_call);
            btnActionRemove = findViewById(R.id.action_remove);
            btnActionVoiceCall.setOnClickListener(this);
            btnActionSms.setOnClickListener(this);
            btnActionVideoCall.setOnClickListener(this);
            btnActionRemove.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public void bind(@Nullable PhoneNumber item) {
            if (null != item) {
                number.setText(item.getNumber());
                type.setText(item.getTypeLabel(type.getResources()));
            }
        }
    }
}
