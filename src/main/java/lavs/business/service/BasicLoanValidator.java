package lavs.business.service;

import lavs.business.model.LoanDetails;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The basic checks on the values of a loan: amounts above zero, a rate between 0
 * and 100, and a renewal date after the start date.
 *
 * <p>It lets {@link LoanService} work before the full validation service is
 * available, and the full service can replace it by implementing
 * {@link LoanValidator}. It does not check values against the mainframe's lists,
 * such as the allowed rate types.
 */
public class BasicLoanValidator implements LoanValidator {

    /** The problem reported when the principal is not above zero. */
    public static final String PRINCIPAL_MESSAGE = "The principal must be greater than zero.";

    /** The problem reported when the rate is outside 0 to 100. */
    public static final String RATE_MESSAGE = "The interest rate must be between 0 and 100.";

    /** The problem reported when the payment amount is not above zero. */
    public static final String PAYMENT_MESSAGE = "The payment amount must be greater than zero.";

    /** The problem reported when the renewal date is not after the start date. */
    public static final String RENEWAL_MESSAGE = "The renewal date must be after the start date.";

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    @Override
    public Map<String, String> validate(LoanDetails details) {
        Objects.requireNonNull(details, "details must not be null");
        Map<String, String> problems = new LinkedHashMap<>();
        if (details.principal().signum() <= 0) {
            problems.put("principal", PRINCIPAL_MESSAGE);
        }
        if (details.rateAmount().signum() < 0 || details.rateAmount().compareTo(ONE_HUNDRED) > 0) {
            problems.put("rateAmount", RATE_MESSAGE);
        }
        if (details.paymentAmount().signum() <= 0) {
            problems.put("paymentAmount", PAYMENT_MESSAGE);
        }
        if (!details.renewalDate().isAfter(details.startDate())) {
            problems.put("renewalDate", RENEWAL_MESSAGE);
        }
        return problems;
    }
}
