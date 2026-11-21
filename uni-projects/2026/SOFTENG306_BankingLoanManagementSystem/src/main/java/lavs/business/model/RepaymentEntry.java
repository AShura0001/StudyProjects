package lavs.business.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * One line of a loan's repayment schedule, worked out by the mainframe. It is
 * read-only.
 *
 * @param paymentNumber the position of the payment in the schedule, starting at 1
 * @param date the day the payment is due
 * @param amountPaid the amount of the payment
 * @param principalAmount the part of the payment that reduces the amount borrowed
 * @param interestAmount the part of the payment that is interest
 * @param amountRemaining what is still owed after this payment
 */
public record RepaymentEntry(
        long paymentNumber,
        LocalDate date,
        BigDecimal amountPaid,
        BigDecimal principalAmount,
        BigDecimal interestAmount,
        BigDecimal amountRemaining) {

    /**
     * Creates the schedule line.
     *
     * @throws NullPointerException if any value is null
     */
    public RepaymentEntry {
        Objects.requireNonNull(date, "date must not be null");
        Objects.requireNonNull(amountPaid, "amountPaid must not be null");
        Objects.requireNonNull(principalAmount, "principalAmount must not be null");
        Objects.requireNonNull(interestAmount, "interestAmount must not be null");
        Objects.requireNonNull(amountRemaining, "amountRemaining must not be null");
    }
}
