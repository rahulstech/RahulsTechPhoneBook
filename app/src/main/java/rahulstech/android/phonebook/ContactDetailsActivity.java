package rahulstech.android.phonebook;

import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.ImageViewCompat;
import rahulstech.android.phonebook.concurrent.AsyncTask;
import rahulstech.android.phonebook.model.ContactDetails;
import rahulstech.android.phonebook.model.Email;
import rahulstech.android.phonebook.model.Event;
import rahulstech.android.phonebook.model.Name;
import rahulstech.android.phonebook.model.Note;
import rahulstech.android.phonebook.model.Organization;
import rahulstech.android.phonebook.model.PhoneNumber;
import rahulstech.android.phonebook.model.PostalAddress;
import rahulstech.android.phonebook.model.Relation;
import rahulstech.android.phonebook.model.Website;
import rahulstech.android.phonebook.util.ContactSorting;
import rahulstech.android.phonebook.util.DateTimeUtil;
import rahulstech.android.phonebook.util.DrawableUtil;
import rahulstech.android.phonebook.util.OpenActivity;
import rahulstech.android.phonebook.util.Settings;
import rahulstech.android.phonebook.view.ListPopups;
import rahulstech.android.phonebook.viewmodel.ContactViewModel;

import static rahulstech.android.phonebook.util.Check.isEmptyString;
import static rahulstech.android.phonebook.util.DrawableUtil.vectorDrawable;
import static rahulstech.android.phonebook.util.Helpers.createContactPhotoPlaceholder;

public class ContactDetailsActivity extends PhoneBookActivity {

    // TODO: viewing custom data types not implemented
    // like whatsapp and telegram account linked to account

    // TODO: contact loading delay: either show loading view or load contact data separately

    // TODO: layout improvement required

    // TODO: unimplemented clicks

    private static final String TAG = "ContactDetailsActivity";

    private ImageView contactPhoto;
    private CheckBox contactStar;
    private TextView contactName;
    private Toolbar toolbar;

    private ContactViewModel vm;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_details);

        vm = getOrCreateViewModel(ContactViewModel.class);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        contactPhoto = findViewById(R.id.contact_photo);
        contactStar = findViewById(R.id.contact_star);
        contactStar.setOnClickListener(v-> onClickStar());
        contactName = findViewById(R.id.display_name);

        initSectionName();
        initSectionPhoneNumber();
        initSectionEmail();
        initSectionEvents();
        initSectionRelatives();
        initSectionAddress();
        initSectionOrganization();
        initSectionWebsite();
        initSectionNote();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.contact_details_top_menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            super.onBackPressed();
        }
        else if (id == R.id.delete) {
            onDeleteContact(vm.getContactDetails());
            return true;
        }
        else if (id == R.id.edit) {
            onEditContact(vm.getContactDetails());
            return true;
        }
        else if (id == R.id.share) {
            onShareContact(vm.getContactDetails());
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAllPermissionsGranted(int requestCode) {
        super.onAllPermissionsGranted(requestCode);
        if (CONTACT_PERMISSION_CODE == requestCode) {
            loadContact();
        }
        else {
            if (vm.hasAnyHaltedTask()) {
                vm.getHaltedTask().run();
                vm.removeHaltedTask();
            }
        }
    }

    private void onClickStar() {
        final boolean checked = contactStar.isChecked();
        ContactDetails details = vm.getContactDetails();
        if (null != details){
            details.getContact();
            AsyncTask.execute(()->vm.getRepository().setContactStarred(details.getContact(),checked),
                    new AsyncTask.AsyncTaskCallback(){
                        @Override
                        public void onError(AsyncTask task) {
                            Log.e(TAG,"contact star error",task.getError());
                            contactStar.setChecked(!checked);
                        }

                        @Override
                        public void onResult(AsyncTask task) {
                            boolean saved = task.getResult();
                            if (!saved) {
                                contactStar.setChecked(!checked);
                            }
                        }
                    });
        }
    }

    private void onDeleteContact(@Nullable ContactDetails details) {
        if (null == details) return;
        String message = getResources().getQuantityString(R.plurals.message_warning_delete_contact,1);
        new AlertDialog.Builder(this)
                .setPositiveButton(R.string.label_cancel,null)
                .setNegativeButton(R.string.label_delete,(di,which)->{
                    vm.removeContact(details,new AsyncTask.AsyncTaskCallback(){
                        @Override
                        public void onError(AsyncTask task) {
                            Log.e(TAG,null,task.getError());
                        }

                        @Override
                        public void onResult(AsyncTask task) {
                            boolean removed = task.getResult();
                            if (removed) finish();
                            else Toast.makeText(ContactDetailsActivity.this,R.string.message_error,Toast.LENGTH_SHORT).show();;
                        }
                    });
                })
                .setMessage(message)
                .show();
    }

    private void onEditContact(@Nullable ContactDetails details) {
        if (null == details) return;
        OpenActivity.editContact(this,details.getContentUri());
    }

    private void onShareContact(@Nullable ContactDetails details) {
        // TODO: show chooser for share option like via sms or vcard
        //  and chooser for selecting what to send
        if (null == details) return;
        List<PhoneNumber> numbers = details.getPhoneNumbers();
        String displayName = details.getDisplayName(ContactSorting.FIRSTNAME_FIRST);
        if (numbers == null || numbers.isEmpty()) return;
        StringBuilder body = new StringBuilder(displayName);
        for (PhoneNumber number : numbers) {
            body.append("\n").append(number.getTypeLabel(getResources())).append(" ")
                    .append(number.getNumber());
        }
        OpenActivity.sharePlainText(this,body.toString());
    }
    private void loadContact() {
        AsyncTask.execute(()->vm.getRepository().getContactDetails(getIntent().getData(),null),new AsyncTask.AsyncTaskCallback(){
            @Override
            public void onError(AsyncTask task) {
                Log.e(TAG,"",task.getError());
                finish();
            }

            @Override
            public void onResult(AsyncTask task) {
                ContactDetails details = task.getResult();
                Log.i(TAG,"details found: "+(null != details));
                onContactLoaded(details);
            }
        });
    }

    private void onContactLoaded(@Nullable ContactDetails details) {
        vm.setContactDetails(details);
        if (null == details) {
            Toast.makeText(this,R.string.message_contact_not_found,Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        ContactSorting sorting = getContactSorting();
        Glide.with(this).load(details.getPhotoUri())
                .placeholder(createContactPhotoPlaceholder(details,sorting,contactPhoto,
                        vectorDrawable(this, R.drawable.placeholder_contact_photo)))
                .into(contactPhoto);
        contactStar.setChecked(details.getContact().isStarred());
        String displayName = details.getDisplayName(sorting);
        contactName.setText(displayName);
        prepareSectionName(details.getName());
        prepareSectionPhoneNumber(details.getPhoneNumbers());
        prepareSectionEmail(details.getEmails());
        prepareSectionEvent(details.getEvents());
        prepareSectionRelative(details.getRelations());
        prepareSectionAddress(details.getAddresses());
        prepareSectionOrganization(details.getOrganization());
        prepareSectionWebsite(details.getWebsites());
        prepareSectionNote(details.getNote());
    }

    private ContactSorting getContactSorting() {
        return Settings.getInstance(this).getContactSorting();
    }

    ///////////////////////////////////////////////////////////////////////////////
    ///                         Section Name                                   ///
    /////////////////////////////////////////////////////////////////////////////

    private View sectionName;
    private GridLayout namesList;

    private void initSectionName() {
        sectionName = findViewById(R.id.section_name);
        namesList = sectionName.findViewById(R.id.grid);
        namesList.setColumnCount(1);
        ImageView icon = sectionName.findViewById(R.id.section_icon);
        icon.setImageDrawable(vectorDrawable(this,R.drawable.ic_baseline_person));
    }

    private void prepareSectionName(@Nullable Name name) {
        sectionName.setVisibility(View.GONE);
        namesList.removeAllViews();
        if (null == name) return;
        String nickname = name.getNickname();
        String phoneticName = name.buildPhoneticName();
        logDebug(TAG,"name="+name);
        boolean visibleSection = false;
        if (!isEmptyString(phoneticName)){
            View view = getLayoutInflater().inflate(R.layout.list_item_two_lines,namesList,true);
            TextView primary = view.findViewById(R.id.text_primary);
            TextView secondary = view.findViewById(R.id.text_secondary);
            primary.setText(phoneticName);
            secondary.setText(R.string.label_phonetic_name);
            visibleSection = true;
        }
        if (!isEmptyString(nickname)){
            View view = getLayoutInflater().inflate(R.layout.list_item_two_lines,namesList,true);
            TextView primary = view.findViewById(R.id.text_primary);
            TextView secondary = view.findViewById(R.id.text_secondary);
            primary.setText(nickname);
            secondary.setText(R.string.label_nickname);
            visibleSection = true;
        }
        if (visibleSection)
            sectionName.setVisibility(View.VISIBLE);
    }



    ///////////////////////////////////////////////////////////////////////////////
    ///                         Section Phone Number                           ///
    /////////////////////////////////////////////////////////////////////////////

    View sectionNumber;
    GridLayout phoneNumbersList;

    void initSectionPhoneNumber() {
        sectionNumber = findViewById(R.id.section_phone_number);
        ImageView icon = sectionNumber.findViewById(R.id.section_icon);
        icon.setImageDrawable(DrawableUtil.vectorDrawable(this,R.drawable.ic_baseline_phone));
        phoneNumbersList = sectionNumber.findViewById(R.id.grid);
        phoneNumbersList.setColumnCount(1);
    }

    void prepareSectionPhoneNumber(@Nullable List<PhoneNumber> numbers) {
        sectionNumber.setVisibility(View.GONE);
        phoneNumbersList.removeAllViews();
        if (null == numbers || numbers.isEmpty()) return;
        Collections.sort(numbers,(n1,n2)->n1.isPrimary() ? -1 : 1);
        for (PhoneNumber number : numbers) {
            View view = getLayoutInflater().inflate(R.layout.contact_details_phone_number,phoneNumbersList,false);
            CheckedTextView primary = view.findViewById(R.id.text_primary);
            TextView secondary = view.findViewById(R.id.text_secondary);
            primary.setText(number.getNumber());
            primary.setChecked(number.isPrimary());
            secondary.setText(number.getTypeLabel(getResources()));
            view.setOnClickListener(v->onClickNumber(number));
            view.setOnLongClickListener(v->onLongClickNumber(v,number));
            view.findViewById(R.id.action_sms).setOnClickListener(v->onClickSms(number));

            phoneNumbersList.addView(view);
        }
        sectionNumber.setVisibility(View.VISIBLE);
    }

    private void onClickNumber(@NonNull PhoneNumber number) {
        makeVoiceCall(number.getNumber());
    }

    private void onClickSms(@NonNull PhoneNumber number) {
        OpenActivity.sendSms(this,number.getNumber());
    }

    private boolean onLongClickNumber(@NonNull View view, @NonNull PhoneNumber number) {
        String[] items = new String[2];
        items[0] = getString(R.string.label_copy);
        items[1] = !number.isPrimary() ? getString(R.string.label_set_default) : getString(R.string.label_unset_default);

        ListPopups.showContextMenu(this,view,number.getNumber(),items,(menu,which)->{
            if (0==which) {
                copyToClipBoard(number.getNumber());
            }
            else if (1==which) {
                changePhoneNumberPrimary(number);
            }
        });
        return true;
    }

    private void makeVoiceCall(String number) {
        if (hasCallPermission()) {
            OpenActivity.makeVoiceCall(this,number);
        }
        else {
            vm.addHaltedTask(()->makeVoiceCall(number));
            requestCallPermission();
        }
    }

    private void changePhoneNumberPrimary(PhoneNumber number) {
        AsyncTask.execute(()->vm.getRepository().changePhoneNumberPrimary(number,!number.isPrimary()),new AsyncTask.AsyncTaskCallback(){
            @Override
            public void onError(AsyncTask task) {
                Log.e(TAG,"",task.getError());
            }

            @Override
            public void onResult(AsyncTask task) {
                if (task.getResult()) loadContact();
            }
        });
    }

    ///////////////////////////////////////////////////////////////////////////////
    ///                           Section Email                                ///
    /////////////////////////////////////////////////////////////////////////////

    ViewGroup sectionEmail;
    GridLayout emailsList;

    void initSectionEmail() {
        sectionEmail = findViewById(R.id.section_email);
        ImageView icon = sectionEmail.findViewById(R.id.section_icon);
        icon.setImageDrawable(DrawableUtil.vectorDrawable(this,R.drawable.ic_baseline_email));
        emailsList = sectionEmail.findViewById(R.id.grid);
        emailsList.setColumnCount(1);
    }

    void prepareSectionEmail(@Nullable List<Email> emails) {
        sectionEmail.setVisibility(View.GONE);
        emailsList.removeAllViews();
        if (null == emails || emails.isEmpty()) return;
        for (Email email : emails) {
            View view = getLayoutInflater().inflate(R.layout.list_item_two_lines,emailsList,false);
            TextView primary = view.findViewById(R.id.text_primary);
            TextView secondary = view.findViewById(R.id.text_secondary);
            primary.setText(email.getAddress());
            secondary.setText(email.getTypeLabel(getResources()));
            view.setOnClickListener(v->onCLickEmail(email));
            view.setOnLongClickListener(v->onLongClickEmail(v,email));
            emailsList.addView(view);
        }
        sectionEmail.setVisibility(View.VISIBLE);
    }

    private void onCLickEmail(@NonNull Email email) {
        OpenActivity.sendEmail(ContactDetailsActivity.this,email.getAddress());
    }

    private boolean onLongClickEmail(@NonNull View view, @NonNull Email email) {
        String[] items = new String[]{getString(R.string.label_copy)};
        ListPopups.showContextMenu(this,view,email.getAddress(),items,(menu,which)->{
            if (0==which) {
                copyToClipBoard(email.getAddress());
            }
        });
        return true;
    }

    ///////////////////////////////////////////////////////////////////////////////
    ///                           Section Event                                ///
    /////////////////////////////////////////////////////////////////////////////

    View sectionEvent;
    GridLayout eventsList;

    void initSectionEvents() {
        sectionEvent = findViewById(R.id.section_event);
        eventsList = sectionEvent.findViewById(R.id.grid);
        eventsList.setColumnCount(1);
        ImageView icon = sectionEvent.findViewById(R.id.section_icon);
        icon.setImageDrawable(DrawableUtil.vectorDrawable(this,R.drawable.ic_baseline_calendar_month));
    }

    void prepareSectionEvent(@Nullable List<Event> events) {
        sectionEvent.setVisibility(View.GONE);
        eventsList.removeAllViews();
        if (null == events || events.isEmpty()) return;
        for (Event event : events) {
            View view = getLayoutInflater().inflate(R.layout.list_item_two_lines,eventsList,false);
            TextView primary = view.findViewById(R.id.text_primary);
            TextView secondary = view.findViewById(R.id.text_secondary);
            primary.setText(DateTimeUtil.formatContactEventStartDate(event.getStartDate(),"d-MMM-yy","MMM dd"));
            secondary.setText(event.getTypeLabel(getResources()));
            view.setOnClickListener(v->onClickEvent(event));
            eventsList.addView(view);
        }
        sectionEvent.setVisibility(View.VISIBLE);
    }

    private void onClickEvent(@NonNull Event event) {
        OpenActivity.viewCalender(ContactDetailsActivity.this, DateTimeUtil.inMillis(event.getStartDate()));
    }

    ///////////////////////////////////////////////////////////////////////////////
    ///                           Section Relative                             ///
    /////////////////////////////////////////////////////////////////////////////

    View sectionRelation;
    GridLayout relativesList;

    void initSectionRelatives() {
        sectionRelation = findViewById(R.id.section_relation);
        relativesList = sectionRelation.findViewById(R.id.grid);
        relativesList.setColumnCount(1);
        ImageView icon = sectionRelation.findViewById(R.id.section_icon);
        icon.setImageDrawable(DrawableUtil.vectorDrawable(this,R.drawable.ic_baseline_relation));
    }

    void prepareSectionRelative(@Nullable List<Relation> relations) {
        sectionRelation.setVisibility(View.GONE);
        relativesList.removeAllViews();
        if (null == relations || relations.isEmpty()) return;
        for (Relation relation : relations) {
            View view = getLayoutInflater().inflate(R.layout.list_item_two_lines,relativesList,false);
            TextView primary = view.findViewById(R.id.text_primary);
            TextView secondary = view.findViewById(R.id.text_secondary);
            primary.setText(relation.getDisplayName());
            secondary.setVisibility(View.VISIBLE);
            secondary.setText(relation.getTypeLabel(getResources()));
            view.setOnClickListener(v->onClickRelation(relation));
            relativesList.addView(view);
        }
        sectionRelation.setVisibility(View.VISIBLE);
    }

    private void onClickRelation(@NonNull Relation relation) {
        // TODO: pick contact by name if multiple contacts available
        //  or show contact details if single contact exists
    }

    ///////////////////////////////////////////////////////////////////////////////
    ///                           Section Addresses                            ///
    /////////////////////////////////////////////////////////////////////////////

    View sectionAddress;
    GridLayout addressesList;

    void initSectionAddress() {
        sectionAddress = findViewById(R.id.section_address);
        ImageView icon = sectionAddress.findViewById(R.id.section_icon);
        icon.setImageDrawable(DrawableUtil.vectorDrawable(this,R.drawable.ic_baseline_location));
        addressesList = sectionAddress.findViewById(R.id.grid);
        addressesList.setColumnCount(getResources().getInteger(R.integer.contact_details_grid_item_count));
    }

    void prepareSectionAddress(@Nullable List<PostalAddress> addresses) {
        sectionAddress.setVisibility(View.GONE);
        addressesList.removeAllViews();
        if (null == addresses || addresses.isEmpty()) return;
        for (PostalAddress address : addresses) {
            View view = getLayoutInflater().inflate(R.layout.contact_details_gird_item_image_two_lines_text,addressesList,false);
            ImageView icon = view.findViewById(R.id.imageview);
            TextView primary = view.findViewById(R.id.text_primary);
            icon.setImageDrawable(DrawableUtil.vectorDrawable(this,R.drawable.ic_baseline_location));
            ImageViewCompat.setImageTintList(icon, ColorStateList.valueOf(getResources().getColor(R.color.color_danger)));
            primary.setText(address.getTypeLabel(getResources()));
            view.setOnClickListener(v->onClickAddress(address));
            addressesList.addView(view);
        }
        sectionAddress.setVisibility(View.VISIBLE);
    }

    private void onClickAddress(@NonNull PostalAddress address) {
        new AlertDialog.Builder(this)
                .setPositiveButton(android.R.string.ok,null)
                .setNegativeButton(android.R.string.search_go,(di,which)->
                        OpenActivity.openMap(ContactDetailsActivity.this,address.getFormattedAddress()))
                .setNeutralButton(android.R.string.copy,(di,which)->{
                    copyToClipBoard(address.getFormattedAddress());
                })
                .setMessage(address.getFormattedAddress())
                .show();
    }



    ///////////////////////////////////////////////////////////////////////////////
    ///                         Section Organization                           ///
    /////////////////////////////////////////////////////////////////////////////

    private View sectionOrganization;
    private TextView organization;

    void initSectionOrganization() {
        sectionOrganization = findViewById(R.id.section_organization);
        organization = sectionOrganization.findViewById(R.id.organization);
    }

    void prepareSectionOrganization(@Nullable Organization org) {
        sectionOrganization.setVisibility(View.GONE);
        organization.setText(null);
        if (null == org) return;
        organization.setText(org.buildDisplayText());
        organization.setOnClickListener(v->onClickOrganization(org));
        organization.setOnLongClickListener(v->onLongClickOrganization(v,org));
        sectionOrganization.setVisibility(View.VISIBLE);
    }

    private void onClickOrganization(@NonNull Organization org) {
        OpenActivity.searchWeb(this,org.buildDisplayText());
    }

    private boolean onLongClickOrganization(@NonNull View view, @NonNull Organization org) {
        String[] items = new String[]{getString(R.string.label_copy)};
        ListPopups.showContextMenu(this,view,org.buildDisplayText(),items,(menu,which)->{
            if (0==which){
                copyToClipBoard(org.buildDisplayText());
            }
        });
        return true;
    }

    ///////////////////////////////////////////////////////////////////////////////
    ///                           Section Website                              ///
    /////////////////////////////////////////////////////////////////////////////

    View sectionWebsite;
    GridLayout websitesList;

    void initSectionWebsite() {
        sectionWebsite = findViewById(R.id.section_website);
        websitesList = sectionWebsite.findViewById(R.id.grid);
        websitesList.setColumnCount(1);
        ImageView icon = sectionWebsite.findViewById(R.id.section_icon);
        icon.setImageDrawable(DrawableUtil.vectorDrawable(this,R.drawable.ic_baseline_website));
    }

    void prepareSectionWebsite(@Nullable List<Website> websites) {
        sectionWebsite.setVisibility(View.GONE);
        websitesList.removeAllViews();
        if (null == websites || websites.isEmpty()) return;
        for (Website site : websites) {
            View view = getLayoutInflater().inflate(R.layout.list_item_two_lines,websitesList,false);
            view.findViewById(R.id.text_secondary).setVisibility(View.GONE);
            TextView secondary = view.findViewById(R.id.text_secondary);
            secondary.setVisibility(View.GONE);
            TextView primary = view.findViewById(R.id.text_primary);
            primary.setText(site.getUrl());
            view.setOnClickListener(v->onClickWebsite(site));
            websitesList.addView(view);
        }
        sectionWebsite.setVisibility(View.VISIBLE);
    }

    private void onClickWebsite(@NonNull Website website) {
        OpenActivity.viewWebsite(this,website.getUrl());
    }

    ///////////////////////////////////////////////////////////////////////////////
    ///                           Section Note                                 ///
    /////////////////////////////////////////////////////////////////////////////

    View sectionNote;
    TextView note;

    void initSectionNote() {
        sectionNote = findViewById(R.id.section_note);
        note = sectionNote.findViewById(R.id.note);
    }

    void prepareSectionNote(Note data) {
        boolean hasNote = null != data && !isEmptyString(data.getNote());
        if (hasNote) {
            note.setText(data.getNote());
            sectionNote.setVisibility(View.VISIBLE);
        }
        else {
            sectionNote.setVisibility(View.GONE);
            note.setText(null);
        }
    }

    ///////////////////////////////////////////////////////////////////////////////
    ///                              Others                                    ///
    /////////////////////////////////////////////////////////////////////////////



    private void copyToClipBoard(String text) {
        if (isEmptyString(text)) return;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager manager = (android.text.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            manager.setText(text);
        }
        else {
            android.content.ClipboardManager manager = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            manager.setPrimaryClip(android.content.ClipData.newPlainText("Address",text));
        }
        Toast.makeText(this, R.string.message_copied_to_clipboard,Toast.LENGTH_SHORT).show();
    }

    private static final int CONTEXT_MENU_NUMBER = 1;

    private static final int CONTEXT_MENU_EMAIL = 2;

    private static final int CONTEXT_MENU_EVENT = 3;

    private static final int ITEM_COPY = 1;

    private static final int ITEM_SET_UNSET_PRIMARY = 2;
}