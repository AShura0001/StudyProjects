package lavs.business.service.validator;

import lavs.business.model.Email;
import lavs.business.service.ValidationError;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EmailValidatorTest {

    private final EmailValidator validator = new EmailValidator();

    @Test
    void validate_withTypeMissing_isRejected() {
        Email email = validEmail();
        email.setType(null);

        List<ValidationError> errors = validator.validate(email);

        assertTrue(errors.stream().anyMatch(e -> e.getField().equals("type")));
    }

    @Test
    void validate_withAddressMissing_isRejected() {
        Email email = validEmail();
        email.setAddress("");

        List<ValidationError> errors = validator.validate(email);

        assertTrue(errors.stream().anyMatch(e -> e.getField().equals("address")));
    }

    @Test
    void validate_withMalformedAddress_isRejected() {
        Email email = validEmail();
        email.setAddress("not-an-email");

        List<ValidationError> errors = validator.validate(email);

        assertTrue(errors.stream().anyMatch(e -> e.getField().equals("address")));
    }

    @Test
    void validate_withAddressMissingDomain_isRejected() {
        Email email = validEmail();
        email.setAddress("jane@");

        List<ValidationError> errors = validator.validate(email);

        assertTrue(errors.stream().anyMatch(e -> e.getField().equals("address")));
    }

    @Test
    void validate_withAddressOverMaxLength_isRejected() {
        Email email = validEmail();
        email.setAddress("a".repeat(55) + "@example.com"); // over 60 chars total

        List<ValidationError> errors = validator.validate(email);

        assertTrue(errors.stream().anyMatch(e -> e.getField().equals("address")));
    }

    @Test
    void validate_withAllRequiredFields_isValid() {
        assertTrue(validator.validate(validEmail()).isEmpty());
    }

    private static Email validEmail() {
        return new Email("jane@example.com", null, null, 0L, false, null, "Personal");
    }
}
