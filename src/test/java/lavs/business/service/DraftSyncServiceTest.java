package lavs.business.service;

import lavs.business.model.Draft;
import lavs.business.model.DraftStatus;
import lavs.business.model.DraftType;
import lavs.business.repository.DraftStorageException;
import lavs.business.repository.DraftUploadGateway;
import lavs.business.repository.FakeDraftRepository;
import lavs.business.repository.FakeDraftUploadGateway;
import lavs.business.repository.FakeDraftUploadGateway.Outcome;
import lavs.business.repository.MainframeUnavailableException;
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

    private void savePending(String id, DraftType type, String createdAt, List<String> dependsOn) {
        repository.save(new Draft(id, type, "text", DraftStatus.PENDING, Instant.parse(createdAt),
                null, Map.of(), dependsOn));
    }

    private void savePending(String id, DraftType type, String createdAt) {
        savePending(id, type, createdAt, List.of());
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
    void saveDraft_withParents_keepsThemInOrder() {
        Draft customer = service.saveDraft(DraftType.CUSTOMER, "customer text");
        Draft loan = service.saveDraft(DraftType.LOAN, "loan text", List.of(customer.getLocalId()));

        assertEquals(List.of(customer.getLocalId()), stored(loan.getLocalId()).getDependsOn());
    }

    @Test
    void saveDraft_withAParentThatWasNeverSaved_isRejectedAndNothingIsSaved() {
        assertThrows(IllegalArgumentException.class,
                () -> service.saveDraft(DraftType.LOAN, "loan text", List.of("ghost")));

        assertTrue(repository.findPending().isEmpty());
    }

    @Test
    void syncPending_withNoDrafts_doesNothing() {
        SyncResult result = service.syncPending();

        assertEquals(new SyncResult(0, 0, 0, false), result);
        assertTrue(gateway.attempts().isEmpty());
    }

    @Test
    void syncPending_withAcceptedDraft_savesItAsSyncedWithTheNumber() {
        Draft draft = service.saveDraft(DraftType.CUSTOMER, "customer text");

        SyncResult result = service.syncPending();

        assertEquals(new SyncResult(1, 0, 0, false), result);
        assertEquals(DraftStatus.SYNCED, stored(draft.getLocalId()).getStatus());
        assertEquals("CUS-1", stored(draft.getLocalId()).getServerNumber().orElseThrow());
    }

    @Test
    void syncPending_whenOffline_leavesTheDraftPendingAndReportsTheLostConnection() {
        Draft draft = service.saveDraft(DraftType.CUSTOMER, "customer text");
        gateway.goOffline();

        SyncResult result = service.syncPending();

        assertEquals(new SyncResult(0, 0, 0, true), result);
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

        assertEquals(new SyncResult(2, 0, 0, false), result);
        assertEquals(DraftStatus.SYNCED, stored(first.getLocalId()).getStatus());
        assertEquals(DraftStatus.SYNCED, stored(second.getLocalId()).getStatus());
    }

    @Test
    void syncPending_withRefusedDraft_savesItAsRejectedWithTheReasons() {
        Draft draft = service.saveDraft(DraftType.LOAN, "loan text");
        Map<String, String> reasons = Map.of("principal", "Must be greater than zero.");
        gateway.rejectDraft(draft.getLocalId(), reasons);

        SyncResult result = service.syncPending();

        assertEquals(new SyncResult(0, 1, 0, false), result);
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
        DraftSyncService silentRefusals = new DraftSyncService(repository, (draft, parentNumbers) -> {
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

        assertEquals(new SyncResult(1, 1, 0, false), result);
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

        assertEquals(new SyncResult(0, 0, 0, false), second);
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

    @Test
    void syncPending_withALoanAndItsCustomer_sendsTheCustomerFirstAndGivesTheLoanTheCustomersNumber() {
        Draft customer = service.saveDraft(DraftType.CUSTOMER, "customer text");
        Draft loan = service.saveDraft(DraftType.LOAN, "loan text", List.of(customer.getLocalId()));

        SyncResult result = service.syncPending();

        assertEquals(new SyncResult(2, 0, 0, false), result);
        assertEquals(List.of(customer.getLocalId(), loan.getLocalId()), gateway.acceptedDraftIds());
        assertEquals(List.of(), gateway.attempts().get(0).parentNumbers());
        assertEquals(List.of("CUS-1"), gateway.attempts().get(1).parentNumbers());
        assertEquals("LOAN-1", stored(loan.getLocalId()).getServerNumber().orElseThrow());
    }

    @Test
    void syncPending_whenTheLoanIsOlderThanItsCustomer_stillSendsTheCustomerFirst() {
        savePending("loan", DraftType.LOAN, "2026-10-01T09:00:00Z", List.of("customer"));
        savePending("customer", DraftType.CUSTOMER, "2026-10-02T09:00:00Z");

        SyncResult result = service.syncPending();

        assertEquals(new SyncResult(2, 0, 0, false), result);
        assertEquals(List.of("customer", "loan"), gateway.acceptedDraftIds());
    }

    @Test
    void syncPending_withACoBorrowerThatHasTwoParents_givesBothNumbersInOrder() {
        savePending("applicant", DraftType.CUSTOMER, "2026-10-01T09:00:00Z");
        savePending("loan", DraftType.LOAN, "2026-10-02T09:00:00Z", List.of("applicant"));
        savePending("partner", DraftType.CUSTOMER, "2026-10-03T09:00:00Z");
        savePending("attachment", DraftType.COBORROWER, "2026-10-04T09:00:00Z", List.of("loan", "partner"));

        SyncResult result = service.syncPending();

        assertEquals(new SyncResult(4, 0, 0, false), result);
        assertEquals(List.of("LOAN-1", "CUS-2"), gateway.attempts().get(3).parentNumbers());
    }

    @Test
    void syncPending_whenTheCustomerIsRefused_leavesTheLoanPendingAndDoesNotSendIt() {
        Draft customer = service.saveDraft(DraftType.CUSTOMER, "bad customer");
        Draft loan = service.saveDraft(DraftType.LOAN, "loan text", List.of(customer.getLocalId()));
        gateway.rejectDraft(customer.getLocalId(), Map.of("surname", "Required."));

        SyncResult result = service.syncPending();

        assertEquals(new SyncResult(0, 1, 1, false), result);
        assertEquals(DraftStatus.PENDING, stored(loan.getLocalId()).getStatus());
        assertEquals(1, gateway.attempts().size());
    }

    @Test
    void syncPending_whenAnEarlierDraftIsRefused_blocksEverythingBuiltOnIt() {
        Draft customer = service.saveDraft(DraftType.CUSTOMER, "bad customer");
        Draft loan = service.saveDraft(DraftType.LOAN, "loan text", List.of(customer.getLocalId()));
        service.saveDraft(DraftType.COBORROWER, "attachment", List.of(loan.getLocalId()));
        gateway.rejectDraft(customer.getLocalId(), Map.of("surname", "Required."));

        SyncResult result = service.syncPending();

        assertEquals(new SyncResult(0, 1, 2, false), result);
    }

    @Test
    void syncPending_afterTheRefusedCustomerIsCorrected_sendsTheBlockedLoanWithTheNewNumber() {
        Draft customer = service.saveDraft(DraftType.CUSTOMER, "bad customer");
        Draft loan = service.saveDraft(DraftType.LOAN, "loan text", List.of(customer.getLocalId()));
        gateway.rejectDraft(customer.getLocalId(), Map.of("surname", "Required."));
        service.syncPending();
        Draft corrected = stored(customer.getLocalId());
        corrected.revise("corrected customer");
        repository.save(corrected);
        gateway.stopRejecting(customer.getLocalId());

        SyncResult result = service.syncPending();

        assertEquals(new SyncResult(2, 0, 0, false), result);
        assertEquals(DraftStatus.SYNCED, stored(loan.getLocalId()).getStatus());
        assertEquals(3, gateway.attempts().size());
        assertEquals(List.of("CUS-1"), gateway.attempts().get(2).parentNumbers());
    }

    @Test
    void syncPending_whenTheCustomerWasAcceptedEarlier_sendsALaterLoanWithItsNumber() {
        Draft customer = service.saveDraft(DraftType.CUSTOMER, "customer text");
        service.syncPending();
        Draft loan = service.saveDraft(DraftType.LOAN, "loan text", List.of(customer.getLocalId()));

        SyncResult result = service.syncPending();

        assertEquals(new SyncResult(1, 0, 0, false), result);
        assertEquals(List.of("CUS-1"), gateway.attempts().get(1).parentNumbers());
        assertEquals(DraftStatus.SYNCED, stored(loan.getLocalId()).getStatus());
    }

    @Test
    void syncPending_whenTheCustomerCannotBeReached_doesNotTryTheLoan() {
        Draft customer = service.saveDraft(DraftType.CUSTOMER, "customer text");
        Draft loan = service.saveDraft(DraftType.LOAN, "loan text", List.of(customer.getLocalId()));
        gateway.goOffline();

        SyncResult result = service.syncPending();

        assertEquals(new SyncResult(0, 0, 0, true), result);
        assertEquals(1, gateway.attempts().size());
        assertEquals(DraftStatus.PENDING, stored(loan.getLocalId()).getStatus());
    }

    @Test
    void syncPending_whenTheConnectionDropsAfterTheCustomer_keepsTheCustomerAndResumesWithTheLoan() {
        Draft customer = service.saveDraft(DraftType.CUSTOMER, "customer text");
        Draft loan = service.saveDraft(DraftType.LOAN, "loan text", List.of(customer.getLocalId()));
        int[] calls = {0};
        DraftUploadGateway dropsAfterTheFirst = (draft, parentNumbers) -> {
            if (calls[0]++ == 0) {
                return "CUS-1";
            }
            throw new MainframeUnavailableException("signal lost");
        };

        SyncResult first = new DraftSyncService(repository, dropsAfterTheFirst).syncPending();
        SyncResult second = service.syncPending();

        assertEquals(new SyncResult(1, 0, 0, true), first);
        assertEquals(new SyncResult(1, 0, 0, false), second);
        assertEquals(List.of("CUS-1"), gateway.attempts().get(0).parentNumbers());
        assertEquals(DraftStatus.SYNCED, stored(loan.getLocalId()).getStatus());
    }

    @Test
    void syncPending_whenTheParentIsMissingFromTheStore_leavesTheDraftBlocked() {
        savePending("loan", DraftType.LOAN, "2026-10-01T09:00:00Z", List.of("ghost"));

        SyncResult result = service.syncPending();

        assertEquals(new SyncResult(0, 0, 1, false), result);
        assertTrue(gateway.attempts().isEmpty());
    }
}
