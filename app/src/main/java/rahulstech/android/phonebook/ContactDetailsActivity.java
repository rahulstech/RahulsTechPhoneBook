package rahulstech.android.phonebook;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;
import rahulstech.android.phonebook.concurrent.AsyncTask;
import rahulstech.android.phonebook.model.ContactDetails;
import rahulstech.android.phonebook.model.Email;
import rahulstech.android.phonebook.model.Event;
import rahulstech.android.phonebook.model.Note;
import rahulstech.android.phonebook.model.Organization;
import rahulstech.android.phonebook.model.PhoneNumber;
import rahulstech.android.phonebook.model.PostalAddress;
import rahulstech.android.phonebook.model.Relation;
import rahulstech.android.phonebook.model.Website;
import rahulstech.android.phonebook.repository.ContactRepository;
import rahulstech.android.phonebook.util.Check;
import rahulstech.android.phonebook.util.DateTimeUtil;
import rahulstech.android.phonebook.util.OpenActivity;
import rahulstech.android.phonebook.viewmodel.ContactViewModel;

public class ContactDetailsActivity extends PhoneBookActivity {

    // TODO: viewing custom data types not implemented
    // like whatsapp and telegram account linked to account

    // TODO: contact loading delay: either show loading view or load contact data separately

    // TODO: layout improvement required

    // TODO: unimplemented clicks

    private static final String TAG = "ContactDetailsActivity";

    ImageView contactPhoto;
    CheckBox contactStar;
    TextView contactName;

    Toolbar toolbar;

    ContactViewModel vm;

    ContactDetails details = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_details);

        vm = ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication()).create(ContactViewModel.class);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        contactPhoto = findViewById(R.id.contact_photo);
        contactStar = findViewById(R.id.contact_star);
        contactStar.setOnClickListener(v->{
            final boolean checked = contactStar.isChecked();
            if (null != details){
                details.getContact();
                AsyncTask.execute(()->ContactRepository.get(this).getContactRepositoryOperation().setContactStarred(details.getContact(),checked),
                        new AsyncTask.AsyncTaskCallback(){
                            @Override
                            public void onError(AsyncTask task) {
                                Log.e(TAG,null,task.getError());
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
        });
        contactName = findViewById(R.id.display_name);

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
        if (id == R.id.delete) {
            onDeleteContact();
        }
        else if (id == R.id.edit) {
            onEditContact();
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

    void onDeleteContact() {
        // TODO: show delete confirmation dialog
    }

    void onEditContact() {
        if (!checkContactLoaded()) return;
        OpenActivity.editContact(this,details.getContentUri());
    }

    void loadContact() {
        Uri data = getIntent().getData();
        if (null == data) finish();
        AsyncTask.execute(()-> ContactRepository.get(this).getContactRepositoryOperation().findContactDetails(data),new AsyncTask.AsyncTaskCallback(){
            @Override
            public void onError(AsyncTask task) {
                Log.e(TAG,null,task.getError());
                finish();
            }

            @Override
            public void onResult(AsyncTask task) {
                onContactLoaded(task.getResult());
            }
        });
    }

    void onContactLoaded(@Nullable ContactDetails details) {
        this.details = details;
        if (null == details) finish();
        Glide.with(this).load(details.getPhotoUri())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(getRoundedTextDrawable(contactPhoto,details.getDisplayName()))
                .into(contactPhoto);
        contactStar.setChecked(details.getContact().isStarred());
        if (null == contactName) {
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            setTitle(details.getDisplayName());
        }
        else {
            contactName.setText(details.getDisplayName());
        }

        prepareSectionPhoneNumber(details.getPhoneNumbers());

        prepareSectionEmail(details.getEmails());

        prepareSectionEvent(details.getEvents());

        prepareSectionRelative(details.getRelatives());

        prepareSectionAddress(details.getAddresses());

        prepareSectionOrganization(details.getOrganizations());

        prepareSectionWebsite(details.getWebsites());

        prepareSectionNote(details.getNote());
    }

    private Drawable getRoundedTextDrawable(@NonNull View view, String text) {
        int radius = view.getMeasuredWidth()/2;
        int color = ColorGenerator.MATERIAL.getRandomColor();
        String label = text.substring(0,1);
        return TextDrawable.builder().buildRoundRect(label,color,radius);
    }

    ///////////////////////////////////////////////////////////////////////////////
    ///                         Section Phone Number                           ///
    /////////////////////////////////////////////////////////////////////////////

    View sectionNumber;
    GridLayout phoneNumbersList;

    void initSectionPhoneNumber() {
        sectionNumber = findViewById(R.id.section_phone_number);
        ImageView icon = sectionNumber.findViewById(R.id.section_icon);
        icon.setImageDrawable(VectorDrawableCompat.create(getResources(),R.drawable.ic_baseline_phone,getTheme()));
        phoneNumbersList = sectionNumber.findViewById(R.id.grid);
        phoneNumbersList.setColumnCount(1);
    }

    void prepareSectionPhoneNumber(@Nullable List<PhoneNumber> numbers) {
        sectionNumber.setVisibility(View.GONE);
        phoneNumbersList.removeAllViews();
        if (null == numbers || numbers.isEmpty()) return;
        for (PhoneNumber number : numbers) {
            View view = getLayoutInflater().inflate(R.layout.contact_details_phone_number,phoneNumbersList,false);
            TextView primary = view.findViewById(R.id.text_primary);
            TextView secondary = view.findViewById(R.id.text_secondary);
            primary.setText(number.getNumber());
            secondary.setText(number.getTypeLabel(getResources()));
            view.setOnClickListener(v->onClickNumber(number));
            view.setOnLongClickListener(v->onLongClickNumber(number));
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

    private boolean onLongClickNumber(@NonNull PhoneNumber number) {
        return false;
    }

    void makeVoiceCall(String number) {
        if (hasCallPermission()) {
            OpenActivity.makeVoiceCall(this,number);
        }
        else {
            vm.addHaltedTask(()->makeVoiceCall(number));
            requestCallPermission();
        }
    }

    ///////////////////////////////////////////////////////////////////////////////
    ///                           Section Email                                ///
    /////////////////////////////////////////////////////////////////////////////

    View sectionEmail;
    GridLayout emailsList;

    void initSectionEmail() {
        sectionEmail = findViewById(R.id.section_email);
        ImageView icon = sectionEmail.findViewById(R.id.section_icon);
        icon.setImageDrawable(VectorDrawableCompat.create(getResources(),R.drawable.ic_baseline_alternate_email,getTheme()));
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
            view.setOnLongClickListener(v->onLongClickEmail(email));
            emailsList.addView(view);
        }
        sectionEmail.setVisibility(View.VISIBLE);
    }

    private void onCLickEmail(@NonNull Email email) {
        OpenActivity.sendEmail(ContactDetailsActivity.this,email.getAddress());
    }

    private boolean onLongClickEmail(@NonNull Email email) {
        return false;
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
        icon.setImageDrawable(VectorDrawableCompat.create(getResources(),R.drawable.ic_baseline_calendar_month,getTheme()));
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
        relativesList.setColumnCount(getResources().getInteger(R.integer.contact_details_grid_item_count));
        ImageView icon = sectionRelation.findViewById(R.id.section_icon);
        icon.setImageDrawable(VectorDrawableCompat.create(getResources(),R.drawable.ic_baseline_relation,getTheme()));
    }

    void prepareSectionRelative(@Nullable List<Relation> relations) {
        sectionRelation.setVisibility(View.GONE);
        relativesList.removeAllViews();
        if (null == relations || relations.isEmpty()) return;
        for (Relation relation : relations) {
            View view = getLayoutInflater().inflate(R.layout.contact_details_gird_item_image_two_lines_text,relativesList,false);
            ImageView photo = view.findViewById(R.id.imageview);
            TextView primary = view.findViewById(R.id.text_primary);
            TextView secondary = view.findViewById(R.id.text_secondary);
            Glide.with(photo).load(relation.getPhotoUri())
                    .placeholder(getRoundedTextDrawable(photo,relation.getDisplayName()))
                    .into(photo);
            primary.setText(relation.getDisplayName());
            secondary.setVisibility(View.VISIBLE);
            secondary.setText(relation.getTypeLabel(getResources()));
            view.setOnClickListener(v->onClickRelation(relation));
            relativesList.addView(view);
        }
        sectionRelation.setVisibility(View.VISIBLE);
    }

    private void onClickRelation(@NonNull Relation relation) {
        if (null != relation.getRelationContactUri())
            OpenActivity.viewContactDetails(ContactDetailsActivity.this,relation.getRelationContactUri());
    }

    ///////////////////////////////////////////////////////////////////////////////
    ///                           Section Addresses                            ///
    /////////////////////////////////////////////////////////////////////////////

    View sectionAddress;
    GridLayout addressesList;

    void initSectionAddress() {
        sectionAddress = findViewById(R.id.section_address);
        ImageView icon = sectionAddress.findViewById(R.id.section_icon);
        icon.setImageDrawable(VectorDrawableCompat.create(getResources(),R.drawable.ic_baseline_location,getTheme()));
        addressesList = sectionAddress.findViewById(R.id.grid);
        addressesList.setColumnCount(getResources().getInteger(R.integer.contact_details_grid_item_count));
    }

    void prepareSectionAddress(@Nullable List<PostalAddress> addresses) {
        sectionAddress.setVisibility(View.GONE);
        addressesList.removeAllViews();
        if (null == addresses || addresses.isEmpty()) return;
        for (PostalAddress address : addresses) {
            // TODO: initialize address views and set listeners
        }
        sectionAddress.setVisibility(View.VISIBLE);
    }

    private void onClickAddress(@NonNull PostalAddress address) {}

    ///////////////////////////////////////////////////////////////////////////////
    ///                         Section Organization                           ///
    /////////////////////////////////////////////////////////////////////////////

    View sectionOrganization;
    GridLayout organizationsList;

    void initSectionOrganization() {
        sectionOrganization = findViewById(R.id.section_organization);
        ImageView icon = sectionOrganization.findViewById(R.id.section_icon);
        icon.setImageDrawable(VectorDrawableCompat.create(getResources(),R.drawable.ic_baseline_organization,getTheme()));
        organizationsList = sectionOrganization.findViewById(R.id.grid);
        organizationsList.setColumnCount(getResources().getInteger(R.integer.contact_details_grid_item_count));
    }

    void prepareSectionOrganization(@Nullable List<Organization> organizations) {
        organizationsList.setVisibility(View.GONE);
        if (null == organizations || organizations.isEmpty()) return;
        for (Organization org : organizations) {
            // TODO: initialize organization views and set listeners
        }
        organizationsList.setVisibility(View.VISIBLE);
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
        icon.setImageDrawable(VectorDrawableCompat.create(getResources(),R.drawable.ic_baseline_http,getTheme()));
    }

    void prepareSectionWebsite(@Nullable List<Website> websites) {
        websitesList.setVisibility(View.GONE);
        if (null == websites || websites.isEmpty()) return;
        for (Website site : websites) {
            View view = getLayoutInflater().inflate(android.R.layout.simple_list_item_1,websitesList,true);
            TextView primary = view.findViewById(android.R.id.text1);
            primary.setText(site.getUrl());
            view.setOnClickListener(v->onClickWebsite(site));
        }
        websitesList.setVisibility(View.VISIBLE);
    }

    private void onClickWebsite(@NonNull Website website) {}

    ///////////////////////////////////////////////////////////////////////////////
    ///                           Section Note                                 ///
    /////////////////////////////////////////////////////////////////////////////

    View sectionNote;
    TextView note;

    void initSectionNote() {
        sectionNote = findViewById(R.id.section_note);
        note = sectionNote.findViewById(R.id.textview);
    }

    void prepareSectionNote(Note data) {
        boolean hasNote = null != data && !Check.isEmptyString(data.getNote());
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

    boolean checkContactLoaded() {
        if (null == details) {
            Toast.makeText(this,R.string.message_contact_not_loaded,Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
}