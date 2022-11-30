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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import rahulstech.android.phonebook.concurrent.AsyncTask;
import rahulstech.android.phonebook.model.ContactAccount;
import rahulstech.android.phonebook.model.ContactDisplay;
import rahulstech.android.phonebook.model.Name;
import rahulstech.android.phonebook.model.PhoneNumber;
import rahulstech.android.phonebook.util.Check;
import rahulstech.android.phonebook.util.ContactSorting;
import rahulstech.android.phonebook.util.Helpers;
import rahulstech.android.phonebook.util.OpenActivity;
import rahulstech.android.phonebook.util.Settings;
import rahulstech.android.phonebook.view.ContactFilter;
import rahulstech.android.phonebook.view.ContactListAdapter;
import rahulstech.android.phonebook.view.OnListItemClickListener;
import rahulstech.android.phonebook.viewmodel.ContactViewModel;

import static rahulstech.android.phonebook.util.Helpers.areAllNonEmpty;
import static rahulstech.android.phonebook.util.Helpers.firstNonEmptyString;

public class ContactsListActivity extends PhoneBookActivity implements OnListItemClickListener {
    // TODO: contact list scrolling not smooth
    // TODO: search contact not properly implemented
    // TODO: contact placeholder image changes on scroll
    //  and contact loading view showing orientation change unnecessarily
    // TODO: floating action button hiding content
    // TODO: appbar improvement required

    private static final String TAG = "ContactsListActivity";

    public static class ContactDisplayComparator implements Comparator<ContactDisplay> {

        public final ContactSorting sorting;

        public ContactDisplayComparator(ContactSorting sorting) {
            this.sorting = sorting;
        }

        boolean isFirstNameFirst() {
            return sorting == ContactSorting.FIRSTNAME_FIRST || sorting == ContactSorting.FIRSTNAME_FIRST_DESC;
        }

        private String getFirstName(@NonNull Name name) {
            String firstName = name.getGivenName();
            if (Check.isEmptyString(firstName)) return name.getFamilyName();
            return firstName;
        }

        private String getLastName(@NonNull Name name) {
            String lastName = name.getFamilyName();
            if (Check.isEmptyString(lastName)) return name.getGivenName();
            return lastName;
        }

        @Override
        public int compare(ContactDisplay o1, ContactDisplay o2) {
            Name n1 = o1.getName();
            Name n2 = o2.getName();
            /*if (null == n1 || null == n2) return 1;
            String left,right;
            if (ContactSorting.FIRSTNAME_FIRST == sorting) {
                left = getFirstName(n1);
                right = getFirstName(n2);
            }
            else if (ContactSorting.LASTNAME_FIRST == sorting) {
                left = getLastName(n1);
                right = getLastName(n2);
            }
            else if (ContactSorting.FIRSTNAME_FIRST_DESC == sorting) {
                right = getFirstName(n1);
                left = getFirstName(n2);
            }
            else {
                right = getLastName(n1);
                left = getLastName(n2);
            }
            if (Check.isEmptyString(left) && Check.isEmptyString(right)) return 0;
            if (Check.isEmptyString(left)) return 1;
            if (Check.isEmptyString(right)) return -1;
            return left.compareToIgnoreCase(right);*/

            String name1 = getComparablePart(n1);
            String name2 = getComparablePart(n2);

            if (Check.isEmptyString(name1) && Check.isEmptyString(name2)) return 0;
            if (Check.isEmptyString(name1)) return 1;
            if (Check.isEmptyString(name2)) return -1;

            return name1.compareToIgnoreCase(name2);


            /*if (null == o1 && null == o2) return 0;
            if (null == o1) return 1;
            if (null == o2) return -1;

            boolean primary = isFirstNameFirst();
            String name1 = o1.getContact().getDisplayName(primary);
            String name2 = o2.getContact().getDisplayName(primary);

            return name1.compareToIgnoreCase(name2);*/
        }

        String getComparablePart(@Nullable Name n) {
            boolean firstNameFirst = isFirstNameFirst();
            String firstName = n.getGivenName();
            String lastName = n.getFamilyName();
            String middleName = n.getMiddleName();
            String suffix = n.getSuffix();
            String prefix = n.getPrefix();
            String comparable;
            if (firstNameFirst) {
                comparable = firstNonEmptyString(firstName,lastName,middleName,suffix,prefix);
            }
            else {
                comparable = firstNonEmptyString(lastName,firstName,middleName,suffix,prefix);
            }
            return comparable;
        }
    }

    Toolbar toolbar;
    SearchView contactSearch;
    RecyclerView contactList;
    FloatingActionButton btnAddContact;
    ContactListAdapter adapter;

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
        adapter = new ContactListAdapter(this);
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
        List<ContactDisplay> contacts = vm.getContactDisplayList();
        if (null == contacts) adapter.showLoading();
        else adapter.changeItems(settings.getContactSorting(),contacts);
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
        else if (id == R.id.settings) {
            // TODO: implement settings screen
            return true;
        }
        return super.onOptionsItemSelected(item);
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
        AsyncTask.execute(()->vm.getRepository().loadContactDisplay(getSelectedContactAccount(),getContactSorting()), new AsyncTask.AsyncTaskCallback(){
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

    private ContactAccount getSelectedContactAccount() {
        return ContactAccount.ALL;
    }

    private ContactSorting getContactSorting() {
        return settings.getContactSorting();
    }

    private void onContactsLoaded(List<ContactDisplay> contacts) {
        vm.setContactDisplayList(contacts);
        adapter.changeItems(getContactSorting(),contacts);
    }

    private void onContactListScrolling() {
        btnAddContact.hide();
    }

    private void onContactListScrollingStop() {
        btnAddContact.show();
    }

    private void onSearchContact(String newText) {
        adapter.getFilter().filter(newText,vm.getContactDisplayList(), ContactFilter.FilterType.NAME);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    ///                               Contact Ordering Chooser                                     ///
    /////////////////////////////////////////////////////////////////////////////////////////////////

    private AlertDialog orderingChooser;
    private View orderingChooserView;
    private RadioGroup ordering1,ordering2 ;

    private void onChooseContactOrdering() {
        if (null == orderingChooser) {
            orderingChooserView = getLayoutInflater().inflate(R.layout.contact_sorting_chooser_dialog_layout, null, false);
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