package lavs.business.service.validator;

import lavs.business.model.Email;
import lavs.business.service.ValidationError;
import lavs.business.service.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Field-level rules for Email, sourced from the Customer Email Details mainframe screen.
 * Type and Address are required; Address must look like a valid email.
 */
public class EmailValidator implements Validator<Email> {

    private static final Pattern EMAIL_SHAPE = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    @Override
    public List<ValidationError> validate(Email email) {
        List<ValidationError> errors = new ArrayList<>();

        requireField(errors, "type", email.getType());
        requireField(errors, "address", email.getAddress());

        checkMaxLength(errors, "type", email.getType(), 15);
        checkMaxLength(errors, "address", email.getAddress(), 60);

        if (email.getAddress() != null && !email.getAddress().isBlank()
                && !EMAIL_SHAPE.matcher(email.getAddress()).matches()) {
            errors.add(new ValidationError("address", "Must be a valid email address"));
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
