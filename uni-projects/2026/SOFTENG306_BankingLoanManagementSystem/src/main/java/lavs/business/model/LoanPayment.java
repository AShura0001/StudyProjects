package lavs.business.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A payment made on a loan, as the mainframe holds it.
 *
 * <p>When the payment comes from a list, the mainframe only sends the id, date and
 * amount, so how the payment was split and what is left owing are null.
 *
 * @param id the mainframe's id for the payment on the loan
 * @param loanNumber the mainframe's number for the loan
 * @param date the day the payment was made
 * @param amountPaid the amount of the payment
 * @param principalAmount the part that reduced the amount borrowed; null if not sent
 * @param interestAmount the part that was interest; null if not sent
 * @param amountRemaining what is still owed after this payment; null if not sent
 */
public record LoanPayment(
        long id,
        String loanNumber,
        LocalDate date,
        BigDecimal amountPaid,
        BigDecimal principalAmount,
        BigDecimal interestAmount,
        BigDecimal amountRemaining) {

    /**
     * Creates the payment.
     *
     * @throws NullPointerException if the loan number, date or amount paid is null
     */
    public LoanPayment {
        Objects.requireNonNull(loanNumber, "loanNumber must not be null");
        Objects.requireNonNull(date, "date must not be null");
        Objects.requireNonNull(amountPaid, "amountPaid must not be null");
    }
}
