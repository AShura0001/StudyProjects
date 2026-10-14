package lavs.business.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DraftTest {

    @Test
    void newDraft_whenCreated_startsAsPending() {
        Draft draft = new Draft(DraftType.LOAN, "{\"principal\": 250000}");

        assertEquals(DraftStatus.PENDING, draft.getStatus());
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
        Instant earlier = Instant.parse("2026-10-01T09:30:00Z");

        Draft draft = new Draft("abc", DraftType.LOAN, "text", DraftStatus.SYNCED, earlier);

        assertEquals(earlier, draft.getCreatedAt());
    }

    @Test
    void rebuiltDraft_withNullCreationTime_isRejected() {
        assertThrows(NullPointerException.class,
                () -> new Draft("abc", DraftType.LOAN, "text", DraftStatus.PENDING, null));
    }

    @Test
    void setStatus_withNewStatus_changesTheStatus() {
        Draft draft = new Draft(DraftType.LOAN, "text");

        draft.setStatus(DraftStatus.SYNCED);

        assertEquals(DraftStatus.SYNCED, draft.getStatus());
    }
}
