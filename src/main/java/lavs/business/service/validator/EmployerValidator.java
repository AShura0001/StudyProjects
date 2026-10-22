package lavs.business.service.validator;

import lavs.business.model.Employer;
import lavs.business.service.ValidationError;
import lavs.business.service.Validator;

import java.util.ArrayList;
import java.util.List;

/**
 * Field-level rules for Employer, sourced from the Customer Employer Details mainframe
 * screen. Type and Name are required. Salary is a double on the real model (not text
 * with a length limit, as the original screen description suggested) - checked for
 * being non-negative instead.
 */
public class EmployerValidator implements Validator<Employer> {

    @Override
    public List<ValidationError> validate(Employer employer) {
        List<ValidationError> errors = new ArrayList<>();

        requireField(errors, "type", employer.get_type());
        requireField(errors, "name", employer.get_name());

        checkMaxLength(errors, "type", employer.get_type(), 20);
        checkMaxLength(errors, "name", employer.get_name(), 50);
        checkMaxLength(errors, "position", employer.get_position(), 50);

        if (employer.get_salary() < 0) {
            errors.add(new ValidationError("salary", "Must not be negative"));
        }

        return errors;
    }

    private static void requireField(List<ValidationError> errors, String field, String value) {
        if (value == null || value.trim().isEmpty()) {
            errors.add(new ValidationError(field, "Is required"));
        }
    }

    private static void checkMaxLength(List<ValidationError> errors, String field, String value, int max) {
        if (value != null && value.length() > max) {
            errors.add(new ValidationError(field, "Must be at most " + max + " characters"));
        }
    }
}
