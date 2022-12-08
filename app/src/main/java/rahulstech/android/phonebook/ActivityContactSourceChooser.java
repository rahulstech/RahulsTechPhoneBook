package rahulstech.android.phonebook;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import rahulstech.android.phonebook.concurrent.AsyncTask;
import rahulstech.android.phonebook.model.RawContact;
import rahulstech.android.phonebook.repository.ContactRepository;
import rahulstech.android.phonebook.view.ContactSourceAdapter;

public class ActivityContactSourceChooser extends PhoneBookActivity {

    private static final String TAG = "ContactSrcChooser";

    public static final int RESULT_ERROR = -2;

    public static final String KEY_ACCOUNT_NAME = "account_name";
    public static final String KEY_ACCOUNT_TYPE = "account_type";
    public static final String KEY_RAW_CONTACT_ID = "raw_contact_id";

    private ContactRepository repository;

    private ListView list;
    private ContactSourceAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFinishOnTouchOutside(true);
        repository = ContactRepository.get(this);

        setContentView(android.R.layout.list_content);
        list = findViewById(android.R.id.list);
        list.setOnItemClickListener((list,view,position,id)->onSourceSelected(adapter.getItem(position)));
        adapter = new ContactSourceAdapter(this);
        list.setAdapter(adapter);
        AsyncTask.execute(()->repository.getRawContactsForContactEditing(getIntent().getData()),new AsyncTask.AsyncTaskCallback(){
            @Override
            public void onError(AsyncTask task) {
                Log.e(TAG,"",task.getError());
                setResult(RESULT_ERROR);
                finish();
            }

            @Override
            public void onResult(AsyncTask task) {
                onContactSourcesLoaded(task.getResult());
            }
        });
    }

    private void onContactSourcesLoaded(@Nullable List<RawContact> sources) {
        if (null == sources || sources.isEmpty()) {
            logDebug(TAG,"no source found");
            finish();
            return;
        }
        adapter.setSources(sources);
    }

    private void onSourceSelected(@NonNull RawContact source) {
        Intent data = new Intent();
        data.putExtra(KEY_RAW_CONTACT_ID,source.getId());
        data.putExtra(KEY_ACCOUNT_NAME,source.getName());
        data.putExtra(KEY_ACCOUNT_TYPE,source.getType());
        setResult(RESULT_OK,data);
        finish();
    }
}