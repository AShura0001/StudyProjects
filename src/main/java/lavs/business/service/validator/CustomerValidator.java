package lavs.business.service.validator;

import lavs.business.model.Customer;
import lavs.business.service.ValidationError;
import lavs.business.service.Validator;

import java.util.ArrayList;
import java.util.List;

/**
 * Field-level rules for Customer, sourced from the Customer Details mainframe screen.
 * First Name is the only required field on that screen; the others have length limits only.
 */
public class CustomerValidator implements Validator<Customer> {

    @Override
    public List<ValidationError> validate(Customer customer) {
        List<ValidationError> errors = new ArrayList<>();

        if (isBlank(customer.get_firstName())) {
            errors.add(new ValidationError("firstName", "First name is required"));
        }
        checkMaxLength(errors, "firstName", customer.get_firstName(), 40);
        checkMaxLength(errors, "title", customer.get_title(), 10);
        checkMaxLength(errors, "familyName", customer.get_familyName(), 40);
        checkMaxLength(errors, "nationality", customer.get_nationality(), 40);
        checkMaxLength(errors, "dateOfBirth", customer.get_dateOfBirth(), 10);

        return errors;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static void checkMaxLength(List<ValidationError> errors, String field, String value, int max) {
        if (value != null && value.length() > max) {
            errors.add(new ValidationError(field, "Must be at most " + max + " characters"));
        }
    }
}
