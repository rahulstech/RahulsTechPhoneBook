package rahulstech.android.phonebook.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import androidx.annotation.NonNull;

public class DateTimeUtil {

    private static final String TAG = "DateTimeUtil";

    public static final String YYYY_MM_DD = "\\d{4}\\-\\d{2}\\-\\d{2}";
    public static final String MM_DD = "\\-\\-\\d{2}\\-\\d{2}";

    public static boolean isPattern(String date, String pattern) {
        Check.isNonEmptyString(date,"date is empty");
        Check.isNonEmptyString(pattern,"pattern is empty");
        return date.matches(pattern);
    }

    public static int[] now() {
        Calendar cal = Calendar.getInstance();
        int[] date = new int[3];
        date[0] = cal.get(Calendar.YEAR);
        date[1] = cal.get(Calendar.MONTH);
        date[2] = cal.get(Calendar.DAY_OF_MONTH);
        return date;
    }

    public static String formatContactEventStartDate(String date, String formatWithYear, String formatWithoutYear) {
        if (Check.isEmptyString(date) || Check.isEmptyString(formatWithYear) || Check.isEmptyString(formatWithoutYear)) return null;
        Calendar cal = Calendar.getInstance();
        if (isPattern(date,YYYY_MM_DD)) {
            int year = Integer.valueOf(date.substring(0,4));
            int month = Integer.valueOf(date.substring(5,7))-1;
            int day = Integer.valueOf(date.substring(8));
            cal.set(year,month,day);
            return new SimpleDateFormat(formatWithYear).format(cal.getTimeInMillis());
        }
        else {
            int month = Integer.valueOf(date.substring(2,4))-1;
            int day = Integer.valueOf(date.substring(5));
            cal.set(Calendar.MONTH,month);
            cal.set(Calendar.DAY_OF_MONTH,day);
            return new SimpleDateFormat(formatWithoutYear).format(cal.getTimeInMillis());
        }
    }

    public static String formatDate(int year, int month, int dayOfMonth, boolean includeYear, String withYear, String withOutYear) {
        if (null == withYear || null == withOutYear) return null;
        Calendar cal = Calendar.getInstance();
        cal.set(year,month,dayOfMonth);
        return includeYear ? new SimpleDateFormat(withYear).format(cal.getTimeInMillis())
                : new SimpleDateFormat(withOutYear).format(cal.getTimeInMillis());
    }

    /**
     *
     * @param year date year
     * @param month month of year valid value 0-11, 0 = January, 11 = December
     * @param date day of month valid value 1-31
     * @param includeYear should include year or not
     * @return formatted contact event start date string
     */
    public static String toContactEventStartDate(int year, int month, int date, boolean includeYear) {
        month = month+1;
        if (includeYear) {
            return String.format("%04d-%02d-%02d",year,month,date);
        }
        return String.format("--%02d-%02d",month,date);
    }

    public static long inMillis(String startDate) {
        int[] date = parseInDate(startDate);
        Calendar calendar = Calendar.getInstance();
        calendar.set(date[0],date[1],date[2]);
        return calendar.getTimeInMillis();
    }

    public static int[] parseInDate(String date) {
        Calendar cal = Calendar.getInstance();
        if (isPattern(date,YYYY_MM_DD)) {
            int year = Integer.valueOf(date.substring(0,4));
            int month = Integer.valueOf(date.substring(5,7))-1;
            int day = Integer.valueOf(date.substring(8));
            cal.set(year,month,day);
        }
        else {
            int month = Integer.valueOf(date.substring(2,4))-1;
            int day = Integer.valueOf(date.substring(5));
            cal.set(Calendar.MONTH,month);
            cal.set(Calendar.DAY_OF_MONTH,day);
        }
        int[] output = new int[3];
        output[0] = cal.get(Calendar.YEAR);
        output[1] = cal.get(Calendar.MONTH);
        output[2] = cal.get(Calendar.DAY_OF_MONTH);
        return output;
    }
}
