package lavs.business.service.validator;

import lavs.business.model.Address;
import lavs.business.service.ValidationError;
import lavs.business.service.Validator;

import java.util.ArrayList;
import java.util.List;

/**
 * Field-level rules for Address, sourced from the Customer Address Details mainframe screen.
 * Type, Line 1 and Country are required; Postcode must be a valid integer if present.
 */
public class AddressValidator implements Validator<Address> {

    @Override
    public List<ValidationError> validate(Address address) {
        List<ValidationError> errors = new ArrayList<>();

        requireField(errors, "type", address.getType());
        requireField(errors, "line1", address.getLine1());
        requireField(errors, "country", address.getCountry());

        checkMaxLength(errors, "type", address.getType(), 15);
        checkMaxLength(errors, "line1", address.getLine1(), 60);
        checkMaxLength(errors, "line2", address.getLine2(), 60);
        checkMaxLength(errors, "suburb", address.getSuburb(), 60);
        checkMaxLength(errors, "city", address.getCity(), 60);
        checkMaxLength(errors, "country", address.getCountry(), 60);

        if (address.getPostCode() != null && !address.getPostCode().isBlank()) {
            try {
                Integer.parseInt(address.getPostCode());
            } catch (NumberFormatException e) {
                errors.add(new ValidationError("postCode", "Postcode must be a whole number"));
            }
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
