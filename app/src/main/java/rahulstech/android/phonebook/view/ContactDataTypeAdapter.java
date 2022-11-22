package rahulstech.android.phonebook.view;

import android.content.Context;
import android.content.res.Resources;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import rahulstech.android.phonebook.model.Website;

public class ContactDataTypeAdapter extends BaseAdapter {

    public static class ContactDataType {
         private int type;
         private CharSequence label;

        public ContactDataType(int type, CharSequence label) {
            this.type = type;
            this.label = String.valueOf(label);
        }

        public int getType() {
            return type;
        }

        public CharSequence getLabel() {
            return label;
        }

        public void setLabel(CharSequence label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return "ContactDataType{" +
                    "type=" + type +
                    ", label=" + label +
                    '}';
        }
    }

    private LayoutInflater inflater;
    private ContactDataType[] types;

    private ContactDataTypeAdapter(Context context, ContactDataType[] types) {
        this.inflater = LayoutInflater.from(context);
        this.types = types;
    }

    public static ContactDataTypeAdapter forPhoneNumber(Context context, CharSequence customLabel) {
        Resources res = context.getResources();
        ContactDataType[] types = new ContactDataType[21];

        types[0] = new ContactDataType(ContactsContract.CommonDataKinds.Phone.TYPE_HOME,
                ContactsContract.CommonDataKinds.Phone.getTypeLabel(res, ContactsContract.CommonDataKinds.Phone.TYPE_HOME,null));
        types[1] = new ContactDataType(ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
                ContactsContract.CommonDataKinds.Phone.getTypeLabel(res, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,null));
        types[2] = new ContactDataType(ContactsContract.CommonDataKinds.Phone.TYPE_WORK,
                ContactsContract.CommonDataKinds.Phone.getTypeLabel(res, ContactsContract.CommonDataKinds.Phone.TYPE_WORK,null));
        types[3] = new ContactDataType(ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK,
                ContactsContract.CommonDataKinds.Phone.getTypeLabel(res, ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK,null));
        types[4] = new ContactDataType(ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME,
                ContactsContract.CommonDataKinds.Phone.getTypeLabel(res, ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME,null));
        types[5] = new ContactDataType(ContactsContract.CommonDataKinds.Phone.TYPE_PAGER,
                ContactsContract.CommonDataKinds.Phone.getTypeLabel(res, ContactsContract.CommonDataKinds.Phone.TYPE_PAGER,null));
        types[6] = new ContactDataType(ContactsContract.CommonDataKinds.Phone.TYPE_OTHER,
                ContactsContract.CommonDataKinds.Phone.getTypeLabel(res, ContactsContract.CommonDataKinds.Phone.TYPE_OTHER,null));
        types[7] = new ContactDataType(ContactsContract.CommonDataKinds.Phone.TYPE_CALLBACK,
                ContactsContract.CommonDataKinds.Phone.getTypeLabel(res, ContactsContract.CommonDataKinds.Phone.TYPE_CALLBACK,null));
        types[8] = new ContactDataType(ContactsContract.CommonDataKinds.Phone.TYPE_CAR,
                ContactsContract.CommonDataKinds.Phone.getTypeLabel(res, ContactsContract.CommonDataKinds.Phone.TYPE_CAR,null));
        types[9] = new ContactDataType(ContactsContract.CommonDataKinds.Phone.TYPE_COMPANY_MAIN,
                ContactsContract.CommonDataKinds.Phone.getTypeLabel(res, ContactsContract.CommonDataKinds.Phone.TYPE_COMPANY_MAIN,null));
        types[10] = new ContactDataType(ContactsContract.CommonDataKinds.Phone.TYPE_ISDN,
                ContactsContract.CommonDataKinds.Phone.getTypeLabel(res, ContactsContract.CommonDataKinds.Phone.TYPE_ISDN,null));
        types[11] = new ContactDataType(ContactsContract.CommonDataKinds.Phone.TYPE_MAIN,
                ContactsContract.CommonDataKinds.Phone.getTypeLabel(res, ContactsContract.CommonDataKinds.Phone.TYPE_MAIN,null));
        types[12] = new ContactDataType(ContactsContract.CommonDataKinds.Phone.TYPE_OTHER_FAX,
                ContactsContract.CommonDataKinds.Phone.getTypeLabel(res, ContactsContract.CommonDataKinds.Phone.TYPE_OTHER_FAX,null));
        types[13] = new ContactDataType(ContactsContract.CommonDataKinds.Phone.TYPE_RADIO,
                ContactsContract.CommonDataKinds.Phone.getTypeLabel(res, ContactsContract.CommonDataKinds.Phone.TYPE_RADIO,null));
        types[14] = new ContactDataType(ContactsContract.CommonDataKinds.Phone.TYPE_TELEX,
                ContactsContract.CommonDataKinds.Phone.getTypeLabel(res, ContactsContract.CommonDataKinds.Phone.TYPE_TELEX,null));
        types[15] = new ContactDataType(ContactsContract.CommonDataKinds.Phone.TYPE_TTY_TDD,
                ContactsContract.CommonDataKinds.Phone.getTypeLabel(res, ContactsContract.CommonDataKinds.Phone.TYPE_TTY_TDD,null));
        types[16] = new ContactDataType(ContactsContract.CommonDataKinds.Phone.TYPE_WORK_MOBILE,
                ContactsContract.CommonDataKinds.Phone.getTypeLabel(res, ContactsContract.CommonDataKinds.Phone.TYPE_WORK_MOBILE,null));
        types[17] = new ContactDataType(ContactsContract.CommonDataKinds.Phone.TYPE_WORK_PAGER,
                ContactsContract.CommonDataKinds.Phone.getTypeLabel(res, ContactsContract.CommonDataKinds.Phone.TYPE_WORK_PAGER,null));
        types[18] = new ContactDataType(ContactsContract.CommonDataKinds.Phone.TYPE_ASSISTANT,
                ContactsContract.CommonDataKinds.Phone.getTypeLabel(res, ContactsContract.CommonDataKinds.Phone.TYPE_ASSISTANT,null));
        types[19] = new ContactDataType(ContactsContract.CommonDataKinds.Phone.TYPE_MMS,
                ContactsContract.CommonDataKinds.Phone.getTypeLabel(res, ContactsContract.CommonDataKinds.Phone.TYPE_MMS,null));
        types[20] = new ContactDataType(ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM,
                ContactsContract.CommonDataKinds.Phone.getTypeLabel(res, ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM,customLabel));

        return new ContactDataTypeAdapter(context,types);
    }

    public static ContactDataTypeAdapter forEmail(Context context, CharSequence customLabel) {
        Resources res = context.getResources();
        ContactDataType[] types = new ContactDataType[5];

        types[0] = new ContactDataType(ContactsContract.CommonDataKinds.Email.TYPE_HOME,
                ContactsContract.CommonDataKinds.Email.getTypeLabel(res, ContactsContract.CommonDataKinds.Email.TYPE_HOME,null));
        types[1] = new ContactDataType(ContactsContract.CommonDataKinds.Email.TYPE_WORK,
                ContactsContract.CommonDataKinds.Email.getTypeLabel(res, ContactsContract.CommonDataKinds.Email.TYPE_WORK,null));
        types[2] = new ContactDataType(ContactsContract.CommonDataKinds.Email.TYPE_OTHER,
                ContactsContract.CommonDataKinds.Email.getTypeLabel(res, ContactsContract.CommonDataKinds.Email.TYPE_OTHER,null));
        types[3] = new ContactDataType(ContactsContract.CommonDataKinds.Email.TYPE_MOBILE,
                ContactsContract.CommonDataKinds.Email.getTypeLabel(res, ContactsContract.CommonDataKinds.Email.TYPE_MOBILE,null));
        types[4] = new ContactDataType(ContactsContract.CommonDataKinds.Email.TYPE_CUSTOM,
                ContactsContract.CommonDataKinds.Email.getTypeLabel(res, ContactsContract.CommonDataKinds.Email.TYPE_CUSTOM,customLabel));

        return new ContactDataTypeAdapter(context,types);
    }

    public static ContactDataTypeAdapter forEvent(Context context, CharSequence customLabel) {
        Resources res = context.getResources();
        ContactDataType[] types = new ContactDataType[4];

        types[0] = new ContactDataType(ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY,
                ContactsContract.CommonDataKinds.Event.getTypeLabel(res, ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY,null));
        types[1] = new ContactDataType(ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY,
                ContactsContract.CommonDataKinds.Event.getTypeLabel(res, ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY,null));
        types[2] = new ContactDataType(ContactsContract.CommonDataKinds.Event.TYPE_OTHER,
                ContactsContract.CommonDataKinds.Event.getTypeLabel(res, ContactsContract.CommonDataKinds.Event.TYPE_OTHER,null));
        types[3] = new ContactDataType(ContactsContract.CommonDataKinds.Event.TYPE_CUSTOM,
                ContactsContract.CommonDataKinds.Event.getTypeLabel(res, ContactsContract.CommonDataKinds.Event.TYPE_CUSTOM,customLabel));

        return new ContactDataTypeAdapter(context,types);
    }

    public static ContactDataTypeAdapter forRelation(Context context, CharSequence customLabel) {
        Resources res = context.getResources();
        ContactDataType[] types = new ContactDataType[15];

        types[0] = new ContactDataType(ContactsContract.CommonDataKinds.Relation.TYPE_ASSISTANT,
                ContactsContract.CommonDataKinds.Relation.getTypeLabel(res, ContactsContract.CommonDataKinds.Relation.TYPE_ASSISTANT,null));
        types[1] = new ContactDataType(ContactsContract.CommonDataKinds.Relation.TYPE_BROTHER,
                ContactsContract.CommonDataKinds.Relation.getTypeLabel(res, ContactsContract.CommonDataKinds.Relation.TYPE_BROTHER,null));
        types[2] = new ContactDataType(ContactsContract.CommonDataKinds.Relation.TYPE_CHILD,
                ContactsContract.CommonDataKinds.Relation.getTypeLabel(res, ContactsContract.CommonDataKinds.Relation.TYPE_CHILD,null));
        types[3] = new ContactDataType(ContactsContract.CommonDataKinds.Relation.TYPE_DOMESTIC_PARTNER,
                ContactsContract.CommonDataKinds.Relation.getTypeLabel(res, ContactsContract.CommonDataKinds.Relation.TYPE_DOMESTIC_PARTNER,null));
        types[4] = new ContactDataType(ContactsContract.CommonDataKinds.Relation.TYPE_FATHER,
                ContactsContract.CommonDataKinds.Relation.getTypeLabel(res, ContactsContract.CommonDataKinds.Relation.TYPE_FATHER,null));
        types[5] = new ContactDataType(ContactsContract.CommonDataKinds.Relation.TYPE_FRIEND,
                ContactsContract.CommonDataKinds.Relation.getTypeLabel(res, ContactsContract.CommonDataKinds.Relation.TYPE_FRIEND,null));
        types[6] = new ContactDataType(ContactsContract.CommonDataKinds.Relation.TYPE_MANAGER,
                ContactsContract.CommonDataKinds.Relation.getTypeLabel(res, ContactsContract.CommonDataKinds.Relation.TYPE_MANAGER,null));
        types[7] = new ContactDataType(ContactsContract.CommonDataKinds.Relation.TYPE_MOTHER,
                ContactsContract.CommonDataKinds.Relation.getTypeLabel(res, ContactsContract.CommonDataKinds.Relation.TYPE_MOTHER,null));
        types[8] = new ContactDataType(ContactsContract.CommonDataKinds.Relation.TYPE_PARTNER,
                ContactsContract.CommonDataKinds.Relation.getTypeLabel(res, ContactsContract.CommonDataKinds.Relation.TYPE_PARTNER,null));
        types[9] = new ContactDataType(ContactsContract.CommonDataKinds.Relation.TYPE_PARENT,
                ContactsContract.CommonDataKinds.Relation.getTypeLabel(res, ContactsContract.CommonDataKinds.Relation.TYPE_PARENT,null));
        types[10] = new ContactDataType(ContactsContract.CommonDataKinds.Relation.TYPE_REFERRED_BY,
                ContactsContract.CommonDataKinds.Relation.getTypeLabel(res, ContactsContract.CommonDataKinds.Relation.TYPE_REFERRED_BY,null));
        types[11] = new ContactDataType(ContactsContract.CommonDataKinds.Relation.TYPE_RELATIVE,
                ContactsContract.CommonDataKinds.Relation.getTypeLabel(res, ContactsContract.CommonDataKinds.Relation.TYPE_RELATIVE,null));
        types[12] = new ContactDataType(ContactsContract.CommonDataKinds.Relation.TYPE_SISTER,
                ContactsContract.CommonDataKinds.Relation.getTypeLabel(res, ContactsContract.CommonDataKinds.Relation.TYPE_SISTER,null));
        types[13] = new ContactDataType(ContactsContract.CommonDataKinds.Relation.TYPE_SPOUSE,
                ContactsContract.CommonDataKinds.Relation.getTypeLabel(res, ContactsContract.CommonDataKinds.Relation.TYPE_SPOUSE,null));
        types[14] = new ContactDataType(ContactsContract.CommonDataKinds.Relation.TYPE_CUSTOM,
                ContactsContract.CommonDataKinds.Relation.getTypeLabel(res, ContactsContract.CommonDataKinds.Relation.TYPE_CUSTOM,customLabel));

        return new ContactDataTypeAdapter(context,types);
    }

    public static ContactDataTypeAdapter forOrganization(Context context, CharSequence customLabel) {
        Resources res = context.getResources();
        ContactDataType[] types = new ContactDataType[3];

        types[0] = new ContactDataType(ContactsContract.CommonDataKinds.Organization.TYPE_WORK,
                ContactsContract.CommonDataKinds.Organization.getTypeLabel(res, ContactsContract.CommonDataKinds.Organization.TYPE_WORK,null));
        types[1] = new ContactDataType(ContactsContract.CommonDataKinds.Organization.TYPE_OTHER,
                ContactsContract.CommonDataKinds.Organization.getTypeLabel(res, ContactsContract.CommonDataKinds.Organization.TYPE_OTHER,null));
        types[2] = new ContactDataType(ContactsContract.CommonDataKinds.Organization.TYPE_CUSTOM,
                ContactsContract.CommonDataKinds.Organization.getTypeLabel(res, ContactsContract.CommonDataKinds.Organization.TYPE_CUSTOM,customLabel));

        return new ContactDataTypeAdapter(context,types);
    }

    @Override
    public int getCount() {
        return types.length;
    }

    @Override
    public ContactDataType getItem(int position) {
        return types[position];
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getType();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView text1;
        if (null == convertView) {
            convertView = inflater.inflate(android.R.layout.simple_list_item_1,parent,false);
            text1 = convertView.findViewById(android.R.id.text1);
            convertView.setTag(text1);
        }
        else {
            text1 = (TextView) convertView.getTag();
        }
        ContactDataType item = getItem(position);
        text1.setText(item.getLabel());
        return convertView;
    }

    public int getPositionForType(int type) {
        for (int i=0; i<getCount(); i++) {
            ContactDataType cdt = getItem(i);
            if (cdt.getType()==type) return i;
        }
        return -1;
    }

    public ContactDataType getContactDataTypeForType(int type) {
        int position = getPositionForType(type);
        if (-1 == position) return null;
        return getItem(position);
    }
}
