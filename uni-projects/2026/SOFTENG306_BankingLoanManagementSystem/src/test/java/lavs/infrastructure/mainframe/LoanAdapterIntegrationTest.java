package lavs.infrastructure.mainframe;

import lavs.business.model.LoanSummary;
import lavs.business.model.RepaymentEntry;
import lavs.business.repository.RecordRejectedException;
import mainframe.client.messages.loan.Retrieve;
import mainframe.client.messages.loan.UpdateStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class LoanAdapterIntegrationTest extends MainframeIntegrationTestSupport {

    private static final String CUSTOMER_NUMBER =
            System.getProperty("mainframe.customerNumber", "01-0001-0000020");

    private static final String UNKNOWN_LOAN = "99-9999-9999999-999";

    private LoanAdapter adapter;
    private String loanNumber;

    @BeforeAll
    void setUpLoan() throws Exception {
        adapter = new LoanAdapter(connection);
        loanNumber = findOrCreateTestLoan(CUSTOMER_NUMBER);
    }

    @Test
    void findSummaryReturnsSummaryOfLoan() {
        Optional<LoanSummary> result = adapter.findSummary(loanNumber);

        LoanSummary summary = result.orElseThrow(() -> new AssertionError("No summary for " + loanNumber));
        assertEquals(loanNumber, summary.loanNumber());
        assertFalse(summary.customerName().isBlank());
        assertTrue(summary.numberOfPayments() > 0, "A loan should have payments");
        assertTrue(summary.totalAmountPaid().signum() > 0);
    }

    @Test
    void findSummaryOfUnknownLoanIsEmpty() {
        assertTrue(adapter.findSummary(UNKNOWN_LOAN).isEmpty());
    }

    @Test
    void findRepaymentScheduleReturnsEveryPaymentInOrder() {
        LoanSummary summary = adapter.findSummary(loanNumber).orElseThrow();

        List<RepaymentEntry> schedule = adapter.findRepaymentSchedule(loanNumber);

        assertEquals(summary.numberOfPayments(), schedule.size(),
                "The schedule should hold every payment, across all pages");
        for (int i = 0; i < schedule.size(); i++) {
            assertEquals(i + 1, schedule.get(i).paymentNumber());
        }
    }

    @Test
    void findRepaymentScheduleOfUnknownLoanIsEmpty() {
        assertTrue(adapter.findRepaymentSchedule(UNKNOWN_LOAN).isEmpty());
    }

    @Test
    void submitForCheckingMovesNewLoanToPending() throws Exception {
        // Uses its own loan: once submitted, a loan can't go back to New.
        String newLoan = createTestLoan(CUSTOMER_NUMBER);

        adapter.submitForChecking(newLoan);

        Retrieve retrieve = new Retrieve(newLoan);
        assertTrue(retrieve.send(connection).getWasSuccessful());
        assertEquals(UpdateStatus.STATUS_PENDING, retrieve.getStatus());

        assertThrows(RecordRejectedException.class, () -> adapter.submitForChecking(newLoan),
                "A loan that is already Pending should not be submitted again");
    }
}
