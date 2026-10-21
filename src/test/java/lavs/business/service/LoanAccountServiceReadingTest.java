package lavs.business.service;

import lavs.business.model.CoBorrowerType;
import lavs.business.model.LoanListing;
import lavs.business.model.LoanPayment;
import lavs.business.model.LoanRecord;
import lavs.business.model.RepaymentEntry;
import lavs.business.repository.LoanCoBorrowerGateway;
import lavs.business.repository.LoanGateway;
import lavs.business.repository.LoanPaymentGateway;
import lavs.business.repository.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The read-only and removal parts of {@link LoanAccountService}. */
class LoanAccountServiceReadingTest {

    private LoanGateway loans;
    private LoanCoBorrowerGateway coBorrowers;
    private LoanPaymentGateway payments;
    private LoanAccountService service;

    @BeforeEach
    void setUp() {
        loans = mock(LoanGateway.class);
        coBorrowers = mock(LoanCoBorrowerGateway.class);
        payments = mock(LoanPaymentGateway.class);
        service = new LoanAccountService(loans, coBorrowers, payments, new BasicLoanValidator());
    }

    @Test
    void loansOf_returnsWhatTheMainframeListsForTheCustomer() {
        LoanListing listing = new LoanListing("LN-1", new BigDecimal("250000"), LocalDate.of(2025, 1, 1), "Active");
        when(loans.listLoans("CUS-1")).thenReturn(List.of(listing));

        assertEquals(List.of(listing), service.loansOf("CUS-1"));
    }

    @Test
    void find_returnsTheLoan_andEmptyForAnUnknownNumber() {
        LoanRecord loan = new LoanRecord("LN-1", "CUS-1", "Jane Doe", "Active", new BigDecimal("250000"), "Fixed",
                new BigDecimal("5.85"), "Yearly", new BigDecimal("1450"), "Monthly",
                LocalDate.of(2025, 1, 1), LocalDate.of(2030, 1, 1));
        when(loans.findLoan("LN-1")).thenReturn(Optional.of(loan));

        assertEquals(Optional.of(loan), service.find("LN-1"));
        assertTrue(service.find("LN-404").isEmpty());
    }

    @Test
    void scheduleOf_returnsTheRepaymentsInOrder() {
        List<RepaymentEntry> schedule = List.of(mock(RepaymentEntry.class));
        when(loans.findRepaymentSchedule("LN-1")).thenReturn(schedule);

        assertEquals(schedule, service.scheduleOf("LN-1"));
    }

    @Test
    void paymentsMade_isTheTotalTheMainframeReports() {
        when(payments.listPayments("LN-1", 1, 1)).thenReturn(new PageResult<LoanPayment>(List.of(), 12, 1));

        assertEquals(12L, service.paymentsMade("LN-1"));
    }

    @Test
    void removeCoBorrower_asksTheMainframeToDeleteThem() {
        service.removeCoBorrower("LN-1", 7L);

        verify(coBorrowers).deleteCoBorrower("LN-1", 7L);
    }

    @Test
    void addCoBorrower_toAnUnknownLoan_isRefused() {
        when(loans.findLoan("LN-404")).thenReturn(Optional.empty());

        LoanRuleException refused = assertThrows(LoanRuleException.class,
                () -> service.addCoBorrower("LN-404", "CUS-2", CoBorrowerType.COBORROWER));

        assertEquals(LoanService.LOAN_NOT_FOUND_MESSAGE, refused.getProblems().get(LoanRuleException.GENERAL));
    }

    @Test
    void aBlankLoanNumber_isRejectedBeforeAskingTheMainframe() {
        assertThrows(IllegalArgumentException.class, () -> service.scheduleOf("  "));
        assertThrows(NullPointerException.class, () -> service.scheduleOf(null));
    }
}
