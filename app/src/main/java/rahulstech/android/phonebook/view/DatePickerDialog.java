package rahulstech.android.phonebook.view;

import android.animation.Animator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import rahulstech.android.phonebook.R;

public class DatePickerDialog extends DialogFragment {

    private int year;
    private int month;
    private int dayOfMonth;

    private CheckBox cbIncludeYear;
    private FrameLayout containerDatePicker;
    private DatePicker datePicker;
    private Button btnNegative;
    private Button btnPositive;

    private OnDateSetListener listener;

    private int widthWithYear;
    private int widthWithoutYear;

    public void setOnDateSetListener(OnDateSetListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.date_picker_dialog_layout,container,true);
        cbIncludeYear = view.findViewById(R.id.include_year);
        cbIncludeYear.setOnCheckedChangeListener((v,checked)->animateShowOrHideYear(checked));
        containerDatePicker = view.findViewById(R.id.container_date_picker);
        datePicker = view.findViewById(R.id.date_picker);
        datePicker.updateDate(year,month,dayOfMonth);
        btnNegative = view.findViewById(R.id.button_negative);
        btnPositive = view.findViewById(R.id.button_positive);
        btnNegative.setOnClickListener(v->onClickNegativeButton());
        btnPositive.setOnClickListener(v->onClickPositiveButton());
        widthWithYear = Animations.measureWidth(containerDatePicker);
        widthWithoutYear = (int) getResources().getDisplayMetrics().density*160;
        return view;
    }

    private void onClickNegativeButton() {
        dismiss();
    }

    private void onClickPositiveButton() {
        year = datePicker.getYear();
        month = datePicker.getMonth();
        dayOfMonth = datePicker.getDayOfMonth();
        datePicker.clearFocus();
        if (null != listener) listener.onDateSet(this,year,month,dayOfMonth);
        dismiss();
    }

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    public int getDayOfMonth() {
        return dayOfMonth;
    }

    public boolean isIncludeYear() {
        return cbIncludeYear.isChecked();
    }

    public void update(int year, int month, int dayOfMonth, boolean includeYear) {
        this.year = year;
        this.month = month;
        this.dayOfMonth = dayOfMonth;
        datePicker.updateDate(year, month, dayOfMonth);
        cbIncludeYear.setChecked(includeYear);
    }

    private void animateShowOrHideYear(boolean show) {
        if (show) {
            Animator anim = Animations.animateWidth(containerDatePicker,widthWithoutYear,widthWithYear,
                    Animations.DURATION_NORMAL,new AccelerateDecelerateInterpolator());
            anim.start();
        }
        else {
            Animator anim = Animations.animateWidth(containerDatePicker,widthWithYear,widthWithoutYear,
                    Animations.DURATION_NORMAL,new AccelerateDecelerateInterpolator());
            anim.start();
        }
    }

    public interface OnDateSetListener {
        void onDateSet(DatePickerDialog dialog, int year, int month, int dayOfMonth);
    }
}
