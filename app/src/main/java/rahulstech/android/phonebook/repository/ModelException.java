package rahulstech.android.phonebook.repository;

public class ModelException extends RepositoryException {

    public ModelException(String message) {
        super(message);
    }

    public ModelException(String message, Throwable cause) {
        super(message, cause);
    }
}
