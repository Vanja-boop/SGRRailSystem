package db;

/** Unchecked wrapper so the rest of the code does not need try/catch around every SQL call. */
public class DatabaseException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
