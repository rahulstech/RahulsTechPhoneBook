package rahulstech.android.phonebook.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

import rahulstech.android.phonebook.model.Account;
import rahulstech.android.phonebook.util.Check;

public class AccountAdapter extends BaseAdapter {

    private List<Account> accounts = Collections.EMPTY_LIST;
    private LayoutInflater inflater;

    public AccountAdapter(Context context) {
        Check.isNonNull(context,"null == context");
        inflater = LayoutInflater.from(context);
    }

    public void setAccounts(List<Account> accounts) {
        this.accounts = accounts;
        if (null == accounts || accounts.isEmpty()) this.accounts = Collections.EMPTY_LIST;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return accounts.size();
    }

    @Override
    public Account getItem(int position) {
        return accounts.get(position);
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
        Account account = getItem(position);
        vh.text1.setText(account.getName());
        // TODO set account type name
        return convertView;
    }

    private static class ViewHolder {
        TextView text1, text2;
    }
}
