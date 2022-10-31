package rahulstech.android.phonebook.view;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

public interface OnListItemLongClickListener {

    boolean onLongClickListItem(RecyclerView.Adapter<?> adapter, View child, int position, int viewType);
}
