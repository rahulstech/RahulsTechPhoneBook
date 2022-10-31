package rahulstech.android.phonebook;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import rahulstech.android.phonebook.concurrent.AsyncTask;
import rahulstech.android.phonebook.model.ContactDetails;
import rahulstech.android.phonebook.model.Email;
import rahulstech.android.phonebook.model.Event;
import rahulstech.android.phonebook.model.PhoneNumber;
import rahulstech.android.phonebook.model.Relation;
import rahulstech.android.phonebook.util.Check;
import rahulstech.android.phonebook.view.ContactDataInputBottomSheet;
import rahulstech.android.phonebook.view.ContactDataTypeAdapter;
import rahulstech.android.phonebook.view.ContactEmailAdapter;
import rahulstech.android.phonebook.view.ContactEventAdapter;
import rahulstech.android.phonebook.view.ContactPhoneNumberAdapter;
import rahulstech.android.phonebook.view.ContactRelationAdapter;
import rahulstech.android.phonebook.view.OnListItemClickListener;
import rahulstech.android.phonebook.view.OnListItemLongClickListener;
import rahulstech.android.phonebook.viewmodel.ContactDetailsViewModel;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.ChipGroup;

public class ContactDetailsActivity extends AppCompatActivity implements View.OnClickListener {

    // TODO: viewing custom data types not implemented
    // like whatsapp and telegram account linked to account

    private static final String TAG = "ContactDetailsActivity";

    public static final String EXTRA_LOOKUP_KEY = "lookup_key";

    ContactDetailsViewModel vm;

    ImageView contactPhoto;
    TextView displayName;
    TextView numberPrimary;

    View sectionActionButtonsPrimary;
    ImageButton btnVoiceCallPrimary;
    ImageButton btnSmsPrimary;
    ImageButton btnVideoCallPrimary;
    ImageButton btnEmailPrimary;

    View sectionNumber;
    RecyclerView phoneNumbers;
    View btnActionAddNumber;
    ContactPhoneNumberAdapter phoneNumberAdapter;

    View sectionEmail;
    RecyclerView emails;
    View btnActionAddEmail;
    ContactEmailAdapter emailAdapter;

    ChipGroup sectionThumbs;

    View sectionOthers;
    RecyclerView otherRecyclerView;
    TextView otherTextView;
    TextView otherSectionLabel;
    View btnActionOtherAdd;

    ContactRelationAdapter relationAdapter;
    ContactEventAdapter eventAdapter;

    String voice_call_number = null;
    String video_call_number = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_details);

        vm = ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication()).create(ContactDetailsViewModel.class);

        contactPhoto = findViewById(R.id.contact_photo);
        displayName = findViewById(R.id.display_name);
        numberPrimary = findViewById(R.id.number_primary);

        sectionActionButtonsPrimary = findViewById(R.id.section_action_buttons_primary);
        btnVoiceCallPrimary = findViewById(R.id.action_voice_call_primary);
        btnSmsPrimary = findViewById(R.id.action_sms_primary);
        btnVideoCallPrimary = findViewById(R.id.action_video_call_primary);
        btnEmailPrimary = findViewById(R.id.action_email_primary);

        sectionThumbs = findViewById(R.id.section_thumbs);
        sectionThumbs.setOnCheckedStateChangeListener(onSectionChanged);

        initSectionNumber();

        initSectionEmail();

        initSectionOthers();

        btnVoiceCallPrimary.setOnClickListener(this);
        btnSmsPrimary.setOnClickListener(this);
        btnVideoCallPrimary.setOnClickListener(this);
        btnEmailPrimary.setOnClickListener(this);

        if (hasRequiredPermissions()) loadContactDetails();
        else vm.addHaltedTask(()->loadContactDetails());
    }

    void initSectionNumber() {
        sectionNumber = findViewById(R.id.section_number);
        phoneNumbers = sectionNumber.findViewById(R.id.recyclerview);

        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setOrientation(RecyclerView.VERTICAL);
        phoneNumbers.setLayoutManager(lm);

        phoneNumberAdapter = new ContactPhoneNumberAdapter(this);
        phoneNumberAdapter.setOnListItemClickListener(onClickPhoneNumber);
        phoneNumberAdapter.setOnListItemLongClickListener(onLongClickPhoneNumber);
        phoneNumbers.setAdapter(phoneNumberAdapter);

        phoneNumbers.setVisibility(View.VISIBLE);

        btnActionAddNumber = sectionNumber.findViewById(R.id.action_add);
        btnActionAddNumber.setOnClickListener(onAddNumber);

        TextView sectionLabel = sectionNumber.findViewById(R.id.section_label);
        sectionLabel.setText(R.string.label_mobile);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!hasRequiredPermissions()) {
            requestRequiredPermissions();
        }
    }

    void initSectionEmail() {
        sectionEmail = findViewById(R.id.section_email);
        emails = sectionEmail.findViewById(R.id.recyclerview);

        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setOrientation(RecyclerView.VERTICAL);
        emails.setLayoutManager(lm);

        emailAdapter = new ContactEmailAdapter(this);
        emailAdapter.setOnListItemClickListener(onClickEmail);
        emails.setAdapter(emailAdapter);

        emails.setVisibility(View.VISIBLE);

        btnActionAddEmail = sectionEmail.findViewById(R.id.action_add);
        btnActionAddEmail.setOnClickListener(onAddEmail);

        TextView sectionLabel = sectionEmail.findViewById(R.id.section_label);
        sectionLabel.setText(R.string.label_email);
    }

    void initSectionOthers() {
        sectionOthers = findViewById(R.id.section_content);
        btnActionOtherAdd = sectionOthers.findViewById(R.id.action_add);
        otherRecyclerView = sectionOthers.findViewById(R.id.recyclerview);

        GridLayoutManager gm = new GridLayoutManager(this,getResources().getInteger(R.integer.contact_details_grid_item_count));
        otherRecyclerView.setLayoutManager(gm);

        relationAdapter = new ContactRelationAdapter(this);
        relationAdapter.setOnListItemClickListener(onClickRelation);

        eventAdapter = new ContactEventAdapter(this);
        eventAdapter.setOnListItemClickListener(onClickEvent);

        otherTextView = sectionOthers.findViewById(R.id.textview);
        otherSectionLabel = sectionOthers.findViewById(R.id.section_label);
    }

    @Override
    public void onClick(View v) {
        ContactDetails details = vm.getLoadedContactDetails();
        if (details.hasPhoneNumberPrimary()) {
            PhoneNumber pn = details.getPhoneNumberPrimary();
            if (v == btnVoiceCallPrimary) {
                makeVoiceCall(pn.getNumber());
            }
            else if (v == btnSmsPrimary) {
                sendSms(pn.getNumber());
            }
            else if (v == btnVideoCallPrimary){
                makeVideoCall(pn.getNumber());
            }
        }
        else if (details.hasEmailPrimary() && v == btnEmailPrimary) {
            sendEmail(details.getEmailPrimary().getAddress());
        }
    }

    private void loadContactDetails() {
        String lookupKey = getIntent().getStringExtra(EXTRA_LOOKUP_KEY);
        vm.findContactDetailsByLookupKey(lookupKey).observe(this, this::onContactDetailsLoaded);
    }

    private AsyncTask.AsyncTaskCallback<Boolean> CONTACT_DATA_SAVE_ERROR_CALLBACK = new AsyncTask.AsyncTaskCallback<Boolean>() {
        @Override
        public void onError(Throwable error) {
            Toast.makeText(ContactDetailsActivity.this,R.string.message_save_fail,Toast.LENGTH_SHORT).show();
        }
    };

    private OnListItemClickListener onClickPhoneNumber = (a, c, p, t) -> {
        int viewId = c.getId();
        PhoneNumber pn = phoneNumberAdapter.getItem(p);
        String number = pn.getNumber();
        if (viewId == R.id.action_voice_call) {
            makeVoiceCall(number);
        }
        else if (viewId == R.id.action_sms) {
            sendSms(number);
        }
        else if (viewId == R.id.action_video_call) {
            makeVideoCall(number);
        }
        else if (viewId == R.id.action_remove) {
            onRemoveContactData(getString(R.string.message_remove,number),()->vm.removePhoneNumber(pn));
        }
    };

    private OnListItemLongClickListener onLongClickPhoneNumber = (a,c,p,t) -> {
        Log.d(TAG,"long clicked phone number @"+p);
        // TODO: show context menu with option set as default

        PhoneNumber pn = phoneNumberAdapter.getItem(p);
        vm.setPrimary(pn);
        return true;
    };

    private View.OnClickListener onAddNumber = v -> {
        ContactDataInputBottomSheet.forPhoneNumber(
                this,
                getString(R.string.add_number),
                null,
                (button, vh) -> {
                    String _number = vh.edittext.getText().toString();
                    if (Check.isEmptyString(_number)) {
                        Toast.makeText(ContactDetailsActivity.this, R.string.message_save_nothing, Toast.LENGTH_SHORT).show();
                    } else {
                        ContactDetails details = vm.getLoadedContactDetails();
                        ContactDataTypeAdapter.ContactDataType type = (ContactDataTypeAdapter.ContactDataType) vh.types.getSelectedItem();
                        PhoneNumber pn = new PhoneNumber();
                        pn.setNumber(_number);
                        pn.setType(type.getType());
                        pn.setTypeLable(type.getLabel());
                        onAddContactData(()->vm.addPhoneNumber(details, pn,CONTACT_DATA_SAVE_ERROR_CALLBACK));
                    }
                    return true;
                }, null);
    };

    private OnListItemClickListener onClickEmail = (a,c,p,t) -> {
        int vid = c.getId();
        Email email = emailAdapter.getItem(p);
        String address = email.getAddress();
        if (vid == R.id.action_remove) {
            onRemoveContactData(getString(R.string.message_remove,address),()->vm.removeEmail(email));
        }
        else {
            sendEmail(address);
        }
    };

    private View.OnClickListener onAddEmail = v -> {
        ContactDataInputBottomSheet.forEmail(this,getString(R.string.add_email),null,(btn,vh)-> {
            String _address = vh.edittext.getText().toString();
            if (Check.isEmptyString(_address)) {
                Toast.makeText(ContactDetailsActivity.this,R.string.message_save_nothing,Toast.LENGTH_SHORT).show();
            }
            else {
                ContactDetails details = vm.getLoadedContactDetails();
                ContactDataTypeAdapter.ContactDataType type = (ContactDataTypeAdapter.ContactDataType) vh.types.getSelectedItem();
                Email email = new Email();
                email.setAddress(_address);
                email.setType(type.getType());
                email.setTypeLabel(type.getLabel());
                onAddContactData(()->vm.addEmail(details,email,CONTACT_DATA_SAVE_ERROR_CALLBACK));
            }
            return true;
        },null);
    };

    private OnListItemClickListener onClickRelation = (a,c,p,t) -> {
        int vid = c.getId();
        Relation relation = relationAdapter.getItem(p);
        if (vid == R.id.action_remove) {
            onRemoveContactData(getString(R.string.message_remove,relation.getDisplayName()),()->vm.removeRelation(relation));
        }
        else {
            Intent i = new Intent(this,ContactDetailsActivity.class);
            i.putExtra(EXTRA_LOOKUP_KEY,relation.getRelativeContactLookupKey());
            startActivity(i);
        }
    };

    private View.OnClickListener onAddRelation = v -> {
        ContactDataInputBottomSheet.forRelation(this,getString(R.string.add_relation),null, (btn,vh)->{
            String _name = vh.edittext.getText().toString();
            if (Check.isEmptyString(_name)) {
                Toast.makeText(ContactDetailsActivity.this,R.string.message_save_nothing,Toast.LENGTH_SHORT).show();
            }
            else {
                ContactDetails details = vm.getLoadedContactDetails();
                ContactDataTypeAdapter.ContactDataType type = (ContactDataTypeAdapter.ContactDataType) vh.types.getSelectedItem();
                Relation r = new Relation();
                r.setDisplayName(_name);
                r.setType(type.getType());
                r.setTypeLabel(type.getLabel());
                onAddContactData(()->vm.addRelation(details,r,CONTACT_DATA_SAVE_ERROR_CALLBACK));
            }
            return true;
        },null);
    };

    private OnListItemClickListener onClickEvent = (a,c,p,t) -> {
        int vid = c.getId();
        Event event = eventAdapter.getItem(p);
        if (vid == R.id.action_remove) {
            onRemoveContactData(getString(R.string.message_remove,event.getStartDate().toString()),()->vm.removeEvent(event));
        }
        else {
            // TODO: open calendar schedules on date

        }
    };

    private View.OnClickListener onAddEvent = v -> {

    };

    private void onAddContactData(Runnable addOperation) {
        if (!hasRequiredPermissions()) {
            vm.addHaltedTask(addOperation);
            requestRequiredPermissions();
        }
        else {
            addOperation.run();
        }
    }

    private void onRemoveContactData(String message, Runnable removeOperation) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(R.string.label_cancel, null)
                .setNegativeButton(R.string.label_remove, (di, which) -> {
                    if (!hasRequiredPermissions()) {
                        vm.addHaltedTask(removeOperation);
                        requestRequiredPermissions();
                    }
                    else {
                        removeOperation.run();
                    }
                })
                .show();
    }

    private ChipGroup.OnCheckedStateChangeListener onSectionChanged = (gr,ids) -> {

        // TODO: implement animated show hide with auto scroll

        boolean selected = !ids.isEmpty();
        if (selected) {
            int selection = ids.get(0);
            // TODO: show the content
            sectionOthers.setVisibility(View.VISIBLE);
            switch (selection) {
                case R.id.section_relation_thumb: {
                    prepareSectionOthers(getString(R.string.label_relation),relationAdapter,onAddRelation);
                }
                break;
                case R.id.section_event_thumb: {
                    prepareSectionOthers(getString(R.string.label_event),eventAdapter,onAddEvent);
                }
                break;
                case R.id.section_note_thumb: {

                }
                default: {}
            }
        }
        else {
            // TODO: hide the content
            sectionOthers.setVisibility(View.GONE);
        }
    };

    void prepareSectionOthers(String label, RecyclerView.Adapter<?> adapter, View.OnClickListener onHandleAdd) {
        Log.i(TAG,"label: "+label+" adapter: "+(null == adapter ? null : "["+adapter.getClass().getSimpleName()+","+adapter.getItemCount()+"]"));
        otherSectionLabel.setText(label);
        otherRecyclerView.setAdapter(adapter);
        otherTextView.setVisibility(View.GONE);
        otherRecyclerView.setVisibility(View.VISIBLE);
        btnActionOtherAdd.setOnClickListener(onHandleAdd);
    }

    void prepareSectionOthers(String label, String text, View.OnClickListener onHandleAdd) {
        otherSectionLabel.setText(label);
        otherTextView.setText(text);
        otherRecyclerView.setVisibility(View.GONE);
        otherTextView.setVisibility(View.VISIBLE);
        btnActionOtherAdd.setOnClickListener(onHandleAdd);
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

    private void sendEmail(String address) {
        Intent i = new Intent(Intent.ACTION_SENDTO);
        i.setData(Uri.parse("mailto:"+address));
        startActionActivity(i);
    }

    private void makeVideoCall(String number) {
        if (hasRequiredPermissions()) {
            Intent i = new Intent("com.android.phone.videocall");
            i.putExtra("videoCall", true);
            i.setData(Uri.parse("tel:" + number));
            startActionActivity(i);
        }
        else {
            vm.addHaltedTask(()->makeVideoCall(number));
            requestRequiredPermissions();
        }
    }

    private void startActionActivity(Intent intent) {
        startActivity(Intent.createChooser(intent,"Choose"));
    }

    private void onContactDetailsLoaded(ContactDetails details) {
        if (null != details) {
            vm.getContactEvents(details).observe(this,events -> eventAdapter.changeItems(events));
            vm.getContactRelations(details).observe(this,relations -> relationAdapter.changeItems(relations));

            Glide.with(this).load(details.getPhotoUri())
                    .placeholder(R.mipmap.placeholder_contact_photo).into(contactPhoto);

            displayName.setText(details.getDisplayName());

            if (details.hasPhoneNumberPrimary()) {
                btnVoiceCallPrimary.setVisibility(View.VISIBLE);
                btnSmsPrimary.setVisibility(View.VISIBLE);
                btnVideoCallPrimary.setVisibility(View.VISIBLE);
                numberPrimary.setText(details.getPhoneNumberPrimary().getNumber());
            }
            else {
                btnVoiceCallPrimary.setVisibility(View.GONE);
                btnSmsPrimary.setVisibility(View.GONE);
                btnVideoCallPrimary.setVisibility(View.GONE);
                numberPrimary.setText(null);
            }
            if (details.hasEmailPrimary()) {
                btnEmailPrimary.setVisibility(View.VISIBLE);
            }
            else {
                btnEmailPrimary.setVisibility(View.GONE);
            }
            phoneNumberAdapter.changeItems(details.getPhoneNumbers());
            emailAdapter.changeItems(details.getEmails());
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    ///                                  Runtime Permission                                     ///
    //////////////////////////////////////////////////////////////////////////////////////////////

    private static final int PERMISSION_CODE = 1;

    private static final String[] PERMISSIONS = new String[]{
            Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS,
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