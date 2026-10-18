package lavs.business.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * What the staff member enters about a loan.
 *
 * <p>This only holds the values. Whether they make sense, such as a principal
 * above zero or a renewal date after the start date, is checked by the
 * validation rules, not here. Money is kept as {@link BigDecimal} so amounts are
 * never rounded by accident.
 *
 * @param principal the amount borrowed
 * @param rateType the kind of rate, for example {@code Fixed}
 * @param rateAmount the interest rate
 * @param rateFrequency how often the rate applies, for example {@code Yearly}
 * @param paymentAmount the amount of each payment
 * @param paymentFrequency how often a payment is made, for example {@code Monthly}
 * @param startDate the day the loan starts
 * @param renewalDate the day the loan comes up for renewal
 */
public record LoanDetails(
        BigDecimal principal,
        String rateType,
        BigDecimal rateAmount,
        String rateFrequency,
        BigDecimal paymentAmount,
        String paymentFrequency,
        LocalDate startDate,
        LocalDate renewalDate) {

    /**
     * Creates the loan details.
     *
     * @throws NullPointerException if any value is null
     */
    public LoanDetails {
        Objects.requireNonNull(principal, "principal must not be null");
        Objects.requireNonNull(rateType, "rateType must not be null");
        Objects.requireNonNull(rateAmount, "rateAmount must not be null");
        Objects.requireNonNull(rateFrequency, "rateFrequency must not be null");
        Objects.requireNonNull(paymentAmount, "paymentAmount must not be null");
        Objects.requireNonNull(paymentFrequency, "paymentFrequency must not be null");
        Objects.requireNonNull(startDate, "startDate must not be null");
        Objects.requireNonNull(renewalDate, "renewalDate must not be null");
    }
}
