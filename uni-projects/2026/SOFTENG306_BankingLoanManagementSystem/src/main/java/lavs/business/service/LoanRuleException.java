package lavs.business.service;

import java.util.Map;

/**
 * Thrown when a loan rule is broken, so the request was not carried out. See
 * {@link RuleViolationException} for how the problems are reported.
 */
public class LoanRuleException extends RuleViolationException {

    /**
     * Creates an exception for the problems found.
     *
     * @param problems the problem for each field, at least one
     * @throws NullPointerException if the problems are null
     * @throws IllegalArgumentException if there are no problems
     */
    public LoanRuleException(Map<String, String> problems) {
        super(problems);
    }
}
