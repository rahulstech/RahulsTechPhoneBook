package rahulstech.android.phonebook.util;

public class  Check {

    public static boolean isEquals(Object l, Object r){
        return l == r || null != l && l.equals(r);
    }

    public static void isNonNull(Object o, String message){
        isTrue(null != o, message);
    }

    public static void isTrue(boolean check, String message) {
        if (!check) {
            throw new CheckFailException(message);
        }
    }

    public static boolean isEmptyString(String text) {
        return null == text || "".equals(text);
    }

    public static void isNonEmptyString(String text, String message) {
        isTrue(!isEmptyString(text),message);
    }
}
