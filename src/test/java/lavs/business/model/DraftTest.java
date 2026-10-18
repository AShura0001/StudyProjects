package lavs.business.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DraftTest {

    private static final Instant TIME = Instant.parse("2026-10-01T09:30:00Z");
    private static final Map<String, String> ERRORS = Map.of("principal", "Must be greater than zero.");

    private static Draft sendingDraft() {
        Draft draft = new Draft(DraftType.LOAN, "text");
        draft.markSending();
        return draft;
    }

    private static Draft syncedDraft() {
        Draft draft = sendingDraft();
        draft.markSynced("LOAN-1");
        return draft;
    }

    private static Draft rejectedDraft() {
        Draft draft = sendingDraft();
        draft.markRejected(ERRORS);
        return draft;
    }

    private static Draft uncertainDraft() {
        Draft draft = sendingDraft();
        draft.markUncertain();
        return draft;
    }

    private static Draft rebuilt(DraftStatus status, String number, Map<String, String> errors) {
        return new Draft("abc", DraftType.LOAN, "text", status, TIME, number, errors);
    }

    @Test
    void newDraft_whenCreated_startsAsPendingWithNoNumberAndNoErrors() {
        Draft draft = new Draft(DraftType.LOAN, "{\"principal\": 250000}");

        assertEquals(DraftStatus.PENDING, draft.getStatus());
        assertTrue(draft.getServerNumber().isEmpty());
        assertTrue(draft.getErrors().isEmpty());
    }

    @Test
    void newDraft_whenTwoAreCreated_haveDifferentIds() {
        Draft first = new Draft(DraftType.CUSTOMER, "first");
        Draft second = new Draft(DraftType.CUSTOMER, "first");

        assertNotEquals(first.getLocalId(), second.getLocalId());
    }

    @Test
    void newDraft_withNullPayload_isRejected() {
        assertThrows(NullPointerException.class,
                () -> new Draft(DraftType.LOAN, null));
    }

    @Test
    void newDraft_whenCreated_hasACreationTime() {
        Draft draft = new Draft(DraftType.CUSTOMER, "text");

        assertNotNull(draft.getCreatedAt());
    }

    @Test
    void rebuiltDraft_whenCreatedWithATime_keepsThatTime() {
        Draft draft = rebuilt(DraftStatus.SYNCED, "LOAN-1", Map.of());

        assertEquals(TIME, draft.getCreatedAt());
    }

    @Test
    void rebuiltDraft_withNullCreationTime_isRejected() {
        assertThrows(NullPointerException.class,
                () -> new Draft("abc", DraftType.LOAN, "text", DraftStatus.PENDING, null, null, Map.of()));
    }

    @Test
    void rebuiltDraft_withNullErrors_isRejected() {
        assertThrows(NullPointerException.class,
                () -> new Draft("abc", DraftType.LOAN, "text", DraftStatus.PENDING, TIME, null, null));
    }

    @Test
    void rebuiltDraft_asSyncedWithItsNumber_keepsTheNumber() {
        Draft draft = rebuilt(DraftStatus.SYNCED, "LOAN-7", Map.of());

        assertEquals("LOAN-7", draft.getServerNumber().orElseThrow());
    }

    @Test
    void rebuiltDraft_asRejectedWithErrors_keepsTheErrors() {
        Draft draft = rebuilt(DraftStatus.REJECTED, null, ERRORS);

        assertEquals(ERRORS, draft.getErrors());
    }

    @Test
    void rebuiltDraft_asSendingOrUncertainWithNothingElse_isAllowed() {
        assertEquals(DraftStatus.SENDING, rebuilt(DraftStatus.SENDING, null, Map.of()).getStatus());
        assertEquals(DraftStatus.UNCERTAIN, rebuilt(DraftStatus.UNCERTAIN, null, Map.of()).getStatus());
    }

    @Test
    void rebuiltDraft_asPendingWithANumber_isRejected() {
        assertThrows(IllegalArgumentException.class, () -> rebuilt(DraftStatus.PENDING, "LOAN-1", Map.of()));
    }

    @Test
    void rebuiltDraft_asPendingWithErrors_isRejected() {
        assertThrows(IllegalArgumentException.class, () -> rebuilt(DraftStatus.PENDING, null, ERRORS));
    }

    @Test
    void rebuiltDraft_asSendingWithANumberOrErrors_isRejected() {
        assertThrows(IllegalArgumentException.class, () -> rebuilt(DraftStatus.SENDING, "LOAN-1", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> rebuilt(DraftStatus.SENDING, null, ERRORS));
    }

    @Test
    void rebuiltDraft_asUncertainWithANumberOrErrors_isRejected() {
        assertThrows(IllegalArgumentException.class, () -> rebuilt(DraftStatus.UNCERTAIN, "LOAN-1", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> rebuilt(DraftStatus.UNCERTAIN, null, ERRORS));
    }

    @Test
    void rebuiltDraft_asSyncedWithoutANumber_isRejected() {
        assertThrows(IllegalArgumentException.class, () -> rebuilt(DraftStatus.SYNCED, null, Map.of()));
    }

    @Test
    void rebuiltDraft_asSyncedWithABlankNumber_isRejected() {
        assertThrows(IllegalArgumentException.class, () -> rebuilt(DraftStatus.SYNCED, " ", Map.of()));
    }

    @Test
    void rebuiltDraft_asSyncedWithErrors_isRejected() {
        assertThrows(IllegalArgumentException.class, () -> rebuilt(DraftStatus.SYNCED, "LOAN-1", ERRORS));
    }

    @Test
    void rebuiltDraft_asRejectedWithoutErrors_isRejected() {
        assertThrows(IllegalArgumentException.class, () -> rebuilt(DraftStatus.REJECTED, null, Map.of()));
    }

    @Test
    void rebuiltDraft_asRejectedWithANumber_isRejected() {
        assertThrows(IllegalArgumentException.class, () -> rebuilt(DraftStatus.REJECTED, "LOAN-1", ERRORS));
    }

    @Test
    void markSending_onPendingDraft_changesTheStatus() {
        Draft draft = new Draft(DraftType.LOAN, "text");

        draft.markSending();

        assertEquals(DraftStatus.SENDING, draft.getStatus());
    }

    @Test
    void markSending_onAnyOtherStatus_isRejected() {
        assertThrows(IllegalStateException.class, () -> sendingDraft().markSending());
        assertThrows(IllegalStateException.class, () -> uncertainDraft().markSending());
        assertThrows(IllegalStateException.class, () -> syncedDraft().markSending());
        assertThrows(IllegalStateException.class, () -> rejectedDraft().markSending());
    }

    @Test
    void markSynced_onSendingDraft_storesTheNumberAndChangesStatus() {
        Draft draft = sendingDraft();

        draft.markSynced("LOAN-1");

        assertEquals(DraftStatus.SYNCED, draft.getStatus());
        assertEquals("LOAN-1", draft.getServerNumber().orElseThrow());
    }

    @Test
    void markSynced_onUncertainDraft_storesTheNumberAndChangesStatus() {
        Draft draft = uncertainDraft();

        draft.markSynced("LOAN-9");

        assertEquals(DraftStatus.SYNCED, draft.getStatus());
        assertEquals("LOAN-9", draft.getServerNumber().orElseThrow());
    }

    @Test
    void markSynced_withNullNumber_isRejected() {
        Draft draft = sendingDraft();

        assertThrows(NullPointerException.class, () -> draft.markSynced(null));
    }

    @Test
    void markSynced_withBlankNumber_isRejected() {
        Draft draft = sendingDraft();

        assertThrows(IllegalArgumentException.class, () -> draft.markSynced("  "));
        assertEquals(DraftStatus.SENDING, draft.getStatus());
    }

    @Test
    void markSynced_onPendingDraft_isRejectedBecauseNothingWasSent() {
        Draft draft = new Draft(DraftType.LOAN, "text");

        assertThrows(IllegalStateException.class, () -> draft.markSynced("LOAN-1"));
    }

    @Test
    void markSynced_onSyncedDraft_isRejected() {
        Draft draft = syncedDraft();

        assertThrows(IllegalStateException.class, () -> draft.markSynced("LOAN-2"));
        assertEquals("LOAN-1", draft.getServerNumber().orElseThrow());
    }

    @Test
    void markSynced_onRejectedDraft_isRejected() {
        Draft draft = rejectedDraft();

        assertThrows(IllegalStateException.class, () -> draft.markSynced("LOAN-1"));
    }

    @Test
    void markRejected_onSendingDraft_storesTheErrorsAndChangesStatus() {
        Draft draft = sendingDraft();

        draft.markRejected(ERRORS);

        assertEquals(DraftStatus.REJECTED, draft.getStatus());
        assertEquals(ERRORS, draft.getErrors());
        assertTrue(draft.getServerNumber().isEmpty());
    }

    @Test
    void markRejected_withManyErrors_keepsTheOrderTheyWereGiven() {
        Draft draft = sendingDraft();
        Map<String, String> errors = new LinkedHashMap<>();
        errors.put("startDate", "Bad date.");
        errors.put("principal", "Too small.");
        errors.put("rate", "Too high.");

        draft.markRejected(errors);

        assertEquals(List.of("startDate", "principal", "rate"), List.copyOf(draft.getErrors().keySet()));
    }

    @Test
    void markRejected_whenTheOriginalMapChangesLater_isNotAffected() {
        Draft draft = sendingDraft();
        Map<String, String> errors = new LinkedHashMap<>();
        errors.put("principal", "Too small.");
        draft.markRejected(errors);

        errors.put("rate", "Too high.");

        assertEquals(Map.of("principal", "Too small."), draft.getErrors());
    }

    @Test
    void getErrors_whenCallerTriesToChangeThem_isRejected() {
        Draft draft = rejectedDraft();

        assertThrows(UnsupportedOperationException.class, () -> draft.getErrors().put("rate", "Too high."));
    }

    @Test
    void markRejected_withNoErrors_isRejected() {
        Draft draft = sendingDraft();

        assertThrows(IllegalArgumentException.class, () -> draft.markRejected(Map.of()));
        assertEquals(DraftStatus.SENDING, draft.getStatus());
    }

    @Test
    void markRejected_withNullErrors_isRejected() {
        Draft draft = sendingDraft();

        assertThrows(NullPointerException.class, () -> draft.markRejected(null));
    }

    @Test
    void markRejected_onPendingDraft_isRejectedBecauseNothingWasSent() {
        Draft draft = new Draft(DraftType.LOAN, "text");

        assertThrows(IllegalStateException.class, () -> draft.markRejected(ERRORS));
    }

    @Test
    void markRejected_onSyncedDraft_isRejected() {
        Draft draft = syncedDraft();

        assertThrows(IllegalStateException.class, () -> draft.markRejected(ERRORS));
        assertEquals(DraftStatus.SYNCED, draft.getStatus());
    }

    @Test
    void markRejected_onRejectedDraft_isRejected() {
        Draft draft = rejectedDraft();

        assertThrows(IllegalStateException.class, () -> draft.markRejected(Map.of("rate", "Too high.")));
    }

    @Test
    void markRejected_onUncertainDraft_isRejected() {
        Draft draft = uncertainDraft();

        assertThrows(IllegalStateException.class, () -> draft.markRejected(ERRORS));
    }

    @Test
    void markUncertain_onSendingDraft_changesTheStatus() {
        Draft draft = sendingDraft();

        draft.markUncertain();

        assertEquals(DraftStatus.UNCERTAIN, draft.getStatus());
        assertTrue(draft.getServerNumber().isEmpty());
        assertTrue(draft.getErrors().isEmpty());
    }

    @Test
    void markUncertain_onAnyOtherStatus_isRejected() {
        assertThrows(IllegalStateException.class, () -> new Draft(DraftType.LOAN, "text").markUncertain());
        assertThrows(IllegalStateException.class, () -> uncertainDraft().markUncertain());
        assertThrows(IllegalStateException.class, () -> syncedDraft().markUncertain());
        assertThrows(IllegalStateException.class, () -> rejectedDraft().markUncertain());
    }

    @Test
    void markNotSent_onSendingDraft_putsItBackToPending() {
        Draft draft = sendingDraft();

        draft.markNotSent();

        assertEquals(DraftStatus.PENDING, draft.getStatus());
    }

    @Test
    void markNotSent_onUncertainDraft_putsItBackToPending() {
        Draft draft = uncertainDraft();

        draft.markNotSent();

        assertEquals(DraftStatus.PENDING, draft.getStatus());
    }

    @Test
    void markNotSent_onPendingSyncedOrRejectedDraft_isRejected() {
        assertThrows(IllegalStateException.class, () -> new Draft(DraftType.LOAN, "text").markNotSent());
        assertThrows(IllegalStateException.class, () -> syncedDraft().markNotSent());
        assertThrows(IllegalStateException.class, () -> rejectedDraft().markNotSent());
    }

    @Test
    void revise_onRejectedDraft_replacesThePayloadAndClearsTheErrors() {
        Draft draft = rejectedDraft();

        draft.revise("corrected text");

        assertEquals("corrected text", draft.getPayload());
        assertEquals(DraftStatus.PENDING, draft.getStatus());
        assertTrue(draft.getErrors().isEmpty());
    }

    @Test
    void revise_onPendingDraft_replacesThePayloadAndStaysPending() {
        Draft draft = new Draft(DraftType.LOAN, "first version");

        draft.revise("second version");

        assertEquals("second version", draft.getPayload());
        assertEquals(DraftStatus.PENDING, draft.getStatus());
    }

    @Test
    void revise_keepsTheIdTypeAndCreationTime() {
        Draft draft = new Draft("abc", DraftType.LOAN, "text", DraftStatus.PENDING, TIME, null, Map.of());

        draft.revise("new text");

        assertEquals("abc", draft.getLocalId());
        assertEquals(DraftType.LOAN, draft.getType());
        assertEquals(TIME, draft.getCreatedAt());
    }

    @Test
    void revise_onSyncedDraft_isRejected() {
        Draft draft = syncedDraft();

        assertThrows(IllegalStateException.class, () -> draft.revise("new text"));
        assertEquals("text", draft.getPayload());
    }

    @Test
    void revise_onSendingDraft_isRejectedBecauseTheOldVersionMayBeOnItsWay() {
        Draft draft = sendingDraft();

        assertThrows(IllegalStateException.class, () -> draft.revise("new text"));
        assertEquals("text", draft.getPayload());
    }

    @Test
    void revise_onUncertainDraft_isRejectedBecauseTheOldVersionMayAlreadyBeThere() {
        Draft draft = uncertainDraft();

        assertThrows(IllegalStateException.class, () -> draft.revise("new text"));
        assertEquals(DraftStatus.UNCERTAIN, draft.getStatus());
    }

    @Test
    void revise_withNullPayload_isRejected() {
        Draft draft = new Draft(DraftType.LOAN, "text");

        assertThrows(NullPointerException.class, () -> draft.revise(null));
    }

    @Test
    void newDraft_withoutParents_dependsOnNothing() {
        Draft draft = new Draft(DraftType.CUSTOMER, "text");

        assertTrue(draft.getDependsOn().isEmpty());
    }

    @Test
    void newDraft_withParents_keepsThemInTheOrderGiven() {
        Draft draft = new Draft(DraftType.COBORROWER, "text", List.of("loan-id", "customer-id"));

        assertEquals(List.of("loan-id", "customer-id"), draft.getDependsOn());
    }

    @Test
    void newDraft_whenTheOriginalListChangesLater_isNotAffected() {
        List<String> parents = new ArrayList<>(List.of("loan-id"));
        Draft draft = new Draft(DraftType.COBORROWER, "text", parents);

        parents.add("customer-id");

        assertEquals(List.of("loan-id"), draft.getDependsOn());
    }

    @Test
    void getDependsOn_whenCallerTriesToChangeIt_isRejected() {
        Draft draft = new Draft(DraftType.LOAN, "text", List.of("customer-id"));

        assertThrows(UnsupportedOperationException.class, () -> draft.getDependsOn().add("other"));
    }

    @Test
    void newDraft_withNullParentList_isRejected() {
        assertThrows(NullPointerException.class, () -> new Draft(DraftType.LOAN, "text", null));
    }

    @Test
    void newDraft_withANullParent_isRejected() {
        assertThrows(NullPointerException.class,
                () -> new Draft(DraftType.LOAN, "text", Arrays.asList("customer-id", null)));
    }

    @Test
    void newDraft_withABlankParent_isRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Draft(DraftType.LOAN, "text", List.of(" ")));
    }

    @Test
    void newDraft_withTheSameParentTwice_isRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Draft(DraftType.COBORROWER, "text", List.of("loan-id", "loan-id")));
    }

    @Test
    void rebuiltDraft_thatDependsOnItself_isRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Draft("abc", DraftType.LOAN, "text", DraftStatus.PENDING, TIME, null, Map.of(),
                        List.of("abc")));
    }

    @Test
    void rebuiltDraft_withParents_keepsThem() {
        Draft draft = new Draft("abc", DraftType.LOAN, "text", DraftStatus.PENDING, TIME, null, Map.of(),
                List.of("customer-id"));

        assertEquals(List.of("customer-id"), draft.getDependsOn());
    }

    @Test
    void revise_keepsWhatTheDraftDependsOn() {
        Draft draft = new Draft(DraftType.LOAN, "text", List.of("customer-id"));

        draft.revise("new text");

        assertEquals(List.of("customer-id"), draft.getDependsOn());
    }
}
