package lavs.business.service.validator;

import lavs.business.model.Employer;
import lavs.business.service.ValidationError;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EmployerValidatorTest {

    private final EmployerValidator validator = new EmployerValidator();

    @Test
    void validate_withTypeMissing_isRejected() {
        Employer employer = validEmployer();
        employer.set_type(null);

        List<ValidationError> errors = validator.validate(employer);

        assertTrue(errors.stream().anyMatch(e -> e.getField().equals("type")));
    }

    @Test
    void validate_withNameMissing_isRejected() {
        Employer employer = validEmployer();
        employer.set_name("");

        List<ValidationError> errors = validator.validate(employer);

        assertTrue(errors.stream().anyMatch(e -> e.getField().equals("name")));
    }

    @Test
    void validate_withPositionMissing_isAllowed() {
        Employer employer = validEmployer();
        employer.set_position(null);

        assertTrue(validator.validate(employer).isEmpty());
    }

    @Test
    void validate_withNegativeSalary_isRejected() {
        Employer employer = validEmployer();
        employer.set_salary(-1.0);

        List<ValidationError> errors = validator.validate(employer);

        assertTrue(errors.stream().anyMatch(e -> e.getField().equals("salary")));
    }

    @Test
    void validate_withZeroSalary_isAllowed() {
        Employer employer = validEmployer();
        employer.set_salary(0.0);

        assertTrue(validator.validate(employer).isEmpty());
    }

    @Test
    void validate_withNameOverMaxLength_isRejected() {
        Employer employer = validEmployer();
        employer.set_name("a".repeat(51)); // limit is 50

        List<ValidationError> errors = validator.validate(employer);

        assertTrue(errors.stream().anyMatch(e -> e.getField().equals("name")));
    }

    @Test
    void validate_withAllRequiredFields_isValid() {
        assertTrue(validator.validate(validEmployer()).isEmpty());
    }

    private static Employer validEmployer() {
        return new Employer(null, null, 0L, "Acme Ltd", "Developer", 75000.0, "Full-time");
    }
}
