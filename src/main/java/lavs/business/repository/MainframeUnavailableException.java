package lavs.business.repository;

/**
 * Thrown when the mainframe cannot be reached, for example because the
 * connection dropped or the server is down.
 *
 * <p>It is unchecked so that callers are not forced to handle network errors
 * that the business layer should not need to know about.
 */
public class MainframeUnavailableException extends RuntimeException {

    /**
     * Creates an exception with a message.
     *
     * @param message what went wrong
     */
    public MainframeUnavailableException(String message) {
        super(message);
    }

    /**
     * Creates an exception with a message and the error that caused it.
     *
     * @param message what went wrong
     * @param cause the underlying error, such as an I/O error
     */
    public MainframeUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
