package rahulstech.android.phonebook;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import rahulstech.android.phonebook.model.ContactDisplay;
import rahulstech.android.phonebook.model.PhoneNumber;
import rahulstech.android.phonebook.view.AbsSectionedRecyclerListViewAdapter;
import rahulstech.android.phonebook.view.ContactListAdapter;
import rahulstech.android.phonebook.view.OnListItemClickListener;
import rahulstech.android.phonebook.viewmodel.ContactDisplayViewModel;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import java.util.List;

import static rahulstech.android.phonebook.ContactDetailsActivity.EXTRA_LOOKUP_KEY;

public class ContactsListActivity extends AppCompatActivity implements OnListItemClickListener {
    // TODO: contact list scrolling not smooth
    // TODO: search contact not properly implemented

    private static final String TAG = "ContactsListActivity";

    private static final int CONTACT_SEARCH_MIN_INPUT = 3;

    EditText searchContacts;
    RecyclerView contactList;
    View btnAddContact;
    ContactListAdapter adapter;

    ContactDisplayViewModel vm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts_list);

        vm = ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication()).create(ContactDisplayViewModel.class);

        searchContacts = findViewById(R.id.search_contacts);
        contactList = findViewById(R.id.contact_list);
        btnAddContact = findViewById(R.id.button_add_contact);
        btnAddContact.setOnClickListener(v -> onAddNewContact());
        LinearLayoutManager layoutManager = new LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false);
        adapter = new ContactListAdapter(this);
        adapter.setOnListItemClickListener(this);
        new ItemTouchHelper(onContactListItemTouchHelperCallback).attachToRecyclerView(contactList);
        contactList.setLayoutManager(layoutManager);
        contactList.setAdapter(adapter);
        searchContacts.addTextChangedListener(contactSearchTextWatcher);

        if (hasRequiredPermissions()) loadContacts();
        else vm.addHaltedTask(()->loadContacts());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!hasRequiredPermissions()) {
            requestRequiredPermissions();
        }
    }

    @Override
    public void onClickListItem(RecyclerView.Adapter<?> adapter, View which, int position, int itemType) {
        ContactDisplay item = this.adapter.getItemChild(position);
        if (null != item) {
            Intent intent = new Intent(this,ContactDetailsActivity.class);
            intent.putExtra(EXTRA_LOOKUP_KEY,item.getContact().getLookupKey());
            startActivity(intent);
        }
    }

    private void onAddNewContact() {
        Intent intent = new Intent(this,ContactInputActivity.class);
        intent.setAction(Intent.ACTION_INSERT);
        startActivity(intent);
    }

    private void loadContacts() {
        vm.getAllContacts().observe(this,contacts -> {
            adapter.changeChildren(contacts);
        });
    }

    private TextWatcher contactSearchTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            String text = s.toString();
            if (text.length() >= CONTACT_SEARCH_MIN_INPUT) {
                adapter.filter(text);
            }
            else if (text.length() == 0) {
                adapter.filter(null);
            }
        }
    };

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    ///                           Voice Call & Sms by Contact Swipe                                ///
    /////////////////////////////////////////////////////////////////////////////////////////////////


    private ItemTouchHelper.Callback onContactListItemTouchHelperCallback
            = new ItemTouchHelper.SimpleCallback(0,ItemTouchHelper.LEFT|ItemTouchHelper.RIGHT){
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView,
                              @NonNull RecyclerView.ViewHolder viewHolder,
                              @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            ContactListAdapter.ContactListItemViewHolder vh = (ContactListAdapter.ContactListItemViewHolder) viewHolder;
            if (vh.getItemViewType() != AbsSectionedRecyclerListViewAdapter.ListItem.TYPE_CHILD) return 0;
            ContactDisplay display = adapter.getItemChild(vh.getAdapterPosition());
            if (null == display || !display.hasPhoneNumber()) return 0;
            return super.getSwipeDirs(recyclerView, viewHolder);
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            ContactListAdapter.ContactListItemViewHolder vh = (ContactListAdapter.ContactListItemViewHolder) viewHolder;
            ContactDisplay display = adapter.getItemChild(vh.getAdapterPosition());
            if (ItemTouchHelper.LEFT == direction) {
                if (display.hasPhoneNumberPrimary()) makeVoiceCall(display.getPhoneNumberPrimary().getNumber());
                else {
                    onChoosePhoneNumber(display.getPhoneNumbers(),(di,which)->makeVoiceCall(display.getPhoneNumbers().get(which).getNumber()));
                }
            } else {
                if (display.hasPhoneNumberPrimary()) sendSms(display.getPhoneNumberPrimary().getNumber());
                else {
                    onChoosePhoneNumber(display.getPhoneNumbers(),(di,which)->sendSms(display.getPhoneNumbers().get(which).getNumber()));
                }
            }
            adapter.notifyItemChanged(vh.getAdapterPosition());
        }
    };

    private void onChoosePhoneNumber(List<PhoneNumber> numbers, AlertDialog.OnClickListener listener) {
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
                    listener.onClick(di,which);
                    di.dismiss();
                })
                .setTitle(R.string.label_mobile)
                .show();
    }

    private void makeVoiceCall(String number) {
        if (hasRequiredPermissions()) {
            Intent i = new Intent(Intent.ACTION_CALL);
            i.setData(Uri.parse("tel:" + number));
            startActivity(i);
        }
        else {
            vm.addHaltedTask(()->makeVoiceCall(number));
            requestRequiredPermissions();
        }
    }

    private void sendSms(String number) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse("sms:"+number));
        startActionActivity(i);
    }

    private void startActionActivity(Intent intent) {
        startActivity(Intent.createChooser(intent,"Choose"));
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ///                                  Runtime Permission                                     ///
    //////////////////////////////////////////////////////////////////////////////////////////////

    private static final int PERMISSION_CODE = 1;

    private static final String[] PERMISSIONS = new String[]{
            Manifest.permission.READ_CONTACTS,Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.CALL_PHONE
    };

    private boolean hasRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // ask runtime permission for sdk >= 23
            for (String permission : PERMISSIONS) {
                if (PackageManager.PERMISSION_DENIED
                        == ActivityCompat.checkSelfPermission(this,permission))
                    return false;
            }
        }
        return true;
    }

    private void requestRequiredPermissions() {
        ActivityCompat.requestPermissions(this,PERMISSIONS,PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (PERMISSION_CODE == requestCode) {
            if (hasRequiredPermissions()) {
                if (vm.hasAnyHaltedTask()) {
                    vm.getHaltedTask().run();
                    vm.removeHaltedTask();
                }
            }
            else {
                Toast.makeText(this,R.string.message_permission_not_granted,Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}