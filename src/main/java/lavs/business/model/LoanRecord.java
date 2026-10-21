package lavs.business.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A loan as the mainframe holds it: who it belongs to, its status and its terms.
 * Values the mainframe left out are null, so a half-filled loan can still be shown.
 *
 * @param loanNumber the mainframe's number for the loan
 * @param customerNumber the number of the customer who owns the loan
 * @param customerName the owner's name, as the mainframe gives it
 * @param status the loan's status, for example {@code Active}
 * @param principal the amount borrowed
 * @param rateType the kind of rate, for example {@code Fixed}
 * @param rateAmount the interest rate in percent
 * @param rateFrequency how often the rate applies, for example {@code Yearly}
 * @param paymentAmount the amount of each repayment
 * @param paymentFrequency how often a repayment is made, for example {@code Monthly}
 * @param startDate the day the loan starts
 * @param renewalDate the day the loan is renewed or ends
 */
public record LoanRecord(
        String loanNumber,
        String customerNumber,
        String customerName,
        String status,
        BigDecimal principal,
        String rateType,
        BigDecimal rateAmount,
        String rateFrequency,
        BigDecimal paymentAmount,
        String paymentFrequency,
        LocalDate startDate,
        LocalDate renewalDate) {

    public LoanRecord {
        Objects.requireNonNull(loanNumber, "loanNumber must not be null");
    }
}
