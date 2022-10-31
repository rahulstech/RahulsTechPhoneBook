package rahulstech.android.phonebook.view;

import android.content.Context;
import android.database.DataSetObserver;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.ListPopupWindow;
import rahulstech.android.phonebook.R;
import rahulstech.android.phonebook.concurrent.AsyncTask;
import rahulstech.android.phonebook.model.Contact;
import rahulstech.android.phonebook.model.Email;
import rahulstech.android.phonebook.model.Event;
import rahulstech.android.phonebook.model.PhoneNumber;
import rahulstech.android.phonebook.model.Relation;
import rahulstech.android.phonebook.repository.ContactRepository;
import rahulstech.android.phonebook.util.Check;

public class ContactDataInputBottomSheet {

    private static final String TAG = "ContactDataIBS";

    public static class ViewHolder {
        public Spinner types;
        public EditText edittext;
        public TextView textview;
        public View button;
    }

    public interface Callback {
        boolean call(View view, ViewHolder vh);
    }

    static class OnSelectCustomContactDataType implements AdapterView.OnItemSelectedListener {

        final Context context;
        final Spinner spinner;
        AlertDialog dialog = null;
        EditText edittext;

        OnSelectCustomContactDataType(Context context, Spinner spinner) {
            this.context = context;
            this.spinner = spinner;
        }

        void show(ContactDataTypeAdapter.ContactDataType type) {
            if (null == dialog) {
                View view = View.inflate(context,R.layout.custom_contact_data_type_input,null);
                edittext = view.findViewById(R.id.edittext);
                dialog = new AlertDialog.Builder(context)
                        .setView(view)
                        .setNegativeButton(R.string.label_cancel,null)
                        .setPositiveButton(android.R.string.ok, (di,which)->{
                            String label = edittext.getText().toString();
                            if (Check.isEmptyString(label)) {
                                spinner.setSelection(0);
                            }
                            else {
                                type.setLabel(label);
                                ((ContactDataTypeAdapter) spinner.getAdapter()).notifyDataSetChanged();
                            }
                        })
                        .create();
            }
            edittext.setText(type.getLabel());
            dialog.show();
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (id == 0) {
                show(((ContactDataTypeAdapter) spinner.getAdapter()).getItem(position));
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {}
    }


    public static void forPhoneNumber(Context context, String titleText,
                                      @Nullable Callback negativeButtonCallback,
                                      @Nullable Callback positiveButtonCallback,
                                      @Nullable PhoneNumber pn) {
        prepareBottomSheet(context,titleText,(container,vh)->{
            View input_view = LayoutInflater.from(context).inflate(R.layout.contact_simple_data_input,(ViewGroup) container,true);
            vh.edittext = input_view.findViewById(R.id.edittext);
            vh.types = input_view.findViewById(R.id.types);
            vh.edittext.setHint(R.string.label_mobile);
            vh.edittext.setInputType(EditorInfo.TYPE_CLASS_PHONE);
            boolean hasPhoneNumber = null != pn;
            ContactDataTypeAdapter adapter = ContactDataTypeAdapter.forPhoneNumber(context,
                    hasPhoneNumber ? pn.getTypeLabel() : context.getString(R.string.label_custom));
            vh.types.setAdapter(adapter);
            vh.types.setOnItemSelectedListener(new OnSelectCustomContactDataType(context,vh.types));

            if (hasPhoneNumber) {
                vh.edittext.setText(pn.getNumber());
                vh.types.setSelection(adapter.getPositionForType(pn.getType()));
            }

            return true;
        },negativeButtonCallback,positiveButtonCallback)
                .show();
    }

    public static void forEmail(Context context, String titleText,
                                @Nullable Callback negativeButtonCallback,
                                @Nullable Callback positiveButtonCallback,
                                @Nullable Email e) {


        prepareBottomSheet(context,titleText,(container,vh)-> {
            View input_view = LayoutInflater.from(context).inflate(R.layout.contact_simple_data_input,(ViewGroup) container,true);
            vh.edittext = input_view.findViewById(R.id.edittext);
            vh.types = input_view.findViewById(R.id.types);
            vh.edittext.setHint(R.string.label_email);
            vh.edittext.setInputType(EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            boolean hasEmail = null != e;
            ContactDataTypeAdapter adapter = ContactDataTypeAdapter.forEmail(context,
                    hasEmail ? e.getTypeLabel() : context.getString(R.string.label_custom));
            vh.types.setAdapter(adapter);
            vh.types.setOnItemSelectedListener(new OnSelectCustomContactDataType(context,vh.types));
            if (hasEmail) {
                vh.edittext.setText(e.getAddress());
                vh.types.setSelection(adapter.getPositionForType(e.getType()));
            }
            return true;
        },negativeButtonCallback,positiveButtonCallback).show();
    }


    public static void forEvent(Context context, String titleText,
                                      @Nullable Callback negativeButtonCallback,
                                      @Nullable Callback positiveButtonCallback,
                                      @Nullable Event e) {
        prepareBottomSheet(context,titleText,(container,vh)->{
            View input_view = LayoutInflater.from(context).inflate(R.layout.contact_event_input,(ViewGroup) container,true);
            vh.edittext = input_view.findViewById(R.id.start_date);
            vh.button = input_view.findViewById(R.id.button_start_date);
            vh.types = input_view.findViewById(R.id.types);
            boolean hasEvent = null != e;
            ContactDataTypeAdapter adapter = ContactDataTypeAdapter.forEvent(context,
                    hasEvent ? e.getTypeLabel() : context.getString(R.string.label_custom));
            vh.types.setAdapter(adapter);
            vh.types.setOnItemSelectedListener(new OnSelectCustomContactDataType(context,vh.types));



            vh.button.setOnClickListener(v -> {

            });

            if (hasEvent) {
                vh.edittext.setText(e.getStartDate().toString());
                vh.types.setSelection(adapter.getPositionForType(e.getType()));
            }

            return true;
        },negativeButtonCallback,positiveButtonCallback)
                .show();
    }

    public static void forRelation(Context context, String titleText,
                                @Nullable Callback negativeButtonCallback,
                                @Nullable Callback positiveButtonCallback,
                                @Nullable Relation r) {

        prepareBottomSheet(context,titleText,(container,vh)-> {
            View input_view = LayoutInflater.from(context).inflate(R.layout.contact_simple_data_input,(ViewGroup) container,true);
            vh.edittext = input_view.findViewById(R.id.edittext);
            vh.types = input_view.findViewById(R.id.types);
            vh.edittext.setHint(R.string.label_relation);
            vh.edittext.setInputType(EditorInfo.TYPE_TEXT_VARIATION_PERSON_NAME);
            boolean hasRelation = null != r;
            ContactDataTypeAdapter adapter = ContactDataTypeAdapter.forRelation(context,
                    hasRelation ? r.getTypeLabel() : context.getString(R.string.label_custom));
            vh.types.setAdapter(adapter);
            vh.types.setOnItemSelectedListener(new OnSelectCustomContactDataType(context,vh.types));

            // TODO: popup position, popup not dismissed after selection
            ListPopupWindow matchedContacts = new ListPopupWindow(context);
            AutoCompleteContactAdapter contactsAdapter = new AutoCompleteContactAdapter(context);
            AsyncTask.execute(()->ContactRepository.get(context).loadContacts(),new AsyncTask.AsyncTaskCallback<List<Contact>>(){
                @Override
                public void onResult(List<Contact> result) {
                    Log.i(TAG,"loadContacts: result="+(null == result ? null : result.size()));
                    contactsAdapter.setOriginal(result);
                }
            });
            contactsAdapter.registerDataSetObserver(new DataSetObserver() {
                @Override
                public void onChanged() {
                    matchedContacts.show();
                }

                @Override
                public void onInvalidated() {
                    matchedContacts.dismiss();
                }
            });
            matchedContacts.setAnchorView(vh.edittext);
            matchedContacts.setAdapter(contactsAdapter);
            matchedContacts.setOnItemClickListener((a,c,position,id)->{
                Contact contact = contactsAdapter.getItem(position);
                vh.edittext.setText(contact.getDisplayName());
                matchedContacts.dismiss();
            });
            vh.edittext.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    contactsAdapter.getFilter().filter(s);
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
            if (hasRelation) {
                vh.edittext.setText(r.getDisplayName());
                vh.types.setSelection(adapter.getPositionForType(r.getType()));
            }
            return true;
        },negativeButtonCallback,positiveButtonCallback).show();
    }

    private static BottomSheetDialog prepareBottomSheet(Context context, String titleText,
                                           Callback initializer,
                                           @Nullable Callback negativeButtonCallback,
                                           @Nullable Callback positiveButtonCallback) {
        Check.isNonNull(context,"null == context");
        LayoutInflater inflater = LayoutInflater.from(context);
        View content = inflater.inflate(R.layout.contact_data_input_bottom_sheet,null);
        TextView title = content.findViewById(R.id.title);
        View btnPositive = content.findViewById(R.id.button_positive);
        View btnNegative = content.findViewById(R.id.button_negative);
        ViewGroup container = content.findViewById(R.id.content_container);
        title.setText(titleText);
        ViewHolder vh = new ViewHolder();
        initializer.call(container,vh);

        BottomSheetDialog bd = new BottomSheetDialog(context);
        bd.setDismissWithAnimation(true);
        bd.setContentView(content);
        btnNegative.setOnClickListener(v -> {
            boolean dismiss = true;
            if (null != negativeButtonCallback) dismiss = negativeButtonCallback.call(btnNegative,vh);
            if (dismiss) bd.dismiss();
        });
        btnPositive.setOnClickListener(v -> {
            boolean dismiss = true;
            if (null != positiveButtonCallback) dismiss = positiveButtonCallback.call(btnPositive,vh);
            if (dismiss) bd.dismiss();
        });
        return bd;
    }


}
