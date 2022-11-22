package rahulstech.android.phonebook.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

import rahulstech.android.phonebook.model.RawContact;
import rahulstech.android.phonebook.util.Check;

public class AccountAdapter extends BaseAdapter {

    private List<RawContact> rawContacts = Collections.EMPTY_LIST;
    private LayoutInflater inflater;

    public AccountAdapter(Context context) {
        Check.isNonNull(context,"null == context");
        inflater = LayoutInflater.from(context);
    }

    public void setAccounts(List<RawContact> rawContacts) {
        this.rawContacts = rawContacts;
        if (null == rawContacts || rawContacts.isEmpty()) this.rawContacts = Collections.EMPTY_LIST;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return rawContacts.size();
    }

    @Override
    public RawContact getItem(int position) {
        return rawContacts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (null == convertView) {
            convertView = inflater.inflate(android.R.layout.simple_list_item_2,parent,false);
            ViewHolder vh = new ViewHolder();
            vh.text1 = convertView.findViewById(android.R.id.text1);
            vh.text2 = convertView.findViewById(android.R.id.text2);
            convertView.setTag(vh);
        }
        ViewHolder vh = (ViewHolder) convertView.getTag();
        RawContact rawContact = getItem(position);
        vh.text1.setText(rawContact.getName());
        // TODO set account type name
        return convertView;
    }

    private static class ViewHolder {
        TextView text1, text2;
    }
}
