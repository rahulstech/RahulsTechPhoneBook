package rahulstech.android.phonebook.view;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import rahulstech.android.phonebook.R;
import rahulstech.android.phonebook.model.Relation;

public class ContactRelationAdapter extends ClickableItemAdapter<Relation, ContactRelationAdapter.RelationViewHolder> {

    public ContactRelationAdapter(@NonNull Context context) {
        super(context);
    }

    @NonNull
    @Override
    public RelationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RelationViewHolder(this,
                getLayoutInflater().inflate(R.layout.contact_detials_gird_item_image_text_cancel,parent,false));
    }

    public static class RelationViewHolder extends ClickableItemAdapter.ClickableItemViewHolder<Relation> implements View.OnClickListener {

        View btnActionRemove;
        ImageView photo;
        TextView name;
        TextView relation;

        public RelationViewHolder(@NonNull ClickableItemAdapter<?, ?> adapter, @NonNull View itemView) {
            super(adapter, itemView);
            btnActionRemove = findViewById(R.id.action_remove);
            photo = findViewById(R.id.imageview);
            name = findViewById(R.id.text_primary);
            relation = findViewById(R.id.text_secondary);
            relation.setVisibility(View.VISIBLE);

            itemView.setOnClickListener(this);
            btnActionRemove.setOnClickListener(this);
        }

        @Override
        public void bind(@Nullable Relation item) {
            if (null != item) {
                Glide.with(photo).load(item.getPhotoUri())
                        .placeholder(R.mipmap.placeholder_contact_photo).into(photo);
                name.setText(item.getDisplayName());
                relation.setText(item.getTypeLabel(relation.getResources()));
            }
        }

        @Override
        public void onClick(View v) {
            dispatchItemClick(v);
        }
    }
}
