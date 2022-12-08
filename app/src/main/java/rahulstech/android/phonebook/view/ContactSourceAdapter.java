package rahulstech.android.phonebook.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import rahulstech.android.phonebook.R;
import rahulstech.android.phonebook.model.RawContact;
import rahulstech.android.phonebook.util.Check;

public class ContactSourceAdapter extends BaseAdapter {

    private List<RawContact> sources = Collections.emptyList();
    private LayoutInflater inflater;

    public ContactSourceAdapter(@NonNull Context context) {
        Check.isNonNull(context,"null == context");
        inflater = LayoutInflater.from(context);
    }

    public void setSources(@Nullable List<RawContact> sources) {
        if (null == sources) this.sources = Collections.emptyList();
        else this.sources = sources;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return sources.size();
    }

    @Override
    public RawContact getItem(int position) {
        return sources.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (null == convertView) {
            convertView = inflater.inflate(R.layout.contact_account_chooser_item,parent,false);
            ViewHolder vh = new ViewHolder();
            vh.text1 = convertView.findViewById(R.id.display_name);
            vh.text2 = convertView.findViewById(R.id.name);
            convertView.setTag(vh);
        }
        ViewHolder vh = (ViewHolder) convertView.getTag();
        RawContact account = getItem(position);
        vh.text1.setText(account.getDisplayName());
        vh.text2.setText(account.getName());
        return convertView;
    }

    public int getPositionByTypeAndName(@Nullable String type, @Nullable String name) {
        int position = 0;
        for (RawContact src : sources) {
            String accountType = src.getType();
            String accountName = src.getName();
            if (Check.isEquals(type,accountType)
                    && Check.isEquals(name,accountName)) return position;
            position++;
        }
        return -1;
    }

    private static class ViewHolder {
        TextView text1, text2;
    }
}
