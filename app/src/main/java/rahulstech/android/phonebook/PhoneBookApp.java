package rahulstech.android.phonebook;

import android.app.Application;
import android.util.Log;

import java.util.List;

import androidx.annotation.Nullable;
import rahulstech.android.phonebook.concurrent.AsyncTask;
import rahulstech.android.phonebook.model.ContactAccount;
import rahulstech.android.phonebook.model.ContactDisplay;
import rahulstech.android.phonebook.repository.ContactRepository;

public class PhoneBookApp extends Application {

    private static final String TAG = "PhoneBookApp";

    private ContactRepository repository;

    private List<ContactAccount> contactAccounts;

    @Nullable
    public List<ContactAccount> getContactAccounts() {
        return contactAccounts;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        repository = ContactRepository.get(this);
        loadContactAccounts();
    }

    private void loadContactAccounts() {
        AsyncTask.execute(()->repository.getAccounts(),new AsyncTask.AsyncTaskCallback(){
            @Override
            public void onError(AsyncTask task) {
                Log.e(TAG,null,task.getError());
            }

            @Override
            public void onResult(AsyncTask task) {
                contactAccounts = task.getResult();
            }
        });
    }


}
