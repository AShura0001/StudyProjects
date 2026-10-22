package lavs.business.service;

import java.util.List;

public interface Validator<T> {
    /**
     * @param target the object to validate
     * @return a list of validation errors, empty if the object is valid
     */
    List<ValidationError> validate(T target);
}
