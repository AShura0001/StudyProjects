package lavs.ui.sync;

import lavs.business.service.DraftSyncService.SyncResult;
import lavs.business.service.SyncListener;

import java.time.Duration;
import java.util.Objects;

/**
 * Feeds the sync indicator from the background sync's events. Pass this as the
 * {@code SyncListener} when the services are started; it needs nothing else.
 */
public final class SyncStatusListener implements SyncListener {

    private final SyncStatus status;

    public SyncStatusListener(SyncStatus status) {
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    @Override
    public void onSyncFinished(SyncResult result) {
        int needsChecking = result.uncertain() + result.rejected();
        if (needsChecking > 0) {
            status.set(SyncState.ATTENTION, needsChecking);
        } else if (result.blocked() > 0) {
            status.set(SyncState.WAITING, result.blocked());
        } else {
            status.set(SyncState.SENT, 0);
        }
    }

    @Override
    public void onConnectionLost(SyncResult result, Duration retryIn) {
        if (result.uncertain() > 0) {
            status.set(SyncState.ATTENTION, result.uncertain());
        } else {
            status.set(SyncState.WAITING, Math.max(1, result.blocked()));
        }
    }

    @Override
    public void onSyncFailed(RuntimeException error) {
        status.set(SyncState.ATTENTION, Math.max(1, status.count()));
    }
}
