package rahulstech.android.phonebook;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputLayout;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;
import rahulstech.android.phonebook.concurrent.AsyncTask;
import rahulstech.android.phonebook.model.ContactDetails;
import rahulstech.android.phonebook.model.Email;
import rahulstech.android.phonebook.model.Event;
import rahulstech.android.phonebook.model.Name;
import rahulstech.android.phonebook.model.Note;
import rahulstech.android.phonebook.model.PhoneNumber;
import rahulstech.android.phonebook.view.Animations;
import rahulstech.android.phonebook.util.Check;
import rahulstech.android.phonebook.util.DateTimeUtil;
import rahulstech.android.phonebook.view.AccountAdapter;
import rahulstech.android.phonebook.view.ContactDataTypeAdapter;
import rahulstech.android.phonebook.view.DatePickerDialog;
import rahulstech.android.phonebook.viewmodel.ContactViewModel;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.O;
import static rahulstech.android.phonebook.BuildConfig.DEBUG;

public class ContactInputActivity extends PhoneBookActivity {
    // TODO: ContactInputActivity not implemented
    // TODO: ui not scrolling while view more
    // TODO: custom label input not working properly,
    //  problems: select selected not showing dialog, dialog showing when selection via setSelection

    private static final String TAG = "ContactInput";

    private static final String KEY_CONTACT_FETCHED = "contact_fetched";
    private static final String KEY_CONTACT_ID = "contact_id";
    private static final String KEY_LOOKUP_KEY = "lookup_key";
    private static final String KEY_CONTACT_NAME_ID = "contact_name_id";
    private static final String KEY_CONTACT_NOTE_ID = "contact_note_id";
    private static final String KEY_VIEW_STRUCTURED_NAME = "view_structured_name";
    private static final String KEY_VIEW_MORE = "view_more";
    private static final String KEY_NUMBERS = "numbers";
    private static final String KEY_EMAILS = "emails";
    private static final String KEY_EVENTS = "events";

    private TextView title;
    private Spinner accounts;
    private AccountAdapter accountAdapter;

    private ContactViewModel vm;

    private boolean mContactFetched = false;
    private long mContactId;
    private String mLookupKey;
    private long mContactNameId;
    private long mContactNoteId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_input);

        vm = ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication()).create(ContactViewModel.class);

        findViewById(R.id.button_cancel).setOnClickListener(v->onCancelContactInput());
        findViewById(R.id.button_save).setOnClickListener(v->onSaveContact());
        title = findViewById(R.id.title);
        accounts = findViewById(R.id.choose_account);
        accountAdapter = new AccountAdapter(this);
        accounts.setAdapter(accountAdapter);

        onInitSectionName();

        onInitSectionNumber();

        onInitSectionEmail();

        onInitSectionEvent();

        onInitSectionNote();

        onInitViewMore();

        initialize(savedInstanceState);
    }

    private void initialize(@Nullable Bundle savedState) {
        String action = getIntent().getAction();
        if (Intent.ACTION_EDIT.equals(action)) {
            title.setText(R.string.label_edit_contact);
            mContactFetched = null == savedState ? false : savedState.getBoolean(KEY_CONTACT_FETCHED,false);
            boolean wasContactLoadingDone = mContactFetched || vm.hasContactDetails();
            Log.i(TAG,"wasContactLoadingDone: "+wasContactLoadingDone);
            if (!wasContactLoadingDone){
                final Uri uri = getIntent().getData();
                loadContact(uri);
            }
            else {
                mContactFetched = true;
            }
        }
        else if (Intent.ACTION_INSERT.equals(action)) {
            // TODO: handle insert
            title.setText(R.string.label_create_contact);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(KEY_CONTACT_FETCHED,mContactFetched);
        outState.putLong(KEY_CONTACT_ID,mContactId);
        outState.putString(KEY_LOOKUP_KEY,mLookupKey);
        outState.putLong(KEY_CONTACT_NAME_ID,mContactNameId);
        outState.putLong(KEY_CONTACT_NOTE_ID,mContactNoteId);
        outState.putBoolean(KEY_VIEW_STRUCTURED_NAME,btnSwitchName.isChecked());
        outState.putBoolean(KEY_VIEW_MORE,btnViewMore.isChecked());

        ArrayList<Parcelable> numbers = new ArrayList<>();
        ArrayList<Parcelable> emails = new ArrayList<>();
        ArrayList<Parcelable> events = new ArrayList<>();
        for (ItemNumber item : itemNumbers) {
            numbers.add(item.onSaveInstanceState());
        }
        for (ItemEmail item : itemEmails) {
            emails.add(item.onSaveInstanceState());
        }
        for (ItemEvent item : itemEvents) {
            events.add(item.onSaveInstanceState());
        }
        outState.putParcelableArrayList(KEY_NUMBERS,numbers);
        outState.putParcelableArrayList(KEY_EMAILS,emails);
        outState.putParcelableArrayList(KEY_EVENTS,events);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedState) {
        super.onRestoreInstanceState(savedState);

        mContactId = savedState.getLong(KEY_CONTACT_ID);
        mLookupKey = savedState.getString(KEY_LOOKUP_KEY);
        mContactNameId = savedState.getLong(KEY_CONTACT_NAME_ID);
        mContactNoteId = savedState.getLong(KEY_CONTACT_NOTE_ID);
        List<Parcelable> numbers = savedState.getParcelableArrayList(KEY_NUMBERS);
        List<Parcelable> emails = savedState.getParcelableArrayList(KEY_EMAILS) ;
        List<Parcelable> events = savedState.getParcelableArrayList(KEY_EVENTS);

        onRestoreName(savedState);

        onRestoreSectionNumber(numbers);

        onRestoreEmails(emails);

        onRestoreEvents(events);
    }

    private void loadContact(@NonNull Uri uri) {
        AsyncTask.execute(()->vm.getOperation().findContactDetails(uri),new AsyncTask.AsyncTaskCallback(){
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
        });
    }

    private void onContactLoaded(@NonNull ContactDetails details) {
        vm.setContactDetails(details);

        this.mContactId = details.getContact().getId();
        this.mLookupKey = details.getContact().getLookupKey();
        this.mContactNameId = null != details.getName() ? details.getName().getId() : 0L;
        this.mContactNoteId = null != details.getNote() ? details.getNote().getId() : 0L;

        onPrepareSectionName(details.getName());

        onPrepareSectionNumber(details.getPhoneNumbers());

        prepareSectionEmail(details.getEmails());

        onPrepareSectionEvent(details.getEvents());

        onPrepareSectionNote(details.getNote());

        mContactFetched = true;
    }

    private void onCancelContactInput() {
        finish();
    }

    private void onSaveContact() {
        List<PhoneNumber> numbers = extractPhoneNumbers();
        List<Email> emails = extraEmails();
        List<Event> events = extractEvents();
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    ///                              Section Name                                      ///
    /////////////////////////////////////////////////////////////////////////////////////

    private CheckBox btnSwitchName;
    private ViewGroup sectionContactName;
    private View sectionDisplayName;
    private View sectionStructuredName;
    private EditText iDisplayName;
    private EditText iPrefix;
    private EditText iFirstName;
    private EditText iMiddleName;
    private EditText iLastName;
    private EditText iSuffix;

    private void onInitSectionName() {
        sectionContactName = findViewById(R.id.section_contact_name);
        btnSwitchName = findViewById(R.id.button_switch_name);
        sectionDisplayName = findViewById(R.id.section_display_name);
        sectionStructuredName = findViewById(R.id.section_structured_name);
        iDisplayName = findViewById(R.id.display_name);
        iPrefix = findViewById(R.id.prefix);
        iFirstName = findViewById(R.id.first_name);
        iMiddleName = findViewById(R.id.middle_name);
        iLastName = findViewById(R.id.last_name);
        iSuffix = findViewById(R.id.suffix);
        btnSwitchName.setOnCheckedChangeListener((v,checked)->setSectionContactName(checked));
    }

    private void onRestoreName(@NonNull Bundle savedState) {
        setSectionContactName(savedState.getBoolean(KEY_VIEW_STRUCTURED_NAME));
    }

    private void onPrepareSectionName(@Nullable Name name) {
        if (null == name) return;
        iDisplayName.setText(name.getDisplayName());
        iPrefix.setText(name.getPrefix());
        iFirstName.setText(name.getGivenName());
        iMiddleName.setText(name.getMiddleName());
        iLastName.setText(name.getFamilyName());
        iSuffix.setText(name.getSuffix());
    }

    private void setSectionContactName(boolean which) {
        Animator anim;
        if (which) {
            // show all name input fields
            anim = Animations.animateHeight(sectionContactName,
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
            anim = Animations.animateHeight(sectionContactName,
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
        // TODO: implement extract name
        return null;
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    ///                             Section Number                                     ///
    /////////////////////////////////////////////////////////////////////////////////////

    private ViewGroup sectionNumber;
    private ViewGroup numbersList;
    private ArrayList<ItemNumber> itemNumbers = new ArrayList<>();

    private void onInitSectionNumber() {
        sectionNumber = findViewById(R.id.section_phone_number);
        numbersList = sectionNumber.findViewById(R.id.grid);
        ImageView icon = sectionNumber.findViewById(R.id.section_icon);
        icon.setImageDrawable(VectorDrawableCompat.create(getResources(),R.drawable.ic_baseline_phone,getTheme()));
        TextView name = sectionNumber.findViewById(R.id.section_name);
        name.setText(R.string.label_mobile);
        sectionNumber.findViewById(R.id.action_add).setOnClickListener(v->onAddNumber(true));

        onAddNumber(false);
    }

    private void onRestoreSectionNumber(@NonNull List<Parcelable> states) {
        numbersList.setVisibility(View.INVISIBLE);
        numbersList.removeAllViews();
        itemNumbers.clear();
        if (!states.isEmpty()) {
            for (int i=0; i<states.size(); i++) {
                ItemNumber item = onAddNumber(false);
                item.onRestoreInstanceState(states.get(i));
            }
        }
        numbersList.setVisibility(View.VISIBLE);
    }

    private void onPrepareSectionNumber(@Nullable List<PhoneNumber> numbers) {
        if (null == numbers || numbers.isEmpty()) return;

        numbersList.setVisibility(View.INVISIBLE);
        numbersList.removeAllViews();
        itemNumbers.clear();
        for (int i=0; i<numbers.size(); i++) {
            ItemNumber item = onAddNumber(false);
            item.setData(numbers.get(i));
        }
        numbersList.setVisibility(View.VISIBLE);
    }

    private ItemNumber onAddNumber(boolean animate) {
        ItemNumber iNum = new ItemNumber(getLayoutInflater(),numbersList);
        itemNumbers.add(iNum);
        iNum.setOnClickRemoveButton(v->onRemoveNumber(iNum));
        iNum.attachToParent(0,animate);
        return iNum;
    }

    private void onRemoveNumber(ItemNumber iNUm) {
        itemNumbers.remove(iNUm);
        iNUm.detachFromParent(true);
    }

    private List<PhoneNumber> extractPhoneNumbers() {
        ArrayList<PhoneNumber> numbers = new ArrayList<>();
        for (ItemNumber item : itemNumbers) {
            PhoneNumber  n = item.extractData();
            numbers.add(n);
        }
        return numbers;
    }

    private static class ItemNumber extends ItemWithTextInput<PhoneNumber> {

        public ItemNumber(LayoutInflater inflater, ViewGroup parent) {
            super(inflater, parent,
                    ContactDataTypeAdapter.forPhoneNumber(parent.getContext(),parent.getContext().getString(R.string.label_custom)));
        }

        @Override
        protected void onSetupEditText(TextInputLayout container, EditText editText) {
            container.setHint(R.string.label_mobile);
            editText.setInputType(EditorInfo.TYPE_CLASS_PHONE);
            editText.addTextChangedListener(new PhoneNumberFormattingTextWatcher());
        }

        @Override
        public long getDataId(@NonNull PhoneNumber data) {
            return data.getId();
        }

        @Override
        public int getTypeFromData(@NonNull PhoneNumber data) {
            return data.getType();
        }

        @Override
        public String getTypeLabelFromData(@NonNull PhoneNumber data) {
            return data.getTypeLabel().toString();
        }

        @Override
        public void setData(@Nullable PhoneNumber data) {
            super.setData(data);
            this.data = data;
            if (DEBUG) Log.d(TAG,"set number: "+data);
            if (data != null) {
                setInputText(data.getNumber());
            }
            else {
                setInputText(null);
            }
        }

        @Override
        @Nullable
        public PhoneNumber getData() {
            return data;
        }

        @Override
        public PhoneNumber extractData() {
            String number = getInputText();
            ContactDataTypeAdapter.ContactDataType type = getSelectedType();
            PhoneNumber data = new PhoneNumber();
            if (hasData()) data.setId(getDataId(getData()));
            data.setNumber(number);
            data.setType(type.getType());
            data.setTypeLable(type.getLabel());

            if (DEBUG) Log.d(TAG,"extract number: "+data);

            return data;
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    ///                              Section Email                                     ///
    /////////////////////////////////////////////////////////////////////////////////////

    private ViewGroup sectionEmail;
    private ViewGroup emailsList;
    private ArrayList<ItemEmail> itemEmails = new ArrayList<>();

    private void onInitSectionEmail() {
        sectionEmail = findViewById(R.id.section_email);
        ImageView icon = sectionEmail.findViewById(R.id.section_icon);
        icon.setImageDrawable(VectorDrawableCompat.create(getResources(),R.drawable.ic_baseline_alternate_email,getTheme()));
        TextView email = sectionEmail.findViewById(R.id.section_name);
        email.setText(R.string.label_email);
        emailsList = sectionEmail.findViewById(R.id.grid);
        sectionEmail.findViewById(R.id.action_add).setOnClickListener(v->onAddEmail(true));

        onAddEmail(false);
    }

    private void onRestoreEmails(@NonNull List<Parcelable> states) {
        emailsList.setVisibility(View.INVISIBLE);
        itemEmails.clear();
        emailsList.removeAllViews();
        for (int i=0; i<states.size(); i++) {
            ItemEmail item = onAddEmail(false);
            item.onRestoreInstanceState(states.get(i));
        }
        emailsList.setVisibility(View.VISIBLE);
    }

    private void prepareSectionEmail(@Nullable List<Email> emails) {
        if (null == emails || emails.isEmpty()) return;
        emailsList.setVisibility(View.INVISIBLE);
        itemEmails.clear();
        emailsList.removeAllViews();
        for (int i=0; i<emails.size(); i++) {
            onAddEmail(false);
            itemEmails.get(i).setData(emails.get(i));
        }
        emailsList.setVisibility(View.VISIBLE);
    }

    private ItemEmail onAddEmail(boolean animate) {
        ItemEmail iEmail = new ItemEmail(getLayoutInflater(),emailsList);
        itemEmails.add(iEmail);
        iEmail.setOnClickRemoveButton(v->onRemoveEmail(iEmail));
        iEmail.attachToParent(0,animate);
        return iEmail;
    }

    private void onRemoveEmail(ItemEmail iEmail) {
        itemEmails.remove(iEmail);
        iEmail.detachFromParent(true);
    }

    private List<Email> extraEmails() {
        List<Email> emails = new ArrayList<>();
        for (ItemEmail item : itemEmails) {
            emails.add(item.extractData());
        }
        return emails;
    }

    private static class ItemEmail extends ItemWithTextInput<Email> {

        public ItemEmail(LayoutInflater inflater, ViewGroup parent) {
            super(inflater, parent,
                    ContactDataTypeAdapter.forEmail(parent.getContext(),parent.getContext().getString(R.string.label_custom)));
        }

        @Override
        protected void onSetupEditText(TextInputLayout container, EditText editText) {
            container.setHint(R.string.label_email);
            editText.setInputType(EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        }

        @Override
        public long getDataId(@NonNull Email data) {
            return data.getId();
        }

        @Override
        public int getTypeFromData(@NonNull Email data) {
            return data.getType();
        }

        @Override
        public String getTypeLabelFromData(@NonNull Email data) {
            return data.getTypeLabel().toString();
        }

        @Override
        public void setData(@Nullable Email data) {
            super.setData(data);
            if (null != data) {
                setInputText(data.getAddress());
            }
            else {
                setInputText(null);
            }
        }

        @Override
        public Email extractData() {
            String address = getInputText();
            ContactDataTypeAdapter.ContactDataType type = getSelectedType();
            Email email = new Email();
            if (hasData()) email.setId(getDataId(getData()));
            email.setAddress(address);
            email.setType(type.getType());
            email.setTypeLabel(type.getLabel());

            return email;
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    ///                              Section Event                                     ///
    /////////////////////////////////////////////////////////////////////////////////////

    private ViewGroup sectionEvent;
    private ViewGroup eventsList;
    private List<ItemEvent> itemEvents = new ArrayList<>();

    private void onInitSectionEvent() {
        sectionEvent = findViewById(R.id.section_event);
        ImageView icon = sectionEvent.findViewById(R.id.section_icon);
        icon.setImageDrawable(VectorDrawableCompat.create(getResources(),R.drawable.ic_baseline_calendar_month,getTheme()));
        TextView name = sectionEvent.findViewById(R.id.section_name);
        name.setText(R.string.label_event);
        eventsList = sectionEvent.findViewById(R.id.grid);
        sectionEvent.findViewById(R.id.action_add).setOnClickListener(v->onAddEvent(false));
    }

    private void onRestoreEvents(@NonNull List<Parcelable> states) {
        eventsList.setVisibility(View.INVISIBLE);
        eventsList.removeAllViews();
        itemEvents.clear();
        for (int i=0; i<states.size(); i++) {
            ItemEvent item = onAddEvent(false);
            item.onRestoreInstanceState(states.get(i));
        }
        eventsList.setVisibility(View.VISIBLE);
    }

    private void onPrepareSectionEvent(@Nullable List<Event> events) {
        if (null == events || events.isEmpty()) return;
        if (itemEvents.isEmpty()) onAddEvent(false);
        itemEvents.get(0).setData(events.get(0));
        for (int i=1; i<events.size(); i++) {
            onAddEvent(false);
            itemEvents.get(i).setData(events.get(i));
        }
    }

    private ItemEvent onAddEvent(boolean animate) {
        ItemEvent item = new ItemEvent(getLayoutInflater(),eventsList,getSupportFragmentManager());
        itemEvents.add(item);
        item.setOnClickRemoveButton(v->onRemoveEvent(item));
        item.attachToParent(0,animate);
        return item;
    }

    private void onRemoveEvent(ItemEvent item) {
        itemEvents.remove(item);
        item.detachFromParent(true);
    }

    private List<Event> extractEvents() {
        List<Event> events = new ArrayList<>();
        for (ItemEvent item : itemEvents) {
            events.add(item.extractData());
        }
        return events;
    }

    private static class ItemEvent extends ItemWithType<Event> implements DatePickerDialog.OnDateSetListener {

        private static final String KEY_STATE_DATE_LABEL = "state_date_label";
        private static final String KEY_YEAR = "year";
        private static final String KEY_MONTH = "month";
        private static final String KEY_DAY_OF_MONTH = "day_of_month";
        private static final String KEY_INCLUDE_YEAR = "include_year";
        private static final String KEY_PICKED = "picked";

        private static final DateFormat WITH_YEAR = new SimpleDateFormat("MMM dd, yyyy");
        private static final DateFormat WITHOUT_YEAR = new SimpleDateFormat("MMMM dd");

        TextView startDate;

        int year,month,dayOfMonth;
        boolean includeYear;
        boolean picked;

        FragmentManager fragmentManager;
        DatePickerDialog datePickerDialog;

        public ItemEvent(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent, @NonNull FragmentManager fragmentManager) {
            super(parent,
                    inflater.inflate(R.layout.contact_input_event,parent,false),
                    ContactDataTypeAdapter.forEvent(parent.getContext(),parent.getResources().getString(R.string.label_custom)));
            this.fragmentManager = fragmentManager;
            startDate = getItemView().findViewById(R.id.start_date);
            startDate.setOnClickListener((v)->showCalendar());
            setData(null);
        }

        @NonNull
        @Override
        public ContactDataTypeAdapter getTypesAdapter() {
            return ContactDataTypeAdapter.forEvent(getContext(),getResources().getString(R.string.label_custom));
        }

        private void showCalendar() {
            if (null == datePickerDialog) {
                datePickerDialog = new DatePickerDialog();
                datePickerDialog.setOnDateSetListener(this);
            }
            datePickerDialog.showNow(fragmentManager,null);
            datePickerDialog.update(year,month,dayOfMonth,includeYear);
        }

        @Override
        public void onDateSet(DatePickerDialog dialog, int year, int month, int dayOfMonth) {
            this.year = year;
            this.month = month;
            this.dayOfMonth = dayOfMonth;
            this.includeYear = dialog.isIncludeYear();
            startDate.setText(
                    DateTimeUtil.formatDate(year,month,dayOfMonth,dialog.isIncludeYear(),WITH_YEAR,WITHOUT_YEAR)
            );
            picked = true;
        }

        @Override
        public long getDataId(@NonNull Event data) {
            return data.getId();
        }

        @Override
        public int getTypeFromData(@NonNull Event data) {
            return data.getType();
        }

        @Override
        public String getTypeLabelFromData(@NonNull Event data) {
            return data.getTypeLabel().toString();
        }

        @Override
        public void setData(@Nullable Event data) {
            super.setData(data);
            this.data = data;
            if (null != data) {
                dataId = data.getId();
                startDate.setText(DateTimeUtil.formatContactEventStartDate(data.getStartDate(),"MMM dd, yyyy","MMMM dd"));
                int[] date = DateTimeUtil.parseInDate(data.getStartDate());
                year = date[0];
                month = date[1];
                dayOfMonth = date[2];
                includeYear = DateTimeUtil.isPattern(data.getStartDate(),DateTimeUtil.YYYY_MM_DD);
                picked = true;
            }
            else {
                dataId = 0;
                startDate.setText(getResources().getString(R.string.label_event_state_date));
                setSelection(0);
                int[] now = DateTimeUtil.now();
                year = now[0];
                month = now[1];
                dayOfMonth = now[2];
                includeYear = true;
                picked = false;
            }
        }

        @Nullable
        @Override
        public Event getData() {
            return data;
        }

        @Override
        public Event extractData() {
            if (picked) {
                ContactDataTypeAdapter.ContactDataType type = getSelectedType();
                Event event = new Event();
                if (hasData()) event.setId(getDataId(getData()));
                event.setStartDate(DateTimeUtil.toContactEventStartDate(year,month,dayOfMonth,includeYear));
                event.setType(type.getType());
                event.setTypeLabel(type.getLabel());
            }
            return null;
        }

        @Override
        public Parcelable onSaveInstanceState() {
            Bundle state = new Bundle((Bundle) super.onSaveInstanceState());
            state.putString(KEY_STATE_DATE_LABEL,startDate.getText().toString());
            state.putInt(KEY_YEAR,year);
            state.putInt(KEY_MONTH,month);
            state.putInt(KEY_DAY_OF_MONTH,dayOfMonth);
            state.putBoolean(KEY_INCLUDE_YEAR,includeYear);
            state.putBoolean(KEY_PICKED,picked);
            return state;
        }

        @Override
        public void onRestoreInstanceState(Parcelable state) {
            if (null == state) return;

            Bundle bundle = (Bundle) state;
            startDate.setText(bundle.getString(KEY_STATE_DATE_LABEL));
            year = bundle.getInt(KEY_YEAR);
            month = bundle.getInt(KEY_MONTH);
            dayOfMonth = bundle.getInt(KEY_DAY_OF_MONTH);
            includeYear = bundle.getBoolean(KEY_INCLUDE_YEAR);
            picked = bundle.getBoolean(KEY_PICKED);

            super.onRestoreInstanceState(state);
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    ///                            Section Address                                     ///
    /////////////////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////////////////
    ///                            Section Organization                                ///
    /////////////////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////////////////
    ///                            Section Relation                                    ///
    /////////////////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////////////////
    ///                            Section Website                                     ///
    /////////////////////////////////////////////////////////////////////////////////////


    ///////////////////////////////////////////////////////////////////////////////////////
    ///                              Section Note                                      ///
    /////////////////////////////////////////////////////////////////////////////////////

    private View sectionNote;
    private EditText editTextNote;
    private View btnClearNote;

    private void onInitSectionNote() {
        sectionNote = findViewById(R.id.section_note);
        editTextNote = findViewById(R.id.input_note);
        btnClearNote = findViewById(R.id.clear_note);
        btnClearNote.setOnClickListener(v->onClearNote());
    }

    private void onPrepareSectionNote(Note note) {
        if (null == note) return;
        editTextNote.setText(note.getNote());
        sectionNote.setVisibility(View.VISIBLE);
    }

    private void onClearNote() {
        editTextNote.setText(null);
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    ///                            Button View More                                    ///
    /////////////////////////////////////////////////////////////////////////////////////

    private CheckBox btnViewMore;

    private void onInitViewMore() {
        btnViewMore = findViewById(R.id.button_view_more);
        btnViewMore.setOnCheckedChangeListener((cb, checked)->onViewMore());
    }

    private void checkViewMoreButtonShouldShow() {
        // TODO: check if all default hidden sections are showing
        boolean allShowing = false;
        if (allShowing) btnViewMore.setVisibility(View.GONE);
        else btnViewMore.setVisibility(View.VISIBLE);
    }

    private void onRestoreViewMore(@NonNull Bundle savedState) {
        btnViewMore.setChecked(savedState.getBoolean(KEY_VIEW_MORE));
    }

    private void onViewMore() {
        // TODO: make all sections visible
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    ///                              Utility Classes                                   ///
    /////////////////////////////////////////////////////////////////////////////////////

    private static abstract class BaseItem<I> {

        private static final String KEY_DATA_ID = "data_id";

        private ViewGroup parent;
        private View itemView;
        private View btnRemove;

        I data;
        long dataId;

        protected BaseItem(@NonNull ViewGroup parent, @NonNull View itemView) {
            this.parent = parent;
            this.itemView = itemView;
            btnRemove = getRemoveButton();
        }

        public Context getContext() { return itemView.getContext(); }

        public Resources getResources() { return getContext().getResources(); }

        @NonNull
        public View getItemView() { return itemView; }

        @NonNull
        public View getRemoveButton() { return itemView.findViewById(R.id.action_remove); }

        void setOnClickRemoveButton(View.OnClickListener listener) {
            btnRemove.setOnClickListener(listener);
        }

        public void setData(@Nullable I data) {
            this.data = data;
            this.dataId = hasData() ? getDataId(data) : 0L;
        }

        @Nullable
        public I getData() { return data; }

        public abstract long getDataId(@NonNull I data);

        public boolean hasData() { return null !=  getData(); }

        @Nullable
        public abstract I extractData();

        public void animateAddToParent(int position, @NonNull Animator anim) {
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    addToParent(position);
                }
            });
            anim.start();
        }

        public void animateRemoveFromParent(@NonNull Animator anim) {
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    itemView.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    removeFromParent();
                }
            });
            anim.start();
        }

        public void addToParent(int position) {
            parent.addView(getItemView(),position);
        }

        public void removeFromParent() {
            parent.removeView(getItemView());
        }

        public void attachToParent(int position, boolean animate) {
            if (!animate) {
                addToParent(position);
            }
            else {
                Animator animation = Animations.expandHeight(getItemView(),Animations.DURATION_FAST);
                animateAddToParent(0,animation);
            }
        }

        public void detachFromParent(boolean animate) {
            if (animate) {
                removeFromParent();
            }
            else {
                animateRemoveFromParent(Animations.shrinkHeight(getItemView(),Animations.DURATION_NORMAL));
            }
        }

        public Parcelable onSaveInstanceState() {
            Bundle state = new Bundle();
            state.putLong(KEY_DATA_ID,dataId);
            return state;
        }

        public void onRestoreInstanceState(Parcelable state) {
            if (null == state) return;
            Bundle bundle = (Bundle) state;
            dataId = bundle.getLong(KEY_DATA_ID,0L);
        }

        protected void logDebug(String tag, String message) {
            if (DEBUG) Log.d(tag,message);
        }
    }

    private static abstract class ItemWithType<I> extends BaseItem<I> {

        public static final String KEY_SELECTION = "selection";
        public static final String KEY_CUSTOM_LABEL = "custom_label";

        public static final int TYPE_CUSTOM = ContactsContract.CommonDataKinds.BaseTypes.TYPE_CUSTOM;

        private Spinner types;
        private ContactDataTypeAdapter typeAdapter;
        private boolean mCustomLabelInputEnabled = true;

        private EditText customTypeLabelInput;
        private AlertDialog customTypeLabelInputDialog;

        private AdapterView.OnItemSelectedListener onTypeSelected = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == typeAdapter.getPositionForType(TYPE_CUSTOM))
                    onShowCustomTypeLabelInputDialog();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        protected ItemWithType(@NonNull ViewGroup parent, @NonNull View itemView, @NonNull ContactDataTypeAdapter typeAdapter) {
            super(parent,itemView);
            this.types = itemView.findViewById(R.id.types);
            this.typeAdapter = typeAdapter;
            types.setAdapter(typeAdapter);
            types.setOnItemSelectedListener(onTypeSelected);
        }

        @NonNull
        public ContactDataTypeAdapter getTypesAdapter() { return typeAdapter; }

        public abstract int getTypeFromData(@NonNull I data);

        public abstract String getTypeLabelFromData(@NonNull I data);

        @Override
        public void setData(@Nullable I data) {
            super.setData(data);
            if (hasData()) {
                int type = getTypeFromData(data);
                int position = typeAdapter.getPositionForType(type);
                if (type == TYPE_CUSTOM) {
                    setCustomTypeLabel(getTypeLabelFromData(data));
                }
                types.setSelection(position);
            }
            else {
                types.setSelection(0);
            }
        }

        public String getCustomTypeLabel() {
            if (null != typeAdapter) {
                return typeAdapter.getItem(typeAdapter.getPositionForType(TYPE_CUSTOM))
                        .getLabel().toString();
            }
            return null;
        }

        public void setCustomTypeLabel(String label) {
            if (null != typeAdapter) {
                typeAdapter.getItem(typeAdapter.getPositionForType(TYPE_CUSTOM)).setLabel(label);
                typeAdapter.notifyDataSetChanged();
            }
        }

        public void onShowCustomTypeLabelInputDialog() {
            View contentView = LayoutInflater.from(getContext()).inflate(R.layout.custom_contact_data_type_input,null);
            customTypeLabelInput = contentView.findViewById(R.id.edittext);
            if (null == customTypeLabelInputDialog) {
                customTypeLabelInputDialog = new AlertDialog.Builder(getContext())
                        .setTitle(" ")
                        .setView(contentView)
                        .setNegativeButton(android.R.string.cancel, (di, which) -> di.dismiss())
                        .setPositiveButton(android.R.string.ok, (di, which) -> {
                            String label = customTypeLabelInput.getText().toString();
                            if (!Check.isEmptyString(label)){
                                setCustomTypeLabel(label);
                                typeAdapter.notifyDataSetChanged();
                            }
                        })
                        .setOnDismissListener((di)->{
                            String label = customTypeLabelInput.getText().toString();
                            if (Check.isEmptyString(label)) {
                                types.setSelection(0);
                            }
                        })
                        .create();
            }
            customTypeLabelInput.setText(getCustomTypeLabel());
            if (mCustomLabelInputEnabled) customTypeLabelInputDialog.show();
        }

        public ContactDataTypeAdapter.ContactDataType getSelectedType() {
            return typeAdapter.getItem(getSelection());
        }

        public int getSelection() {
            return types.getSelectedItemPosition();
        }

        public void setSelection(int position) {
            types.setSelection(position);
        }

        @Override
        public Parcelable onSaveInstanceState() {
            Bundle state = new Bundle((Bundle) super.onSaveInstanceState());
            state.putInt(KEY_SELECTION,getSelection());
            state.putString(KEY_CUSTOM_LABEL,getCustomTypeLabel());
            return state;
        }

        @Override
        public void onRestoreInstanceState(@Nullable Parcelable state) {
            if (null == state) return;
            Bundle bundle = (Bundle) state;
            setCustomTypeLabel(bundle.getString(KEY_CUSTOM_LABEL));
            mCustomLabelInputEnabled = false;
            setSelection(bundle.getInt(KEY_SELECTION));
            mCustomLabelInputEnabled = true;;
            super.onRestoreInstanceState(state);
        }
    }

    private static abstract class ItemWithTextInput<I> extends ItemWithType<I> {

        public static final String KEY_INPUT_TEXT = "input_text";

        private TextInputLayout container;
        private EditText editText;

        public ItemWithTextInput(LayoutInflater inflater, ViewGroup parent, ContactDataTypeAdapter typeAdapter) {
            super(parent,
                    inflater.inflate(R.layout.contact_input_data_label_close_button,parent,false),
                    typeAdapter);
            container = getItemView().findViewById(R.id.input_container);
            editText = getItemView().findViewById(R.id.edittext);
            if (SDK_INT > O) editText.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
            onSetupEditText(container,editText);
            setData(null);
        }

        protected abstract void onSetupEditText(TextInputLayout container, EditText editText);

        @Override
        public void detachFromParent(boolean animate) {
            editText.clearFocus();
            super.detachFromParent(animate);
        }

        public String getInputText() {
            return editText.getText().toString();
        }

        public void setInputText(String text) {
            editText.setText(text);
        }

        @Override
        public Parcelable onSaveInstanceState() {
            Bundle state = new Bundle((Bundle) super.onSaveInstanceState());
            String inputText = getInputText();
            state.putString(KEY_INPUT_TEXT,inputText);
            return state;
        }

        @Override
        public void onRestoreInstanceState(@Nullable Parcelable state) {
            if (null == state) return;
            Bundle bundle = (Bundle) state;
            String inputText = bundle.getString(KEY_INPUT_TEXT);
            setInputText(inputText);
            super.onRestoreInstanceState(state);
        }
    }
}