package rahulstech.android.phonebook;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import rahulstech.android.phonebook.concurrent.AsyncTask;
import rahulstech.android.phonebook.model.ContactDisplay;
import rahulstech.android.phonebook.model.PhoneNumber;
import rahulstech.android.phonebook.repository.ContactRepository;
import rahulstech.android.phonebook.util.OpenActivity;
import rahulstech.android.phonebook.util.PlaceHolderList;
import rahulstech.android.phonebook.view.ContactListAdapter;
import rahulstech.android.phonebook.view.OnListItemClickListener;
import rahulstech.android.phonebook.viewmodel.ContactViewModel;

import static rahulstech.android.phonebook.BuildConfig.DEBUG;

public class ContactsListActivity extends PhoneBookActivity implements OnListItemClickListener {
    // TODO: contact list scrolling not smooth
    // TODO: search contact not properly implemented
    // TODO: contact placeholder image changes on scroll
    //  and contact loading view showing orientation change unnecessarily

    private static final String TAG = "ContactsListActivity";

    private static final PlaceHolderList<ContactDisplay> CONTACTS_PLACE_HOLDERS = new PlaceHolderList<>(30);

    RecyclerView contactList;
    View btnAddContact;
    ContactListAdapter adapter;

    ContactViewModel vm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts_list);

        vm = ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication()).create(ContactViewModel.class);

        contactList = findViewById(R.id.contact_list);
        btnAddContact = findViewById(R.id.button_add_contact);
        btnAddContact.setOnClickListener(v -> onAddNewContact());
        adapter = new ContactListAdapter(this);
        adapter.setOnListItemClickListener(this);
        contactList.setLayoutManager(new LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false));
        contactList.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        List<ContactDisplay> contacts = null == vm.getContactDisplayList() ? CONTACTS_PLACE_HOLDERS : vm.getContactDisplayList();
        adapter.changeItems(contacts);
    }

    @Override
    public void onAllPermissionsGranted(int requestCode) {
        super.onAllPermissionsGranted(requestCode);
        if (CONTACT_PERMISSION_CODE == requestCode) loadContacts();
        else if (CALL_PERMISSION_CODE == requestCode) {
            if (vm.hasAnyHaltedTask()) {
                vm.getHaltedTask().run();
                vm.removeHaltedTask();
            }
        }
    }

    @Override
    public void onClickListItem(RecyclerView.Adapter<?> a, View which, int position, int itemType) {
        ContactDisplay item = adapter.getItem(position);
        if (null == item) return;
        int vid = which.getId();
        if (R.id.action_voice_call == vid) {
            makeVoiceCall(item);
        }
        else if (R.id.action_sms == vid) {
            sendSms(item);
        }
        else {
            OpenActivity.viewContactDetails(this,item.getContentUri());
        }
    }

    private void onAddNewContact() {
        OpenActivity.addContact(this);
    }

    private void loadContacts() {
        AsyncTask.execute(()->ContactRepository.get(ContactsListActivity.this).getContactRepositoryOperation().findAllContacts(),
                new AsyncTask.AsyncTaskCallback(){
            @Override
            public void onError(AsyncTask task) {
                onContactsLoaded(Collections.emptyList());
                Log.e(TAG,null,task.getError());
            }

            @Override
            public void onResult(AsyncTask task) {
                onContactsLoaded(task.getResult());
            }
        });
    }

    void onContactsLoaded(List<ContactDisplay> contacts) {
        vm.setContactDisplayList(contacts);
        adapter.changeItems(contacts);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    ///                                     Voice Call & Sms                                       ///
    /////////////////////////////////////////////////////////////////////////////////////////////////

    private static final int ACTION_VOICE_CALL = 1;

    private static final int ACTION_SEND_SMS = 2;

    private void onChoosePhoneNumber(CharSequence title, List<PhoneNumber> numbers, int action) {
        if (null == numbers || numbers.isEmpty()) return;
        new AlertDialog.Builder(this)
                .setSingleChoiceItems(new ArrayAdapter<PhoneNumber>(this,android.R.layout.simple_list_item_single_choice,numbers){
                    @NonNull
                    @Override
                    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                        TextView view = (TextView) super.getView(position, convertView, parent);
                        view.setText(getItem(position).getNumber());
                        return view;
                    }
                },0,(di,which)->{
                    String number = numbers.get(which).getNumber();
                    if (ACTION_VOICE_CALL == action) {
                        makeVoiceCall(number);
                    }
                    else {
                        OpenActivity.sendSms(ContactsListActivity.this,number);
                    }
                    di.dismiss();
                })
                .setTitle(title)
                .show();
    }

    private void makeVoiceCall(final ContactDisplay display) {
        if (display.hasPhoneNumberPrimary()) {
            makeVoiceCall(display.getPhoneNumberPrimary().getNumber());
        }
        else {
            onChoosePhoneNumber(getString(R.string.label_choose_voice_call_number),display.getPhoneNumbers(),ACTION_VOICE_CALL);
        }
    }

    private void sendSms(ContactDisplay display) {
        if (display.hasPhoneNumberPrimary()) {
            OpenActivity.sendSms(this,display.getPhoneNumberPrimary().getNumber());
        }
        else {
            onChoosePhoneNumber(getString(R.string.label_choose_sms_number),display.getPhoneNumbers(),ACTION_SEND_SMS);
        }
    }

    void makeVoiceCall(String number) {
        if (hasCallPermission()) {
            OpenActivity.makeVoiceCall(ContactsListActivity.this,number);
        }
        else {
            vm.addHaltedTask(()->makeVoiceCall(number));
            requestCallPermission();
        }
    }
}