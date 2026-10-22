package lavs.business.service;

import lavs.business.model.LoanPayment;
import lavs.business.model.LoanRecord;
import lavs.business.model.RepaymentEntry;
import lavs.business.model.RepaymentProgress;
import lavs.business.model.RepaymentStatus;
import lavs.business.repository.LoanCoBorrowerGateway;
import lavs.business.repository.LoanGateway;
import lavs.business.repository.LoanPaymentGateway;
import lavs.business.repository.PageResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Which repayments count as paid, and the totals worked out from them. */
class LoanAccountServiceProgressTest {

    private static RepaymentEntry entry(int number, String principal, String interest) {
        return new RepaymentEntry(number, LocalDate.of(2026, number, 1),
                new BigDecimal(principal).add(new BigDecimal(interest)),
                new BigDecimal(principal), new BigDecimal(interest), BigDecimal.ZERO);
    }

    private static final List<RepaymentEntry> SCHEDULE = List.of(
            entry(1, "300", "200"), entry(2, "310", "190"), entry(3, "320", "180"));

    private static List<RepaymentStatus> statuses(RepaymentProgress progress) {
        return progress.repayments().stream().map(RepaymentProgress.Repayment::status).toList();
    }

    @Test
    void theFirstRecordedPaymentsArePaid_theNextIsDue_theRestUpcoming() {
        RepaymentProgress progress = LoanAccountService.progress(new BigDecimal("10000"), SCHEDULE, 1);

        assertEquals(List.of(RepaymentStatus.PAID, RepaymentStatus.DUE, RepaymentStatus.UPCOMING), statuses(progress));
        assertEquals(1, progress.paymentsMade());
        assertEquals(3, progress.paymentsTotal());
        assertEquals(LocalDate.of(2026, 2, 1), progress.nextDue());
    }

    @Test
    void totalsCountOnlyThePaidRepayments() {
        RepaymentProgress progress = LoanAccountService.progress(new BigDecimal("10000"), SCHEDULE, 2);

        assertEquals(0, new BigDecimal("610").compareTo(progress.principalPaid()));
        assertEquals(0, new BigDecimal("390").compareTo(progress.interestPaid()));
        assertEquals(0, new BigDecimal("1000").compareTo(progress.totalPaid()));
        assertEquals(0, new BigDecimal("9390").compareTo(progress.outstanding()));
    }

    @Test
    void nothingPaid_theFirstIsDue() {
        RepaymentProgress progress = LoanAccountService.progress(new BigDecimal("10000"), SCHEDULE, 0);

        assertEquals(List.of(RepaymentStatus.DUE, RepaymentStatus.UPCOMING, RepaymentStatus.UPCOMING), statuses(progress));
        assertEquals(0, progress.paymentsMade());
        assertEquals(0, BigDecimal.ZERO.compareTo(progress.totalPaid()));
    }

    @Test
    void morePaymentsThanRepayments_allArePaid_andNothingIsDue() {
        RepaymentProgress progress = LoanAccountService.progress(new BigDecimal("500"), SCHEDULE, 7);

        assertEquals(List.of(RepaymentStatus.PAID, RepaymentStatus.PAID, RepaymentStatus.PAID), statuses(progress));
        assertEquals(3, progress.paymentsMade());
        assertTrue(progress.nextDueDate().isEmpty());
        assertEquals(0, BigDecimal.ZERO.compareTo(progress.outstanding()), "never below zero");
    }

    @Test
    void anEmptySchedule_orAMissingPrincipal_isHandled() {
        RepaymentProgress empty = LoanAccountService.progress(null, List.of(), 2);

        assertEquals(0, empty.paymentsTotal());
        assertEquals(0, empty.paymentsMade());
        assertTrue(empty.nextDueDate().isEmpty());
        assertEquals(0, BigDecimal.ZERO.compareTo(empty.outstanding()));
    }

    @Test
    void progressOf_readsTheScheduleAndTheRecordedPaymentsFromTheMainframe() {
        LoanGateway loans = mock(LoanGateway.class);
        LoanPaymentGateway payments = mock(LoanPaymentGateway.class);
        when(loans.findRepaymentSchedule("LN-1")).thenReturn(SCHEDULE);
        when(payments.listPayments(eq("LN-1"), eq(1L), anyLong()))
                .thenReturn(new PageResult<LoanPayment>(List.of(), 2, 1));
        LoanAccountService service = new LoanAccountService(loans, mock(LoanCoBorrowerGateway.class), payments,
                new BasicLoanValidator());
        LoanRecord loan = new LoanRecord("LN-1", "CUS-1", "Jane Doe", "Active", new BigDecimal("10000"),
                "Fixed", new BigDecimal("5"), "Yearly", new BigDecimal("500"), "Monthly",
                LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1));

        RepaymentProgress progress = service.progressOf(loan);

        assertEquals(List.of(RepaymentStatus.PAID, RepaymentStatus.PAID, RepaymentStatus.DUE), statuses(progress));
        assertEquals(LocalDate.of(2026, 3, 1), progress.nextDue());
    }
}
