package lavs.business.service;

import lavs.business.model.LoanDetails;

import java.util.Map;

/**
 * Checks the values of a loan, such as a principal above zero or a renewal date
 * after the start date, before the loan is saved.
 *
 * <p>The validation service implements this. {@link LoanService} calls it with
 * every loan it is asked to create and reports whatever it returns.
 */
public interface LoanValidator {

    /**
     * Checks the values of a loan.
     *
     * @param details the values the staff member entered
     * @return the problem with each wrong value, by field name (the names of the
     *         {@link LoanDetails} components, such as {@code principal}), in the order
     *         they should be shown; empty if everything is acceptable. Never null.
     */
    Map<String, String> validate(LoanDetails details);
}
