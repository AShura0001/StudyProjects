package lavs.ui.service;

import java.util.List;
import java.util.Map;

/**
 * What the loan screen needs from the layer below. Every method blocks, so
 * callers must keep them off the JavaFX thread.
 */
public interface LoanRecords {

    record CoBorrower(String name, String customerNumber) {
    }

    record ScheduleEntry(int number, String dueDate, String principal, String interest,
                         String total, String status) {
    }

    record LoanDetails(String loanNumber, String loanName, String loanAmount,
                       String interestRate, String loanTerm, String startDate,
                       String maturityDate, String monthlyPayment, String outstandingBalance,
                       String statusText, int paymentsMade, int paymentsTotal,
                       String totalPaid, String principalPaid, String interestPaid,
                       String remainingBalance, String nextPaymentDue,
                       List<CoBorrower> coBorrowers, List<ScheduleEntry> schedule) {
    }

    /** @return the loan, or null when the number is unknown */
    LoanDetails details(String loanNumber);

    /**
     * @return field name to message; an empty map means the save was accepted
     * @throws NotSupportedYetException when saving is not connected yet
     */
    Map<String, String> save(LoanDetails loan);
}
