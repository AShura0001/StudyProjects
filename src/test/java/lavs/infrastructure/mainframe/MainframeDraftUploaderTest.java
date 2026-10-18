package lavs.infrastructure.mainframe;

import lavs.business.model.CoBorrowerType;
import lavs.business.model.CustomerReference;
import lavs.business.model.Draft;
import lavs.business.model.DraftType;
import lavs.business.model.LoanDetails;
import lavs.business.model.LoanPayloads;
import lavs.business.repository.MainframeUnavailableException;
import lavs.business.repository.RecordRejectedException;
import lavs.business.repository.UploadOutcomeUnknownException;
import mainframe.client.MessageRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainframeDraftUploaderTest {

    private static final LoanDetails DETAILS = new LoanDetails(new BigDecimal("250000.00"), "Fixed",
            new BigDecimal("5.85"), "Yearly", new BigDecimal("1450.00"), "Monthly",
            LocalDate.of(2026, 10, 1), LocalDate.of(2031, 10, 1));
    private static final CustomerReference EXISTING = CustomerReference.existing("01-0001-0000020");
    private static final CustomerReference DRAFT_CUSTOMER = CustomerReference.draft("draft-customer");

    private final ScriptedConnection connection = new ScriptedConnection();
    private final MainframeDraftUploader uploader = new MainframeDraftUploader(connection);

    private static Draft loanDraft(CustomerReference customer) {
        return new Draft(DraftType.LOAN, LoanPayloads.loanPayload(customer, DETAILS));
    }

    private static Draft coBorrowerDraft(CoBorrowerType type, CustomerReference customer) {
        return new Draft(DraftType.COBORROWER, LoanPayloads.coBorrowerPayload(type, customer));
    }

    private void loanReplyWith(String loanNumber) {
        connection.replySuccess("loanNumber", loanNumber, "customerNumber", "01-0001-0000020", "rateType", "Fixed",
                "rateAmount", "5.85", "rateFrequency", "Yearly", "paymentAmount", "1450.0",
                "paymentFrequency", "Monthly", "principal", "250000.0", "startDate", "2026-10-01",
                "renewalDate", "2031-10-01", "status", "New");
    }

    private void coBorrowerReplyWith(String id) {
        connection.replySuccess("coborrowerNumber", "C-2", "id", id, "loanNumber", "LN-1", "type", "Coborrower");
    }

    @Test
    void newUploader_withANullConnection_isRejected() {
        assertThrows(NullPointerException.class, () -> new MainframeDraftUploader(null));
    }

    @Test
    void upload_ofALoanForAnExistingCustomer_sendsTheLoanAndReturnsTheLoanNumber() {
        loanReplyWith("LN-7");

        String number = uploader.upload(loanDraft(EXISTING), List.of());

        assertEquals("LN-7", number);
        MessageRequest sent = connection.requests().get(0);
        assertEquals("loan.create", sent.getRequestType());
        assertEquals("01-0001-0000020", sent.getValue("customerNumber"));
        assertEquals("250000.0", sent.getValue("principal"));
        assertEquals("1450.0", sent.getValue("paymentAmount"));
        assertEquals("5.85", sent.getValue("rateAmount"));
        assertEquals("Fixed", sent.getValue("rateType"));
        assertEquals("Yearly", sent.getValue("rateFrequency"));
        assertEquals("Monthly", sent.getValue("paymentFrequency"));
        assertEquals("2026-10-01", sent.getValue("startDate"));
        assertEquals("2031-10-01", sent.getValue("renewalDate"));
    }

    @Test
    void upload_ofALoanForACustomerThatWasADraft_usesTheCustomersNumberItWasGiven() {
        loanReplyWith("LN-7");

        uploader.upload(loanDraft(DRAFT_CUSTOMER), List.of("CUS-42"));

        assertEquals("CUS-42", connection.requests().get(0).getValue("customerNumber"));
    }

    @Test
    void upload_checksTheMainframeCanBeReachedBeforeSendingAnything() {
        loanReplyWith("LN-7");

        uploader.upload(loanDraft(EXISTING), List.of());

        assertEquals(1, connection.pings());
        assertEquals(1, connection.requests().size());
    }

    @Test
    void upload_whenTheMainframeCannotBeReached_sendsNothingAndSaysSo() {
        connection.setConnected(false);

        MainframeUnavailableException thrown = assertThrows(MainframeUnavailableException.class,
                () -> uploader.upload(loanDraft(EXISTING), List.of()));

        assertEquals(MainframeDraftUploader.UNREACHABLE_MESSAGE, thrown.getMessage());
        assertTrue(connection.requests().isEmpty());
    }

    @Test
    void upload_whenTheMainframeRefuses_reportsItsReasonAsARefusal() {
        connection.replyRefused("The interest rate is not allowed.");

        RecordRejectedException thrown = assertThrows(RecordRejectedException.class,
                () -> uploader.upload(loanDraft(EXISTING), List.of()));

        assertEquals("The interest rate is not allowed.", thrown.getMessage());
        assertTrue(thrown.getFieldErrors().isEmpty());
    }

    @Test
    void upload_whenTheConnectionFailsAfterTheCheckPassed_reportsAnUnknownOutcome() {
        connection.replyUnreachable();

        assertThrows(UploadOutcomeUnknownException.class, () -> uploader.upload(loanDraft(EXISTING), List.of()));
    }

    @Test
    void upload_whenTheReplyCannotBeRead_reportsAnUnknownOutcome() {
        connection.replyUnreadable();

        assertThrows(UploadOutcomeUnknownException.class, () -> uploader.upload(loanDraft(EXISTING), List.of()));
    }

    @Test
    void upload_whenTheSuccessfulReplyIsIncomplete_reportsAnUnknownOutcomeAndKeepsTheCause() {
        connection.replySuccess("loanNumber", "LN-7");

        UploadOutcomeUnknownException thrown = assertThrows(UploadOutcomeUnknownException.class,
                () -> uploader.upload(loanDraft(EXISTING), List.of()));

        assertNotNull(thrown.getCause());
    }

    @Test
    void upload_whenTheSuccessfulReplyHasNoLoanNumber_reportsAnUnknownOutcome() {
        connection.replySuccess("customerNumber", "01-0001-0000020", "rateType", "Fixed", "rateAmount", "5.85",
                "rateFrequency", "Yearly", "paymentAmount", "1450.0", "paymentFrequency", "Monthly",
                "principal", "250000.0", "startDate", "2026-10-01", "renewalDate", "2031-10-01", "status", "New");

        assertThrows(UploadOutcomeUnknownException.class, () -> uploader.upload(loanDraft(EXISTING), List.of()));
    }

    @Test
    void upload_ofALoanForADraftCustomerWithoutItsNumber_isRefusedWithoutContactingTheMainframe() {
        RecordRejectedException thrown = assertThrows(RecordRejectedException.class,
                () -> uploader.upload(loanDraft(DRAFT_CUSTOMER), List.of()));

        assertEquals(MainframeDraftUploader.MISSING_PARENT_MESSAGE, thrown.getMessage());
        assertEquals(0, connection.pings());
        assertTrue(connection.requests().isEmpty());
    }

    @Test
    void upload_ofADraftWhoseTextCannotBeRead_isRefusedWithoutContactingTheMainframe() {
        Draft garbage = new Draft(DraftType.LOAN, "this is not a loan");

        RecordRejectedException thrown =
                assertThrows(RecordRejectedException.class, () -> uploader.upload(garbage, List.of()));

        assertEquals(MainframeDraftUploader.UNREADABLE_DRAFT_MESSAGE, thrown.getMessage());
        assertEquals(0, connection.pings());
    }

    @Test
    void upload_ofACustomer_isRefusedBecauseCustomerDraftsAreNotSupportedYet() {
        Draft customer = new Draft(DraftType.CUSTOMER, "any text");

        RecordRejectedException thrown =
                assertThrows(RecordRejectedException.class, () -> uploader.upload(customer, List.of()));

        assertEquals(MainframeDraftUploader.CUSTOMER_NOT_SUPPORTED_MESSAGE, thrown.getMessage());
        assertEquals(0, connection.pings());
        assertTrue(connection.requests().isEmpty());
    }

    @Test
    void upload_ofACoBorrowerForAnExistingCustomer_attachesThemToTheLoan() {
        coBorrowerReplyWith("5");

        String number = uploader.upload(coBorrowerDraft(CoBorrowerType.COBORROWER, CustomerReference.existing("C-2")),
                List.of("LN-1"));

        assertEquals("5", number);
        MessageRequest sent = connection.requests().get(0);
        assertEquals("loan.addCoborrower", sent.getRequestType());
        assertEquals("C-2", sent.getValue("coborrowerNumber"));
        assertEquals("LN-1", sent.getValue("loanNumber"));
        assertEquals("Coborrower", sent.getValue("type"));
    }

    @Test
    void upload_ofAGuarantor_usesTheGuarantorType() {
        coBorrowerReplyWith("6");

        uploader.upload(coBorrowerDraft(CoBorrowerType.GUARANTOR, CustomerReference.existing("C-2")),
                List.of("LN-1"));

        assertEquals("Guarantor", connection.requests().get(0).getValue("type"));
    }

    @Test
    void upload_ofACoBorrowerWhoWasADraftCustomer_usesTheLoanThenTheCustomerNumbers() {
        coBorrowerReplyWith("5");

        uploader.upload(coBorrowerDraft(CoBorrowerType.COBORROWER, DRAFT_CUSTOMER), List.of("LN-1", "CUS-9"));

        assertEquals("LN-1", connection.requests().get(0).getValue("loanNumber"));
        assertEquals("CUS-9", connection.requests().get(0).getValue("coborrowerNumber"));
    }

    @Test
    void upload_ofACoBorrowerWithoutTheLoansNumber_isRefusedWithoutContactingTheMainframe() {
        RecordRejectedException thrown = assertThrows(RecordRejectedException.class,
                () -> uploader.upload(coBorrowerDraft(CoBorrowerType.COBORROWER, EXISTING), List.of()));

        assertEquals(MainframeDraftUploader.MISSING_PARENT_MESSAGE, thrown.getMessage());
        assertTrue(connection.requests().isEmpty());
    }

    @Test
    void upload_ofADraftCoBorrowerWithoutTheirCustomersNumber_isRefused() {
        assertThrows(RecordRejectedException.class,
                () -> uploader.upload(coBorrowerDraft(CoBorrowerType.COBORROWER, DRAFT_CUSTOMER), List.of("LN-1")));
    }

    @Test
    void upload_ofACoBorrowerDraftWhoseTextCannotBeRead_isRefused() {
        Draft garbage = new Draft(DraftType.COBORROWER, "{}");

        assertThrows(RecordRejectedException.class, () -> uploader.upload(garbage, List.of("LN-1")));
    }

    @Test
    void upload_ofACoBorrowerWhenTheReplyHasNoId_reportsAnUnknownOutcome() {
        connection.replySuccess("coborrowerNumber", "C-2", "loanNumber", "LN-1", "type", "Coborrower");

        assertThrows(UploadOutcomeUnknownException.class, () -> uploader.upload(
                coBorrowerDraft(CoBorrowerType.COBORROWER, CustomerReference.existing("C-2")), List.of("LN-1")));
    }

    @Test
    void upload_withNullArguments_isRejected() {
        assertThrows(NullPointerException.class, () -> uploader.upload(null, List.of()));
        assertThrows(NullPointerException.class, () -> uploader.upload(loanDraft(EXISTING), null));
    }
}
