package lavs.business.repository;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Thrown when the mainframe was reached but refused a record, for example because
 * a value is not allowed.
 *
 * <p>It carries the reason for each field that was refused, so the staff member
 * can be shown what to correct. It is unchecked so that callers are not forced to
 * handle it where a rejection cannot happen.
 */
public class RecordRejectedException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    /**
     * Creates an exception with a message and the reasons for each field.
     *
     * @param message the overall reason the mainframe gave
     * @param fieldErrors the reason for each refused field, by field name; an empty
     *                    map means the mainframe gave no per-field reasons
     * @throws NullPointerException if the field errors are null
     */
    public RecordRejectedException(String message, Map<String, String> fieldErrors) {
        super(message);
        Objects.requireNonNull(fieldErrors, "fieldErrors must not be null");
        this.fieldErrors = Collections.unmodifiableMap(new LinkedHashMap<>(fieldErrors));
    }

    /**
     * Gets the reason for each refused field, in the order they were given.
     *
     * @return an unmodifiable map from field name to reason
     */
    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
