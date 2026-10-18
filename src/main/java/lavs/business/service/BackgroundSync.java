package lavs.business.service;

import lavs.business.model.Draft;
import lavs.business.model.DraftType;
import lavs.business.service.DraftSyncService.SyncResult;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runs {@link DraftSyncService} in the background so the screen never waits for
 * the mainframe, and tries again by itself after the connection was lost.
 *
 * <p>Saving a draft is immediate and never waits behind a slow upload. Sending
 * happens on the {@link Scheduler}'s single worker, one pass at a time. When a
 * pass loses the connection, the next attempt is planned after a growing delay
 * from the {@link RetryPolicy}. Refused or blocked drafts are not retried
 * automatically, because they will not change until the staff member corrects
 * them.
 */
public class BackgroundSync {

    private final DraftSyncService service;
    private final Scheduler scheduler;
    private final RetryPolicy retryPolicy;
    private final SyncListener listener;
    private final AtomicBoolean passQueued = new AtomicBoolean(false);
    private volatile boolean stopped;

    // Only the worker touches these two, so they need no locking.
    private int consecutiveLosses;
    private Scheduler.Handle waitingRetry;

    /**
     * Creates a background sync that nobody is listening to.
     *
     * @param service the service that saves and sends drafts
     * @param scheduler the single worker that runs the passes
     * @param retryPolicy how long to wait between attempts after a lost connection
     * @throws NullPointerException if any argument is null
     */
    public BackgroundSync(DraftSyncService service, Scheduler scheduler, RetryPolicy retryPolicy) {
        this(service, scheduler, retryPolicy, new SyncListener() {
        });
    }

    /**
     * Creates a background sync that reports what it does to a listener.
     *
     * @param service the service that saves and sends drafts
     * @param scheduler the single worker that runs the passes
     * @param retryPolicy how long to wait between attempts after a lost connection
     * @param listener told about every pass, on the worker thread
     * @throws NullPointerException if any argument is null
     */
    public BackgroundSync(DraftSyncService service, Scheduler scheduler, RetryPolicy retryPolicy,
                          SyncListener listener) {
        this.service = Objects.requireNonNull(service, "service must not be null");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler must not be null");
        this.retryPolicy = Objects.requireNonNull(retryPolicy, "retryPolicy must not be null");
        this.listener = Objects.requireNonNull(listener, "listener must not be null");
    }

    /**
     * Saves a record that does not depend on any other as a pending draft and
     * asks for it to be sent. The draft is on disk when this returns, whatever
     * the state of the connection.
     *
     * @param type the kind of record
     * @param payload the record as plain text
     * @return the draft that was saved
     * @throws NullPointerException if either argument is null
     * @throws lavs.business.repository.DraftStorageException if the draft could not be saved
     */
    public Draft saveDraft(DraftType type, String payload) {
        return saveDraft(type, payload, List.of());
    }

    /**
     * Saves a record that depends on other drafts as a pending draft and asks for
     * it to be sent once they have been accepted. The draft is on disk when this
     * returns, whatever the state of the connection.
     *
     * @param type the kind of record
     * @param payload the record as plain text
     * @param dependsOn the local IDs of the drafts the record belongs to
     * @return the draft that was saved
     * @throws NullPointerException if any argument, or any ID in the list, is null
     * @throws IllegalArgumentException if an ID is blank or repeated, or no draft
     *                                  with one of the IDs has been saved
     * @throws lavs.business.repository.DraftStorageException if the draft could not be saved
     */
    public Draft saveDraft(DraftType type, String payload, List<String> dependsOn) {
        Draft draft = service.saveDraft(type, payload, dependsOn);
        requestSync();
        return draft;
    }

    /**
     * Gets the drafts that may or may not be on the mainframe and are waiting for a
     * person to check. They are never sent again until someone settles them with
     * {@link #confirmSynced(String, String)} or {@link #confirmNotSent(String)}.
     *
     * @return the uncertain drafts, oldest first
     * @throws lavs.business.repository.DraftStorageException if the drafts could not be read
     */
    public List<Draft> findUncertain() {
        return service.findUncertain();
    }

    /**
     * Records that a person found the record of an uncertain draft on the mainframe,
     * and asks for a pass, since drafts waiting for it can now be sent.
     *
     * @param localId the local ID of the uncertain draft
     * @param serverNumber the number the mainframe gave the record
     * @return the draft, now synced
     * @throws NullPointerException if either argument is null
     * @throws IllegalArgumentException if there is no such draft or the number is blank
     * @throws IllegalStateException if the draft is not uncertain
     * @throws lavs.business.repository.DraftStorageException if the draft could not be saved
     */
    public Draft confirmSynced(String localId, String serverNumber) {
        Draft draft = service.confirmSynced(localId, serverNumber);
        requestSync();
        return draft;
    }

    /**
     * Records that a person checked and did not find the record of an uncertain
     * draft on the mainframe, and asks for a pass so that it is sent again.
     *
     * @param localId the local ID of the uncertain draft
     * @return the draft, now pending
     * @throws NullPointerException if the ID is null
     * @throws IllegalArgumentException if there is no such draft
     * @throws IllegalStateException if the draft is not uncertain
     * @throws lavs.business.repository.DraftStorageException if the draft could not be saved
     */
    public Draft confirmNotSent(String localId) {
        Draft draft = service.confirmNotSent(localId);
        requestSync();
        return draft;
    }

    /**
     * Asks for a pass over the pending drafts as soon as the worker is free. Call
     * this once when the application starts, to send what was left from last time
     * and to turn any attempt that was cut short into an uncertain draft. Asking
     * again before that pass starts does not queue another one. A retry that was
     * waiting is cancelled, because this pass replaces it. Does nothing once
     * {@link #stop()} has been called.
     */
    public void requestSync() {
        if (stopped) {
            return;
        }
        if (passQueued.compareAndSet(false, true)) {
            scheduler.execute(() -> {
                passQueued.set(false);
                runPass();
            });
        }
    }

    /** Stops the background work. Passes that have not started and retries that are waiting are dropped. */
    public void stop() {
        stopped = true;
        scheduler.shutdown();
    }

    private void runPass() {
        if (stopped) {
            return;
        }
        cancelWaitingRetry();
        SyncResult result;
        try {
            result = service.syncPending();
        } catch (RuntimeException e) {
            listener.onSyncFailed(e);
            return;
        }
        if (result.connectionLost()) {
            consecutiveLosses++;
            Duration delay = retryPolicy.delayAfter(consecutiveLosses);
            waitingRetry = scheduler.schedule(this::runPass, delay);
            listener.onConnectionLost(result, delay);
        } else {
            consecutiveLosses = 0;
            listener.onSyncFinished(result);
        }
    }

    private void cancelWaitingRetry() {
        if (waitingRetry != null) {
            waitingRetry.cancel();
            waitingRetry = null;
        }
    }
}
