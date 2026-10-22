package lavs.business.service;

import lavs.business.model.Address;
import lavs.business.model.Customer;
import lavs.business.model.Email;
import lavs.business.model.Employer;
import lavs.business.model.PhoneNumber;
import lavs.business.service.validator.AddressValidator;
import lavs.business.service.validator.CustomerValidator;
import lavs.business.service.validator.EmailValidator;
import lavs.business.service.validator.EmployerValidator;
import lavs.business.service.validator.PhoneValidator;

import java.util.List;

/**
 * Runs field-level and cross-field rules before anything is saved or sent to the mainframe.
 * Each entity's rules live in their own small Validator class; this service just dispatches
 * to the right one, so adding a rule means adding or editing a validator, not this class.
 */
public class ValidationService {

    private final Validator<Customer> customerValidator;
    private final Validator<Address> addressValidator;
    private final Validator<PhoneNumber> phoneValidator;
    private final Validator<Email> emailValidator;
    private final Validator<Employer> employerValidator;

    public ValidationService() {
        this(new CustomerValidator(), new AddressValidator(), new PhoneValidator(), new EmailValidator(), new EmployerValidator());
    }

    // Constructor allowing validators to be swapped out, e.g. for testing ValidationService itself.
    public ValidationService(Validator<Customer> customerValidator, Validator<Address> addressValidator,
                              Validator<PhoneNumber> phoneValidator, Validator<Email> emailValidator,
                              Validator<Employer> employerValidator) {
        this.customerValidator = customerValidator;
        this.addressValidator = addressValidator;
        this.phoneValidator = phoneValidator;
        this.emailValidator = emailValidator;
        this.employerValidator = employerValidator;
    }

    public List<ValidationError> validateCustomer(Customer customer) {
        return customerValidator.validate(customer);
    }

    public List<ValidationError> validateAddress(Address address) {
        return addressValidator.validate(address);
    }

    public List<ValidationError> validatePhone(PhoneNumber phone) {
        return phoneValidator.validate(phone);
    }

    public List<ValidationError> validateEmail(Email email) {
        return emailValidator.validate(email);
    }

    public List<ValidationError> validateEmployer(Employer employer) {
        return employerValidator.validate(employer);
    }
}
