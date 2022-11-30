package rahulstech.android.phonebook.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import java.util.Collections;
import java.util.List;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import rahulstech.android.phonebook.util.Check;

public abstract class ClickableItemAdapter<T,VH extends ClickableItemAdapter.ClickableItemViewHolder<T>> extends RecyclerView.Adapter<VH> {

    @NonNull
    private Context context;
    @NonNull
    private final LayoutInflater inflater;
    private OnListItemClickListener listItemClickListener;
    private OnListItemLongClickListener listItemLongClickListener;

    protected ClickableItemAdapter(@NonNull Context context) {
        Check.isNonNull(context,"null == context");
        this.context = context;
        inflater = LayoutInflater.from(context);
    }

    @NonNull
    public Context getContext() {
        return context;
    }

    @NonNull
    public LayoutInflater getLayoutInflater() {
        return inflater;
    }

    public void setOnListItemClickListener(OnListItemClickListener listItemClickListener) {
        this.listItemClickListener = listItemClickListener;
    }

    public void setOnListItemLongClickListener(OnListItemLongClickListener listItemLongClickListener) {
        this.listItemLongClickListener = listItemLongClickListener;
    }

    public void dispatchItemClick(View child, int position, int itemType) {
        if (null != listItemClickListener) {
            listItemClickListener.onClickListItem(this,child,position,itemType);
        }
    }

    public boolean dispatchItemLongClick(View child, int adapterPosition, int itemViewType) {
        if (null != listItemLongClickListener) {
            return listItemLongClickListener.onLongClickListItem(this,child,adapterPosition,itemViewType);
        }
        return false;
    }

    public abstract T getItem(int position);

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        T item = getItem(position);
        holder.bind(item);
    }

    public static abstract class ClickableItemViewHolder<T> extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        private final ClickableItemAdapter<?,?> adapter;

        protected ClickableItemViewHolder(@NonNull ClickableItemAdapter<?,?> adapter, @NonNull View itemView) {
            super(itemView);
            Check.isNonNull(adapter,"null == adapter");
            this.adapter = adapter;
        }

        public ClickableItemAdapter<?, ?> getAdapter() {
            return adapter;
        }

        @Override
        public void onClick(View v) {
            dispatchItemClick(v);
        }

        @Override
        public boolean onLongClick(View v) {
            return dispatchItemLongClick(v);
        }

        public <V extends View> V findViewById(@IdRes int resId) {
            return itemView.findViewById(resId);
        }

        public abstract void bind(@Nullable T item);

        public void dispatchItemClick(View child) {
            adapter.dispatchItemClick(child,getAdapterPosition(),getItemViewType());
        }

        public boolean dispatchItemLongClick(View child) {
            return adapter.dispatchItemLongClick(child,getAdapterPosition(),getItemViewType());
        }
    }

}
