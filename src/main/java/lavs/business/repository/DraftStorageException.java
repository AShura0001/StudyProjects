package lavs.business.repository;

/**
 * Thrown when a {@link DraftRepository} cannot read or write drafts, for example
 * because the disk is full or a file is damaged.
 *
 * <p>It is unchecked so that callers are not forced to handle file-specific
 * errors that the business layer should not need to know about.
 */
public class DraftStorageException extends RuntimeException {

    /**
     * Creates an exception with a message.
     *
     * @param message what went wrong
     */
    public DraftStorageException(String message) {
        super(message);
    }

    /**
     * Creates an exception with a message and the error that caused it.
     *
     * @param message what went wrong
     * @param cause the underlying error, such as an I/O error
     */
    public DraftStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
