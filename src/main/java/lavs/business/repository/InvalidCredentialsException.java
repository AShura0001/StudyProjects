package lavs.business.repository;

/**
 * Thrown when the mainframe refuses a username and password.
 *
 * <p>It is unchecked so that callers only handle it where a login is attempted.
 */
public class InvalidCredentialsException extends RuntimeException {

    /**
     * Creates an exception with a message.
     *
     * @param message the reason the mainframe gave
     */
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
