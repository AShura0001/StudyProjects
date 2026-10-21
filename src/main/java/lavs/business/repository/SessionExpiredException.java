package lavs.business.repository;

/**
 * Thrown when the mainframe no longer accepts the current login session, for example
 * because the session ran out or someone else logged in with the same account. Only
 * logging in again fixes it.
 *
 * <p>It is a kind of {@link MainframeUnavailableException}, so code that already
 * handles "cannot reach the mainframe right now" treats it the same way: nothing was
 * saved, and the work can be tried again later. In particular a draft being sent goes
 * back to waiting, not to uncertain, because the mainframe refused it before doing anything.
 */
public class SessionExpiredException extends MainframeUnavailableException {

    /** Shown to staff when their session has ended. */
    public static final String MESSAGE = "Your session with the mainframe has ended. Please log in again.";

    public SessionExpiredException() {
        super(MESSAGE);
    }
}
