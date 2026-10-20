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

    /** The fields of the Add loan form; everything arrives trimmed. */
    record NewLoan(String principal, String rateType, String rateAmount, String rateFrequency,
                   String paymentAmount, String paymentFrequency, String startDate, String renewalDate) {
    }

    /** Either the new loan number, or field name to message. */
    record Created(String loanNumber, Map<String, String> errors) {
    }

    /** @return field name to message; an empty map means the co-borrower was added */
    Map<String, String> addCoBorrower(String loanNumber, String coBorrowerNumber, String type);

    Created createLoan(String customerNumber, NewLoan loan);

    /** One repayment: its number, due date (2025-01-09) and total as plain digits (100.00). */
    record PaymentDetails(int number, String date, String amount) {
    }

    /**
     * @return field name to message; an empty map means the change was accepted
     * @throws NotSupportedYetException when saving is not connected yet
     */
    Map<String, String> savePayment(String loanNumber, PaymentDetails payment);

    /** The terms the repayment schedule is worked out from. */
    record ScheduleTerms(String paymentAmount, String paymentFrequency, String renewalDate) {
    }

    /** @return the loan's schedule terms, or null when the loan is unknown */
    ScheduleTerms scheduleTerms(String loanNumber);

    /**
     * @return field name to message; an empty map means the change was accepted
     * @throws NotSupportedYetException when saving is not connected yet
     */
    Map<String, String> saveScheduleTerms(String loanNumber, ScheduleTerms terms);
}
