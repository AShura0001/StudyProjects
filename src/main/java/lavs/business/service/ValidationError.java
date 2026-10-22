package lavs.business.service;

/**
 * One problem found while checking a record: the field it belongs to, and a message a
 * person can act on.
 */
public class ValidationError {
    private final String field;
    private final String message;

    public ValidationError(String field, String message) {
        this.field = field;
        this.message = message;
    }

    public String getField() {
        return field;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return field + ": " + message;
    }
}
