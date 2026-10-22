package lavs.business.service;

import lavs.business.model.Address;
import lavs.business.model.ContactChanges;
import lavs.business.model.Customer;
import lavs.business.model.CustomerPayloads;
import lavs.business.model.CustomerPayloads.CustomerContent;
import lavs.business.model.CustomerProfile;
import lavs.business.model.CustomerReference;
import lavs.business.model.Draft;
import lavs.business.model.DraftType;
import lavs.business.model.Email;
import lavs.business.model.Employer;
import lavs.business.model.Note;
import lavs.business.model.PhoneNumber;
import lavs.business.repository.CustomerAddressGateway;
import lavs.business.repository.CustomerEmailGateway;
import lavs.business.repository.CustomerEmployerGateway;
import lavs.business.repository.CustomerGateway;
import lavs.business.repository.CustomerNoteGateway;
import lavs.business.repository.CustomerPhoneNumberGateway;
import lavs.business.repository.CustomerSearchResult;
import lavs.business.repository.DraftRepository;
import lavs.business.repository.MainframeUnavailableException;
import lavs.business.repository.PageResult;
import lavs.business.repository.RecordRejectedException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    /** Organisation new customers are registered under until the team decides where it comes from. */
    public static final long DEFAULT_ORGANISATION = 1;

    /** Title given to a note saved without one. */
    public static final String DEFAULT_NOTE_TYPE = "General";

    /** Longest note the screens accept, in characters. */
    public static final int MAX_NOTE_LENGTH = 2000;

    /** Types given to a phone or email added without one, as the details screen has no type picker. */
    public static final String NEW_PHONE_TYPE = "Mobile";
    public static final String NEW_EMAIL_TYPE = "Personal";

    private final CustomerGateway customerGateway;
    private final CustomerAddressGateway addressGateway;
    private final CustomerPhoneNumberGateway phoneGateway;
    private final CustomerEmailGateway emailGateway;
    private final CustomerEmployerGateway employerGateway;
    private final CustomerNoteGateway noteGateway;
    private final ValidationService validationService;
    private final DraftRepository drafts;
    private final BackgroundSync sync;

    public CustomerService(CustomerGateway customerGateway,
                            CustomerAddressGateway addressGateway,
                            CustomerPhoneNumberGateway phoneGateway,
                            CustomerEmailGateway emailGateway,
                            CustomerEmployerGateway employerGateway,
                            CustomerNoteGateway noteGateway,
                            ValidationService validationService,
                            DraftRepository drafts,
                            BackgroundSync sync) {
        this.customerGateway = customerGateway;
        this.addressGateway = addressGateway;
        this.phoneGateway = phoneGateway;
        this.emailGateway = emailGateway;
        this.employerGateway = employerGateway;
        this.noteGateway = Objects.requireNonNull(noteGateway, "noteGateway must not be null");
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

    /**
     * Registers a brand new customer on the mainframe. On top of the field rules,
     * a new customer needs a title, family name, nationality and a real date of
     * birth (YYYY-MM-DD) that is not in the future.
     *
     * @throws ValidationException listing every problem found
     */
    public Customer registerCustomer(Customer customer) throws IOException {
        Objects.requireNonNull(customer, "customer must not be null");
        List<ValidationError> errors = new ArrayList<>();
        requireText(errors, "title", customer.get_title(), "Title is required");
        requireText(errors, "familyName", customer.get_familyName(), "Family name is required");
        requireText(errors, "nationality", customer.get_nationality(), "Nationality is required");
        String dob = customer.get_dateOfBirth();
        if (dob == null || dob.isBlank()) {
            errors.add(new ValidationError("dateOfBirth", "Date of birth is required"));
        } else {
            try {
                if (LocalDate.parse(dob).isAfter(LocalDate.now())) {
                    errors.add(new ValidationError("dateOfBirth", "Date of birth cannot be in the future"));
                }
            } catch (DateTimeParseException e) {
                errors.add(new ValidationError("dateOfBirth", "Date of birth must be a real date"));
            }
        }
        for (ValidationError error : validationService.validateCustomer(customer)) {
            if (errors.stream().noneMatch(e -> e.getField().equals(error.getField()))) {
                errors.add(error);
            }
        }
        validateOrThrow(errors);
        if (customer.getOrganisation() == 0) {
            customer.setOrganisation(DEFAULT_ORGANISATION);
        }
        return customerGateway.createCustomer(customer)
                .orElseThrow(() -> new RecordRejectedException("The mainframe did not accept this customer.", Map.of()));
    }

    /**
     * One page of customers whose name or number matches.
     *
     * @param term what to look for; null or blank lists everyone
     * @param page the page, counting from 1
     */
    @SuppressWarnings("unchecked")
    public CustomerSearchResult<Customer> search(String term, long page, long pageSize) throws IOException {
        String cleaned = term == null || term.isBlank() ? null : term.trim();
        CustomerSearchResult<Customer> result = customerGateway.searchCustomer(page, cleaned, pageSize);
        if (result == null) {
            throw new IOException("The mainframe did not answer the search.");
        }
        return result;
    }

    /** Every customer matching {@code term}, reading at most {@code maxPages} pages of 100. */
    public List<Customer> searchAll(String term, int maxPages) throws IOException {
        List<Customer> found = new ArrayList<>();
        for (long page = 1; page <= maxPages; page++) {
            CustomerSearchResult<Customer> result = search(term, page, PAGE_SIZE);
            found.addAll(result.getList());
            if (result.getList().isEmpty() || found.size() >= result.getTotal()) {
                break;
            }
        }
        return found;
    }

    /** The phone marked primary, or the first one when none is. */
    public Optional<PhoneNumber> primaryPhone(String customerNumber) throws IOException {
        return Optional.ofNullable(preferred(listPhones(customerNumber), PhoneNumber::is_isPrimary));
    }

    /** The email marked primary, or the first one when none is. */
    public Optional<Email> primaryEmail(String customerNumber) throws IOException {
        return Optional.ofNullable(preferred(listEmails(customerNumber), Email::isPrimary));
    }

    /**
     * The address marked primary (or the first one), read in full: the list the
     * mainframe gives leaves out the street lines.
     */
    public Optional<Address> primaryAddress(String customerNumber) throws IOException {
        Address summary = preferred(listAddresses(customerNumber), a -> Boolean.TRUE.equals(a.getIsPrimary()));
        if (summary == null) {
            return Optional.empty();
        }
        Address full = addressGateway.findAddressByCustomerNumberAndId(summary.getId(), customerNumber).orElse(summary);
        full.setCustomerNumber(customerNumber);
        return Optional.of(full);
    }

    /** The customer's notes, each read in full (the list leaves out the text). */
    public List<Note> listNotes(String customerNumber) throws IOException {
        List<Note> notes = new ArrayList<>();
        for (Note summary : fetchAllPages(page -> noteGateway.ListCustomerNotes(page, customerNumber, PAGE_SIZE))) {
            notes.add(noteGateway.findNoteByCustomerNumberAndId(summary.get_id(), customerNumber).orElse(summary));
        }
        return notes;
    }

    public Optional<Note> findNote(String customerNumber, long noteId) throws IOException {
        Optional<Note> found = noteGateway.findNoteByCustomerNumberAndId(noteId, customerNumber);
        found.ifPresent(n -> n.set_customerNumber(customerNumber));
        return found;
    }

    /**
     * Adds a note to the customer. A note without a title is called "General".
     *
     * @throws ValidationException if the note is empty or too long
     * @throws RecordRejectedException if the mainframe refused the note
     */
    public Note addNote(Note note) throws IOException {
        validateOrThrow(noteErrors(note));
        return noteGateway.addCustomerNote(note)
                .orElseThrow(() -> new RecordRejectedException("The mainframe did not accept the new note.", Map.of()));
    }

    /**
     * Saves changes to a note.
     *
     * @throws ValidationException if the note is empty or too long
     * @throws RecordRejectedException if the mainframe refused the change
     */
    public Note updateNote(Note note) throws IOException {
        validateOrThrow(noteErrors(note));
        return noteGateway.updateCustomerNote(note)
                .orElseThrow(() -> new RecordRejectedException("The mainframe did not accept the note change.", Map.of()));
    }

    public boolean deleteNote(String customerNumber, long noteId) throws IOException {
        return noteGateway.deleteNoteByCustomerNumberAndId(noteId, customerNumber);
    }

    /** Field names: lines, type. Fills in the default title. */
    private static List<ValidationError> noteErrors(Note note) {
        Objects.requireNonNull(note, "note must not be null");
        List<ValidationError> errors = new ArrayList<>();
        if (text(note.get_type()).isEmpty()) {
            note.set_type(DEFAULT_NOTE_TYPE);
        }
        String lines = note.get_lines() == null ? "" : note.get_lines().strip();
        note.set_lines(lines);
        if (lines.isEmpty()) {
            errors.add(new ValidationError("lines", "Write something in the note"));
        } else if (lines.length() > MAX_NOTE_LENGTH) {
            errors.add(new ValidationError("lines", "A note can be at most " + MAX_NOTE_LENGTH + " characters"));
        }
        if (note.get_type().length() > 40) {
            errors.add(new ValidationError("type", "The title can be at most 40 characters"));
        }
        return errors;
    }

    public Optional<Employer> findEmployer(String customerNumber, long employerId) throws IOException {
        return employerGateway.findEmployerByCustomerNumberAndId(employerId, customerNumber);
    }

    /**
     * Brings the customer's name, primary phone and email in line with {@code changes},
     * and deletes the employers and addresses listed. Only what differs from the
     * mainframe is sent. Everything is checked before anything is sent; if one write is
     * refused, the ones before it stay saved and calling again only sends what still differs.
     *
     * <p>The primary address can only be removed together with every other address;
     * otherwise another address must be made primary first.
     *
     * <p>The primary phone number and primary email follow the same rule: they can only be
     * removed together with every other one, so another must be made primary first.
     *
     * <p>Notes listed in removedNoteIds are deleted, then phone numbers and emails, last.
     *
     * <p>Error fields: firstName, familyName, phone, email, addresses, phones, emails.
     *
     * @throws ValidationException listing every problem found; nothing was sent
     * @throws RecordRejectedException if the customer is gone or the mainframe refused a change
     */
    public void saveContactChanges(ContactChanges changes) throws IOException {
        Objects.requireNonNull(changes, "changes must not be null");
        String number = changes.customerNumber();
        Customer customer = getCustomer(number)
                .orElseThrow(() -> new RecordRejectedException("That customer no longer exists.", Map.of()));
        List<ValidationError> errors = new ArrayList<>();

        boolean nameChanged = !changes.firstName().equals(text(customer.get_firstName()))
                || !changes.familyName().equals(text(customer.get_familyName()));
        if (nameChanged) {
            customer.set_firstName(changes.firstName());
            customer.set_familyName(changes.familyName());
            requireText(errors, "familyName", changes.familyName(), "Family name is required");
            for (ValidationError error : validationService.validateCustomer(customer)) {
                addOnce(errors, error.getField(), labelled("familyName".equals(error.getField())
                        ? "Family name" : "First name", error.getMessage()));
            }
        }

        PhoneNumber phone = phoneChange(number, changes, errors);
        Email email = emailChange(number, changes.email(), errors);

        if (!changes.removedAddressIds().isEmpty()) {
            List<Address> addresses = listAddresses(number);
            boolean removesPrimary = addresses.stream().anyMatch(a -> Boolean.TRUE.equals(a.getIsPrimary())
                    && changes.removedAddressIds().contains(a.getId()));
            boolean keepsSome = addresses.stream().anyMatch(a -> !changes.removedAddressIds().contains(a.getId()));
            if (removesPrimary && keepsSome) {
                addOnce(errors, "addresses", "Make another address primary before removing the primary address");
            }
        }
        if (!changes.removedPhoneIds().isEmpty()
                && removesPrimaryButNotAll(listPhones(number), PhoneNumber::get_id, PhoneNumber::is_isPrimary,
                changes.removedPhoneIds())) {
            addOnce(errors, "phones", "Make another phone number primary before removing the primary phone number");
        }
        if (!changes.removedEmailIds().isEmpty()
                && removesPrimaryButNotAll(listEmails(number), Email::getId, Email::isPrimary,
                changes.removedEmailIds())) {
            addOnce(errors, "emails", "Make another email primary before removing the primary email");
        }
        validateOrThrow(errors);

        if (nameChanged && customerGateway.updateCustomer(customer).isEmpty()) {
            throw refused("name");
        }
        if (phone != null && (phone.get_id() == 0
                ? phoneGateway.addCustomerPhoneNumber(phone)
                : phoneGateway.updateCustomerNumber(phone)).isEmpty()) {
            throw refused("phone number");
        }
        if (email != null && (email.getId() == 0
                ? emailGateway.addCustomerEmail(email)
                : emailGateway.updateCustomerEmail(email)).isEmpty()) {
            throw refused("email");
        }
        for (long employerId : changes.removedEmployerIds()) {
            if (!deleteEmployer(number, employerId)) {
                throw refused("employer removal");
            }
        }
        for (long addressId : changes.removedAddressIds()) {
            if (!deleteAddress(number, addressId)) {
                throw refused("address removal");
            }
        }
        for (long noteId : changes.removedNoteIds()) {
            if (!deleteNote(number, noteId)) {
                throw refused("note removal");
            }
        }
        for (long phoneId : changes.removedPhoneIds()) {
            if (!deletePhone(number, phoneId)) {
                throw refused("phone number removal");
            }
        }
        for (long emailId : changes.removedEmailIds()) {
            if (!deleteEmail(number, emailId)) {
                throw refused("email removal");
            }
        }
    }

    /** True when the primary one is among those removed while others would stay. */
    private static <T> boolean removesPrimaryButNotAll(List<T> items, java.util.function.ToLongFunction<T> id,
                                                       java.util.function.Predicate<T> isPrimary, List<Long> removed) {
        boolean removesPrimary = items.stream().anyMatch(i -> isPrimary.test(i) && removed.contains(id.applyAsLong(i)));
        boolean keepsSome = items.stream().anyMatch(i -> !removed.contains(id.applyAsLong(i)));
        return removesPrimary && keepsSome;
    }

    /** @return the phone record to send, or null when nothing changed or the change is invalid */
    private PhoneNumber phoneChange(String customerNumber, ContactChanges changes, List<ValidationError> errors)
            throws IOException {
        PhoneNumber current = primaryPhone(customerNumber).orElse(null);
        String code = changes.phoneCountryCode();
        String digits = changes.phoneNumber();
        if (current != null && digits.equals(text(current.get_number()))
                && (code == null || code.equals(text(current.get_countryCode())))) {
            return null;
        }
        if (digits.isEmpty()) {
            if (current != null) {
                addOnce(errors, "phone", "A phone number cannot be removed here");
            }
            return null;
        }
        PhoneNumber phone;
        if (current == null) {
            if (code == null) {
                addOnce(errors, "phone", "Choose a country code for the new phone number");
                return null;
            }
            phone = new PhoneNumber(false, code, null, customerNumber, 0, true, null, NEW_PHONE_TYPE);
        } else {
            phone = phoneGateway.findCustomerNumberByNumberAndId(current.get_id(), customerNumber).orElse(current);
            phone.set_customerNumber(customerNumber);
            if (code != null) {
                phone.set_countryCode(code);
            }
        }
        phone.set_number(digits);
        for (ValidationError error : validationService.validatePhone(phone)) {
            addOnce(errors, "phone", labelled("Phone number", error.getMessage()));
        }
        return phone;
    }

    /** @return the email record to send, or null when nothing changed or the change is invalid */
    private Email emailChange(String customerNumber, String address, List<ValidationError> errors) throws IOException {
        Email current = primaryEmail(customerNumber).orElse(null);
        if (address.equals(current == null ? "" : text(current.getAddress()))) {
            return null;
        }
        if (address.isEmpty()) {
            if (current != null) {
                addOnce(errors, "email", "An email cannot be removed here");
            }
            return null;
        }
        Email email;
        if (current == null) {
            email = new Email(address, null, customerNumber, 0, true, null, NEW_EMAIL_TYPE);
        } else {
            email = emailGateway.findEmailByCustomerNumberAndId(current.getId(), customerNumber).orElse(current);
            email.setCustomerNumber(customerNumber);
            email.setAddress(address);
        }
        for (ValidationError error : validationService.validateEmail(email)) {
            addOnce(errors, "email", labelled("Email", error.getMessage()));
        }
        return email;
    }

    private static RecordRejectedException refused(String what) {
        return new RecordRejectedException("The mainframe did not accept the " + what
                + " change. Check the details and try again.", Map.of());
    }

    /** "Must be a valid email address" reads "Email must be a valid email address". */
    private static String labelled(String label, String message) {
        if (message.startsWith("Must") || message.startsWith("Is ")) {
            return label + " " + Character.toLowerCase(message.charAt(0)) + message.substring(1);
        }
        return message;
    }

    private static void addOnce(List<ValidationError> errors, String field, String message) {
        if (errors.stream().noneMatch(e -> e.getField().equals(field))) {
            errors.add(new ValidationError(field, message));
        }
    }

    private static void requireText(List<ValidationError> errors, String field, String value, String message) {
        if (value == null || value.isBlank()) {
            addOnce(errors, field, message);
        }
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }

    private static <T> T preferred(List<T> items, java.util.function.Predicate<T> isPrimary) {
        return items.stream().filter(isPrimary).findFirst().orElse(items.isEmpty() ? null : items.get(0));
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

    /**
     * Every address of the customer, each read in full: the list the mainframe gives
     * leaves out the street lines, suburb and postcode. Primary first, then mailing.
     */
    public List<Address> listAddressesInFull(String customerNumber) throws IOException {
        List<Address> full = new ArrayList<>();
        for (Address summary : listAddresses(customerNumber)) {
            Address address = addressGateway.findAddressByCustomerNumberAndId(summary.getId(), customerNumber)
                    .orElse(summary);
            address.setCustomerNumber(customerNumber);
            full.add(address);
        }
        full.sort(java.util.Comparator
                .comparing((Address a) -> !Boolean.TRUE.equals(a.getIsPrimary()))
                .thenComparing(a -> !Boolean.TRUE.equals(a.getIsMailing())));
        return full;
    }

    public Optional<Address> findAddress(String customerNumber, long addressId) throws IOException {
        Optional<Address> found = addressGateway.findAddressByCustomerNumberAndId(addressId, customerNumber);
        found.ifPresent(a -> a.setCustomerNumber(customerNumber));
        return found;
    }

    /**
     * Adds an address. A customer's first address always becomes their primary one;
     * a new primary address takes the flag off the old one. An "Other" address needs
     * a description in otherType.
     *
     * @throws ValidationException listing every problem found
     * @throws RecordRejectedException if the mainframe refused the address
     */
    public Address addAddress(Address address) throws IOException {
        validateOrThrow(addressErrors(address));
        if (listAddresses(address.getCustomerNumber()).isEmpty()) {
            address.setIsPrimary(true);
        }
        if (Boolean.TRUE.equals(address.getIsPrimary())) {
            clearExistingPrimaryAddress(address.getCustomerNumber(), 0);
        }
        return addressGateway.addCustomerAddress(address)
                .orElseThrow(() -> new RecordRejectedException("The mainframe did not accept the new address.", Map.of()));
    }

    /**
     * Saves changes to an address. Making it primary takes the flag off the old primary.
     *
     * @throws ValidationException listing every problem found
     * @throws RecordRejectedException if the mainframe refused the change
     */
    public Address updateAddress(Address address) throws IOException {
        validateOrThrow(addressErrors(address));
        if (Boolean.TRUE.equals(address.getIsPrimary())) {
            clearExistingPrimaryAddress(address.getCustomerNumber(), address.getId());
        }
        return addressGateway.updateCustomerAddress(address)
                .orElseThrow(() -> new RecordRejectedException("The mainframe did not accept the address change.", Map.of()));
    }

    private List<ValidationError> addressErrors(Address address) {
        List<ValidationError> errors = new ArrayList<>(validationService.validateAddress(address));
        if ("Other".equalsIgnoreCase(text(address.getType())) && text(address.getOtherType()).isEmpty()) {
            errors.add(new ValidationError("otherType", "Describe what kind of address this is"));
        }
        if (address.getIsPrimary() == null) {
            address.setIsPrimary(false);
        }
        if (address.getIsMailing() == null) {
            address.setIsMailing(false);
        }
        return errors;
    }

    public boolean deleteAddress(String customerNumber, long addressId) throws IOException {
        return addressGateway.deleteCustomerAddress(customerNumber, addressId);
    }

    /**
     * Takes the primary flag off every other address. The list leaves out the street
     * lines, so each one is read in full first; sending the list item back would wipe them.
     */
    private void clearExistingPrimaryAddress(String customerNumber, long keepId) throws IOException {
        for (Address summary : listAddresses(customerNumber)) {
            if (Boolean.TRUE.equals(summary.getIsPrimary()) && summary.getId() != keepId) {
                Address existing = addressGateway.findAddressByCustomerNumberAndId(summary.getId(), customerNumber)
                        .orElseThrow(() -> new RecordRejectedException(
                                "The current primary address could not be read, so it was left unchanged.", Map.of()));
                existing.setCustomerNumber(customerNumber);
                existing.setIsPrimary(false);
                addressGateway.updateCustomerAddress(existing);
            }
        }
    }

    // ---- Phone ----

    public List<PhoneNumber> listPhones(String customerNumber) throws IOException {
        return fetchAllPages(page -> phoneGateway.ListCustomerPhoneNumbers(page, customerNumber, PAGE_SIZE));
    }

    /** One phone number in full, for changing it on its own form. */
    public Optional<PhoneNumber> findPhone(String customerNumber, long phoneId) throws IOException {
        Optional<PhoneNumber> found = phoneGateway.findCustomerNumberByNumberAndId(phoneId, customerNumber);
        found.ifPresent(p -> p.set_customerNumber(customerNumber));
        return found;
    }

    public PhoneNumber addPhone(PhoneNumber phone) throws IOException {
        validateOrThrow(validationService.validatePhone(phone));
        if (phone.is_isPrimary()) {
            clearExistingPrimaryPhone(phone.get_customerNumber(), 0);
        }
        return phoneGateway.addCustomerPhoneNumber(phone).orElseThrow(() -> new IllegalStateException("Mainframe did not accept the new phone"));
    }

    public PhoneNumber updatePhone(PhoneNumber phone) throws IOException {
        validateOrThrow(validationService.validatePhone(phone));
        if (phone.is_isPrimary()) {
            clearExistingPrimaryPhone(phone.get_customerNumber(), phone.get_id());
        }
        return phoneGateway.updateCustomerNumber(phone).orElseThrow(() -> new IllegalStateException("Mainframe did not accept the phone update"));
    }

    public boolean deletePhone(String customerNumber, long phoneId) throws IOException {
        return phoneGateway.deleteCustomerNumberByNumberAndId(phoneId, customerNumber);
    }

    /** Like the address version: reads each old primary in full so nothing else is lost. */
    private void clearExistingPrimaryPhone(String customerNumber, long keepId) throws IOException {
        for (PhoneNumber summary : listPhones(customerNumber)) {
            if (summary.is_isPrimary() && summary.get_id() != keepId) {
                PhoneNumber existing = phoneGateway.findCustomerNumberByNumberAndId(summary.get_id(), customerNumber)
                        .orElse(summary);
                existing.set_customerNumber(customerNumber);
                if (existing.get_number() == null) {
                    existing.set_number(summary.get_number());
                }
                existing.set_isPrimary(false);
                phoneGateway.updateCustomerNumber(existing);
            }
        }
    }

    // ---- Email ----

    public List<Email> listEmails(String customerNumber) throws IOException {
        return fetchAllPages(page -> emailGateway.ListCustomerEmails(page, customerNumber, PAGE_SIZE));
    }

    /** One email in full, for changing it on its own form. */
    public Optional<Email> findEmail(String customerNumber, long emailId) throws IOException {
        Optional<Email> found = emailGateway.findEmailByCustomerNumberAndId(emailId, customerNumber);
        found.ifPresent(e -> e.setCustomerNumber(customerNumber));
        return found;
    }

    public Email addEmail(Email email) throws IOException {
        validateOrThrow(validationService.validateEmail(email));
        if (email.isPrimary()) {
            clearExistingPrimaryEmail(email.getCustomerNumber(), 0);
        }
        return emailGateway.addCustomerEmail(email).orElseThrow(() -> new IllegalStateException("Mainframe did not accept the new email"));
    }

    public Email updateEmail(Email email) throws IOException {
        validateOrThrow(validationService.validateEmail(email));
        if (email.isPrimary()) {
            clearExistingPrimaryEmail(email.getCustomerNumber(), email.getId());
        }
        return emailGateway.updateCustomerEmail(email).orElseThrow(() -> new IllegalStateException("Mainframe did not accept the email update"));
    }

    public boolean deleteEmail(String customerNumber, long emailId) throws IOException {
        return emailGateway.deleteCustomerEmail(customerNumber, emailId);
    }

    /** Like the address version: reads each old primary in full so nothing else is lost. */
    private void clearExistingPrimaryEmail(String customerNumber, long keepId) throws IOException {
        for (Email summary : listEmails(customerNumber)) {
            if (summary.isPrimary() && summary.getId() != keepId) {
                Email existing = emailGateway.findEmailByCustomerNumberAndId(summary.getId(), customerNumber)
                        .orElse(summary);
                existing.setCustomerNumber(customerNumber);
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

    /**
     * Adds an employer to the customer.
     *
     * @throws ValidationException if a field breaks a rule (name and type are required)
     * @throws RecordRejectedException if the mainframe refused the employer
     */
    public Employer addEmployer(Employer employer) throws IOException {
        validateOrThrow(validationService.validateEmployer(employer));
        return employerGateway.addCustomerEmployer(employer)
                .orElseThrow(() -> new RecordRejectedException("The mainframe did not accept the new employer.", Map.of()));
    }

    /**
     * Saves changes to an employer.
     *
     * @throws ValidationException if a field breaks a rule (name and type are required)
     * @throws RecordRejectedException if the mainframe refused the change
     */
    public Employer updateEmployer(Employer employer) throws IOException {
        validateOrThrow(validationService.validateEmployer(employer));
        return employerGateway.updateCustomerEmployer(employer)
                .orElseThrow(() -> new RecordRejectedException("The mainframe did not accept the employer change.", Map.of()));
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
