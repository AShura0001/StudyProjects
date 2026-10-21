package lavs.business.service;

import lavs.business.service.DraftSyncService.SyncResult;

import java.time.Duration;

/**
 * Hears what the background sync is doing, for example so the screen can show
 * "saved locally, not yet sent".
 *
 * <p>All methods are called on the sync worker thread, not on the screen thread.
 * A JavaFX screen must hand the work over with {@code Platform.runLater}, and
 * these methods must return quickly and not throw. Every method does nothing by
 * default, so a listener only needs to override what it cares about.
 */
public interface SyncListener {

    /**
     * Called when a pass finished without losing the connection. Some drafts may
     * still have been refused, blocked or made uncertain, as the result says.
     *
     * @param result what the pass achieved
     */
    default void onSyncFinished(SyncResult result) {
    }

    /**
     * Called when a pass stopped because the mainframe could not be reached or an
     * attempt lost its outcome (check {@link SyncResult#uncertain()}). Everything not
     * yet sent stays saved, and another attempt is already planned.
     *
     * @param result what the pass achieved before it stopped
     * @param retryIn how long until the next attempt
     */
    default void onConnectionLost(SyncResult result, Duration retryIn) {
    }

    /**
     * Called when a pass could not be completed because the local draft store
     * failed. No automatic retry is planned, because sending again could create
     * a duplicate; the next call to {@link BackgroundSync#requestSync()} tries again.
     *
     * @param error what went wrong
     */
    default void onSyncFailed(RuntimeException error) {
    }

    /**
     * Told after every pass how many saved drafts, old or new, are uncertain or refused
     * and so wait for a person. Called after onSyncFinished or onConnectionLost.
     *
     * @param count the number of drafts that need checking
     */
    default void onNeedsChecking(int count) {
    }
}
