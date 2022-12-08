package rahulstech.android.phonebook;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import rahulstech.android.phonebook.concurrent.AsyncTask;
import rahulstech.android.phonebook.model.ContactDetails;
import rahulstech.android.phonebook.model.Email;
import rahulstech.android.phonebook.model.Event;
import rahulstech.android.phonebook.model.Name;
import rahulstech.android.phonebook.model.Note;
import rahulstech.android.phonebook.model.Organization;
import rahulstech.android.phonebook.model.PhoneNumber;
import rahulstech.android.phonebook.model.PostalAddress;
import rahulstech.android.phonebook.model.RawContact;
import rahulstech.android.phonebook.model.Relation;
import rahulstech.android.phonebook.model.Website;
import rahulstech.android.phonebook.util.Check;
import rahulstech.android.phonebook.util.DrawableUtil;
import rahulstech.android.phonebook.view.Animations;
import rahulstech.android.phonebook.view.ContactInputItemManager;
import rahulstech.android.phonebook.view.ContactSourceAdapter;
import rahulstech.android.phonebook.viewmodel.ContactViewModel;

import static android.provider.ContactsContract.CommonDataKinds.BaseTypes.TYPE_CUSTOM;
import static android.provider.ContactsContract.Intents.Insert.EMAIL;
import static android.provider.ContactsContract.Intents.Insert.EMAIL_TYPE;
import static android.provider.ContactsContract.Intents.Insert.NAME;
import static android.provider.ContactsContract.Intents.Insert.PHONE;
import static android.provider.ContactsContract.Intents.Insert.PHONE_TYPE;
import static android.provider.ContactsContract.Intents.Insert.SECONDARY_EMAIL;
import static android.provider.ContactsContract.Intents.Insert.SECONDARY_EMAIL_TYPE;
import static android.provider.ContactsContract.Intents.Insert.SECONDARY_PHONE;
import static android.provider.ContactsContract.Intents.Insert.SECONDARY_PHONE_TYPE;
import static android.provider.ContactsContract.Intents.Insert.TERTIARY_EMAIL;
import static android.provider.ContactsContract.Intents.Insert.TERTIARY_EMAIL_TYPE;
import static android.provider.ContactsContract.Intents.Insert.TERTIARY_PHONE;
import static android.provider.ContactsContract.Intents.Insert.TERTIARY_PHONE_TYPE;
import static rahulstech.android.phonebook.util.Check.isEquals;
import static rahulstech.android.phonebook.util.Helpers.anyNonEmpty;

public class ContactInputActivity extends PhoneBookActivity {
    // TODO: Contact photo not implemented
    // TODO: contact group not implemented
    // TODO: rethink contact multi source chooser

    private static final String TAG = "ContactInput";

    private static final int RC_CONTACT_SOURCE = 3;

    private static final String KEY_CONTACT_FETCHED = "contact_fetched";
    private static final String KEY_INITIALIZED = "initialized";
    private static final String KEY_SOURCE_SELECTION = "source_selection";
    private static final String KEY_VIEW_STRUCTURED_NAME = "view_structured_name";
    private static final String KEY_VIEW_MORE = "view_more";
    private static final String KEY_VIEW_MORE_NAME = "view_more_name";
    private static final String KEY_NUMBERS = "numbers";
    private static final String KEY_EMAILS = "emails";
    private static final String KEY_EVENTS = "events";
    private static final String KEY_ADDRESS = "address";
    private static final String KEY_RELATIONS = "relations";
    private static final String KEY_SECTION_ORGANIZATION_VISIBLE = "section_org_visible";
    private static final String KEY_WEBSITES = "websites";
    private static final String KEY_SECTION_NOTE_VISIBLE = "section_note_visible";

    private ContactViewModel vm;

    private boolean mContactFetched = false;
    private int mSourceSelection = -1;
    private boolean mInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vm = getOrCreateViewModel(ContactViewModel.class);
        if (!showContactSourceChooserIfNeeded()) {
            initialize(savedInstanceState);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_CONTACT_SOURCE) {
            logDebug(TAG,"resultCode="+resultCode);
            if (resultCode == RESULT_OK) {
                long rawContactId = data.getLongExtra(ActivityContactSourceChooser.KEY_RAW_CONTACT_ID,0);
                String name = data.getStringExtra(ActivityContactSourceChooser.KEY_ACCOUNT_NAME);
                String type = data.getStringExtra(ActivityContactSourceChooser.KEY_ACCOUNT_TYPE);
                RawContact source = new RawContact(0,rawContactId,name,type);
                vm.setEditContactSource(source);
                initialize(null);
            }
            else {
                finish();
            }
        }
    }

    private boolean showContactSourceChooserIfNeeded() {
        if (isEditOperation() && null==vm.getEditContactSource()) {
            AsyncTask.execute(()->vm.getRepository().getRawContactsForContactEditing(getIntent().getData()),new AsyncTask.AsyncTaskCallback(){
                @Override
                public void onError(AsyncTask task) {
                    Log.e(TAG,"",task.getError());
                    Toast.makeText(ContactInputActivity.this,
                            R.string.message_error_contact_edit,Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onResult(AsyncTask task) {
                    onChooseContactSource(task.getResult());
                }
            });
            return true;
        }
        return false;
    }

    private void onChooseContactSource(@Nullable List<RawContact> sources) {
        Log.i(TAG,"onChooseContactSource: no of source: "+(null==sources ? "NULL" : sources.size()));
        if (null == sources || sources.isEmpty()) {
            // no source for contact
            Toast.makeText(this,R.string.message_error_contact_edit,Toast.LENGTH_SHORT).show();
            finish();
        }
        else if (sources.size()==1) {
            // no linked contact
            vm.setEditContactSource(sources.get(0));
            initialize(null);
        }
        else {
            // has linked contact
            Intent intent = new Intent(this,ActivityContactSourceChooser.class);
            intent.setData(getIntent().getData());
            startActivityForResult(intent,RC_CONTACT_SOURCE);
        }
    }

    private boolean isEditOperation() {
        return Intent.ACTION_EDIT.equals(getIntent().getAction());
    }

    private void initialize(@Nullable Bundle savedState) {
        setContentView(R.layout.activity_contact_input);
        onInitSectionName();
        onInitSectionNumber();
        onInitSectionEmail();
        onInitSectionEvent();
        onInitSectionAddress();
        onInitSectionRelation();
        onInitSectionOrganization();
        onInitSectionWebsite();
        onInitSectionNote();
        onInitOther();
        if (isEditOperation()) {
            title.setText(R.string.label_edit_contact);
            mContactFetched = null != savedState && savedState.getBoolean(KEY_CONTACT_FETCHED, false);
            boolean wasContactLoadingDone = mContactFetched && vm.hasContactDetails();
            logDebug(TAG,"wasContactLoadingDone "+wasContactLoadingDone);
            if (!wasContactLoadingDone) loadContact(getIntent().getData());
            else mContactFetched = true;
        }
        else
            title.setText(R.string.label_create_contact);
        mInitialized = true;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_CONTACT_FETCHED,mContactFetched);
        outState.putBoolean(KEY_INITIALIZED,mInitialized);
        if (mInitialized) {
            onSaveName(outState);
            onSaveSectionNumberState(outState);
            onSaveSectionEmailState(outState);
            onSaveSectionEventState(outState);
            onSaveSectionAddressState(outState);
            onSaveSectionRelationState(outState);
            onSaveSectionOrganizationState(outState);
            onSaveSectionWebsiteState(outState);
            onSaveSectionNoteState(outState);
            onSaveOther(outState);
        }
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedState) {
        super.onRestoreInstanceState(savedState);
        mInitialized = savedState.getBoolean(KEY_INITIALIZED,false);
        if (mInitialized) {
            onRestoreName(savedState);
            onRestoreSectionNumberState(savedState);
            onRestoreSectionEmailState(savedState);
            onRestoreSectionEventState(savedState);
            onRestoreSectionAddressState(savedState);
            onRestoreSectionRelationState(savedState);
            onRestoreSectionOrganizationState(savedState);
            onRestoreSectionWebsiteState(savedState);
            onRestoreSectionNoteState(savedState);
            onRestoreOther(savedState);
        }
    }

    private void loadContact(@NonNull Uri uri) {
        AsyncTask.AsyncTaskCallback callback = new AsyncTask.AsyncTaskCallback(){
            @Override
            public void onError(AsyncTask task) {
                Log.e(TAG,null,task.getError());
                Toast.makeText(ContactInputActivity.this,R.string.message_error_contact_edit,Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onResult(AsyncTask task) {
                ContactDetails details = task.getResult();
                if (null == details)
                    finish();
                else
                    onContactLoaded(details);
            }
        };
        AsyncTask.execute(()->vm.getRepository().getContactDetails(uri,vm.getEditContactSource()),callback);
    }

    private void onContactLoaded(@NonNull ContactDetails details) {
        vm.setContactDetails(details);
        RawContact rawContact = details.getRawContact();
        int position = contactSourceAdapter.getPositionByTypeAndName(rawContact.getType(),rawContact.getName());
        selectContactSource(position);
        onPrepareSectionName(details.getName());
        managerItemNumber.setData(details.getPhoneNumbers());
        managerItemEmails.setData(details.getEmails());
        managerItemEvents.setData(details.getEvents());
        onSetAddresses(details.getAddresses());
        onSetRelations(details.getRelations());
        onSetOrganization(details.getOrganization());
        onSetWebsites(details.getWebsites());
        onPrepareSectionNote(details.getNote());
        checkViewMoreButtonShouldShow();
        mContactFetched = true; // setting value last to ensure views prepared
    }

    private void onCancelContactInput() {
        // TODO: must provide warning if not saved
        finish();
    }

    private void onSaveContactDetails() {
        final ContactDetails newDetails = extractContactDetails();
        final ContactDetails oldDetails = vm.getContactDetails();
        if (null == newDetails) {
            if (isEditOperation()) {
                new AlertDialog.Builder(this)
                        .setMessage(getString(R.string.message_empty_input_delete_contact))
                        .setPositiveButton(R.string.label_cancel,(di,which)->{
                            finish();
                        })
                        .setNegativeButton(R.string.label_delete,(di,which)->{
                            vm.removeContact(oldDetails,null);
                            finish();
                        })
                        .setCancelable(false)
                        .show();
            }
            else {
                Toast.makeText(this,R.string.message_save_nothing,Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }
        else if (isEditOperation() && isEquals(newDetails,oldDetails)){
            Toast.makeText(this,R.string.message_save_nothing,Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        AsyncTask.AsyncTaskCallback callback = new AsyncTask.AsyncTaskCallback(){
            @Override
            public void onError(AsyncTask task) {
                Log.e(TAG,"",task.getError());
            }

            @Override
            public void onResult(AsyncTask task) {
                boolean saved = task.getResult();
                if (saved) {
                    Toast.makeText(ContactInputActivity.this,R.string.message_saved_successful,Toast.LENGTH_SHORT).show();
                    finish();
                }
                else {
                    Toast.makeText(ContactInputActivity.this,R.string.message_save_fail,Toast.LENGTH_SHORT).show();
                }
            }
        };

        if (isEditOperation()) {
            vm.updateContactDetails(oldDetails,newDetails,callback);
        }
        else {
            vm.insertContactDetails(newDetails,callback);
        }
    }

    private ContactDetails extractContactDetails() {
        RawContact rawContact;
        if (isEditOperation()) {
            rawContact = vm.getEditContactSource();
        }
        else {
            RawContact selected = (RawContact) contactSourceChooser.getSelectedItem();
            rawContact = new RawContact(selected);
        }
        Name name = extractName();
        List<PhoneNumber> numbers = managerItemNumber.extractAllData();
        List<Email> emails = managerItemEmails.extractAllData();
        List<Event> events = managerItemEvents.extractAllData();
        List<PostalAddress> addresses = managerItemAddress.extractAllData();
        List<Relation> relations = managerItemRelation.extractAllData();
        Organization organization = extractOrganization();
        List<Website> websites = managerItemWebsite.extractAllData();
        Note note = extractNote();
        if (null != name || null != numbers || null != emails
                || null != events || null != addresses || null != relations
                || null != organization || null != websites || null != note) {
            ContactDetails details = new ContactDetails(null);
            details.setRawContact(rawContact);
            details.setName(name);
            details.setPhoneNumbers(numbers);
            details.setEmails(emails);
            details.setEvents(events);
            details.setAddresses(addresses);
            details.setRelations(relations);
            details.setOrganization(organization);
            details.setWebsites(websites);
            details.setNote(note);
            return details;
        }
        return null;
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    ///                              Section Name                                      ///
    /////////////////////////////////////////////////////////////////////////////////////

    private CheckBox btnSwitchName;
    private ViewGroup sectionContactNameBasic;
    private View sectionDisplayName;
    private View sectionStructuredName;
    private View sectionMoreName;
    private EditText iDisplayFirstName;
    private EditText iDisplayLastName;
    private EditText iPrefix;
    private EditText iFirstName;
    private EditText iMiddleName;
    private EditText iLastName;
    private EditText iSuffix;
    private EditText iPhFirstName;
    private EditText iPhMiddleName;
    private EditText iPhLastName;
    private EditText iNickname;

    private void onInitSectionName() {
        sectionContactNameBasic = findViewById(R.id.section_contact_name_basic);
        btnSwitchName = findViewById(R.id.button_switch_name);
        sectionDisplayName = findViewById(R.id.section_display_name);
        sectionStructuredName = findViewById(R.id.section_structured_name);
        sectionMoreName = findViewById(R.id.section_more_name);
        iDisplayFirstName = findViewById(R.id.display_first_name);
        iDisplayLastName = findViewById(R.id.display_last_name);
        iPrefix = findViewById(R.id.prefix);
        iFirstName = findViewById(R.id.first_name);
        iMiddleName = findViewById(R.id.middle_name);
        iLastName = findViewById(R.id.last_name);
        iSuffix = findViewById(R.id.suffix);
        iPhFirstName = findViewById(R.id.phonetic_first_name);
        iPhMiddleName = findViewById(R.id.phonetic_middle_name);
        iPhLastName = findViewById(R.id.phonetic_last_name);
        iNickname = findViewById(R.id.nickname);
        ContactInputItemManager.turnOffAutofill(iDisplayFirstName);
        ContactInputItemManager.turnOffAutofill(iDisplayLastName);
        ContactInputItemManager.turnOffAutofill(iPrefix);
        ContactInputItemManager.turnOffAutofill(iFirstName);
        ContactInputItemManager.turnOffAutofill(iMiddleName);
        ContactInputItemManager.turnOffAutofill(iLastName);
        ContactInputItemManager.turnOffAutofill(iSuffix);
        ContactInputItemManager.turnOffAutofill(iPhFirstName);
        ContactInputItemManager.turnOffAutofill(iPhMiddleName);
        ContactInputItemManager.turnOffAutofill(iPhLastName);
        ContactInputItemManager.turnOffAutofill(iNickname);
        btnSwitchName.setOnCheckedChangeListener((v,checked)-> setSectionContactNameBasic(checked));

        if (!isEditOperation()) {
            Name name = extraName();
            onPrepareSectionName(name);
        }
    }

    private void onSaveName(@NotNull Bundle outState) {
        outState.putBoolean(KEY_VIEW_STRUCTURED_NAME,btnSwitchName.isChecked());
        outState.putBoolean(KEY_VIEW_MORE_NAME,sectionMoreName.getVisibility() == View.VISIBLE);
    }

    private void onRestoreName(@NonNull Bundle savedState) {
        setSectionContactNameBasic(savedState.getBoolean(KEY_VIEW_STRUCTURED_NAME));
        sectionMoreName.setVisibility(savedState.getBoolean(KEY_VIEW_MORE_NAME) ? View.VISIBLE : View.GONE);
    }

    private void onPrepareSectionName(@Nullable Name name) {
        if (null == name) return;
        iDisplayFirstName.setText(name.getGivenName());
        iDisplayLastName.setText(name.getFamilyName());
        iPrefix.setText(name.getPrefix());
        iFirstName.setText(name.getGivenName());
        iMiddleName.setText(name.getMiddleName());
        iLastName.setText(name.getFamilyName());
        iSuffix.setText(name.getSuffix());
        iPhFirstName.setText(name.getPhoneticGivenName());
        iPhMiddleName.setText(name.getPhoneticMiddleName());
        iPhLastName.setText(name.getPhoneticFamilyName());
        iNickname.setText(name.getNickname());
        if (anyNonEmpty(name.getNickname(),name.getPhoneticGivenName(),
                name.getPhoneticFamilyName(),name.getPhoneticMiddleName())) {
            sectionMoreName.setVisibility(View.VISIBLE);
        }
    }

    private void setSectionContactNameBasic(boolean which) {
        String firstName,lastName;
        Animator anim;
        if (which) {
            firstName = iDisplayFirstName.getText().toString();
            lastName = iDisplayLastName.getText().toString();
            iFirstName.setText(firstName);
            iLastName.setText(lastName);
            // show all name input fields
            anim = Animations.animateHeight(sectionContactNameBasic,
                    Animations.measureHeight(sectionDisplayName), Animations.measureHeight(sectionStructuredName),
                    Animations.DURATION_NORMAL, new AccelerateDecelerateInterpolator());
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    sectionDisplayName.setVisibility(View.GONE);
                    sectionStructuredName.setVisibility(View.VISIBLE);
                }
            });
        }
        else {
            // show only display name
            firstName = iFirstName.getText().toString();
            lastName = iLastName.getText().toString();
            iDisplayFirstName.setText(firstName);
            iDisplayLastName.setText(lastName);
            anim = Animations.animateHeight(sectionContactNameBasic,
                    Animations.measureHeight(sectionStructuredName), Animations.measureHeight(sectionDisplayName),
                    Animations.DURATION_NORMAL, new AccelerateDecelerateInterpolator());
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    sectionStructuredName.setVisibility(View.GONE);
                    sectionDisplayName.setVisibility(View.VISIBLE);
                }
            });
        }
        anim.start();
    }

    private Name extractName() {
        String prefix = iPrefix.getText().toString();
        String middleName = iMiddleName.getText().toString();
        String suffix = iSuffix.getText().toString();
        String firstName;
        if (btnSwitchName.isChecked()) {
            firstName = iFirstName.getText().toString();
        }
        else {
            firstName = iDisplayFirstName.getText().toString();
        }
        String lastName;
        if (btnSwitchName.isChecked()) {
            lastName = iLastName.getText().toString();
        }
        else {
            lastName = iDisplayLastName.getText().toString();
        }
        String phFirstName = iPhFirstName.getText().toString();
        String phMiddleName = iPhMiddleName.getText().toString();
        String phLastName = iPhLastName.getText().toString();
        String nickname = iNickname.getText().toString();

        if (!anyNonEmpty(prefix,firstName,middleName,lastName,suffix,
                phFirstName,phMiddleName,phLastName,nickname)) return null;

        Name name = new Name();
        name.setPrefix(prefix);
        name.setGivenName(firstName);
        name.setMiddleName(middleName);
        name.setFamilyName(lastName);
        name.setSuffix(suffix);
        name.setPhoneticGivenName(phFirstName);
        name.setPhoneticMiddleName(phMiddleName);
        name.setPhoneticFamilyName(phLastName);
        name.setNickname(nickname);
        name.buildDisplayNameFirstNameFirst();
        return name;
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    ///                             Section Number                                     ///
    /////////////////////////////////////////////////////////////////////////////////////

    private ViewGroup sectionNumber;
    private ContactInputItemManager<PhoneNumber, ContactInputItemManager.BaseItem<PhoneNumber>> managerItemNumber;

    private void onInitSectionNumber() {
        sectionNumber = findViewById(R.id.section_phone_number);
        ViewGroup numbersList = sectionNumber.findViewById(R.id.grid);
        ImageView icon = sectionNumber.findViewById(R.id.section_icon);
        icon.setImageDrawable(DrawableUtil.vectorDrawable(this,R.drawable.ic_baseline_phone));
        TextView name = sectionNumber.findViewById(R.id.section_name);
        name.setText(R.string.label_mobile);
        sectionNumber.findViewById(R.id.action_add).setOnClickListener(v->managerItemNumber.addItemFirst(true));
        managerItemNumber = ContactInputItemManager.forPhoneNumber(this,numbersList);
        managerItemNumber.addItemFirst(false);

        if (!isEditOperation()) {
            ArrayList<PhoneNumber> numbers = allExtraPhoneNumber();
            logDebug(TAG,"extra numbers: "+numbers);
            if (!numbers.isEmpty()) managerItemNumber.setData(numbers);
        }
    }
    
    private void onSaveSectionNumberState(@NonNull Bundle outState) {
        outState.putParcelable(KEY_NUMBERS,managerItemNumber.onSaveInstanceState());
    }
    
    private void onRestoreSectionNumberState(@NonNull Bundle savedState) {
        managerItemNumber.onRestoreInstanceState(savedState.getParcelable(KEY_NUMBERS));
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    ///                              Section Email                                     ///
    /////////////////////////////////////////////////////////////////////////////////////

    private ViewGroup sectionEmail;
    private ContactInputItemManager<Email, ContactInputItemManager.BaseItem<Email>> managerItemEmails;

    private void onInitSectionEmail() {
        sectionEmail = findViewById(R.id.section_email);
        ImageView icon = sectionEmail.findViewById(R.id.section_icon);
        icon.setImageDrawable(DrawableUtil.vectorDrawable(this,R.drawable.ic_baseline_email));
        TextView email = sectionEmail.findViewById(R.id.section_name);
        email.setText(R.string.label_email);
        ViewGroup emailsList = sectionEmail.findViewById(R.id.grid);
        sectionEmail.findViewById(R.id.action_add).setOnClickListener(v->managerItemEmails.addItemFirst(true));
        managerItemEmails = ContactInputItemManager.forEmail(this, emailsList);
        managerItemEmails.addItemFirst(false);

        if (!isEditOperation()) {
            ArrayList<Email> emails = allExtraEmail();
            logDebug(TAG,"extra emails: "+emails);
            if (!emails.isEmpty()) managerItemEmails.setData(emails);
        }
    }

    private void onSaveSectionEmailState(@NonNull Bundle outState) {
        outState.putParcelable(KEY_EMAILS,managerItemEmails.onSaveInstanceState());
    }

    private void onRestoreSectionEmailState(@NonNull Bundle savedState) {
        managerItemEmails.onRestoreInstanceState(savedState.getParcelable(KEY_EMAILS));
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    ///                              Section Event                                     ///
    /////////////////////////////////////////////////////////////////////////////////////

    private ViewGroup sectionEvent;
    private ContactInputItemManager<Event, ContactInputItemManager.BaseItem<Event>> managerItemEvents;

    private void onInitSectionEvent() {
        sectionEvent = findViewById(R.id.section_event);
        ImageView icon = sectionEvent.findViewById(R.id.section_icon);
        icon.setImageDrawable(DrawableUtil.vectorDrawable(this,R.drawable.ic_baseline_calendar_month));
        TextView name = sectionEvent.findViewById(R.id.section_name);
        name.setText(R.string.label_event);
        ViewGroup eventsList = sectionEvent.findViewById(R.id.grid);
        sectionEvent.findViewById(R.id.action_add).setOnClickListener(v->managerItemEvents.addItemFirst(true));
        managerItemEvents = ContactInputItemManager.forEvent(this,eventsList,getSupportFragmentManager());
        managerItemEvents.addItemFirst(false);
    }

    private void onSaveSectionEventState(@NonNull Bundle outState) {
        outState.putParcelable(KEY_EVENTS,managerItemEvents.onSaveInstanceState());
    }

    private void onRestoreSectionEventState(@NonNull Bundle savedState) {
        managerItemEvents.onRestoreInstanceState(savedState.getParcelable(KEY_EVENTS));
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    ///                            Section Address                                     ///
    /////////////////////////////////////////////////////////////////////////////////////

    private ViewGroup sectionAddress;
    private ContactInputItemManager<PostalAddress, ContactInputItemManager.BaseItem<PostalAddress>> managerItemAddress;

    private void onInitSectionAddress() {
        sectionAddress = findViewById(R.id.section_postal_address);
        ImageView icon = sectionAddress.findViewById(R.id.section_icon);
        icon.setImageDrawable(DrawableUtil.vectorDrawable(this,R.drawable.ic_baseline_location));
        TextView name = sectionAddress.findViewById(R.id.section_name);
        name.setText(R.string.label_address);
        ViewGroup addressesList = sectionAddress.findViewById(R.id.grid);
        sectionAddress.findViewById(R.id.action_add).setOnClickListener(v->managerItemAddress.addItemFirst(true));
        managerItemAddress = ContactInputItemManager.forPostalAddress(this,addressesList);
    }

    private void onSaveSectionAddressState(@NonNull Bundle outState) {
        managerItemAddress.setSectionVisible(View.VISIBLE == sectionAddress.getVisibility());
        outState.putParcelable(KEY_ADDRESS,managerItemAddress.onSaveInstanceState());
    }

    private void onRestoreSectionAddressState(@NonNull Bundle savedState) {
        managerItemAddress.onRestoreInstanceState(savedState.getParcelable(KEY_ADDRESS));
        sectionAddress.setVisibility(managerItemAddress.isSectionVisible() ? View.VISIBLE : View.GONE);
    }

    private void onSetAddresses(@Nullable List<PostalAddress> addresses) {
        managerItemAddress.setData(addresses);
        if (null != addresses && !addresses.isEmpty()) sectionAddress.setVisibility(View.VISIBLE);
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    ///                            Section Relation                                    ///
    /////////////////////////////////////////////////////////////////////////////////////

    private ViewGroup sectionRelation;
    private ContactInputItemManager<Relation, ContactInputItemManager.BaseItem<Relation>> managerItemRelation;

    private void onInitSectionRelation() {
        sectionRelation = findViewById(R.id.section_relation);
        ImageView icon = sectionRelation.findViewById(R.id.section_icon);
        icon.setImageDrawable(DrawableUtil.vectorDrawable(this,R.drawable.ic_baseline_relation));
        TextView name = sectionRelation.findViewById(R.id.section_name);
        name.setText(R.string.label_relation);
        ViewGroup relationsList = sectionRelation.findViewById(R.id.grid);
        sectionRelation.findViewById(R.id.action_add).setOnClickListener(v->managerItemRelation.addItemFirst(true));
        managerItemRelation = ContactInputItemManager.forRelation(this,relationsList);
    }

    private void onSaveSectionRelationState(@NonNull Bundle outState) {
        managerItemRelation.setSectionVisible(View.VISIBLE == sectionRelation.getVisibility());
        outState.putParcelable(KEY_RELATIONS,managerItemRelation.onSaveInstanceState());
    }

    private void onRestoreSectionRelationState(@NonNull Bundle savedState) {
        managerItemRelation.onRestoreInstanceState(savedState.getParcelable(KEY_RELATIONS));
        sectionRelation.setVisibility(managerItemRelation.isSectionVisible() ? View.VISIBLE : View.GONE);
    }

    private void onSetRelations(@Nullable List<Relation> relations) {
        managerItemRelation.setData(relations);
        if (null != relations && !relations.isEmpty()) sectionRelation.setVisibility(View.VISIBLE);
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    ///                            Section Organization                                ///
    /////////////////////////////////////////////////////////////////////////////////////

    private ViewGroup sectionOrganization;
    private EditText iCompany;
    private EditText iTitle;
    private EditText iDepartment;

    private void onInitSectionOrganization() {
        sectionOrganization = findViewById(R.id.section_organization);
        iCompany = findViewById(R.id.org_company);
        iTitle = findViewById(R.id.org_title);
        iDepartment = findViewById(R.id.org_department);
        findViewById(R.id.clear_organization).setOnClickListener(v->clearOrganization());
    }

    private void clearOrganization() {
        iCompany.setText(null);
        iTitle.setText(null);
        iDepartment.setText(null);
    }

    private void onSaveSectionOrganizationState(@NonNull Bundle outState) {
        outState.putBoolean(KEY_SECTION_ORGANIZATION_VISIBLE,sectionOrganization.getVisibility()==View.VISIBLE);
    }

    private void onRestoreSectionOrganizationState(@NonNull Bundle savedState) {
        boolean visible = savedState.getBoolean(KEY_SECTION_ORGANIZATION_VISIBLE,false);
        sectionOrganization.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void onSetOrganization(@Nullable Organization org) {
        if (null != org) {
            iCompany.setText(org.getCompany());
            iTitle.setText(org.getTitle());
            iDepartment.setText(org.getDepartment());
            sectionOrganization.setVisibility(View.VISIBLE);
        }
    }

    private Organization extractOrganization() {
        String company = iCompany.getText().toString();
        String title = iTitle.getText().toString();
        String department = iDepartment.getText().toString();
        if (!anyNonEmpty(company,title,department)) return null;
        Organization org = new Organization();
        org.setCompany(company);
        org.setTitle(title);
        org.setDepartment(department);
        return org;
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    ///                            Section Website                                     ///
    /////////////////////////////////////////////////////////////////////////////////////

    private ViewGroup sectionWebsite;
    private ContactInputItemManager<Website, ContactInputItemManager.BaseItem<Website>> managerItemWebsite;

    private void onInitSectionWebsite() {
        sectionWebsite = findViewById(R.id.section_website);
        ImageView icon = sectionWebsite.findViewById(R.id.section_icon);
        icon.setImageDrawable(DrawableUtil.vectorDrawable(this,R.drawable.ic_baseline_website));
        TextView name = sectionWebsite.findViewById(R.id.section_name);
        name.setText(R.string.label_website);
        ViewGroup websitesList = sectionWebsite.findViewById(R.id.grid);
        sectionWebsite.findViewById(R.id.action_add).setOnClickListener(v->managerItemWebsite.addItemFirst(true));
        managerItemWebsite = ContactInputItemManager.forWebsite(this,websitesList);
    }

    private void onSaveSectionWebsiteState(@NonNull Bundle outState) {
        managerItemWebsite.setSectionVisible(View.VISIBLE == sectionWebsite.getVisibility());
        outState.putParcelable(KEY_WEBSITES,managerItemWebsite.onSaveInstanceState());
    }

    private void onRestoreSectionWebsiteState(@NonNull Bundle savedState) {
        managerItemWebsite.onRestoreInstanceState(savedState.getParcelable(KEY_WEBSITES));
        sectionWebsite.setVisibility(managerItemWebsite.isSectionVisible() ? View.VISIBLE : View.GONE);
    }

    private void onSetWebsites(@Nullable List<Website> websites) {
        managerItemWebsite.setData(websites);
        if (null != websites && !websites.isEmpty()) sectionWebsite.setVisibility(View.VISIBLE);
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    ///                              Section Note                                      ///
    /////////////////////////////////////////////////////////////////////////////////////

    private View sectionNote;
    private EditText editTextNote;
    private View btnClearNote;

    // TODO: note input not scrolling

    private void onInitSectionNote() {
        sectionNote = findViewById(R.id.section_note);
        editTextNote = findViewById(R.id.input_note);
        btnClearNote = findViewById(R.id.clear_note);
        btnClearNote.setOnClickListener(v->onClearNote());
    }

    private void onSaveSectionNoteState(@NonNull Bundle outState) {
        boolean visible = View.VISIBLE == sectionNote.getVisibility();
        outState.putBoolean(KEY_SECTION_NOTE_VISIBLE,visible);
    }

    private void onRestoreSectionNoteState(@NonNull Bundle savedState) {
        boolean visible = savedState.getBoolean(KEY_SECTION_NOTE_VISIBLE,false);
        sectionNote.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void onPrepareSectionNote(Note note) {
        if (null == note) return;
        editTextNote.setText(note.getNote());
        sectionNote.setVisibility(View.VISIBLE);
    }

    private Note extractNote() {
        String text = editTextNote.getText().toString();
        if (Check.isEmptyString(text)) return null;
        Note note = new Note();
        note.setNote(text);
        return note;
    }

    private void onClearNote() {
        editTextNote.setText(null);
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    ///                            Section Others                                      ///
    /////////////////////////////////////////////////////////////////////////////////////

    private TextView title;
    private Spinner contactSourceChooser;
    private ImageView contactPhoto;
    private CheckBox btnViewMore;
    private ContactSourceAdapter contactSourceAdapter;

    private void onInitOther() {
        findViewById(R.id.button_cancel).setOnClickListener(v->onCancelContactInput());
        findViewById(R.id.button_save).setOnClickListener(v-> onSaveContactDetails());
        title = findViewById(R.id.title);
        contactSourceChooser = findViewById(R.id.choose_account);
        contactPhoto = findViewById(R.id.contact_photo);
        btnViewMore = findViewById(R.id.button_view_more);
        if (isEditOperation()) contactSourceChooser.setEnabled(false);
        contactSourceAdapter = new ContactSourceAdapter(this);
        contactSourceChooser.setAdapter(contactSourceAdapter);
        AsyncTask.execute(()->vm.getRepository().getAllContactSources(),new AsyncTask.AsyncTaskCallback(){
            @Override
            public void onError(AsyncTask task) {
                Log.e(TAG,"",task.getError());
                Toast.makeText(ContactInputActivity.this,R.string.message_error_contact_edit,Toast.LENGTH_SHORT).show();
                finish();
            }
            @Override
            public void onResult(AsyncTask task) {
                onContactSourcesLoaded(task.getResult());
            }
        });
        btnViewMore.setOnCheckedChangeListener((cb, checked)->onViewMore());
        contactPhoto.setOnClickListener(v->onChooseContactPhoto());
    }

    private void onChooseContactPhoto() {
        // TODO: pick contact photo

    }

    private void onContactSourcesLoaded(@Nullable List<RawContact> sources) {
        contactSourceAdapter.setSources(sources);
        selectContactSource(mSourceSelection);
    }

    private void selectContactSource(int position) {
        if (position >= 0 && position < contactSourceAdapter.getCount())
            contactSourceChooser.setSelection(position);
    }

    private void onSaveOther(@NonNull Bundle outState) {
        outState.putInt(KEY_SOURCE_SELECTION,contactSourceChooser.getSelectedItemPosition());
        outState.putBoolean(KEY_VIEW_MORE,btnViewMore.isChecked());
    }

    private void onRestoreOther(@NonNull Bundle savedState) {
        mSourceSelection = savedState.getInt(KEY_SOURCE_SELECTION,-1);
        selectContactSource(mSourceSelection);
        btnViewMore.setChecked(savedState.getBoolean(KEY_VIEW_MORE));
    }

    private void checkViewMoreButtonShouldShow() {
        boolean anyHidden = btnViewMore.getVisibility() == View.GONE || sectionMoreName.getVisibility() == View.GONE
                || sectionAddress.getVisibility() == View.GONE || sectionRelation.getVisibility() == View.GONE
                || sectionOrganization.getVisibility() == View.GONE || sectionWebsite.getVisibility() == View.GONE
                || sectionNote.getVisibility() == View.GONE;
        if (!anyHidden) btnViewMore.setVisibility(View.GONE);
    }

    private void onViewMore() {
        // TODO: make all sections visible
        btnViewMore.setVisibility(View.GONE);
        sectionMoreName.setVisibility(View.VISIBLE);
        sectionAddress.setVisibility(View.VISIBLE);
        sectionRelation.setVisibility(View.VISIBLE);
        sectionOrganization.setVisibility(View.VISIBLE);
        sectionWebsite.setVisibility(View.VISIBLE);
        sectionNote.setVisibility(View.VISIBLE);
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    ///                              Utility Methods                                   ///
    /////////////////////////////////////////////////////////////////////////////////////

    private Name extraName() {
        String displayName = getIntent().getStringExtra(NAME);
        if (Check.isEmptyString(displayName)) return null;
        Name name = new Name();
        name.setDisplayName(displayName);
        return name;
    }

    private ArrayList<PhoneNumber> allExtraPhoneNumber() {
        PhoneNumber number1 = extraPhoneNumber(PHONE,PHONE_TYPE);
        PhoneNumber number2 = extraPhoneNumber(SECONDARY_PHONE,SECONDARY_PHONE_TYPE);
        PhoneNumber number3 = extraPhoneNumber(TERTIARY_PHONE,TERTIARY_PHONE_TYPE);
        ArrayList<PhoneNumber> numbers = new ArrayList<>();
        if (null!=number1) numbers.add(number1);
        if (null!=number2) numbers.add(number2);
        if (null!=number3) numbers.add(number3);
        return numbers;
    }

    private ArrayList<Email> allExtraEmail() {
        Email email1 = extraEmail(EMAIL,EMAIL_TYPE);
        Email email2 = extraEmail(SECONDARY_EMAIL,SECONDARY_EMAIL_TYPE);
        Email email3 = extraEmail(TERTIARY_EMAIL,TERTIARY_EMAIL_TYPE);
        ArrayList<Email> emails = new ArrayList<>();
        if (null!=email1) emails.add(email1);
        if (null!=email2) emails.add(email2);
        if (null!=email3) emails.add(email3);
        return emails;
    }

    private PhoneNumber extraPhoneNumber(String keyPhone, String keyType) {
        String phone = getIntent().getStringExtra(keyPhone);
        if (Check.isEmptyString(phone)) return null;
        PhoneNumber number = new PhoneNumber();
        number.setNumber(phone);
        try {
            number.setType(extraContactDataType(keyType));
            number.setTypeLabel(null);
        }
        catch (NullPointerException ignore) {
            number.setType(ContactsContract.CommonDataKinds.Phone.TYPE_MAIN);
        }
        catch (NumberFormatException ex) {
            number.setType(TYPE_CUSTOM);
            number.setTypeLabel(extraContactDataTypeLabel(keyType));
        }
        return number;
    }

    private Email extraEmail(String keyEmail, String keyType) {
        String address = getIntent().getStringExtra(keyEmail);
        if (Check.isEmptyString(address)) return null;
        Email email = new Email();
        email.setAddress(address);
        try {
            email.setType(extraContactDataType(keyType));
            email.setTypeLabel(null);
        }
        catch (NullPointerException ignore) {
            email.setType(ContactsContract.CommonDataKinds.Email.TYPE_HOME);
        }
        catch (NumberFormatException ex) {
            email.setType(TYPE_CUSTOM);
            email.setTypeLabel(extraContactDataTypeLabel(keyType));
        }
        return email;
    }

    private String extraContactDataTypeLabel(String key) {
        return getIntent().getStringExtra(key);
    }
    private int extraContactDataType(String key) {
        String val = getIntent().getStringExtra(key);
        return Integer.parseInt(val);
    }
}