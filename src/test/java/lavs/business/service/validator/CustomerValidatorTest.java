package lavs.business.service.validator;

import lavs.business.model.Customer;
import lavs.business.service.ValidationError;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CustomerValidatorTest {

    private final CustomerValidator validator = new CustomerValidator();

    @Test
    void validate_withFirstNameMissing_isRejected() {
        Customer customer = new Customer();
        customer.set_familyName("Doe");

        List<ValidationError> errors = validator.validate(customer);

        assertTrue(errors.stream().anyMatch(e -> e.getField().equals("firstName")));
    }

    @Test
    void validate_withSurnameMissing_isAllowed() {
        Customer customer = new Customer();
        customer.set_firstName("Jane");

        assertTrue(validator.validate(customer).isEmpty());
    }

    @Test
    void validate_withFirstNameOverMaxLength_isRejected() {
        Customer customer = new Customer();
        customer.set_firstName("a".repeat(41)); // limit is 40

        List<ValidationError> errors = validator.validate(customer);

        assertTrue(errors.stream().anyMatch(e -> e.getField().equals("firstName")));
    }

    @Test
    void validate_withFirstNameAtExactlyMaxLength_isAllowed() {
        Customer customer = new Customer();
        customer.set_firstName("a".repeat(40));

        assertTrue(validator.validate(customer).isEmpty());
    }

    @Test
    void validate_withBlankFirstName_isRejected() {
        Customer customer = new Customer();
        customer.set_firstName("   ");

        List<ValidationError> errors = validator.validate(customer);

        assertTrue(errors.stream().anyMatch(e -> e.getField().equals("firstName")));
    }
}
