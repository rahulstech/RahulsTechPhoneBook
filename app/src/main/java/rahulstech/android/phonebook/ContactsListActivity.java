package rahulstech.android.phonebook;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import rahulstech.android.phonebook.concurrent.AsyncTask;
import rahulstech.android.phonebook.model.ContactDisplay;
import rahulstech.android.phonebook.repository.ContactRepository;
import rahulstech.android.phonebook.view.ContactListAdapter;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import java.util.List;
import java.util.Queue;

import static rahulstech.android.phonebook.ContactDetailsActivity.EXTRA_CONTACT_ID;

public class ContactsListActivity extends AppCompatActivity implements ContactListAdapter.OnListItemClickListener {

    private static final String TAG = "ContactsListActivity";

    private static final int RC_READ_CONTACTS = 1;

    private static final int CONTACT_SEARCH_MIN_INPUT = 3;

    EditText searchContacts;
    RecyclerView contactList;
    ContactListAdapter adapter;

    AsyncTask.AsyncTaskCallback<Void,List<ContactDisplay>> asyncTaskCallback = new AsyncTask.AsyncTaskCallback<Void, List<ContactDisplay>>() {
        @Override
        public void onError(Throwable error) {
            Toast.makeText(ContactsListActivity.this, "Unable to load contacts", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onResult(List<ContactDisplay> contacts) {
            adapter.changeContacts(contacts);
        }
    };
    LoadContactsListTask task = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts_list);
        searchContacts = findViewById(R.id.search_contacts);
        contactList = findViewById(R.id.contact_list);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false);
        adapter = new ContactListAdapter(this);
        adapter.setOnItemClickListener(this);
        contactList.setLayoutManager(layoutManager);
        contactList.setAdapter(adapter);
        searchContacts.addTextChangedListener(contactSearchTextWatcher);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hasContactsPermission()) {
            loadContacts();
        }
    }

    @Override
    protected void onPause() {
        if (null != task) task.cancel();
        super.onPause();
    }

    @Override
    public void onClickListItem(RecyclerView.Adapter<?> adapter, int position) {
        Log.d(TAG,"item @"+position+" clicked");
        ContactListAdapter.ListItem item = this.adapter.getItem(position);
        if (null != item && null != item.contact) {
            Log.i(TAG,"show contact details for contactId="+item.contact.getContactId());
            Intent intent = new Intent(this,ContactDetailsActivity.class);
            intent.putExtra(EXTRA_CONTACT_ID,item.contact.getContactId());
            startActivity(intent);
        }
    }

    private void loadContacts() {
        if (null != task) {
            task.cancel();
        }
        task = new LoadContactsListTask(ContactRepository.get(this),asyncTaskCallback);
        task.execute(null);
    }

    private boolean hasContactsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // ask runtime permission for sdk >= 23
            String readContacts = Manifest.permission.READ_CONTACTS;
            if (PackageManager.PERMISSION_DENIED == checkSelfPermission(readContacts)) {
                requestPermissions(new String[]{readContacts},RC_READ_CONTACTS);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (RC_READ_CONTACTS == requestCode) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadContacts();
            }
            else {
                Toast.makeText(this, "Contact Permission Not Granted", Toast.LENGTH_SHORT).show();
            }
        }
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

    private static class LoadContactsListTask extends AsyncTask<Void,List<ContactDisplay>> {

        ContactRepository repo;

        public LoadContactsListTask(ContactRepository repo, AsyncTaskCallback<Void,List<ContactDisplay>> callback) {
            this.repo = repo;
            setAsyncTaskCallback(callback);
        }

        @Override
        protected List<ContactDisplay> onExecuteTask(Void args) throws Exception {
            ContactRepository repo = this.repo;
            List<ContactDisplay> contactsList = repo.loadContactDisplay();
            return contactsList;
        }
    }
}