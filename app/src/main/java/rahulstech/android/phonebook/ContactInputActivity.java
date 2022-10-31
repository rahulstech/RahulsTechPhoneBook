package rahulstech.android.phonebook;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.widget.TextViewCompat;
import androidx.lifecycle.ViewModelProvider;
import rahulstech.android.phonebook.concurrent.AsyncTask;
import rahulstech.android.phonebook.model.Account;
import rahulstech.android.phonebook.model.Email;
import rahulstech.android.phonebook.model.Event;
import rahulstech.android.phonebook.model.PhoneNumber;
import rahulstech.android.phonebook.model.Relation;
import rahulstech.android.phonebook.util.Check;
import rahulstech.android.phonebook.view.AccountAdapter;
import rahulstech.android.phonebook.view.ContactDataInputBottomSheet;
import rahulstech.android.phonebook.view.ContactDataTypeAdapter;
import rahulstech.android.phonebook.viewmodel.ContactDetailsViewModel;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class ContactInputActivity extends AppCompatActivity implements View.OnClickListener {
    // TODO: ContactInputActivity not implemented
    // TODO: ui not scrolling while view more

    private static final String TAG = "ContactInp";
    Toolbar toolbar;
    Spinner accounts;
    AccountAdapter accountAdapter;

    ImageView btnSwitchName;
    ViewGroup sectionName;
    View sectionDisplayName;
    View sectionStructuredName;
    EditText iDisplayName;
    EditText iPrefix;
    EditText iFirstName;
    EditText iMiddleName;
    EditText iLastName;

    ViewGroup sectionNumber;
    View btnAddNumber;
    ViewGroup sectionEmail;
    View btnAddEmail;
    TextView btnViewMore;
    View sectionMore;
    ViewGroup sectionEvent;
    View btnAddEvent;
    ViewGroup sectionAddress;
    View btnAddAddress;
    ViewGroup sectionWebsite;
    View btnAddWebsite;
    EditText note;
    View btnAddNote;

    ContactDetailsViewModel vm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_input);

        vm = ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication()).create(ContactDetailsViewModel.class);

        toolbar = findViewById(R.id.toolbar);
        accounts = findViewById(R.id.choose_account);
        accountAdapter = new AccountAdapter(this);
        accounts.setAdapter(accountAdapter);

        btnViewMore = findViewById(R.id.view_more);
        btnViewMore.setOnClickListener(this);
        sectionMore = findViewById(R.id.section_more);

        initSectionName();

        initSectionNumber();

        initSectionEmail();

        if (hasRequiredPermissions()) loadInitialData();
        else vm.addHaltedTask(()->loadInitialData());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!hasRequiredPermissions()) {
            requestRequiredPermissions();
        }
    }

    @Override
    public void onClick(View v) {
        if (btnSwitchName == v) {
            toggleSectionName();
        }
        else if (btnAddNumber == v) {
            addOrEditNumber(null);
        }
        else if (btnAddEmail == v) {
            addOrEditEmail(null);
        }
        else if (btnViewMore == v) {
            toggleSectionMore();
        }
    }

    private void loadInitialData() {
        vm.loadContactAccounts(new AsyncTask.AsyncTaskCallback<List<Account>>(){
            @Override
            public void onResult(List<Account> result) {
                accountAdapter.setAccounts(result);
                Log.d(TAG,"loaded accounts: "+accountAdapter.getCount());
            }
        });
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    ///                           Section Initialization                               ///
    /////////////////////////////////////////////////////////////////////////////////////

    void initSectionName() {
        sectionName = findViewById(R.id.section_name);
        btnSwitchName = findViewById(R.id.button_switch_name);
        sectionDisplayName = findViewById(R.id.section_display_name);
        sectionStructuredName = findViewById(R.id.section_structured_name);
        iDisplayName = findViewById(R.id.display_name);
        iPrefix = findViewById(R.id.preix);
        iFirstName = findViewById(R.id.first_name);
        iMiddleName = findViewById(R.id.middle_name);
        iLastName = findViewById(R.id.last_name);

        btnSwitchName.setOnClickListener(this);
    }

    void initSectionNumber() {
        sectionNumber = findViewById(R.id.section_number);
        btnAddNumber = findViewById(R.id.button_add_number);

        btnAddNumber.setOnClickListener(this);
    }

    void initSectionEmail() {
        sectionEmail = findViewById(R.id.section_email);
        btnAddEmail = findViewById(R.id.button_add_email);

        btnAddEmail.setOnClickListener(this);
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    ///                               Handle Actions                                   ///
    /////////////////////////////////////////////////////////////////////////////////////

    void toggleSectionName() {
        if (sectionStructuredName.getVisibility() == View.VISIBLE) {
            sectionStructuredName.setVisibility(View.GONE);
            sectionDisplayName.setVisibility(View.VISIBLE);
            btnSwitchName.setImageResource(R.drawable.ic_baseline_arrow_drop_down);
        }
        else {
            sectionDisplayName.setVisibility(View.GONE);
            sectionStructuredName.setVisibility(View.VISIBLE);
            btnSwitchName.setImageResource(R.drawable.ic_baseline_arrow_drop_up);
        }
    }

    void toggleSectionMore() {
        if (sectionMore.getVisibility() == View.VISIBLE) {
            TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(btnViewMore,0,0,
                    R.drawable.ic_baseline_arrow_drop_down,0);
            sectionMore.setVisibility(View.GONE);
        }
        else {
            TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(btnViewMore,0,0,
                    R.drawable.ic_baseline_arrow_drop_down,0);
            sectionMore.setVisibility(View.VISIBLE);
        }
    }

    void addOrEditNumber(PhoneNumber old) {
        String title = null == old ? getString(R.string.add_email) : getString(R.string.edit_email);
        ContactDataInputBottomSheet.forPhoneNumber(this,title,null,(btn,vh)->{
            String number = vh.edittext.getText().toString();
            ContactDataTypeAdapter.ContactDataType type = (ContactDataTypeAdapter.ContactDataType) vh.types.getSelectedItem();
            if (!Check.isEmptyString(number)) {

            }
            return true;
        },old);
    }

    void addOrEditEmail(Email old) {
        String title = null == old ? getString(R.string.add_email) : getString(R.string.edit_email);
        ContactDataInputBottomSheet.forEmail(this, title, null, (btn,vh)->{
            String address = vh.edittext.getText().toString();
            ContactDataTypeAdapter.ContactDataType type = (ContactDataTypeAdapter.ContactDataType) vh.types.getSelectedItem();
            if (!Check.isEmptyString(address)) {
                Email e = new Email();
                e.setAddress(address);
                e.setType(e.getType());
                e.setTypeLabel(e.getTypeLabel());

            }
            return true;
        },old);
    }

    void addOrEditEvent(Event old) {
        String title = null == old ? getString(R.string.add_event) : getString(R.string.edit_event);
        ContactDataInputBottomSheet.forEvent(this,title,null,(btn,vh)->true,old);
    }

    void addOrEditRelation(Relation old) {
        String title = old == null ? getString(R.string.add_relation) : getString(R.string.edit_relation);
        ContactDataInputBottomSheet.forRelation(this,title,null,(btn,vh)->true,old);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ///                                  Runtime Permission                                     ///
    //////////////////////////////////////////////////////////////////////////////////////////////

    private static final int PERMISSION_CODE = 1;

    private static final String[] PERMISSIONS = new String[]{
            Manifest.permission.READ_CONTACTS,Manifest.permission.WRITE_CONTACTS
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