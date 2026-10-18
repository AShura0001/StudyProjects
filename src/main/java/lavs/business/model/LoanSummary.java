package lavs.business.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * The summary of a loan, worked out by the mainframe. It is read-only.
 *
 * @param loanNumber the mainframe's number for the loan
 * @param customerName the name of the customer the loan belongs to
 * @param numberOfPayments how many payments the loan has in total
 * @param totalAmountPaid the total paid over the life of the loan
 * @param totalInterestPaid the interest included in that total
 * @param finalPaymentDate the day the last payment is due
 * @param finalPaymentAmount the amount of the last payment
 */
public record LoanSummary(
        String loanNumber,
        String customerName,
        long numberOfPayments,
        BigDecimal totalAmountPaid,
        BigDecimal totalInterestPaid,
        LocalDate finalPaymentDate,
        BigDecimal finalPaymentAmount) {

    /**
     * Creates the summary.
     *
     * @throws NullPointerException if any value is null
     */
    public LoanSummary {
        Objects.requireNonNull(loanNumber, "loanNumber must not be null");
        Objects.requireNonNull(customerName, "customerName must not be null");
        Objects.requireNonNull(totalAmountPaid, "totalAmountPaid must not be null");
        Objects.requireNonNull(totalInterestPaid, "totalInterestPaid must not be null");
        Objects.requireNonNull(finalPaymentDate, "finalPaymentDate must not be null");
        Objects.requireNonNull(finalPaymentAmount, "finalPaymentAmount must not be null");
    }
}
