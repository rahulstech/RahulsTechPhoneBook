package rahulstech.android.phonebook.util;

public class CheckFailException extends RuntimeException {

    public CheckFailException(String message) {
        super(message);
    }

    public CheckFailException(String message, Throwable cause) {
        super(message, cause);
    }
}
