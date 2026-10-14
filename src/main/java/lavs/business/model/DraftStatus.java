package lavs.business.model;

/**
 * Where a {@link Draft} is in its life.
 */
public enum DraftStatus {

    /** Saved locally and not yet confirmed by the mainframe. */
    PENDING,

    /** Confirmed by the mainframe. */
    SYNCED,

    /** Refused by the mainframe, and waiting to be corrected. */
    REJECTED
}
