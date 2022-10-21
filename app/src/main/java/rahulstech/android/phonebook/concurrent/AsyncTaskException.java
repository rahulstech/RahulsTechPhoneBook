package rahulstech.android.phonebook.concurrent;

public class AsyncTaskException extends RuntimeException {

    public AsyncTaskException(String message) {
        super(message);
    }

    public AsyncTaskException(String message, Throwable cause) {
        super(message, cause);
    }
}
