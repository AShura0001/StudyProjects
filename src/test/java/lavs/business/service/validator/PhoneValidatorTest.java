package lavs.business.service.validator;

import lavs.business.model.PhoneNumber;
import lavs.business.service.ValidationError;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PhoneValidatorTest {

    private final PhoneValidator validator = new PhoneValidator();

    @Test
    void validate_withTypeMissing_isRejected() {
        PhoneNumber phone = validPhone();
        phone.set_type(null);

        List<ValidationError> errors = validator.validate(phone);

        assertTrue(errors.stream().anyMatch(e -> e.getField().equals("type")));
    }

    @Test
    void validate_withCountryCodeMissing_isRejected() {
        PhoneNumber phone = validPhone();
        phone.set_countryCode("");

        List<ValidationError> errors = validator.validate(phone);

        assertTrue(errors.stream().anyMatch(e -> e.getField().equals("countryCode")));
    }

    @Test
    void validate_withNumberMissing_isRejected() {
        PhoneNumber phone = validPhone();
        phone.set_number(null);

        List<ValidationError> errors = validator.validate(phone);

        assertTrue(errors.stream().anyMatch(e -> e.getField().equals("number")));
    }

    @Test
    void validate_withLettersInNumber_isRejected() {
        PhoneNumber phone = validPhone();
        phone.set_number("21ABC4567");

        List<ValidationError> errors = validator.validate(phone);

        assertTrue(errors.stream().anyMatch(e -> e.getField().equals("number")));
    }

    @Test
    void validate_withNumberContainingPhoneSymbols_isAllowed() {
        PhoneNumber phone = validPhone();
        phone.set_number("+64 (21) 123-4567");

        assertTrue(validator.validate(phone).isEmpty());
    }

    @Test
    void validate_withTypeOverMaxLength_isRejected() {
        PhoneNumber phone = validPhone();
        phone.set_type("a".repeat(16)); // limit is 15

        List<ValidationError> errors = validator.validate(phone);

        assertTrue(errors.stream().anyMatch(e -> e.getField().equals("type")));
    }

    @Test
    void validate_withCountryCodeAtExactlyMaxLength_isAllowed() {
        PhoneNumber phone = validPhone();
        phone.set_countryCode("1".repeat(10)); // limit is 10

        assertTrue(validator.validate(phone).isEmpty());
    }

    @Test
    void validate_withAllRequiredFields_isValid() {
        assertTrue(validator.validate(validPhone()).isEmpty());
    }

    private static PhoneNumber validPhone() {
        PhoneNumber phone = new PhoneNumber(true, "64", null, null, 0L, false, null, "Mobile");
        phone.set_number("211234567");
        return phone;
    }
}
