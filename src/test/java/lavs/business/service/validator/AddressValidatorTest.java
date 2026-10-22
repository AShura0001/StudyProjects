package lavs.business.service.validator;

import lavs.business.model.Address;
import lavs.business.service.ValidationError;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AddressValidatorTest {

    private final AddressValidator validator = new AddressValidator();

    @Test
    void validate_withLine1Missing_isRejected() {
        Address address = validAddress();
        address.setLine1(null);

        List<ValidationError> errors = validator.validate(address);

        assertTrue(errors.stream().anyMatch(e -> e.getField().equals("line1")));
    }

    @Test
    void validate_withCountryMissing_isRejected() {
        Address address = validAddress();
        address.setCountry("");

        List<ValidationError> errors = validator.validate(address);

        assertTrue(errors.stream().anyMatch(e -> e.getField().equals("country")));
    }

    @Test
    void validate_withLine2Missing_isAllowed() {
        Address address = validAddress();
        address.setLine2(null);

        assertTrue(validator.validate(address).isEmpty());
    }

    @Test
    void validate_withNonNumericPostcode_isRejected() {
        Address address = validAddress();
        address.setPostCode("not-a-number");

        List<ValidationError> errors = validator.validate(address);

        assertTrue(errors.stream().anyMatch(e -> e.getField().equals("postCode")));
    }

    @Test
    void validate_withTypeOverMaxLength_isRejected() {
        Address address = validAddress();
        address.setType("a".repeat(16)); // limit is 15

        List<ValidationError> errors = validator.validate(address);

        assertTrue(errors.stream().anyMatch(e -> e.getField().equals("type")));
    }

    @Test
    void validate_withTypeAtExactlyMaxLength_isAllowed() {
        Address address = validAddress();
        address.setType("a".repeat(15)); // exactly the limit

        assertTrue(validator.validate(address).isEmpty());
    }

    @Test
    void validate_withAllRequiredFields_isValid() {
        assertTrue(validator.validate(validAddress()).isEmpty());
    }

    private static Address validAddress() {
        Address address = new Address();
        address.setType("Home");
        address.setLine1("1 Queen Street");
        address.setLine2("Apt 4");
        address.setCountry("New Zealand");
        address.setPostCode("1010");
        return address;
    }
}
