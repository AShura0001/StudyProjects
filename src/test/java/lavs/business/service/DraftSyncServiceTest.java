package lavs.business.service;

import lavs.business.model.Draft;
import lavs.business.model.DraftStatus;
import lavs.business.model.DraftType;
import lavs.business.repository.DraftStorageException;
import lavs.business.repository.FakeDraftRepository;
import lavs.business.repository.FakeDraftUploadGateway;
import lavs.business.repository.FakeDraftUploadGateway.Outcome;
import lavs.business.repository.RecordRejectedException;
import lavs.business.service.DraftSyncService.SyncResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DraftSyncServiceTest {

    private final FakeDraftRepository repository = new FakeDraftRepository();
    private final FakeDraftUploadGateway gateway = new FakeDraftUploadGateway();
    private final DraftSyncService service = new DraftSyncService(repository, gateway);

    private Draft stored(String id) {
        return repository.findById(id).orElseThrow();
    }

    private void savePending(String id, DraftType type, String createdAt) {
        repository.save(new Draft(id, type, "text", DraftStatus.PENDING, Instant.parse(createdAt), null, Map.of()));
    }

    @Test
    void newService_withNullRepository_isRejected() {
        assertThrows(NullPointerException.class, () -> new DraftSyncService(null, gateway));
    }

    @Test
    void newService_withNullGateway_isRejected() {
        assertThrows(NullPointerException.class, () -> new DraftSyncService(repository, null));
    }

    @Test
    void saveDraft_withARecord_savesItAsPendingWithoutSendingAnything() {
        Draft draft = service.saveDraft(DraftType.CUSTOMER, "customer text");

        assertEquals(DraftStatus.PENDING, stored(draft.getLocalId()).getStatus());
        assertEquals("customer text", stored(draft.getLocalId()).getPayload());
        assertTrue(gateway.attempts().isEmpty());
    }

    @Test
    void saveDraft_whenTheMainframeIsOffline_stillSavesTheDraft() {
        gateway.goOffline();

        Draft draft = service.saveDraft(DraftType.LOAN, "loan text");

        assertTrue(repository.findById(draft.getLocalId()).isPresent());
    }

    @Test
    void saveDraft_whenTheStoreFails_reportsTheErrorAndSendsNothing() {
        repository.setFailingToSave(true);

        assertThrows(DraftStorageException.class, () -> service.saveDraft(DraftType.LOAN, "loan text"));
        assertTrue(gateway.attempts().isEmpty());
    }

    @Test
    void syncPending_withNoDrafts_doesNothing() {
        SyncResult result = service.syncPending();

        assertEquals(new SyncResult(0, 0, false), result);
        assertTrue(gateway.attempts().isEmpty());
    }

    @Test
    void syncPending_withAcceptedDraft_savesItAsSyncedWithTheNumber() {
        Draft draft = service.saveDraft(DraftType.CUSTOMER, "customer text");

        SyncResult result = service.syncPending();

        assertEquals(new SyncResult(1, 0, false), result);
        assertEquals(DraftStatus.SYNCED, stored(draft.getLocalId()).getStatus());
        assertEquals("CUS-1", stored(draft.getLocalId()).getServerNumber().orElseThrow());
    }

    @Test
    void syncPending_whenOffline_leavesTheDraftPendingAndReportsTheLostConnection() {
        Draft draft = service.saveDraft(DraftType.CUSTOMER, "customer text");
        gateway.goOffline();

        SyncResult result = service.syncPending();

        assertEquals(new SyncResult(0, 0, true), result);
        assertEquals(DraftStatus.PENDING, stored(draft.getLocalId()).getStatus());
        assertTrue(stored(draft.getLocalId()).getServerNumber().isEmpty());
    }

    @Test
    void syncPending_whenOffline_stopsAfterTheFirstFailedAttempt() {
        service.saveDraft(DraftType.CUSTOMER, "one");
        service.saveDraft(DraftType.CUSTOMER, "two");
        service.saveDraft(DraftType.CUSTOMER, "three");
        gateway.goOffline();

        service.syncPending();

        assertEquals(1, gateway.attempts().size());
    }

    @Test
    void syncPending_afterTheConnectionReturns_sendsTheDraftsThatWereWaiting() {
        Draft first = service.saveDraft(DraftType.CUSTOMER, "one");
        Draft second = service.saveDraft(DraftType.CUSTOMER, "two");
        gateway.goOffline();
        service.syncPending();
        gateway.goOnline();

        SyncResult result = service.syncPending();

        assertEquals(new SyncResult(2, 0, false), result);
        assertEquals(DraftStatus.SYNCED, stored(first.getLocalId()).getStatus());
        assertEquals(DraftStatus.SYNCED, stored(second.getLocalId()).getStatus());
    }

    @Test
    void syncPending_withRefusedDraft_savesItAsRejectedWithTheReasons() {
        Draft draft = service.saveDraft(DraftType.LOAN, "loan text");
        Map<String, String> reasons = Map.of("principal", "Must be greater than zero.");
        gateway.rejectDraft(draft.getLocalId(), reasons);

        SyncResult result = service.syncPending();

        assertEquals(new SyncResult(0, 1, false), result);
        assertEquals(DraftStatus.REJECTED, stored(draft.getLocalId()).getStatus());
        assertEquals(reasons, stored(draft.getLocalId()).getErrors());
    }

    @Test
    void syncPending_whenARefusalGivesNoFieldReasons_keepsTheMessageAsAGeneralError() {
        Draft draft = service.saveDraft(DraftType.LOAN, "loan text");
        gateway.rejectDraft(draft.getLocalId(), Map.of());

        service.syncPending();

        assertEquals(DraftStatus.REJECTED, stored(draft.getLocalId()).getStatus());
        assertEquals(List.of(DraftSyncService.GENERAL_ERROR_FIELD),
                List.copyOf(stored(draft.getLocalId()).getErrors().keySet()));
    }

    @Test
    void syncPending_whenARefusalHasNoFieldReasonsAndNoMessage_usesADefaultMessage() {
        DraftSyncService silentRefusals = new DraftSyncService(repository, (draft, parentNumber) -> {
            throw new RecordRejectedException(" ", Map.of());
        });
        Draft draft = silentRefusals.saveDraft(DraftType.LOAN, "loan text");

        silentRefusals.syncPending();

        assertEquals(Map.of(DraftSyncService.GENERAL_ERROR_FIELD, "The mainframe refused this record."),
                stored(draft.getLocalId()).getErrors());
    }

    @Test
    void syncPending_whenOneDraftIsRefused_stillSendsTheOthers() {
        Draft refused = service.saveDraft(DraftType.CUSTOMER, "bad");
        Draft fine = service.saveDraft(DraftType.CUSTOMER, "good");
        gateway.rejectDraft(refused.getLocalId(), Map.of("surname", "Required."));

        SyncResult result = service.syncPending();

        assertEquals(new SyncResult(1, 1, false), result);
        assertEquals(DraftStatus.REJECTED, stored(refused.getLocalId()).getStatus());
        assertEquals(DraftStatus.SYNCED, stored(fine.getLocalId()).getStatus());
    }

    @Test
    void syncPending_withSeveralDrafts_sendsTheOldestFirst() {
        savePending("newest", DraftType.CUSTOMER, "2026-10-03T09:00:00Z");
        savePending("oldest", DraftType.CUSTOMER, "2026-10-01T09:00:00Z");
        savePending("middle", DraftType.CUSTOMER, "2026-10-02T09:00:00Z");

        service.syncPending();

        assertEquals(List.of("oldest", "middle", "newest"), gateway.acceptedDraftIds());
    }

    @Test
    void syncPending_calledAgain_doesNotSendSyncedDraftsASecondTime() {
        service.saveDraft(DraftType.CUSTOMER, "one");
        service.syncPending();

        SyncResult second = service.syncPending();

        assertEquals(new SyncResult(0, 0, false), second);
        assertEquals(1, gateway.attempts().size());
    }

    @Test
    void syncPending_calledAgain_doesNotSendRejectedDraftsAgain() {
        Draft draft = service.saveDraft(DraftType.CUSTOMER, "bad");
        gateway.rejectDraft(draft.getLocalId(), Map.of("surname", "Required."));
        service.syncPending();

        service.syncPending();

        assertEquals(1, gateway.attempts().size());
        assertEquals(Outcome.REJECTED, gateway.attempts().get(0).outcome());
    }

    @Test
    void syncPending_whenSavingTheResultFails_reportsTheError() {
        Draft draft = service.saveDraft(DraftType.CUSTOMER, "one");
        repository.setFailingToSave(true);

        assertThrows(DraftStorageException.class, () -> service.syncPending());

        assertEquals(List.of(draft.getLocalId()), gateway.acceptedDraftIds());
    }
}
