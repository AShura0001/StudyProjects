package lavs.business.service;

import lavs.business.model.Address;
import lavs.business.model.Customer;
import lavs.business.model.CustomerPayloads;
import lavs.business.model.CustomerPayloads.CustomerContent;
import lavs.business.model.CustomerProfile;
import lavs.business.model.CustomerReference;
import lavs.business.model.Draft;
import lavs.business.model.DraftType;
import lavs.business.model.Email;
import lavs.business.model.Employer;
import lavs.business.model.PhoneNumber;
import lavs.business.repository.CustomerAddressGateway;
import lavs.business.repository.CustomerEmailGateway;
import lavs.business.repository.CustomerEmployerGateway;
import lavs.business.repository.CustomerGateway;
import lavs.business.repository.CustomerPhoneNumberGateway;
import lavs.business.repository.DraftRepository;
import lavs.business.repository.MainframeUnavailableException;
import lavs.business.repository.PageResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Adds, edits and retrieves customers, and manages their addresses, phone numbers,
 * emails and employers. Enforces the loan pre-conditions: a customer needs at least
 * one mailing address and at least one phone or email before a loan can be started.
 *
 * Every write goes through ValidationService first; invalid data never reaches a gateway.
 * List gateways are paginated (CustomerSearchResult/PageResult); this service walks every
 * page internally so its own callers can keep working with plain Lists.
 *
 * Implements CustomerProfileSource so LoanService can ask it for the loan-rule facts
 * about a customer, whether that customer already exists on the mainframe (looked up
 * live) or is still only a local draft (read back from its saved payload).
 */
public class CustomerService implements CustomerProfileSource {

    private static final long PAGE_SIZE = 100;

    private final CustomerGateway customerGateway;
    private final CustomerAddressGateway addressGateway;
    private final CustomerPhoneNumberGateway phoneGateway;
    private final CustomerEmailGateway emailGateway;
    private final CustomerEmployerGateway employerGateway;
    private final ValidationService validationService;
    private final DraftRepository drafts;
    private final BackgroundSync sync;

    public CustomerService(CustomerGateway customerGateway,
                            CustomerAddressGateway addressGateway,
                            CustomerPhoneNumberGateway phoneGateway,
                            CustomerEmailGateway emailGateway,
                            CustomerEmployerGateway employerGateway,
                            ValidationService validationService,
                            DraftRepository drafts,
                            BackgroundSync sync) {
        this.customerGateway = customerGateway;
        this.addressGateway = addressGateway;
        this.phoneGateway = phoneGateway;
        this.emailGateway = emailGateway;
        this.employerGateway = employerGateway;
        this.validationService = validationService;
        this.drafts = Objects.requireNonNull(drafts, "drafts must not be null");
        this.sync = Objects.requireNonNull(sync, "sync must not be null");
    }

    // ---- Customer: existing, synced customers (direct mainframe calls) ----

    public Optional<Customer> getCustomer(String customerNumber) throws IOException {
        return customerGateway.findByCustomerNumber(customerNumber);
    }

    public Customer createCustomer(Customer customer) throws IOException {
        validateOrThrow(validationService.validateCustomer(customer));
        return customerGateway.createCustomer(customer).orElseThrow(() -> new IllegalStateException("Mainframe did not accept the new customer"));
    }

    public Customer updateCustomer(Customer customer) throws IOException {
        validateOrThrow(validationService.validateCustomer(customer));
        return customerGateway.updateCustomer(customer).orElseThrow(() -> new IllegalStateException("Mainframe did not accept the customer update"));
    }

    public boolean toggleCustomerStatus(String customerNumber) throws IOException {
        return customerGateway.updateStatus(customerNumber);
    }

    // ---- Customer: brand new, offline-capable (saved as a CUSTOMER draft) ----

    /**
     * Saves a brand new customer, and whatever address/phone/email/employer records
     * were entered for them, as one CUSTOMER draft. Works without a connection: the
     * draft is written locally first and sent to the mainframe in the background,
     * matching how LoanService.createLoan saves a loan.
     *
     * Every value is validated before anything is saved - customer fields, and every
     * address/phone/email/employer in the lists given. Every problem found is
     * collected and reported at once; nothing is saved if any of it is invalid.
     *
     * NOTE: customer.setOrganisation(...) must be set by the caller before this is
     * called - the mainframe's customer.create message requires it, and this class
     * does not know what value it should be (likely a fixed ID for Countrywide
     * itself within the mainframe; confirm the real value with the team / AppServices
     * wiring before this is used for real).
     *
     * @return the saved draft; use its localId with CustomerReference.draft(...) to
     *         refer to this customer before it has synced (for example, to pass to
     *         LoanService.createLoan)
     * @throws ValidationException if the customer or any child record is invalid
     * @throws lavs.business.repository.DraftStorageException if the draft could not be saved
     */
    public Draft createCustomerDraft(Customer customer, List<Address> addresses, List<PhoneNumber> phones, List<Email> emails, List<Employer> employers) {
        Objects.requireNonNull(customer, "customer must not be null");
        Objects.requireNonNull(addresses, "addresses must not be null");
        Objects.requireNonNull(phones, "phones must not be null");
        Objects.requireNonNull(emails, "emails must not be null");
        Objects.requireNonNull(employers, "employers must not be null");

        List<ValidationError> errors = new ArrayList<>(validationService.validateCustomer(customer));
        addresses.forEach(a -> errors.addAll(validationService.validateAddress(a)));
        phones.forEach(p -> errors.addAll(validationService.validatePhone(p)));
        emails.forEach(e -> errors.addAll(validationService.validateEmail(e)));
        employers.forEach(e -> errors.addAll(validationService.validateEmployer(e)));
        validateOrThrow(errors);

        String payload = CustomerPayloads.customerPayload(customer, addresses, phones, emails, employers);
        return sync.saveDraft(DraftType.CUSTOMER, payload);
    }

    // ---- Address ----

    public List<Address> listAddresses(String customerNumber) throws IOException {
        return fetchAllPages(page -> addressGateway.ListCustomerAddresses(page, customerNumber, PAGE_SIZE));
    }

    public Address addAddress(Address address) throws IOException {
        validateOrThrow(validationService.validateAddress(address));
        if (Boolean.TRUE.equals(address.getIsPrimary())) {
            clearExistingPrimaryAddress(address.getCustomerNumber());
        }
        return addressGateway.addCustomerAddress(address).orElseThrow(() -> new IllegalStateException("Mainframe did not accept the new address"));
    }

    public Address updateAddress(Address address) throws IOException {
        validateOrThrow(validationService.validateAddress(address));
        if (Boolean.TRUE.equals(address.getIsPrimary())) {
            clearExistingPrimaryAddress(address.getCustomerNumber());
        }
        return addressGateway.updateCustomerAddress(address).orElseThrow(() -> new IllegalStateException("Mainframe did not accept the address update"));
    }

    public boolean deleteAddress(String customerNumber, long addressId) throws IOException {
        return addressGateway.deleteCustomerAddress(customerNumber, addressId);
    }

    private void clearExistingPrimaryAddress(String customerNumber) throws IOException {
        for (Address existing : listAddresses(customerNumber)) {
            if (Boolean.TRUE.equals(existing.getIsPrimary())) {
                existing.setIsPrimary(false);
                addressGateway.updateCustomerAddress(existing);
            }
        }
    }

    // ---- Phone ----

    public List<PhoneNumber> listPhones(String customerNumber) throws IOException {
        return fetchAllPages(page -> phoneGateway.ListCustomerPhoneNumbers(page, customerNumber, PAGE_SIZE));
    }

    public PhoneNumber addPhone(PhoneNumber phone) throws IOException {
        validateOrThrow(validationService.validatePhone(phone));
        if (phone.is_isPrimary()) {
            clearExistingPrimaryPhone(phone.get_customerNumber());
        }
        return phoneGateway.addCustomerPhoneNumber(phone).orElseThrow(() -> new IllegalStateException("Mainframe did not accept the new phone"));
    }

    public PhoneNumber updatePhone(PhoneNumber phone) throws IOException {
        validateOrThrow(validationService.validatePhone(phone));
        if (phone.is_isPrimary()) {
            clearExistingPrimaryPhone(phone.get_customerNumber());
        }
        return phoneGateway.updateCustomerNumber(phone).orElseThrow(() -> new IllegalStateException("Mainframe did not accept the phone update"));
    }

    public boolean deletePhone(String customerNumber, long phoneId) throws IOException {
        return phoneGateway.deleteCustomerNumberByNumberAndId(phoneId, customerNumber);
    }

    private void clearExistingPrimaryPhone(String customerNumber) throws IOException {
        for (PhoneNumber existing : listPhones(customerNumber)) {
            if (existing.is_isPrimary()) {
                existing.set_isPrimary(false);
                phoneGateway.updateCustomerNumber(existing);
            }
        }
    }

    // ---- Email ----

    public List<Email> listEmails(String customerNumber) throws IOException {
        return fetchAllPages(page -> emailGateway.ListCustomerEmails(page, customerNumber, PAGE_SIZE));
    }

    public Email addEmail(Email email) throws IOException {
        validateOrThrow(validationService.validateEmail(email));
        if (email.isPrimary()) {
            clearExistingPrimaryEmail(email.getCustomerNumber());
        }
        return emailGateway.addCustomerEmail(email).orElseThrow(() -> new IllegalStateException("Mainframe did not accept the new email"));
    }

    public Email updateEmail(Email email) throws IOException {
        validateOrThrow(validationService.validateEmail(email));
        if (email.isPrimary()) {
            clearExistingPrimaryEmail(email.getCustomerNumber());
        }
        return emailGateway.updateCustomerEmail(email).orElseThrow(() -> new IllegalStateException("Mainframe did not accept the email update"));
    }

    public boolean deleteEmail(String customerNumber, long emailId) throws IOException {
        return emailGateway.deleteCustomerEmail(customerNumber, emailId);
    }

    private void clearExistingPrimaryEmail(String customerNumber) throws IOException {
        for (Email existing : listEmails(customerNumber)) {
            if (existing.isPrimary()) {
                existing.setPrimary(false);
                emailGateway.updateCustomerEmail(existing);
            }
        }
    }

    // ---- Employer ----
    // An unemployed customer simply has no Employer records; nothing enforces that one
    // must exist. Employer has no Primary flag, so there is no single-primary rule here.

    public List<Employer> listEmployers(String customerNumber) throws IOException {
        return fetchAllPages(page -> employerGateway.ListCustomerEmployers(page, customerNumber, PAGE_SIZE));
    }

    public Employer addEmployer(Employer employer) throws IOException {
        validateOrThrow(validationService.validateEmployer(employer));
        return employerGateway.addCustomerEmployer(employer).orElseThrow(() -> new IllegalStateException("Mainframe did not accept the new employer"));
    }

    public Employer updateEmployer(Employer employer) throws IOException {
        validateOrThrow(validationService.validateEmployer(employer));
        return employerGateway.updateCustomerEmployer(employer).orElseThrow(() -> new IllegalStateException("Mainframe did not accept the employer update"));
    }

    public boolean deleteEmployer(String customerNumber, long employerId) throws IOException {
        return employerGateway.deleteEmployerByCustomerNumberAndId(employerId, customerNumber);
    }

    // ---- CustomerProfileSource ----
    // LoanService calls profileOf before a loan can be started or moved to Pending.

    @Override
    public Optional<CustomerProfile> profileOf(CustomerReference customer) {
        if (customer instanceof CustomerReference.ExistingCustomer existing) {
            return profileOfExisting(existing.customerNumber());
        }
        if (customer instanceof CustomerReference.DraftCustomer draft) {
            return profileOfDraft(draft.draftId());
        }
        throw new IllegalArgumentException("Unknown CustomerReference type: " + customer);
    }

    private Optional<CustomerProfile> profileOfExisting(String customerNumber) {
        try {
            Optional<Customer> found = getCustomer(customerNumber);
            if (found.isEmpty()) {
                return Optional.empty();
            }
            List<Address> addresses = listAddresses(customerNumber);
            List<PhoneNumber> phones = listPhones(customerNumber);
            List<Email> emails = listEmails(customerNumber);
            List<Employer> employers = listEmployers(customerNumber);
            return Optional.of(buildProfile(addresses, phones, emails, employers));
        } catch (IOException e) {
            throw new MainframeUnavailableException("Could not reach the mainframe to build the customer profile", e);
        }
    }

    /**
     * Reads a CUSTOMER draft's saved payload and builds its profile from the
     * address/phone/email/employer records bundled inside it. Does not touch the
     * mainframe, so it works fully offline.
     *
     * @throws IllegalArgumentException if the draft exists but is not a CUSTOMER
     *                                  draft, or its payload cannot be read
     */
    private Optional<CustomerProfile> profileOfDraft(String draftId) {
        Optional<Draft> found = drafts.findById(draftId);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        Draft draft = found.get();
        if (draft.getType() != DraftType.CUSTOMER) {
            throw new IllegalArgumentException("draft " + draftId + " is not a customer");
        }
        CustomerContent content = CustomerPayloads.readCustomer(draft.getPayload());
        return Optional.of(buildProfile(content.addresses(), content.phoneNumbers(), content.emails(), content.employers()));
    }

    /**
     * Pure computation of a CustomerProfile from already-known data. Used for both
     * the existing-customer and draft-customer paths above, so the three facts are
     * computed the same way regardless of where the data came from.
     */
    public static CustomerProfile buildProfile(List<Address> addresses, List<PhoneNumber> phones, List<Email> emails, List<Employer> employers) {
        boolean hasMailingAddress = addresses != null && addresses.stream().anyMatch(a -> Boolean.TRUE.equals(a.getIsMailing()));
        boolean hasPhoneOrEmail = (phones != null && !phones.isEmpty()) || (emails != null && !emails.isEmpty());
        boolean hasEmployer = employers != null && !employers.isEmpty();
        return new CustomerProfile(hasMailingAddress, hasPhoneOrEmail, hasEmployer);
    }

    // ---- Pagination helper ----

    @FunctionalInterface
    private interface PageFetcher<T> {
        PageResult<T> fetch(long page) throws IOException;
    }

    private static <T> List<T> fetchAllPages(PageFetcher<T> fetcher) throws IOException {
        List<T> all = new ArrayList<>();
        long page = 1;
        while (true) {
            PageResult<T> result = fetcher.fetch(page);
            List<T> items = result.getList();
            all.addAll(items);
            if (items.isEmpty() || all.size() >= result.getTotal()) {
                break;
            }
            page++;
        }
        return all;
    }

    private static void validateOrThrow(List<ValidationError> errors) {
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}
