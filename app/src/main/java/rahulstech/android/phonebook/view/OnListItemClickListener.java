package rahulstech.android.phonebook.view;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

public interface OnListItemClickListener {

    void onClickListItem(RecyclerView.Adapter<?> adapter, View which, int position, int itemType);
}
