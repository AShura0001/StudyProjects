package lavs.business.service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Thrown when a business rule is broken, so the request was not carried out.
 *
 * <p>It carries every problem found, not just the first, so the staff member can
 * fix them all at once. Each problem is stored against the field it concerns, in
 * the order it should be shown. A problem that is not about one field is stored
 * under {@link #GENERAL}. It is unchecked so callers are not forced to handle it
 * where a rule cannot be broken.
 */
public abstract class RuleViolationException extends RuntimeException {

    /** The key used for a problem that is not about one particular field. */
    public static final String GENERAL = "general";

    private final Map<String, String> problems;

    /**
     * Creates an exception for the problems found.
     *
     * @param problems the problem for each field, at least one
     * @throws NullPointerException if the problems are null
     * @throws IllegalArgumentException if there are no problems
     */
    protected RuleViolationException(Map<String, String> problems) {
        super(messageOf(problems));
        this.problems = Collections.unmodifiableMap(new LinkedHashMap<>(problems));
    }

    /**
     * Gets the problem found for each field, in the order they should be shown.
     *
     * @return an unmodifiable map from field name to problem, never empty
     */
    public Map<String, String> getProblems() {
        return problems;
    }

    private static String messageOf(Map<String, String> problems) {
        Objects.requireNonNull(problems, "problems must not be null");
        if (problems.isEmpty()) {
            throw new IllegalArgumentException("a rule exception needs at least one problem");
        }
        return String.join(" ", problems.values());
    }
}
