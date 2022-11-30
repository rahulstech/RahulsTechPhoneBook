package rahulstech.android.phonebook.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import rahulstech.android.phonebook.R;
import rahulstech.android.phonebook.model.ContactAccount;
import rahulstech.android.phonebook.util.Check;

public class AccountAdapter extends BaseAdapter {

    private List<ContactAccount> accounts = Collections.emptyList();
    private LayoutInflater inflater;

    public AccountAdapter(@NonNull Context context) {
        Check.isNonNull(context,"null == context");
        inflater = LayoutInflater.from(context);
    }

    public void setAccounts(@Nullable List<ContactAccount> accounts) {
        if (null == accounts) this.accounts = Collections.emptyList();
        else this.accounts = accounts;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return accounts.size();
    }

    @Override
    public ContactAccount getItem(int position) {
        return accounts.get(position);
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
        ContactAccount account = getItem(position);
        vh.text1.setText(account.displayName);
        vh.text2.setText(account.name);
        return convertView;
    }

    public int getPositionByTypeAndName(@Nullable String type, @Nullable String name) {
        int position = 0;
        for (ContactAccount acc : accounts) {
            String accountType = acc.type;
            String accountName = acc.name;
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
