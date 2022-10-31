package rahulstech.android.phonebook.view;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import rahulstech.android.phonebook.concurrent.AsyncTask;
import rahulstech.android.phonebook.util.Check;

public abstract class AbsSectionedRecyclerListViewAdapter<T, VH extends AbsSectionedRecyclerListViewAdapter.SectionedListItemViewHolder<T>>
        extends ClickableItemAdapter<AbsSectionedRecyclerListViewAdapter.ListItem<T>, VH> {

    private static final String TAG = "AbsSectionedRLVAdapter";

    private DiffUtil.ItemCallback<ListItem<T>> DIFF_CALLBACK = new DiffUtil.ItemCallback<ListItem<T>>() {
        @Override
        public boolean areItemsTheSame(@NonNull ListItem<T> oldItem, @NonNull ListItem<T> newItem) {
            return oldItem.itemType == newItem.itemType;
        }

        @Override
        public boolean areContentsTheSame(@NonNull ListItem<T> oldItem, @NonNull ListItem<T> newItem) {
            return oldItem.equals(newItem);
        }
    };

    private List<T> original = Collections.EMPTY_LIST;

    private AsyncListDiffer<ListItem<T>> mDiffer;

    private ChangeItemTask<T> changeItemTask;
    private FilterItemTask<T> filterItemTask;

    protected AbsSectionedRecyclerListViewAdapter(@NonNull Context context) {
        super(context);
        mDiffer = new AsyncListDiffer<>(this,DIFF_CALLBACK);
        changeItemTask = new ChangeItemTask<>(this);
        filterItemTask = new FilterItemTask<>(this);
    }

    public void changeChildren(List<T> children) {
        runChangeItemsTask(children);
        setOriginalChildren(children);
    }

    public void changeListItems(List<ListItem<T>> newItems) {
        Log.d(TAG,"changing list items");
        mDiffer.submitList(newItems);
    }

    public ListItem<T> getItem(int position) {
        return mDiffer.getCurrentList().get(position);
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).itemType;
    }

    public String getItemHeader(int position) {
        return getItem(position).headerText;
    }

    public T getItemChild(int position) {
        return getItem(position).child;
    }

    public void filter(String phrase) {
        if (null == phrase || "".equals(phrase)) {
            runChangeItemsTask(original);
        }
        else {
            runFilterItemsTask(phrase,original);
        }
    }

    @Override
    public int getItemCount() {
        return mDiffer.getCurrentList().size();
    }

    @NonNull
    public abstract SectionedListItemViewHolder<T> onCreateSectionedListItemViewHolder(@NonNull ViewGroup parent, @NonNull LayoutInflater inflater);

    @NonNull
    @Override
    public final VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        SectionedListItemViewHolder<T> vh = onCreateSectionedListItemViewHolder(parent, getLayoutInflater());
        return (VH) vh;
    }

    protected abstract List<ListItem<T>> buildListItems(List<T> children);

    protected abstract List<T> filterChildren(String phrase, List<T> original);

    private void runChangeItemsTask(final List<T> children) {
        //changeItemTask.cancel();
        changeItemTask.execute(children);
    }

    private void runFilterItemsTask(String phrase, List<T> children) {
        //filterItemTask.cancel();
        filterItemTask.execute(new FilterData<>(phrase,children));
    }

    private void setOriginalChildren(List<T> original) {
        this.original = original;
    }

    public static class ListItem<T> {
        public static final int TYPE_HEADER = 1;

        public static final int TYPE_CHILD = 2;

        public int itemType;

        public String headerText;

        public T child;

        public ListItem() {}

        public ListItem<T> setHeader(String headerText) {
            this.itemType = TYPE_HEADER;
            this.headerText = headerText;
            this.child = null;
            return this;
        }

        public ListItem<T> setChild(T child) {
            this.itemType = TYPE_CHILD;
            this.headerText = null;
            this.child = child;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ListItem)) return false;
            ListItem<?> that = (ListItem<?>) o;
            if (this.itemType != that.itemType) return false;
            if (TYPE_HEADER == this.itemType) return Check.isEquals(this.headerText,that.headerText);
            else return Check.isEquals(this.child,that.child);
        }
    }

    public static abstract class SectionedListItemViewHolder<T> extends ClickableItemAdapter.ClickableItemViewHolder<ListItem<T>> {

        public SectionedListItemViewHolder(@NonNull ClickableItemAdapter<?,?> adapter,
                                           @NonNull View itemView) {
            super(adapter,itemView);
            onInit(itemView);
        }

        protected abstract void onInit(@NonNull View itemView);

        public abstract View getHeaderView();

        public abstract View getChildView();

        @Override
        public final void bind(ListItem<T> item) {
            Log.d(TAG,"bind called");
            if (null != item) {
                int itemType = item.itemType;
                if (ListItem.TYPE_HEADER == itemType){
                    getChildView().setVisibility(View.GONE);
                    getHeaderView().setVisibility(View.VISIBLE);
                    bindHeader(item.headerText);
                    Log.d(TAG,"bind header complete");
                }
                else {
                    getHeaderView().setVisibility(View.GONE);
                    getChildView().setVisibility(View.VISIBLE);
                    bindChild(item.child);
                    Log.d(TAG,"bind child complete");
                }
            }
        }

        public abstract void bindHeader(@Nullable String headerText);

        public abstract void bindChild(@Nullable T child);
    }

    private static class ChangeItemTask<T> extends AsyncTask<List<T>,List<ListItem<T>>> {

        final WeakReference<AbsSectionedRecyclerListViewAdapter> adapterRef;

        ChangeItemTask(AbsSectionedRecyclerListViewAdapter adapter) {
            adapterRef = new WeakReference<>(adapter);
        }

        @Override
        public void onResult(@Nullable List<ListItem<T>> result) {
            AbsSectionedRecyclerListViewAdapter adapter = adapterRef.get();
            if (null != adapter) {
                adapter.changeListItems(result);
            }
        }

        @Override
        protected List<ListItem<T>> onExecuteTask(List<T> arg) throws Exception {
            final AbsSectionedRecyclerListViewAdapter adapter = this.adapterRef.get();
            if (null == adapter) return null;
            List<ListItem<T>> items = adapter.buildListItems(arg);
            Log.d(TAG,"building complete");
            return items;
        }
    }

    private static class FilterData<T> {
        String phrase;

        List<T> children;

        FilterData(String phrase, List<T> children) {
            this.phrase = phrase;
            this.children = children;
        }
    }

    private static class FilterItemTask<T> extends AsyncTask<FilterData<T>,List<T>> {

        final WeakReference<AbsSectionedRecyclerListViewAdapter> adapterRef;

        public FilterItemTask(AbsSectionedRecyclerListViewAdapter adapter) {
            adapterRef = new WeakReference<>(adapter);
        }

        @Override
        public void onResult(@Nullable List<T> result) {
            AbsSectionedRecyclerListViewAdapter adapter = adapterRef.get();
            if (null != adapter) {
                adapter.runChangeItemsTask(result);
            }
        }

        @Override
        protected List<T> onExecuteTask(FilterData<T> data) throws Exception {
            final AbsSectionedRecyclerListViewAdapter adapter = this.adapterRef.get();
            if (null == adapter) return null;
            List<T> filtered = adapter.filterChildren(data.phrase,data.children);
            return filtered;
        }
    }
}
