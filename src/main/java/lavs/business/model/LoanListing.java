package lavs.business.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * One line of a customer's list of loans.
 *
 * @param loanNumber the mainframe's number for the loan
 * @param principal the amount borrowed, or null when the mainframe left it out
 * @param startDate the day the loan starts, or null when the mainframe left it out
 * @param status the loan's status, or null when the mainframe left it out
 */
public record LoanListing(String loanNumber, BigDecimal principal, LocalDate startDate, String status) {

    public LoanListing {
        Objects.requireNonNull(loanNumber, "loanNumber must not be null");
    }
}
