package lavs.business.model;

/**
 * Where a {@link Draft} is in its life.
 */
public enum DraftStatus {

    /** Saved locally and waiting to be sent. Nothing is known to have reached the mainframe. */
    PENDING,

    /**
     * An attempt to send it has started and has no recorded outcome yet. It is
     * saved in this state <em>before</em> sending, so a crash during the attempt
     * leaves a trace.
     */
    SENDING,

    /**
     * It may or may not have reached the mainframe, and nobody knows which. It is
     * not sent again until a person has checked, because sending it again could
     * create a second copy.
     */
    UNCERTAIN,

    /** Confirmed by the mainframe. */
    SYNCED,

    /** Refused by the mainframe, and waiting to be corrected. */
    REJECTED
}
