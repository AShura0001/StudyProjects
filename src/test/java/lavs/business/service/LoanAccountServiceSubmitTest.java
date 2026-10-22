package lavs.business.service;

import lavs.business.model.CoBorrower;
import lavs.business.model.CoBorrowerType;
import lavs.business.model.CustomerProfile;
import lavs.business.model.LoanRecord;
import lavs.business.repository.LoanCoBorrowerGateway;
import lavs.business.repository.LoanGateway;
import lavs.business.repository.LoanPaymentGateway;
import lavs.business.repository.MainframeUnavailableException;
import lavs.business.repository.PageResult;
import lavs.business.repository.RecordRejectedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Moving a loan from New to Pending: the rules that must hold first. */
class LoanAccountServiceSubmitTest {

    private static final LoanRecord NEW_LOAN = loan("New", new BigDecimal("250000"));
    private static final LoanRecord PENDING_LOAN = loan("Pending", new BigDecimal("250000"));
    private static final CustomerProfile COMPLETE = new CustomerProfile(true, true, true);

    private LoanGateway loans;
    private LoanCoBorrowerGateway coBorrowers;
    private CustomerProfileSource profiles;
    private LoanAccountService service;

    private static LoanRecord loan(String status, BigDecimal principal) {
        return new LoanRecord("LN-1", "CUS-1", "Jane Doe", status, principal, "Fixed",
                new BigDecimal("5.85"), "Yearly", new BigDecimal("1450"), "Monthly",
                LocalDate.of(2025, 1, 1), LocalDate.of(2030, 1, 1));
    }

    @BeforeEach
    void setUp() {
        loans = mock(LoanGateway.class);
        coBorrowers = mock(LoanCoBorrowerGateway.class);
        profiles = mock(CustomerProfileSource.class);
        service = new LoanAccountService(loans, coBorrowers, mock(LoanPaymentGateway.class),
                new BasicLoanValidator(), profiles);
        when(loans.findLoan("LN-1")).thenReturn(Optional.of(NEW_LOAN));
        when(profiles.profileOf(any())).thenReturn(Optional.of(COMPLETE));
        withCoBorrowers();
    }

    private void withCoBorrowers(CoBorrower... people) {
        when(coBorrowers.listCoBorrowers(eq("LN-1"), anyLong(), anyLong()))
                .thenReturn(new PageResult<CoBorrower>(List.of(people), people.length, 1));
    }

    private Map<String, String> problemsOf(String loanNumber) {
        return assertThrows(LoanRuleException.class, () -> service.submitForChecking(loanNumber)).getProblems();
    }

    @Test
    void submitForChecking_ofACompleteLoan_movesItToPendingAndReturnsTheNewVersion() {
        when(loans.findLoan("LN-1")).thenReturn(Optional.of(NEW_LOAN), Optional.of(PENDING_LOAN));

        LoanRecord result = service.submitForChecking("LN-1");

        verify(loans).submitForChecking("LN-1");
        assertEquals("Pending", result.status());
    }

    @Test
    void submitForChecking_whenTheStatusIsNotNew_isRefusedAndSendsNothing() {
        when(loans.findLoan("LN-1")).thenReturn(Optional.of(loan("Active", new BigDecimal("250000"))));

        Map<String, String> problems = problemsOf("LN-1");

        assertEquals(LoanAccountService.STATUS_MESSAGE, problems.get(LoanAccountService.STATUS_FIELD));
        verify(loans, never()).submitForChecking(any());
    }

    @Test
    void submitForChecking_acceptsTheStatusInAnyCase() {
        when(loans.findLoan("LN-1")).thenReturn(Optional.of(loan("NEW", new BigDecimal("250000"))));

        service.submitForChecking("LN-1");

        verify(loans).submitForChecking("LN-1");
    }

    @Test
    void submitForChecking_whenTheMainframeLeftADetailOut_saysTheLoanIsIncomplete() {
        LoanRecord partial = new LoanRecord("LN-1", "CUS-1", "Jane Doe", "New", null, "Fixed",
                new BigDecimal("5.85"), "Yearly", new BigDecimal("1450"), "Monthly",
                LocalDate.of(2025, 1, 1), LocalDate.of(2030, 1, 1));
        when(loans.findLoan("LN-1")).thenReturn(Optional.of(partial));

        Map<String, String> problems = problemsOf("LN-1");

        assertEquals(LoanAccountService.DETAILS_MESSAGE, problems.get(LoanAccountService.DETAILS_FIELD));
        verify(loans, never()).submitForChecking(any());
    }

    @Test
    void submitForChecking_whenAValueBreaksARule_reportsIt() {
        when(loans.findLoan("LN-1")).thenReturn(Optional.of(loan("New", BigDecimal.ZERO)));

        Map<String, String> problems = problemsOf("LN-1");

        assertEquals(BasicLoanValidator.PRINCIPAL_MESSAGE, problems.get("principal"));
    }

    @Test
    void submitForChecking_whenTheCustomerHasNoMailingAddress_isRefused() {
        when(profiles.profileOf(any())).thenReturn(Optional.of(new CustomerProfile(false, true, true)));

        Map<String, String> problems = problemsOf("LN-1");

        assertEquals(LoanService.MAILING_ADDRESS_MESSAGE, problems.get(LoanService.MAILING_ADDRESS_FIELD));
        assertEquals(1, problems.size());
    }

    @Test
    void submitForChecking_whenTheCustomerHasNeitherPhoneNorEmail_isRefused() {
        when(profiles.profileOf(any())).thenReturn(Optional.of(new CustomerProfile(true, false, true)));

        Map<String, String> problems = problemsOf("LN-1");

        assertEquals(LoanService.CONTACT_METHOD_MESSAGE, problems.get(LoanService.CONTACT_METHOD_FIELD));
        assertEquals(1, problems.size());
    }

    @Test
    void submitForChecking_whenTheCustomerHasNoEmployerAndNoGuarantor_isRefused() {
        when(profiles.profileOf(any())).thenReturn(Optional.of(new CustomerProfile(true, true, false)));

        Map<String, String> problems = problemsOf("LN-1");

        assertEquals(LoanAccountService.GUARANTOR_MESSAGE, problems.get(LoanAccountService.GUARANTOR_FIELD));
        verify(loans, never()).submitForChecking(any());
    }

    @Test
    void submitForChecking_whenTheCustomerHasNoEmployerButAGuarantor_goesThrough() {
        when(profiles.profileOf(any())).thenReturn(Optional.of(new CustomerProfile(true, true, false)));
        withCoBorrowers(new CoBorrower(7, "LN-1", "CUS-2", "Sam Lee", CoBorrowerType.GUARANTOR));

        service.submitForChecking("LN-1");

        verify(loans).submitForChecking("LN-1");
    }

    @Test
    void submitForChecking_aCoBorrowerAloneDoesNotReplaceTheGuarantor() {
        when(profiles.profileOf(any())).thenReturn(Optional.of(new CustomerProfile(true, true, false)));
        withCoBorrowers(new CoBorrower(7, "LN-1", "CUS-2", "Sam Lee", CoBorrowerType.COBORROWER));

        assertTrue(problemsOf("LN-1").containsKey(LoanAccountService.GUARANTOR_FIELD));
    }

    @Test
    void submitForChecking_whenTheCustomerHasAnEmployer_needsNoGuarantor() {
        service.submitForChecking("LN-1");

        verify(loans).submitForChecking("LN-1");
        verify(coBorrowers, never()).listCoBorrowers(any(), anyLong(), anyLong());
    }

    @Test
    void submitForChecking_reportsEveryProblemAtOnce() {
        when(loans.findLoan("LN-1")).thenReturn(Optional.of(loan("Active", BigDecimal.ZERO)));
        when(profiles.profileOf(any())).thenReturn(Optional.of(new CustomerProfile(false, false, false)));

        Map<String, String> problems = problemsOf("LN-1");

        assertEquals(List.of(LoanAccountService.STATUS_FIELD, "principal", LoanService.MAILING_ADDRESS_FIELD,
                LoanService.CONTACT_METHOD_FIELD, LoanAccountService.GUARANTOR_FIELD),
                List.copyOf(problems.keySet()));
    }

    @Test
    void submitForChecking_ofAnUnknownLoan_isRefused() {
        when(loans.findLoan("LN-404")).thenReturn(Optional.empty());

        Map<String, String> problems = problemsOf("LN-404");

        assertEquals(LoanService.LOAN_NOT_FOUND_MESSAGE, problems.get(LoanRuleException.GENERAL));
    }

    @Test
    void submitForChecking_whenTheLoanHasNoCustomerNumber_saysTheCustomerWasNotFound() {
        when(loans.findLoan("LN-1")).thenReturn(Optional.of(new LoanRecord("LN-1", null, null, "New",
                new BigDecimal("1"), "Fixed", new BigDecimal("1"), "Yearly", new BigDecimal("1"), "Monthly",
                LocalDate.of(2025, 1, 1), LocalDate.of(2030, 1, 1))));

        assertEquals(LoanService.CUSTOMER_NOT_FOUND_MESSAGE, problemsOf("LN-1").get(LoanRuleException.GENERAL));
    }

    @Test
    void submitForChecking_whenTheCustomerCannotBeFound_isRefused() {
        when(profiles.profileOf(any())).thenReturn(Optional.empty());

        assertEquals(LoanService.CUSTOMER_NOT_FOUND_MESSAGE, problemsOf("LN-1").get(LoanRuleException.GENERAL));
    }

    @Test
    void submitForChecking_whenTheMainframeCannotBeReachedForTheCustomer_givesTheOfflineMessage() {
        when(profiles.profileOf(any())).thenThrow(new MainframeUnavailableException("socket closed"));

        MainframeUnavailableException offline = assertThrows(MainframeUnavailableException.class,
                () -> service.submitForChecking("LN-1"));

        assertEquals(LoanAccountService.OFFLINE_MESSAGE, offline.getMessage());
    }

    @Test
    void submitForChecking_whenTheMainframeCannotBeReachedForTheChange_givesTheOfflineMessage() {
        doThrow(new MainframeUnavailableException("socket closed")).when(loans).submitForChecking("LN-1");

        MainframeUnavailableException offline = assertThrows(MainframeUnavailableException.class,
                () -> service.submitForChecking("LN-1"));

        assertEquals(LoanAccountService.OFFLINE_MESSAGE, offline.getMessage());
    }

    @Test
    void submitForChecking_whenTheMainframeRefusesTheChange_passesItsReasonOn() {
        doThrow(new RecordRejectedException("The loan cannot change status.", Map.of()))
                .when(loans).submitForChecking("LN-1");

        RecordRejectedException refused = assertThrows(RecordRejectedException.class,
                () -> service.submitForChecking("LN-1"));

        assertEquals("The loan cannot change status.", refused.getMessage());
    }

    @Test
    void submitForChecking_withABlankLoanNumber_isRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.submitForChecking(" "));
        assertThrows(NullPointerException.class, () -> service.submitForChecking(null));
    }

    @Test
    void newService_withoutAProfileSource_isRejected() {
        assertThrows(NullPointerException.class, () -> new LoanAccountService(loans, coBorrowers,
                mock(LoanPaymentGateway.class), new BasicLoanValidator(), null));
    }
}
