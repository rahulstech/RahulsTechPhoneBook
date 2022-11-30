package rahulstech.android.phonebook;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import rahulstech.android.phonebook.concurrent.AsyncTask;
import rahulstech.android.phonebook.model.ContactAccount;
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
import rahulstech.android.phonebook.view.AccountAdapter;
import rahulstech.android.phonebook.view.ContactInputItemManager;
import rahulstech.android.phonebook.viewmodel.ContactViewModel;

import static rahulstech.android.phonebook.util.Helpers.anyNonEmpty;

public class ContactInputActivity extends PhoneBookActivity {
    // TODO: ContactInputActivity not implemented
    // TODO: custom label input not working properly,

    private static final String TAG = "ContactInput";

    private static final String KEY_CONTACT_FETCHED = "contact_fetched";
    private static final String KEY_VIEW_STRUCTURED_NAME = "view_structured_name";
    private static final String KEY_VIEW_MORE = "view_more";
    private static final String KEY_NUMBERS = "numbers";
    private static final String KEY_EMAILS = "emails";
    private static final String KEY_EVENTS = "events";
    private static final String KEY_ADDRESS = "address";
    private static final String KEY_RELATIONS = "relations";
    private static final String KEY_ORGANIZATION_ID = "org_id";
    private static final String KEY_SECTION_ORGANIZATION_VISIBLE = "section_org_visible";
    private static final String KEY_WEBSITES = "websites";
    private static final String KEY_SECTION_NOTE_VISIBLE = "section_note_visible";

    private ContactViewModel vm;

    private boolean mContactFetched = false;
    private long mContactNameId;
    private long mOrganizationId;
    private long mContactNoteId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_input);

        vm = getOrCreateViewModel(ContactViewModel.class);

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

        initialize(savedInstanceState);
    }

    private boolean isEditOperation() {
        return Intent.ACTION_EDIT.equals(getIntent().getAction());
    }

    private void initialize(@Nullable Bundle savedState) {
        if (isEditOperation()) {
            title.setText(R.string.label_edit_contact);
            mContactFetched = null == savedState ? false : savedState.getBoolean(KEY_CONTACT_FETCHED,false);
            boolean wasContactLoadingDone = mContactFetched && vm.hasContactDetails();
            logDebug(TAG,"wasContactLoadingDone "+wasContactLoadingDone);
            if (!wasContactLoadingDone){
                final Uri uri = getIntent().getData();
                loadContact(uri);
            }
            else {
                mContactFetched = true;
            }
        }
        else {
            title.setText(R.string.label_create_contact);
            // TODO: extract extras
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(KEY_CONTACT_FETCHED,mContactFetched);
        outState.putBoolean(KEY_VIEW_STRUCTURED_NAME,btnSwitchName.isChecked());
        outState.putBoolean(KEY_VIEW_MORE,btnViewMore.isChecked());
        
        onSaveSectionNumberState(outState);
        onSaveSectionEmailState(outState);
        onSaveSectionEventState(outState);
        onSaveSectionAddressState(outState);
        onSaveSectionRelationState(outState);
        onSaveSectionOrganizationState(outState);
        onSaveSectionWebsiteState(outState);
        onSaveSectionNoteState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedState) {
        super.onRestoreInstanceState(savedState);

        if (vm.hasContactDetails()) setIds(vm.getContactDetails());

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

    private void loadContact(@NonNull Uri uri) {
        AsyncTask.AsyncTaskCallback callback = new AsyncTask.AsyncTaskCallback(){
            @Override
            public void onError(AsyncTask task) {
                Log.e(TAG,null,task.getError());
                Toast.makeText(ContactInputActivity.this,R.string.message_contact_load_error,Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onResult(AsyncTask task) {
                ContactDetails details = task.getResult();
                if (null == details) {
                    Log.i(TAG,"no contact found for uri="+uri);
                    finish();
                }
                else {
                    onContactLoaded(details);
                }
            }
        };
        vm.loadContactDetails(uri,callback);
    }

    private void onContactLoaded(@NonNull ContactDetails details) {
        setIds(details);
        RawContact rawContact = details.getRawContact();
        int position = accountAdapter.getPositionByTypeAndName(rawContact.getType(),rawContact.getName());
        if (position >= 0) accounts.setSelection(position);
        accounts.setEnabled(false);
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

    private void setIds(@NonNull ContactDetails details) {
        mContactNameId = null != details.getName() ? details.getName().getId() : 0;
        mOrganizationId = null != details.getOrganization() ? details.getOrganization().getId() : 0;
        mContactNoteId = null != details.getNote() ? details.getNote().getId() : 0;
    }

    private void onCancelContactInput() {
        // TODO: must provide warning if not saved
        finish();
    }

    private void onSaveContactDetails() {
        ContactAccount account = (ContactAccount) accounts.getSelectedItem();
        RawContact rawContact = new RawContact(account.type,account.name);
        Name name = extractName();
        List<PhoneNumber> numbers = managerItemNumber.extractAllData();
        List<Email> emails = managerItemEmails.extractAllData();
        List<Event> events = managerItemEvents.extractAllData();
        List<PostalAddress> addresses = managerItemAddress.extractAllData();
        List<Relation> relations = managerItemRelation.extractAllData();
        Organization organization = extractOrganization();
        List<Website> websites = managerItemWebsite.extractAllData();
        Note note = extractNote();

        boolean hasAnythingToSave = (null != name && !Check.isEmptyString(name.getDisplayName())) ||
                (null != numbers && !numbers.isEmpty()) || (null != emails && !emails.isEmpty()) ||
                (null != events && !events.isEmpty()) || (null != relations && !relations.isEmpty()) ||
                (null != addresses && !addresses.isEmpty()) || (null != websites && !websites.isEmpty()) ||
                (null != organization && organization.hasValues()) ||
                (null != note && !Check.isEmptyString(note.getNote()));

        if (!hasAnythingToSave) {
            Toast.makeText(this,R.string.message_save_nothing,Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

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
            vm.updateContactDetails(vm.getContactDetails(),details,callback);
        }
        else {
            vm.insertContactDetails(details,callback);
        }
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
        btnSwitchName.setOnCheckedChangeListener((v,checked)-> setSectionContactNameBasic(checked));
    }

    private void onRestoreName(@NonNull Bundle savedState) {
        setSectionContactNameBasic(savedState.getBoolean(KEY_VIEW_STRUCTURED_NAME));
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
        boolean needExpand = !Check.isEmptyString(name.getPrefix()) || !Check.isEmptyString(name.getMiddleName())
                ||!Check.isEmptyString(name.getSuffix());
        btnSwitchName.setChecked(needExpand);
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
        // TODO: nickname
        String prefix = iPrefix.getText().toString();
        String middleName = iMiddleName.getText().toString();
        String suffix = iSuffix.getText().toString();
        String firstName = iDisplayFirstName.getText().toString();
        if (Check.isEmptyString(firstName)) firstName = iFirstName.getText().toString();
        String lastName = iDisplayLastName.getText().toString();
        if (Check.isEmptyString(lastName)) lastName = iLastName.getText().toString();
        String phFirstName = iPhFirstName.getText().toString();
        String phMiddleName = iPhMiddleName.getText().toString();
        String phLastName = iPhLastName.getText().toString();
        Name name = new Name();
        name.setId(mContactNameId);
        name.setPrefix(prefix);
        name.setGivenName(firstName);
        name.setMiddleName(middleName);
        name.setFamilyName(lastName);
        name.setSuffix(suffix);
        name.setPhoneticGivenName(phFirstName);
        name.setPhoneticMiddleName(phMiddleName);
        name.setPhoneticFamilyName(phLastName);
        name.buildDisplayName();
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
        sectionNumber.findViewById(R.id.action_add).setOnClickListener(v->managerItemNumber.addItemFirst(true).focus());
        managerItemNumber = ContactInputItemManager.forPhoneNumber(this,numbersList);
        managerItemNumber.addItemFirst(false);
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
        sectionEmail.findViewById(R.id.action_add).setOnClickListener(v->managerItemEmails.addItemFirst(true).focus());
        managerItemEmails = ContactInputItemManager.forEmail(this, emailsList);
        managerItemEmails.addItemFirst(false);
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
        sectionAddress.findViewById(R.id.action_add).setOnClickListener(v->managerItemAddress.addItemFirst(true).focus());
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
        sectionRelation.findViewById(R.id.action_add).setOnClickListener(v->managerItemRelation.addItemFirst(true).focus());
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
        outState.putLong(KEY_ORGANIZATION_ID,mOrganizationId);
        outState.putBoolean(KEY_SECTION_ORGANIZATION_VISIBLE,sectionOrganization.getVisibility()==View.VISIBLE);
    }

    private void onRestoreSectionOrganizationState(@NonNull Bundle savedState) {
        mOrganizationId = savedState.getLong(KEY_ORGANIZATION_ID,0);
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
        if (0==mOrganizationId && !anyNonEmpty(company,title,department)) return null;
        Organization org = new Organization();
        org.setId(mOrganizationId);
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
        sectionWebsite.findViewById(R.id.action_add).setOnClickListener(v->managerItemWebsite.addItemFirst(true).focus());
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
        Note note = new Note();
        note.setId(mContactNoteId);
        note.setNote(text);
        if (0==note.getId() && Check.isEmptyString(text)) return null;
        return note;
    }

    private void onClearNote() {
        editTextNote.setText(null);
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    ///                            Button View More                                    ///
    /////////////////////////////////////////////////////////////////////////////////////

    private TextView title;
    private Spinner accounts;
    private AccountAdapter accountAdapter;
    private CheckBox btnViewMore;

    private void onInitOther() {
        findViewById(R.id.button_cancel).setOnClickListener(v->onCancelContactInput());
        findViewById(R.id.button_save).setOnClickListener(v-> onSaveContactDetails());
        title = findViewById(R.id.title);
        accounts = findViewById(R.id.choose_account);
        btnViewMore = findViewById(R.id.button_view_more);

        if (isEditOperation()) accounts.setEnabled(false);
        accountAdapter = new AccountAdapter(this);
        accounts.setAdapter(accountAdapter);
        vm.getAccounts(new AsyncTask.AsyncTaskCallback(){
            @Override
            public void onError(AsyncTask task) {
                Log.e(TAG,"",task.getError());
            }

            @Override
            public void onResult(AsyncTask task) {
                accountAdapter.setAccounts(task.getResult());
            }
        });
        btnViewMore.setOnCheckedChangeListener((cb, checked)->onViewMore());
    }

    private void onRestoreOther(@NonNull Bundle savedState) {
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
}