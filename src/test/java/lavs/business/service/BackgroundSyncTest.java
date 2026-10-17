package lavs.business.service;

import lavs.business.model.Draft;
import lavs.business.model.DraftStatus;
import lavs.business.model.DraftType;
import lavs.business.repository.DraftStorageException;
import lavs.business.repository.FakeDraftRepository;
import lavs.business.repository.FakeDraftUploadGateway;
import lavs.business.service.DraftSyncService.SyncResult;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackgroundSyncTest {

    private static final class RecordingListener implements SyncListener {
        private final List<SyncResult> finished = new ArrayList<>();
        private final List<SyncResult> lost = new ArrayList<>();
        private final List<Duration> retryDelays = new ArrayList<>();
        private final List<RuntimeException> failures = new ArrayList<>();
        private Runnable afterFinished = () -> { };

        @Override
        public void onSyncFinished(SyncResult result) {
            finished.add(result);
            afterFinished.run();
        }

        @Override
        public void onConnectionLost(SyncResult result, Duration retryIn) {
            lost.add(result);
            retryDelays.add(retryIn);
        }

        @Override
        public void onSyncFailed(RuntimeException error) {
            failures.add(error);
        }
    }

    private final FakeDraftRepository repository = new FakeDraftRepository();
    private final FakeDraftUploadGateway gateway = new FakeDraftUploadGateway();
    private final ManualScheduler scheduler = new ManualScheduler();
    private final RecordingListener listener = new RecordingListener();
    private final RetryPolicy policy = new RetryPolicy(Duration.ofSeconds(1), Duration.ofSeconds(60), () -> 1.0);
    private final DraftSyncService service = new DraftSyncService(repository, gateway);
    private final BackgroundSync sync = new BackgroundSync(service, scheduler, policy, listener);

    private DraftStatus statusOf(Draft draft) {
        return repository.findById(draft.getLocalId()).orElseThrow().getStatus();
    }

    @Test
    void newBackgroundSync_withNullArguments_isRejected() {
        assertThrows(NullPointerException.class, () -> new BackgroundSync(null, scheduler, policy, listener));
        assertThrows(NullPointerException.class, () -> new BackgroundSync(service, null, policy, listener));
        assertThrows(NullPointerException.class, () -> new BackgroundSync(service, scheduler, null, listener));
        assertThrows(NullPointerException.class, () -> new BackgroundSync(service, scheduler, policy, null));
    }

    @Test
    void saveDraft_savesTheDraftAtOnceAndLeavesSendingToTheWorker() {
        Draft draft = sync.saveDraft(DraftType.CUSTOMER, "customer text");

        assertEquals(DraftStatus.PENDING, statusOf(draft));
        assertTrue(gateway.attempts().isEmpty());
        assertEquals(1, scheduler.immediateCount());
    }

    @Test
    void saveDraft_onceTheWorkerRuns_sendsTheDraft() {
        Draft draft = sync.saveDraft(DraftType.CUSTOMER, "customer text");

        scheduler.runImmediate();

        assertEquals(DraftStatus.SYNCED, statusOf(draft));
        assertEquals(List.of(new SyncResult(1, 0, 0, false)), listener.finished);
    }

    @Test
    void saveDraft_withParents_sendsThemAfterTheirParentsWithTheirNumbers() {
        Draft customer = sync.saveDraft(DraftType.CUSTOMER, "customer text");
        Draft loan = sync.saveDraft(DraftType.LOAN, "loan text", List.of(customer.getLocalId()));

        scheduler.runImmediate();

        assertEquals(List.of(customer.getLocalId(), loan.getLocalId()), gateway.acceptedDraftIds());
        assertEquals(List.of("CUS-1"), gateway.attempts().get(1).parentNumbers());
    }

    @Test
    void saveDraft_whenTheStoreFails_reportsTheErrorAndQueuesNoPass() {
        repository.setFailingToSave(true);

        assertThrows(DraftStorageException.class, () -> sync.saveDraft(DraftType.CUSTOMER, "customer text"));

        assertEquals(0, scheduler.immediateCount());
    }

    @Test
    void requestSync_askedSeveralTimesBeforeTheWorkerIsFree_runsOnlyOnePass() {
        sync.saveDraft(DraftType.CUSTOMER, "customer text");

        sync.requestSync();
        sync.requestSync();
        scheduler.runImmediate();

        assertEquals(1, listener.finished.size());
    }

    @Test
    void requestSync_whileAPassIsRunning_queuesAnotherPass() {
        sync.saveDraft(DraftType.CUSTOMER, "first");
        listener.afterFinished = () -> {
            listener.afterFinished = () -> { };
            sync.requestSync();
        };

        scheduler.runImmediate();

        assertEquals(2, listener.finished.size());
    }

    @Test
    void connectionLost_leavesTheDraftPendingAndPlansARetryAfterTheFirstDelay() {
        gateway.goOffline();
        Draft draft = sync.saveDraft(DraftType.CUSTOMER, "customer text");

        scheduler.runImmediate();

        assertEquals(DraftStatus.PENDING, statusOf(draft));
        assertEquals(List.of(Duration.ofSeconds(1)), scheduler.waitingDelays());
        assertEquals(List.of(new SyncResult(0, 0, 0, true)), listener.lost);
        assertEquals(List.of(Duration.ofSeconds(1)), listener.retryDelays);
        assertTrue(listener.finished.isEmpty());
    }

    @Test
    void connectionLost_overAndOver_makesEachRetryWaitLonger() {
        gateway.goOffline();
        sync.saveDraft(DraftType.CUSTOMER, "customer text");
        scheduler.runImmediate();

        scheduler.fireNextDelayed();
        scheduler.fireNextDelayed();

        assertEquals(List.of(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(4)),
                listener.retryDelays);
    }

    @Test
    void retry_whenTheConnectionHasReturned_sendsTheDraftAndPlansNothingMore() {
        gateway.goOffline();
        Draft draft = sync.saveDraft(DraftType.CUSTOMER, "customer text");
        scheduler.runImmediate();
        gateway.goOnline();

        scheduler.fireNextDelayed();

        assertEquals(DraftStatus.SYNCED, statusOf(draft));
        assertEquals(1, listener.finished.size());
        assertTrue(scheduler.waitingDelays().isEmpty());
    }

    @Test
    void connectionLost_afterARecovery_startsTheDelaysFromTheBeginningAgain() {
        gateway.goOffline();
        sync.saveDraft(DraftType.CUSTOMER, "first");
        scheduler.runImmediate();
        scheduler.fireNextDelayed();
        gateway.goOnline();
        scheduler.fireNextDelayed();
        gateway.goOffline();

        sync.saveDraft(DraftType.CUSTOMER, "second");
        scheduler.runImmediate();

        assertEquals(List.of(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(1)),
                listener.retryDelays);
    }

    @Test
    void requestSync_whileARetryIsWaiting_runsNowAndCancelsTheRetry() {
        gateway.goOffline();
        Draft draft = sync.saveDraft(DraftType.CUSTOMER, "customer text");
        scheduler.runImmediate();
        gateway.goOnline();

        sync.requestSync();
        scheduler.runImmediate();

        assertEquals(DraftStatus.SYNCED, statusOf(draft));
        assertTrue(scheduler.waitingDelays().isEmpty());
        assertFalse(scheduler.fireNextDelayed());
    }

    @Test
    void refusedDraft_isReportedButNotRetriedAutomatically() {
        Draft draft = sync.saveDraft(DraftType.LOAN, "loan text");
        gateway.rejectDraft(draft.getLocalId(), Map.of("principal", "Too small."));

        scheduler.runImmediate();

        assertEquals(DraftStatus.REJECTED, statusOf(draft));
        assertEquals(List.of(new SyncResult(0, 1, 0, false)), listener.finished);
        assertTrue(scheduler.waitingDelays().isEmpty());
    }

    @Test
    void blockedDraft_isReportedButNotRetriedAutomatically() {
        Draft customer = sync.saveDraft(DraftType.CUSTOMER, "customer text");
        sync.saveDraft(DraftType.LOAN, "loan text", List.of(customer.getLocalId()));
        gateway.rejectDraft(customer.getLocalId(), Map.of("surname", "Required."));

        scheduler.runImmediate();

        assertEquals(List.of(new SyncResult(0, 1, 1, false)), listener.finished);
        assertTrue(scheduler.waitingDelays().isEmpty());
    }

    @Test
    void storeFailureDuringAPass_isReportedAndNotRetriedAutomatically() {
        sync.saveDraft(DraftType.CUSTOMER, "customer text");
        repository.setFailingToSave(true);

        scheduler.runImmediate();

        assertEquals(1, listener.failures.size());
        assertTrue(listener.failures.get(0) instanceof DraftStorageException);
        assertTrue(scheduler.waitingDelays().isEmpty());
        assertTrue(listener.finished.isEmpty());
    }

    @Test
    void requestSync_afterAStoreFailure_runsAnotherPass() {
        sync.saveDraft(DraftType.CUSTOMER, "customer text");
        repository.setFailingToSave(true);
        scheduler.runImmediate();
        repository.setFailingToSave(false);

        sync.requestSync();
        scheduler.runImmediate();

        assertEquals(1, listener.finished.size());
    }

    @Test
    void stop_shutsTheSchedulerDownAndIgnoresLaterRequests() {
        sync.stop();

        sync.requestSync();

        assertTrue(scheduler.isShutDown());
        assertEquals(0, scheduler.immediateCount());
    }

    @Test
    void stop_whenAPassWasAlreadyQueued_doesNotRunIt() {
        sync.saveDraft(DraftType.CUSTOMER, "customer text");

        sync.stop();
        scheduler.runImmediate();

        assertTrue(listener.finished.isEmpty());
        assertTrue(gateway.attempts().isEmpty());
    }

    @Test
    void withoutAListener_aFinishedPassStillWorks() {
        BackgroundSync quiet = new BackgroundSync(service, scheduler, policy);
        Draft draft = quiet.saveDraft(DraftType.CUSTOMER, "customer text");

        scheduler.runImmediate();

        assertEquals(DraftStatus.SYNCED, statusOf(draft));
    }

    @Test
    void withoutAListener_aLostConnectionStillPlansARetry() {
        BackgroundSync quiet = new BackgroundSync(service, scheduler, policy);
        gateway.goOffline();
        quiet.saveDraft(DraftType.CUSTOMER, "customer text");

        scheduler.runImmediate();

        assertEquals(List.of(Duration.ofSeconds(1)), scheduler.waitingDelays());
    }

    @Test
    void withoutAListener_aStoreFailureDoesNotEscapeTheWorker() {
        BackgroundSync quiet = new BackgroundSync(service, scheduler, policy);
        quiet.saveDraft(DraftType.CUSTOMER, "customer text");
        repository.setFailingToSave(true);

        scheduler.runImmediate();

        assertTrue(scheduler.waitingDelays().isEmpty());
    }
}
