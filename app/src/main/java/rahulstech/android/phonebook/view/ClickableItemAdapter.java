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

    private DiffUtil.ItemCallback<T> mDefaultDifferItemCallback = new DiffUtil.ItemCallback<T>() {
        @Override
        public boolean areItemsTheSame(@NonNull Object oldItem, @NonNull Object newItem) {
            return Check.isEquals(oldItem, newItem);
        }

        @Override
        public boolean areContentsTheSame(@NonNull Object oldItem, @NonNull Object newItem) {
            return Check.isEquals(oldItem, newItem);
        }
    };

    private AsyncListDiffer<T> mDiffer;

    private final LayoutInflater inflater;
    private OnListItemClickListener listItemClickListener;
    private OnListItemLongClickListener listItemLongClickListener;

    protected ClickableItemAdapter(@NonNull Context context) {
        this(context,null);
    }

    protected ClickableItemAdapter(@NonNull Context context, @Nullable DiffUtil.ItemCallback<T> callback) {
        Check.isNonNull(context,"null == context");
        inflater = LayoutInflater.from(context);
        mDiffer = new AsyncListDiffer(this,null == callback ? mDefaultDifferItemCallback : callback);
    }

    public LayoutInflater getLayoutInflater() {
        return inflater;
    }

    public void changeItems(List<T> newItems) {
        List items = newItems;
        if (null == items || items.isEmpty()) {
            items = Collections.EMPTY_LIST;
        }
        mDiffer.submitList(items);
    }

    public T getItem(int position) {
        return mDiffer.getCurrentList().get(position);
    }

    @Override
    public int getItemCount() {
        return mDiffer.getCurrentList().size();
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
