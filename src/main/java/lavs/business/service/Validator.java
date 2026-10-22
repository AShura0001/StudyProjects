package lavs.business.service;

import java.util.List;

/**
 * One set of rules for one kind of record. It returns every problem it finds rather than
 * stopping at the first, so a screen can show them all at once.
 *
 * @param <T> the kind of record checked
 */
public interface Validator<T> {
    /**
     * @param target the object to validate
     * @return a list of validation errors, empty if the object is valid
     */
    List<ValidationError> validate(T target);
}
