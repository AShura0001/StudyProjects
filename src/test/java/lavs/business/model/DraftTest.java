package lavs.business.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
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

    private static Draft rejectedDraft() {
        Draft draft = new Draft(DraftType.LOAN, "text");
        draft.markRejected(ERRORS);
        return draft;
    }

    private static Draft syncedDraft() {
        Draft draft = new Draft(DraftType.LOAN, "text");
        draft.markSynced("LOAN-1");
        return draft;
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
        Draft draft = new Draft("abc", DraftType.LOAN, "text", DraftStatus.SYNCED, TIME, "LOAN-1", Map.of());

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
        Draft draft = new Draft("abc", DraftType.LOAN, "text", DraftStatus.SYNCED, TIME, "LOAN-7", Map.of());

        assertEquals("LOAN-7", draft.getServerNumber().orElseThrow());
    }

    @Test
    void rebuiltDraft_asRejectedWithErrors_keepsTheErrors() {
        Draft draft = new Draft("abc", DraftType.LOAN, "text", DraftStatus.REJECTED, TIME, null, ERRORS);

        assertEquals(ERRORS, draft.getErrors());
    }

    @Test
    void rebuiltDraft_asPendingWithANumber_isRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Draft("abc", DraftType.LOAN, "text", DraftStatus.PENDING, TIME, "LOAN-1", Map.of()));
    }

    @Test
    void rebuiltDraft_asPendingWithErrors_isRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Draft("abc", DraftType.LOAN, "text", DraftStatus.PENDING, TIME, null, ERRORS));
    }

    @Test
    void rebuiltDraft_asSyncedWithoutANumber_isRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Draft("abc", DraftType.LOAN, "text", DraftStatus.SYNCED, TIME, null, Map.of()));
    }

    @Test
    void rebuiltDraft_asSyncedWithABlankNumber_isRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Draft("abc", DraftType.LOAN, "text", DraftStatus.SYNCED, TIME, " ", Map.of()));
    }

    @Test
    void rebuiltDraft_asSyncedWithErrors_isRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Draft("abc", DraftType.LOAN, "text", DraftStatus.SYNCED, TIME, "LOAN-1", ERRORS));
    }

    @Test
    void rebuiltDraft_asRejectedWithoutErrors_isRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Draft("abc", DraftType.LOAN, "text", DraftStatus.REJECTED, TIME, null, Map.of()));
    }

    @Test
    void rebuiltDraft_asRejectedWithANumber_isRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Draft("abc", DraftType.LOAN, "text", DraftStatus.REJECTED, TIME, "LOAN-1", ERRORS));
    }

    @Test
    void markSynced_onPendingDraft_storesTheNumberAndChangesStatus() {
        Draft draft = new Draft(DraftType.LOAN, "text");

        draft.markSynced("LOAN-1");

        assertEquals(DraftStatus.SYNCED, draft.getStatus());
        assertEquals("LOAN-1", draft.getServerNumber().orElseThrow());
    }

    @Test
    void markSynced_withNullNumber_isRejected() {
        Draft draft = new Draft(DraftType.LOAN, "text");

        assertThrows(NullPointerException.class, () -> draft.markSynced(null));
    }

    @Test
    void markSynced_withBlankNumber_isRejected() {
        Draft draft = new Draft(DraftType.LOAN, "text");

        assertThrows(IllegalArgumentException.class, () -> draft.markSynced("  "));
        assertEquals(DraftStatus.PENDING, draft.getStatus());
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
    void markRejected_onPendingDraft_storesTheErrorsAndChangesStatus() {
        Draft draft = new Draft(DraftType.LOAN, "text");

        draft.markRejected(ERRORS);

        assertEquals(DraftStatus.REJECTED, draft.getStatus());
        assertEquals(ERRORS, draft.getErrors());
        assertTrue(draft.getServerNumber().isEmpty());
    }

    @Test
    void markRejected_withManyErrors_keepsTheOrderTheyWereGiven() {
        Draft draft = new Draft(DraftType.LOAN, "text");
        Map<String, String> errors = new LinkedHashMap<>();
        errors.put("startDate", "Bad date.");
        errors.put("principal", "Too small.");
        errors.put("rate", "Too high.");

        draft.markRejected(errors);

        assertEquals(List.of("startDate", "principal", "rate"), List.copyOf(draft.getErrors().keySet()));
    }

    @Test
    void markRejected_whenTheOriginalMapChangesLater_isNotAffected() {
        Draft draft = new Draft(DraftType.LOAN, "text");
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
        Draft draft = new Draft(DraftType.LOAN, "text");

        assertThrows(IllegalArgumentException.class, () -> draft.markRejected(Map.of()));
        assertEquals(DraftStatus.PENDING, draft.getStatus());
    }

    @Test
    void markRejected_withNullErrors_isRejected() {
        Draft draft = new Draft(DraftType.LOAN, "text");

        assertThrows(NullPointerException.class, () -> draft.markRejected(null));
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
    void revise_withNullPayload_isRejected() {
        Draft draft = new Draft(DraftType.LOAN, "text");

        assertThrows(NullPointerException.class, () -> draft.revise(null));
    }
}
