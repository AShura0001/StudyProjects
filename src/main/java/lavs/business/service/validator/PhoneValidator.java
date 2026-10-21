package lavs.business.service.validator;

import lavs.business.model.PhoneNumber;
import lavs.business.service.ValidationError;
import lavs.business.service.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Field-level rules for PhoneNumber, sourced from the Customer Phone Details mainframe
 * screen. Type, Country Code and Number are all required.
 */
public class PhoneValidator implements Validator<PhoneNumber> {

    private static final Pattern DIGITS_ONLY = Pattern.compile("^[0-9 ()+-]+$");

    @Override
    public List<ValidationError> validate(PhoneNumber phone) {
        List<ValidationError> errors = new ArrayList<>();

        requireField(errors, "type", phone.get_type());
        requireField(errors, "countryCode", phone.get_countryCode());
        requireField(errors, "number", phone.get_number());

        checkMaxLength(errors, "type", phone.get_type(), 15);
        checkMaxLength(errors, "countryCode", phone.get_countryCode(), 10);
        checkMaxLength(errors, "number", phone.get_number(), 30);

        if (phone.get_number() != null && !phone.get_number().isBlank()
                && !DIGITS_ONLY.matcher(phone.get_number()).matches()) {
            errors.add(new ValidationError("number", "Must only contain digits and phone symbols"));
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
