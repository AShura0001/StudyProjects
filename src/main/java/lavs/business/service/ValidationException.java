package lavs.business.service;

import java.util.List;

/**
 * Thrown by CustomerService when ValidationService finds one or more errors on
 * data that was about to be saved or sent. Carries the full list of errors so
 * the UI can show every problem at once, rather than one at a time.
 */
public class ValidationException extends RuntimeException {
    private final List<ValidationError> errors;

    public ValidationException(List<ValidationError> errors) {
        super("Validation failed: " + errors);
        this.errors = errors;
    }

    public List<ValidationError> getErrors() {
        return errors;
    }
}
