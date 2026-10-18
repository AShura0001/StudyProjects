package lavs.business.repository;

/**
 * Thrown when an upload reached the mainframe, or may have, but nobody can tell
 * whether the record was saved there. For example the connection dropped after the
 * request was sent and before the reply arrived.
 *
 * <p>This is different from {@link MainframeUnavailableException}, which means the
 * mainframe could not be reached and so nothing was sent. After this exception the
 * record must not simply be sent again, because it may already exist.
 *
 * <p>It is unchecked so that callers are not forced to handle network errors that
 * the business layer should not need to know about.
 */
public class UploadOutcomeUnknownException extends RuntimeException {

    /**
     * Creates an exception with a message.
     *
     * @param message what went wrong
     */
    public UploadOutcomeUnknownException(String message) {
        super(message);
    }

    /**
     * Creates an exception with a message and the error that caused it.
     *
     * @param message what went wrong
     * @param cause the underlying error, such as an I/O error
     */
    public UploadOutcomeUnknownException(String message, Throwable cause) {
        super(message, cause);
    }
}
