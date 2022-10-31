package rahulstech.android.phonebook.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateTimeUtil {

    private static final String TAG = "DateTimeUtil";

    public static final String YYYY_MM_DD = "\\d{4}\\-\\d{2}\\-\\d{2}";
    public static final String MM_DD = "\\-\\-\\d{2}\\-\\d{2}";


    public static String formatContactEventStartDate(String date, SimpleDateFormat full, SimpleDateFormat noYear) {
        if (Check.isEmptyString(date) || null == full || null == noYear) return null;
        if (date.matches(YYYY_MM_DD)) {
            int year = Integer.valueOf(date.substring(0,4));
            int month = Integer.valueOf(date.substring(5,7));
            int day = Integer.valueOf(date.substring(8));

            Date tmp = new Date(year,month,day);
            return full.format(tmp);
        }
        else {
            int month = Integer.valueOf(date.substring(2,4));
            int day = Integer.valueOf(date.substring(5));

            Date tmp = new Date();
            tmp.setMonth(month);
            tmp.setDate(day);
            return noYear.format(tmp);
        }
    }
}
