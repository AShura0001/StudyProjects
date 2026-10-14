package lavs.business.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
}
