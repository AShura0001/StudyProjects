package lavs.business.repository;

import lavs.business.model.Draft;
import lavs.business.model.DraftType;
import lavs.business.repository.FakeDraftUploadGateway.Attempt;
import lavs.business.repository.FakeDraftUploadGateway.Outcome;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FakeDraftUploadGatewayTest {

    private final FakeDraftUploadGateway fake = new FakeDraftUploadGateway();

    @Test
    void upload_ofTwoCustomers_returnsNumbersThatCountUp() {
        String first = fake.upload(new Draft(DraftType.CUSTOMER, "a"), List.of());
        String second = fake.upload(new Draft(DraftType.CUSTOMER, "b"), List.of());

        assertEquals("CUS-1", first);
        assertEquals("CUS-2", second);
    }

    @Test
    void upload_ofDifferentTypes_countsEachTypeSeparately() {
        fake.upload(new Draft(DraftType.CUSTOMER, "a"), List.of());

        String loan = fake.upload(new Draft(DraftType.LOAN, "b"), List.of("CUS-1"));
        String coBorrower = fake.upload(new Draft(DraftType.COBORROWER, "c"), List.of("LOAN-1", "CUS-2"));

        assertEquals("LOAN-1", loan);
        assertEquals("COB-1", coBorrower);
    }

    @Test
    void upload_ofTheSameDraftTwice_createsTwoRecords() {
        Draft draft = new Draft(DraftType.LOAN, "a");

        String first = fake.upload(draft, List.of("CUS-1"));
        String second = fake.upload(draft, List.of("CUS-1"));

        assertNotEquals(first, second);
    }

    @Test
    void upload_withNullParentNumbers_isRejected() {
        assertThrows(NullPointerException.class,
                () -> fake.upload(new Draft(DraftType.CUSTOMER, "a"), null));
    }

    @Test
    void upload_whenOffline_throwsUnavailableAndRecordsTheAttempt() {
        Draft draft = new Draft(DraftType.CUSTOMER, "a");
        fake.goOffline();

        assertThrows(MainframeUnavailableException.class, () -> fake.upload(draft, List.of()));

        assertEquals(List.of(new Attempt(draft.getLocalId(), DraftType.CUSTOMER, List.of(),
                Outcome.UNREACHABLE, null)), fake.attempts());
    }

    @Test
    void upload_afterGoingBackOnline_isAcceptedAgain() {
        Draft draft = new Draft(DraftType.CUSTOMER, "a");
        fake.goOffline();
        assertThrows(MainframeUnavailableException.class, () -> fake.upload(draft, List.of()));

        fake.goOnline();

        assertEquals("CUS-1", fake.upload(draft, List.of()));
    }

    @Test
    void upload_whenDraftIsRejected_throwsWithTheFieldErrors() {
        Draft draft = new Draft(DraftType.LOAN, "a");
        Map<String, String> errors = Map.of("principal", "Must be greater than zero.");
        fake.rejectDraft(draft.getLocalId(), errors);

        RecordRejectedException thrown =
                assertThrows(RecordRejectedException.class, () -> fake.upload(draft, List.of("CUS-1")));

        assertEquals(errors, thrown.getFieldErrors());
        assertEquals(Outcome.REJECTED, fake.attempts().get(0).outcome());
    }

    @Test
    void upload_afterStopRejecting_isAcceptedAgain() {
        Draft draft = new Draft(DraftType.LOAN, "a");
        fake.rejectDraft(draft.getLocalId(), Map.of("rate", "Too high."));
        assertThrows(RecordRejectedException.class, () -> fake.upload(draft, List.of("CUS-1")));

        fake.stopRejecting(draft.getLocalId());

        assertEquals("LOAN-1", fake.upload(draft, List.of("CUS-1")));
    }

    @Test
    void upload_ofAnotherDraft_isNotAffectedByARejection() {
        Draft refused = new Draft(DraftType.LOAN, "a");
        Draft fine = new Draft(DraftType.LOAN, "b");
        fake.rejectDraft(refused.getLocalId(), Map.of("rate", "Too high."));

        assertEquals("LOAN-1", fake.upload(fine, List.of("CUS-1")));
    }

    @Test
    void attempts_afterSeveralUploads_keepOrderParentNumbersAndAssignedNumber() {
        Draft customer = new Draft(DraftType.CUSTOMER, "a");
        Draft loan = new Draft(DraftType.LOAN, "b");

        String customerNumber = fake.upload(customer, List.of());
        String loanNumber = fake.upload(loan, List.of(customerNumber));

        assertEquals(List.of(
                new Attempt(customer.getLocalId(), DraftType.CUSTOMER, List.of(), Outcome.ACCEPTED, customerNumber),
                new Attempt(loan.getLocalId(), DraftType.LOAN, List.of(customerNumber), Outcome.ACCEPTED, loanNumber)),
                fake.attempts());
    }

    @Test
    void acceptedDraftIds_afterAFailedAndASuccessfulAttempt_listsOnlyTheAccepted() {
        Draft draft = new Draft(DraftType.CUSTOMER, "a");
        fake.goOffline();
        assertThrows(MainframeUnavailableException.class, () -> fake.upload(draft, List.of()));
        fake.goOnline();
        fake.upload(draft, List.of());

        assertEquals(List.of(draft.getLocalId()), fake.acceptedDraftIds());
    }

    @Test
    void upload_whenOffline_doesNotUseUpANumber() {
        Draft draft = new Draft(DraftType.CUSTOMER, "a");
        fake.goOffline();
        assertThrows(MainframeUnavailableException.class, () -> fake.upload(draft, List.of()));
        fake.goOnline();

        assertEquals("CUS-1", fake.upload(draft, List.of()));
        assertNull(fake.attempts().get(0).assignedNumber());
    }

    @Test
    void attempts_whenTheReturnedListIsChanged_doesNotChangeTheDiary() {
        fake.upload(new Draft(DraftType.CUSTOMER, "a"), List.of());

        assertThrows(UnsupportedOperationException.class, () -> fake.attempts().clear());
        assertEquals(1, fake.attempts().size());
    }
}
