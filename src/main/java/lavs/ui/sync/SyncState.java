package lavs.ui.sync;

/** Where the background sync of saved changes has got to, as the user should see it. */
public enum SyncState {
    /** Nothing waiting; everything reached the mainframe. */
    SENT,
    /** A sync is running right now. */
    SENDING,
    /** Changes are saved on this computer and will go when the connection returns. */
    WAITING,
    /** Something was rejected, or an upload was cut off and needs a person to check it. */
    ATTENTION
}
