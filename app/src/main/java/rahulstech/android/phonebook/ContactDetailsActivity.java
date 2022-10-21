package rahulstech.android.phonebook;

import androidx.appcompat.app.AppCompatActivity;
import rahulstech.android.phonebook.concurrent.AsyncTask;
import rahulstech.android.phonebook.model.ContactDetails;
import rahulstech.android.phonebook.repository.ContactRepository;

import android.os.Bundle;
import android.widget.Toast;

public class ContactDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_CONTACT_ID = "contact_id";

    AsyncTask.AsyncTaskCallback<Void,ContactDetails> callback = new AsyncTask.AsyncTaskCallback<Void,ContactDetails>() {

        @Override
        public void onResult(ContactDetails result) {
            setTitle(result.getDisplayName());
        }
    };

    LoadContactDetailsTask task = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_details);
    }

    @Override
    protected void onResume() {
        super.onResume();
        long contactId = getIntent().getLongExtra(EXTRA_CONTACT_ID,-1);
        if (contactId == -1) {
            Toast.makeText(this, "contact does not exists", Toast.LENGTH_SHORT).show();
            finish();
        }
        task = new LoadContactDetailsTask(ContactRepository.get(this),contactId,callback);
        task.execute(null);
    }

    @Override
    protected void onPause() {
        if (null != task) task.cancel();
        super.onPause();
    }

    private static class LoadContactDetailsTask extends AsyncTask<Void, ContactDetails> {

        final long contactId;
        ContactRepository repo;

        LoadContactDetailsTask(ContactRepository repo, long contactId, AsyncTaskCallback<Void,ContactDetails> callback) {
            this.contactId = contactId;
            this.repo = repo;
            setAsyncTaskCallback(callback);
        }

        public long getContactId() {
            return contactId;
        }

        @Override
        protected ContactDetails onExecuteTask(Void args) throws Exception {
            return repo.getContactDetails(contactId);
        }
    }
}