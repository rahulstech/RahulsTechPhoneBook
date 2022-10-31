package rahulstech.android.phonebook.view;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rahulstech.android.phonebook.R;
import rahulstech.android.phonebook.model.Contact;
import rahulstech.android.phonebook.util.Check;

public class AutoCompleteContactAdapter extends BaseAdapter implements Filterable {

    private static final String TAG = "ACContactAdapter";

    private List<Contact> original = Collections.EMPTY_LIST;

    private List<Contact> filtered = Collections.EMPTY_LIST;

    private Filter filter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<Contact> all = original;
            FilterResults results = new FilterResults();
            String search = String.valueOf(constraint);
            if (all.isEmpty() || Check.isEmptyString(search)) {
                results.count = 0;
                results.values = Collections.EMPTY_LIST;
            }
            else {
                String search_lowercase = search.toLowerCase();
                List<Contact> matched = new ArrayList<>();
                for (Contact c : all) {
                    if (c.getDisplayName().toLowerCase().startsWith(search_lowercase)) {
                        matched.add(c);
                    }
                }
                results.count = matched.size();
                results.values = matched.isEmpty() ? Collections.EMPTY_LIST : matched;
            }
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            Log.i(TAG,"filtered: search="+constraint+" count="+results.count);
            if (0==results.count) {
                notifyDataSetInvalidated();
            }
            else {
                filtered = (List<Contact>) results.values;
                notifyDataSetChanged();
            }
        }
    };

    private LayoutInflater inflater;

    public AutoCompleteContactAdapter(Context context) {
        this.inflater = LayoutInflater.from(context);
    }

    public void setOriginal(List<Contact> contacts) {
        this.original = contacts;
        if (null == contacts) this.original = Collections.EMPTY_LIST;
    }

    @Override
    public int getCount() {
        return filtered.size();
    }

    @Override
    public Contact getItem(int position) {
        return filtered.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (null == convertView) {
            convertView = inflater.inflate(R.layout.contact_autocomplete_list_item,parent,false);
            ViewHolder vh = new ViewHolder();
            vh.photo = convertView.findViewById(R.id.contact_thumbnail);
            vh.name = convertView.findViewById(R.id.contact_name);
            convertView.setTag(vh);
        }
        Contact item = getItem(position);
        ViewHolder vh = (ViewHolder) convertView.getTag();
        vh.name.setText(item.getDisplayName());
        Glide.with(vh.photo).load(item.getPhotoUri())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.mipmap.placeholder_contact_photo)
                .into(vh.photo);
        return convertView;
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    private static class ViewHolder {
        ImageView photo;
        TextView name;
    }
}
