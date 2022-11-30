package rahulstech.android.phonebook.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;
import rahulstech.android.phonebook.R;
import rahulstech.android.phonebook.model.Email;
import rahulstech.android.phonebook.model.Event;
import rahulstech.android.phonebook.model.PhoneNumber;
import rahulstech.android.phonebook.model.PostalAddress;
import rahulstech.android.phonebook.model.Relation;
import rahulstech.android.phonebook.model.Website;
import rahulstech.android.phonebook.util.Check;
import rahulstech.android.phonebook.util.DateTimeUtil;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.O;
import static rahulstech.android.phonebook.BuildConfig.DEBUG;

public abstract class ContactInputItemManager<D,I extends ContactInputItemManager.BaseItem<D>> {

    private static final String TAG = "ContactInputManager";

    private static final String KEY_SECTION_VISIBILITY = "visibility";
    private static final String KEY_SECTION_STATES = "states";

    public static final int INSERT_POSITION_LAST = -1;

    @NonNull
    private Context context;
    @NonNull
    private ViewGroup parent;
    @NonNull
    private LayoutInflater inflater;

    private List<I> mItems = new ArrayList<>();

    private boolean sectionVisible = true;

    public static ContactInputItemManager<PhoneNumber,BaseItem<PhoneNumber>> forPhoneNumber(@NonNull Context context, @NonNull ViewGroup parent) {
        return new ContactInputItemManager<PhoneNumber, BaseItem<PhoneNumber>>(context,parent) {
            @NonNull
            @Override
            protected BaseItem<PhoneNumber> onCreateItem(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent, int position) {
                return new ItemPhoneNumber(inflater,parent);
            }
        };
    }

    public static ContactInputItemManager<Email,BaseItem<Email>> forEmail(@NonNull Context context, @NonNull ViewGroup parent) {
        return new ContactInputItemManager<Email, BaseItem<Email>>(context,parent) {

            @NonNull
            @Override
            protected BaseItem<Email> onCreateItem(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent, int position) {
                return new ItemWithTextInput<Email>(inflater,parent,
                        ContactDataTypeAdapter.forEmail(getContext())) {

                    @Override
                    protected void onInitItemWithTextInput() {
                        super.onInitItemWithTextInput();
                        getContainer().setHint(R.string.label_email);
                        getEditText().setInputType(EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                    }

                    @Override
                    public int getTypeFromData(@NonNull Email data) {
                        return data.getType();
                    }

                    @Override
                    public CharSequence getTypeLabelFromData(@NonNull Email data) {
                        return data.getTypeLabel();
                    }

                    @Override
                    protected void onSetData(@NonNull Email data) {
                        setInputText(data.getAddress());
                    }

                    @Override
                    public long getDataIdFromData(@NonNull Email data) {
                        return data.getId();
                    }

                    @Nullable
                    @Override
                    public Email extractData() {
                        String address = getInputText().toString();
                        ContactDataTypeAdapter.ContactDataType type = getSelectedType();
                        if (0==getDataId() && Check.isEmptyString(address)) return null;
                        Email email = new Email();
                        email.setId(getDataId());
                        email.setAddress(address);
                        email.setType(type.getType());
                        email.setTypeLabel(type.getLabel());
                        return email;
                    }
                };
            }
        };
    }

    public static ContactInputItemManager<Event,BaseItem<Event>> forEvent(@NonNull Context context, @NonNull ViewGroup parent, @NonNull FragmentManager fragmentManager) {
        return new ContactInputItemManager<Event, BaseItem<Event>>(context,parent) {
            @NonNull
            @Override
            protected BaseItem<Event> onCreateItem(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent, int position) {
                return new ItemEvent(inflater,parent,fragmentManager);
            }
        };
    }

    public static ContactInputItemManager<PostalAddress,BaseItem<PostalAddress>> forPostalAddress(@NonNull Context context, @NonNull ViewGroup parent) {
        return new ContactInputItemManager<PostalAddress, BaseItem<PostalAddress>>(context,parent) {
            @NonNull
            @Override
            protected BaseItem<PostalAddress> onCreateItem(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent, int position) {
                return new ItemPostalAddress(inflater,parent);
            }
        };
    }

    public static ContactInputItemManager<Relation,BaseItem<Relation>> forRelation(@NonNull Context context, @NonNull ViewGroup parent) {
        return new ContactInputItemManager<Relation, BaseItem<Relation>>(context,parent) {
            @NonNull
            @Override
            protected BaseItem<Relation> onCreateItem(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent, int position) {
                return new ItemRelation(inflater,parent);
            }
        };
    }

    public static ContactInputItemManager<Website,BaseItem<Website>> forWebsite(@NonNull Context context, @NonNull ViewGroup parent) {
        return new ContactInputItemManager<Website, BaseItem<Website>>(context,parent) {
            @NonNull
            @Override
            protected BaseItem<Website> onCreateItem(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent, int position) {
                return new ItemWithTextInput<Website>(
                        inflater, parent,ContactDataTypeAdapter.EMPTY_ADAPTER
                ) {
                    @Override
                    protected void onInitItemWithType() {
                        super.onInitItemWithType();
                        getTypePicker().setVisibility(View.GONE);
                    }

                    @Override
                    protected void onInitItemWithTextInput() {
                        super.onInitItemWithTextInput();
                        getContainer().setHint(R.string.label_website);
                        getEditText().setInputType(EditorInfo.TYPE_TEXT_VARIATION_URI);
                    }

                    @Override
                    public int getTypeFromData(@NonNull Website data) {return 0;}

                    @Override
                    public CharSequence getTypeLabelFromData(@NonNull Website data) {return ContactDataTypeAdapter.TYPE_OTHER.getLabel();}

                    @Override
                    protected void onSetData(@NonNull Website data) {
                        setInputText(data.getUrl());
                    }

                    @Override
                    public long getDataIdFromData(@NonNull Website data) {return data.getId();}

                    @Nullable
                    @Override
                    public Website extractData() {
                        String url = getInputText().toString();
                        if (0==getDataId()&&Check.isEmptyString(url)) return null;
                        Website website = new Website();
                        website.setId(getDataId());
                        website.setUrl(url);
                        return website;
                    }
                };
            }
        };
    }

    public static void turnOffAutofill(@NonNull EditText editText) {
        if (SDK_INT > O) editText.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
    }

    protected ContactInputItemManager(@NonNull Context context, @NonNull ViewGroup parent) {
        Check.isNonNull(context,"null == context");
        Check.isNonNull(parent, "null == parent");
        this.context = context;
        this.parent = parent;
        inflater = LayoutInflater.from(context);
    }

    @NonNull
    public Context getContext() {
        return context;
    }

    public void setSectionVisible(boolean sectionVisible) {
        this.sectionVisible = sectionVisible;
    }

    public boolean isSectionVisible() {
        return sectionVisible;
    }

    public Parcelable onSaveInstanceState() {
        ArrayList<Parcelable> states = new ArrayList<>();
        for (I item : mItems) {
            Parcelable state = item.onSaveInstanceState();
            states.add(state);
        }
        Bundle bundle = new Bundle();
        bundle.putBoolean(KEY_SECTION_VISIBILITY,isSectionVisible());
        bundle.putParcelableArrayList(KEY_SECTION_STATES,states);
        return bundle;
    }

    public void onRestoreInstanceState(@NonNull Parcelable source) {
        if (null == source) return;
        // TODO: improve item creation logic while restore
        Bundle savedState = (Bundle) source;
        setSectionVisible(savedState.getBoolean(KEY_SECTION_VISIBILITY,true));
        ArrayList<Parcelable> states = savedState.getParcelableArrayList(KEY_SECTION_STATES);
        parent.setVisibility(View.INVISIBLE);
        parent.removeAllViews();
        mItems.clear();
        final int size = states.size();
        for (int i=0; i<size; i++) {
            I item = onAddItem(i,false);
            item.onRestoreInstanceState(states.get(i));
        }
        parent.setVisibility(View.VISIBLE);
    }

    public void setData(@Nullable List<D> data) {
        // TODO: improve item creation logic while setting data
        parent.setVisibility(View.INVISIBLE);
        mItems.clear();
        parent.removeAllViews();
        if (null != data && !data.isEmpty()) {
            final int size = data.size();
            for (int i = 0; i < size; i++) {
                I item = onAddItem(i, false);
                item.setData(data.get(i));
            }
        }
        parent.setVisibility(View.VISIBLE);
    }

    public I addItemFirst(boolean animate) {
        return onAddItem(0,animate,null);
    }

    public I addItemFirst(boolean animate, @Nullable Runnable runOnAdded) {
        return onAddItem(0,animate,runOnAdded);
    }

    public I onAddItem(int position,boolean animate) {
        return onAddItem(position,animate,null);
    }

    public I onAddItem(int position,boolean animate, @Nullable Runnable runOnAdded) {
        final int index = position == INSERT_POSITION_LAST ? mItems.size() : position;
        if (index < 0) throw new IndexOutOfBoundsException("valid insert index 0 to "+(mItems.size())+" provided "+position);
        I item = onCreateItem(inflater,parent,index);
        item.setOnClickRemoveListener(v->onRemoveItem(item, true));
        if (animate) {
            Animator animator = item.getEnterAnimator();
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    onAddItem(index,item,null);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (null != runOnAdded) runOnAdded.run();
                }
            });
            animator.start();
        }
        else {
            onAddItem(index,item,runOnAdded);
        }
        return item;
    }

    private void onAddItem(int index, I item, @Nullable Runnable runOnAdded) {
        mItems.add(index, item);
        parent.addView(item.getItemView(),index);
        if (null != runOnAdded) runOnAdded.run();
    }

    public void onRemoveItem(@NonNull I item, boolean animate) {
        Check.isNonNull(item,"null == item");
        if (animate) {
            Animator animator = item.getExitAnimator();
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {}

                @Override
                public void onAnimationEnd(Animator animation) {
                    onRemoveItem(item);
                }
            });
            animator.start();
        }
        else {
            onRemoveItem(item);
        }
    }

    private void onRemoveItem(I item) {
        parent.removeView(item.getItemView());
        mItems.remove(item);
    }

    public List<D> extractAllData() {
        List<D> list = new ArrayList<>();
        int size = mItems.size();
        for (int i=0; i<size; i++) {
            D d = mItems.get(i).extractData();
            if (null != d) list.add(d);
        }
        return list;
    }

    @NonNull
    protected abstract I onCreateItem(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent, int position);

    public static abstract class BaseItem<I> {

        private static final String KEY_DATA_ID = "data_id";

        @NonNull
        private ViewGroup parent;
        @NonNull
        private View itemView;

        private I data;
        private long dataId;

        protected BaseItem(@NonNull ViewGroup parent, @NonNull View itemView) {
            Check.isNonNull(parent, "null == parent");
            Check.isNonNull(itemView, "null == itemView");
            this.parent = parent;
            this.itemView = itemView;
        }

        @NonNull
        public Context getContext() { return parent.getContext(); }

        @NonNull
        public ViewGroup getParent() {
            return parent;
        }

        @NonNull
        public View getItemView() { return itemView; }

        @NonNull
        public View getRemoveButton() { return itemView.findViewById(R.id.action_remove); }

        public void setOnClickRemoveListener(View.OnClickListener listener) {
            getRemoveButton().setOnClickListener(listener);
        }

        public void focus() {}

        public void setData(@Nullable I data) {
            this.data = data;
            if (null == data) {
                dataId = 0L;
                onSetDefault();
            }
            else {
                dataId = getDataIdFromData(data);
                onSetData(data);
            }
        }

        protected abstract void onSetData(@NonNull I data);

        protected abstract void onSetDefault();

        @Nullable
        public I getData() { return data; }

        public boolean hasData() { return null !=  getData(); }

        public abstract long getDataIdFromData(@NonNull I data);

        public long getDataId() { return dataId; }

        @Nullable
        public abstract I extractData();

        @NonNull
        public Animator getEnterAnimator() {
            return Animations.expandHeight(getItemView(),Animations.DURATION_FAST);
        }

        @NonNull
        public Animator getExitAnimator() {
            return Animations.shrinkHeight(getItemView(),Animations.DURATION_NORMAL);
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

    public static abstract class ItemWithType<I> extends BaseItem<I> {

        public static final String KEY_SELECTION = "selection";
        public static final String KEY_CUSTOM_LABEL = "custom_label";

        public static final int TYPE_CUSTOM = ContactsContract.CommonDataKinds.BaseTypes.TYPE_CUSTOM;

        private Button types;
        private ContactDataTypeAdapter typeAdapter;

        private int selection = 0;

        protected ItemWithType(@NonNull ViewGroup parent, @NonNull View itemView, @NonNull ContactDataTypeAdapter typeAdapter) {
            super(parent,itemView);
            this.types = itemView.findViewById(R.id.types);
            this.typeAdapter = typeAdapter;
            this.types.setOnClickListener(v->onShowTypesDialog());
            setSelection(0);
            onInitItemWithType();
        }

        protected void onInitItemWithType() {}

        public Button getTypePicker() {return types;}

        @NonNull
        public ContactDataTypeAdapter getTypesAdapter() { return typeAdapter; }

        public abstract int getTypeFromData(@NonNull I data);

        public abstract CharSequence getTypeLabelFromData(@NonNull I data);

        @Override
        public void setData(@Nullable I data) {
            super.setData(data);
            if (hasData()) {
                int type = getTypeFromData(data);
                int position = typeAdapter.getPositionForType(type);
                if (type == TYPE_CUSTOM) {
                    setCustomTypeLabel(getTypeLabelFromData(data));
                }
                setSelection(position);
            }
            else {
                setSelection(0);
                typeAdapter.getItem(typeAdapter.getPositionForType(TYPE_CUSTOM)).setLabel(getContext().getString(R.string.label_custom));
            }
        }

        public CharSequence getCustomTypeLabel() {
            return typeAdapter.getItem(typeAdapter.getPositionForType(TYPE_CUSTOM))
                    .getLabel().toString();
        }

        public void setCustomTypeLabel(CharSequence label) {
            int position = typeAdapter.getPositionForType(TYPE_CUSTOM);
            typeAdapter.getItem(position).setLabel(label);
            logDebug(TAG,"setCustomTypeLabel: position: "+position+" to_set="+label+" new_label="+typeAdapter.getItem(position).getLabel());
        }

        protected void onShowTypesDialog() {
            new AlertDialog.Builder(getContext())
                    .setSingleChoiceItems(typeAdapter,selection,(dialog, which) -> {
                        setSelection(which);
                        if (typeAdapter.getItem(which).getType() == TYPE_CUSTOM) {
                            onShowCustomTypeLabelInputDialog();
                        }
                        dialog.dismiss();
                    })
                    .show();
        }

        protected void onShowCustomTypeLabelInputDialog() {
            View contentView = LayoutInflater.from(getContext()).inflate(R.layout.custom_contact_data_type_input,null);
            EditText customTypeLabelInput = contentView.findViewById(R.id.edittext);
            customTypeLabelInput.setText(getCustomTypeLabel());
            new AlertDialog.Builder(getContext())
                    .setTitle(" ")
                    .setView(contentView)
                    .setNegativeButton(android.R.string.cancel, (di, which) -> di.dismiss())
                    .setPositiveButton(android.R.string.ok, (di, which) -> {
                        CharSequence label = customTypeLabelInput.getText();
                        if (label.length() > 0){
                            setCustomTypeLabel(label);
                        }
                        else {
                            setSelection(0);
                        }
                    })
                    .setOnDismissListener((di)->{
                        customTypeLabelInput.clearFocus();
                    })
                    .show();
        }

        public ContactDataTypeAdapter.ContactDataType getSelectedType() {
            return typeAdapter.getItem(getSelection());
        }

        public int getSelection() {return selection;}

        public void setSelection(int position) {
            this.selection = position;
            if (position >= 0 && position < typeAdapter.getCount())
                types.setText(getSelectedType().getLabel());
            else
                types.setText(null);
        }

        @Override
        public Parcelable onSaveInstanceState() {
            Bundle state = new Bundle((Bundle) super.onSaveInstanceState());
            final int selection = getSelection();
            final CharSequence customLabel = getCustomTypeLabel();
            state.putInt(KEY_SELECTION,selection);
            state.putCharSequence(KEY_CUSTOM_LABEL,customLabel);
            return state;
        }

        @Override
        public void onRestoreInstanceState(@Nullable Parcelable state) {
            if (null == state) return;
            Bundle bundle = (Bundle) state;
            int selection = bundle.getInt(KEY_SELECTION);
            CharSequence customLabel = bundle.getCharSequence(KEY_CUSTOM_LABEL);
            setCustomTypeLabel(customLabel);
            setSelection(selection);
            super.onRestoreInstanceState(state);
        }
    }

    public static abstract class ItemWithTextInput<I> extends ItemWithType<I> {

        public static final String KEY_INPUT_TEXT = "input_text";

        private TextInputLayout container;
        private EditText editText;

        protected ItemWithTextInput(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent, @NonNull ContactDataTypeAdapter typeAdapter) {
            super(parent, inflater.inflate(R.layout.contact_input_data_label_close_button,parent,false),typeAdapter);
            container = getItemView().findViewById(R.id.input_container);
            editText = getItemView().findViewById(R.id.edittext);
            turnOffAutofill(editText);
            onInitItemWithTextInput();
        }

        protected void onInitItemWithTextInput() {}

        public EditText getEditText() {
            return editText;
        }

        public TextInputLayout getContainer() {
            return container;
        }

        @Override
        public void focus() {
            editText.requestFocus();
        }

        @Override
        protected void onSetDefault() {
            editText.setText(null);
        }

        public CharSequence getInputText() {
            return editText.getText();
        }

        public void setInputText(CharSequence text) {
            editText.setText(text);
        }

        @Override
        public Parcelable onSaveInstanceState() {
            Bundle state = new Bundle((Bundle) super.onSaveInstanceState());
            final CharSequence inputText = getInputText();
            state.putCharSequence(KEY_INPUT_TEXT,inputText);
            return state;
        }

        @Override
        public void onRestoreInstanceState(@Nullable Parcelable state) {
            if (null == state) return;
            Bundle bundle = (Bundle) state;
            CharSequence inputText = bundle.getCharSequence(KEY_INPUT_TEXT);
            setInputText(inputText);
            super.onRestoreInstanceState(state);
        }
    }

    public static class ItemPhoneNumber extends ItemWithTextInput<PhoneNumber> {

        private static final String KEY_PRIMARY = "primary";

        private boolean primary = false;

        public ItemPhoneNumber(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent) {
            super(inflater,parent,
                    ContactDataTypeAdapter.forPhoneNumber(inflater.getContext()));
        }

        @Override
        protected void onInitItemWithTextInput() {
            getContainer().setHint(R.string.label_mobile);
            getEditText().setInputType(EditorInfo.TYPE_CLASS_PHONE);
            getEditText().addTextChangedListener(new PhoneNumberFormattingTextWatcher());
        }

        @Override
        public int getTypeFromData(@NonNull PhoneNumber data) {
            return data.getType();
        }

        @Override
        public CharSequence getTypeLabelFromData(@NonNull PhoneNumber data) {
            return data.getTypeLabel(getContext().getResources());
        }

        @Override
        public long getDataIdFromData(@NonNull PhoneNumber data) {
            return data.getId();
        }

        @Override
        protected void onSetData(@NonNull PhoneNumber data) {
            primary = data.isPrimary();
            setInputText(data.getNumber());
        }

        @Nullable
        @Override
        public PhoneNumber extractData() {
            String number = getInputText().toString();
            ContactDataTypeAdapter.ContactDataType type = getSelectedType();
            if (0==getDataId() && Check.isEmptyString(number)) return null;
            PhoneNumber data = new PhoneNumber();
            data.setId(getDataId());
            data.setPrimary(primary);
            data.setNumber(number);
            data.setType(type.getType());
            data.setTypeLabel(type.getLabel());
            return data;
        }

        @Override
        public Parcelable onSaveInstanceState() {
            Bundle state = new Bundle((Bundle) super.onSaveInstanceState());
            state.putBoolean(KEY_PRIMARY,primary);
            return state;
        }

        @Override
        public void onRestoreInstanceState(@Nullable Parcelable state) {
            if (null != state) {
                Bundle savedState = (Bundle) state;
                primary = savedState.getBoolean(KEY_PRIMARY,false);
            }
            super.onRestoreInstanceState(state);
        }
    }

    public static class ItemEvent extends ItemWithType<Event> {

        private static final String KEY_STATE_DATE_LABEL = "state_date_label";
        private static final String KEY_YEAR = "year";
        private static final String KEY_MONTH = "month";
        private static final String KEY_DAY_OF_MONTH = "day_of_month";
        private static final String KEY_INCLUDE_YEAR = "include_year";
        private static final String KEY_PICKED = "picked";

        private final String WITH_YEAR = "MMM dd, yyyy";
        private final String WITHOUT_YEAR = "MMMM dd";

        private TextView startDate;
        private int year,month,dayOfMonth;
        private boolean includeYear;
        private boolean picked;

        private FragmentManager fragmentManager;

        public ItemEvent(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent,
                         @NonNull FragmentManager fragmentManager) {
            super(parent,
                    inflater.inflate(R.layout.contact_input_event,parent,false),
                    ContactDataTypeAdapter.forEvent(parent.getContext()));
            this.fragmentManager = fragmentManager;
            startDate = getItemView().findViewById(R.id.start_date);
            startDate.setOnClickListener((v)->showCalendar());
            onSetDefault();
        }

        private void showCalendar() {
            DatePickerDialog datePickerDialog = new DatePickerDialog();
            datePickerDialog.setOnDateSetListener((dialog,year,month,dayOfMonth)->{
                    this.year = year;
                    this.month = month;
                    this.dayOfMonth = dayOfMonth;
                    this.includeYear = dialog.isIncludeYear();
                    startDate.setText(
                            DateTimeUtil.formatDate(year,month,dayOfMonth,dialog.isIncludeYear(),WITH_YEAR,WITHOUT_YEAR)
                    );
                    picked = true;
            });
            datePickerDialog.showNow(fragmentManager,null);
            datePickerDialog.update(year,month,dayOfMonth,includeYear);
        }

        @Override
        public long getDataIdFromData(@NonNull Event data) {
            return data.getId();
        }

        @Override
        public int getTypeFromData(@NonNull Event data) {
            return data.getType();
        }

        @Override
        public CharSequence getTypeLabelFromData(@NonNull Event data) {
            return data.getTypeLabel();
        }

        @Override
        protected void onSetData(@NonNull Event data) {
            startDate.setText(DateTimeUtil.formatContactEventStartDate(data.getStartDate(),WITH_YEAR,WITHOUT_YEAR));
            int[] date = DateTimeUtil.parseInDate(data.getStartDate());
            year = date[0];
            month = date[1];
            dayOfMonth = date[2];
            includeYear = DateTimeUtil.isPattern(data.getStartDate(),DateTimeUtil.YYYY_MM_DD);
            picked = true;
        }

        @Override
        protected void onSetDefault() {
            startDate.setText(getContext().getString(R.string.label_event_state_date));
            setSelection(0);
            int[] now = DateTimeUtil.now();
            year = now[0];
            month = now[1];
            dayOfMonth = now[2];
            includeYear = true;
            picked = false;
        }

        @Nullable
        @Override
        public Event extractData() {
            if (picked) {
                ContactDataTypeAdapter.ContactDataType type = getSelectedType();
                Event event = new Event();
                event.setId(getDataId());
                event.setStartDate(DateTimeUtil.toContactEventStartDate(year,month,dayOfMonth,includeYear));
                event.setType(type.getType());
                event.setTypeLabel(type.getLabel());
                return event;
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

    public static class ItemRelation extends ItemWithTextInput<Relation> {

        public ItemRelation(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent) {
            super(inflater,parent,
                    ContactDataTypeAdapter.forRelation(inflater.getContext()));
            getContainer().setHint(R.string.label_relation);
            getEditText().setInputType(EditorInfo.TYPE_TEXT_VARIATION_PERSON_NAME);
        }

        @Override
        protected void onSetData(@NonNull Relation data) {
            setInputText(data.getDisplayName());
        }

        @Override
        public long getDataIdFromData(@NonNull Relation data) {
            return data.getId();
        }

        @Nullable
        @Override
        public Relation extractData() {
            String displayName = getInputText().toString();
            ContactDataTypeAdapter.ContactDataType type = getSelectedType();
            if (0==getDataId()&&Check.isEmptyString(displayName)) return null;
            Relation relation = new Relation();
            relation.setId(getDataId());
            relation.setDisplayName(displayName);
            relation.setType(type.getType());
            relation.setTypeLabel(type.getLabel());
            return relation;
        }

        @Override
        public int getTypeFromData(@NonNull Relation data) {
            return data.getType();
        }

        @Override
        public CharSequence getTypeLabelFromData(@NonNull Relation data) {
            return data.getTypeLabel();
        }
    }

    public static class ItemPostalAddress extends ItemWithTextInput<PostalAddress> {

        public ItemPostalAddress(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent) {
            super(inflater,parent, ContactDataTypeAdapter.forPostalAddress(parent.getContext()));
        }

        @Override
        protected void onInitItemWithTextInput() {
            super.onInitItemWithTextInput();
            getContainer().setHint(R.string.label_address);
            getEditText().setInputType(EditorInfo.TYPE_TEXT_VARIATION_POSTAL_ADDRESS);
            getEditText().setSingleLine(false);
            getEditText().setImeOptions(EditorInfo.IME_NULL);
        }

        @Override
        protected void onSetData(@NonNull PostalAddress data) {
            setInputText(data.getFormattedAddress());
        }

        @Override
        protected void onSetDefault() {
            super.onSetDefault();
        }

        @Override
        public long getDataIdFromData(@NonNull PostalAddress data) {
            return data.getId();
        }

        @Nullable
        @Override
        public PostalAddress extractData() {
            String address = getInputText().toString();
            ContactDataTypeAdapter.ContactDataType type = getSelectedType();
            if (0==getDataId() && Check.isEmptyString(address)) return null;
            PostalAddress postalAddress = new PostalAddress();
            postalAddress.setId(getDataId());
            postalAddress.setFormattedAddress(address);
            postalAddress.setType(type.getType());
            postalAddress.setTypeLabel(type.getLabel());

            return postalAddress;
        }

        @Override
        public int getTypeFromData(@NonNull PostalAddress data) {
            return data.getType();
        }

        @Override
        public CharSequence getTypeLabelFromData(@NonNull PostalAddress data) {
            return data.getTypeLabel();
        }
    }
}
