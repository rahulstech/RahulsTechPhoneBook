package rahulstech.android.phonebook;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import rahulstech.android.phonebook.concurrent.AsyncTask;
import rahulstech.android.phonebook.model.ContactDetails;
import rahulstech.android.phonebook.model.PhoneNumber;
import rahulstech.android.phonebook.model.RawContact;
import rahulstech.android.phonebook.util.ContactDetailsComparator;
import rahulstech.android.phonebook.util.ContactSorting;
import rahulstech.android.phonebook.util.OpenActivity;
import rahulstech.android.phonebook.util.Settings;
import rahulstech.android.phonebook.view.ContactFilter;
import rahulstech.android.phonebook.view.ContactSourceAdapter;
import rahulstech.android.phonebook.view.ContactsAdapter;
import rahulstech.android.phonebook.view.OnListItemClickListener;
import rahulstech.android.phonebook.viewmodel.ContactViewModel;

public class ContactsListActivity extends PhoneBookActivity implements OnListItemClickListener {
    // TODO: contact list scrolling not smooth
    // TODO: search contact not properly implemented
    // TODO: contact placeholder image changes on scroll
    // TODO: appbar improvement required
    // TODO: contact source changed not properly handled
    // TODO: current selected source must be shown
    // TODO: implement alphabet indexer

    private static final String TAG = "ContactsListActivity";

    private Toolbar toolbar;
    private SearchView contactSearch;
    private RecyclerView contactList;
    private FloatingActionButton btnAddContact;
    private ContactsAdapter adapter;
    private ContactSourceAdapter contactSourceAdapter;
    private ContactViewModel vm;
    private Settings settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts_list);

        vm = getOrCreateViewModel(ContactViewModel.class);

        settings = new Settings(this);
        toolbar = findViewById(R.id.toolbar);
        contactSearch = findViewById(R.id.search_contacts);
        contactList = findViewById(R.id.contact_list);
        btnAddContact = findViewById(R.id.button_add_contact);

        btnAddContact.setOnClickListener(v -> onAddNewContact());
        contactSourceAdapter = new ContactSourceAdapter(this);
        adapter = new ContactsAdapter(this);
        adapter.setSorting(getContactSorting());
        adapter.setOnListItemClickListener(this);
        contactList.setLayoutManager(new LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false));
        contactList.setAdapter(adapter);
        setSupportActionBar(toolbar);
        contactSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {return false;}

            @Override
            public boolean onQueryTextChange(String newText) {
                onSearchContact(newText);
                return true;
            }
        });
        contactList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (RecyclerView.SCROLL_STATE_IDLE == newState) {
                    onContactListScrollingStop();
                }
                else {
                    onContactListScrolling();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        List<ContactDetails> contacts = vm.getContacts();
        if (null == contacts) adapter.showLoading();
        else adapter.changeItems(getContactSorting(),contacts);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_contact_list,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.ordering) {
            onChooseContactOrdering();
            return true;
        }
        else if (id == R.id.manage_source) {
            onShowContactSources();
        }
        else if (id == R.id.settings) {
            // TODO: implement settings screen
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAllPermissionsGranted(int requestCode) {
        super.onAllPermissionsGranted(requestCode);
        if (CONTACT_PERMISSION_CODE == requestCode) {
            loadAllContactSources();
            loadContacts();
        }
        else if (CALL_PERMISSION_CODE == requestCode) {
            if (vm.hasAnyHaltedTask()) {
                vm.getHaltedTask().run();
                vm.removeHaltedTask();
            }
        }
    }

    @Override
    public void onClickListItem(RecyclerView.Adapter<?> a, View which, int position, int itemType) {
        ContactDetails item = adapter.getItem(position);
        if (null == item) return;
        int vid = which.getId();
        PhoneNumber primary = item.getPhoneNumberPrimary();
        List<PhoneNumber> all = item.getPhoneNumbers();
        if (R.id.action_voice_call == vid) {
            makeVoiceCall(primary,all);
        }
        else if (R.id.action_sms == vid) {
            sendSms(primary,all);
        }
        else {
            OpenActivity.viewContactDetails(this,item.getContentUri());
        }
    }

    private void onAddNewContact() {
        OpenActivity.addContact(this);
    }

    private int getContactSourceSelection() {
        String name = settings.getDisplayContactSourceName();
        String type = settings.getDisplayContactSourceType();
        int position = contactSourceAdapter.getPositionByTypeAndName(type,name);
        return position;
    }

    private void onShowContactSources() {
        int position = getContactSourceSelection();
        new AlertDialog.Builder(this)
                .setSingleChoiceItems(contactSourceAdapter,position,(dialog,which)->{
                    RawContact source = contactSourceAdapter.getItem(which);
                    onChangeContactSource(source);
                    dialog.dismiss();
                })
                .show();
    }

    private void onChangeContactSource(@NonNull RawContact source) {
        settings.setDisplayContactSource(source.getName(), source.getType()).save();
    }

    private void onContactSourcesLoaded(@Nullable List<RawContact> sources) {
        ArrayList<RawContact> modified = new ArrayList<>();
        modified.add(RawContact.ALL_SOURCE);
        if (null != sources) modified.addAll(sources);
        contactSourceAdapter.setSources(modified);
    }

    private void loadContacts() {
        AsyncTask.execute(()->{
            List<ContactDetails> contacts = vm.getRepository().getAllContacts();
            ContactSorting sorting = getContactSorting();
            ContactDetailsComparator comparator = new ContactDetailsComparator(sorting);
            Collections.sort(contacts,comparator);
            return contacts;
        }, new AsyncTask.AsyncTaskCallback(){
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

    private void loadAllContactSources() {
        AsyncTask.execute(()->vm.getRepository().getAllContactSources(),new AsyncTask.AsyncTaskCallback(){
            @Override
            public void onError(AsyncTask task) {
                Log.e(TAG,"",task.getError());
            }

            @Override
            public void onResult(AsyncTask task) {
                onContactSourcesLoaded(task.getResult());
            }
        });
    }

    private ContactSorting getContactSorting() {
        return settings.getContactSorting();
    }

    private void onContactsLoaded(List<ContactDetails> contacts) {
        vm.setContacts(contacts);
        adapter.changeItems(getContactSorting(),contacts);
    }

    private void onContactListScrolling() {
        btnAddContact.hide();
    }

    private void onContactListScrollingStop() {
        btnAddContact.show();
    }

    private void onSearchContact(String newText) {
        adapter.getFilter().filter(newText,vm.getContacts(),ContactFilter.FilterType.NAME);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    ///                               Contact Ordering Chooser                                     ///
    /////////////////////////////////////////////////////////////////////////////////////////////////

    private AlertDialog orderingChooser;
    private View orderingChooserView;
    private RadioGroup ordering1,ordering2 ;

    private void onChooseContactOrdering() {
        if (null == orderingChooser) {
            orderingChooserView = getLayoutInflater().inflate(R.layout.contact_sorting_chooser_dialog_layout,null, false);
            ordering1 = orderingChooserView.findViewById(R.id.ordering1);
            ordering2 = orderingChooserView.findViewById(R.id.ordering2);

            orderingChooser = new AlertDialog.Builder(this)
                    .setPositiveButton(R.string.label_save, (di, which) -> {
                        int btnO1Id = ordering1.getCheckedRadioButtonId();
                        int btnO2Id = ordering2.getCheckedRadioButtonId();
                        ContactSorting sorting;
                        if (btnO1Id == R.id.order_fn1 && btnO2Id == R.id.order_asc) {
                            sorting = ContactSorting.FIRSTNAME_FIRST;
                        } else if (btnO1Id == R.id.order_fn1 && btnO2Id == R.id.order_desc) {
                            sorting = ContactSorting.FIRSTNAME_FIRST_DESC;
                        } else if (btnO1Id == R.id.order_ln1 && btnO2Id == R.id.order_asc) {
                            sorting = ContactSorting.LASTNAME_FIRST;
                        } else {
                            sorting = ContactSorting.LASTNAME_FIRST_DESC;
                        }
                        settings.setContactSorting(sorting).save();
                        adapter.showLoading();
                        loadContacts();
                    })
                    .setNeutralButton(R.string.label_cancel, null)
                    .setView(orderingChooserView)
                    .create();
        }
        ContactSorting sorting = getContactSorting();
        if (ContactSorting.FIRSTNAME_FIRST == sorting) {
            ordering1.check(R.id.order_fn1);
            ordering2.check(R.id.order_asc);
        }
        else if (ContactSorting.LASTNAME_FIRST == sorting) {
            ordering1.check(R.id.order_ln1);
            ordering2.check(R.id.order_asc);
        }
        else if (ContactSorting.FIRSTNAME_FIRST_DESC == sorting) {
            ordering1.check(R.id.order_fn1);
            ordering2.check(R.id.order_desc);
        }
        else {
            ordering1.check(R.id.order_ln1);
            ordering2.check(R.id.order_desc);
        }
        orderingChooser.show();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    ///                                     Voice Call & Sms                                       ///
    /////////////////////////////////////////////////////////////////////////////////////////////////

    private static final int ACTION_VOICE_CALL = 1;

    private static final int ACTION_SEND_SMS = 2;

    private void onChoosePhoneNumber(CharSequence title, List<PhoneNumber> numbers, int action) {
        if (null == numbers || numbers.isEmpty()) {
            Log.i(TAG,"no number to show phone number chooser");
            return;
        }
        new AlertDialog.Builder(this)
                .setSingleChoiceItems(new ArrayAdapter<PhoneNumber>(this,android.R.layout.select_dialog_item,numbers){
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

    private void makeVoiceCall(final PhoneNumber primary, final List<PhoneNumber> all) {
        if (null != primary) {
            makeVoiceCall(primary.getNumber());
        }
        else {
            onChoosePhoneNumber(getString(R.string.label_choose_voice_call_number),all,ACTION_VOICE_CALL);
        }
    }

    private void sendSms(final PhoneNumber primary, final List<PhoneNumber> all) {
        if (null != primary) {
            OpenActivity.sendSms(this,primary.getNumber());
        }
        else {
            onChoosePhoneNumber(getString(R.string.label_choose_sms_number),all,ACTION_SEND_SMS);
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