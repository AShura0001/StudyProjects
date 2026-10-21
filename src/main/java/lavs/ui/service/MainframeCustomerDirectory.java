package lavs.ui.service;

import lavs.business.model.Address;
import lavs.business.model.Customer;
import lavs.business.model.Email;
import lavs.business.model.Note;
import lavs.business.model.PhoneNumber;
import lavs.business.repository.CustomerSearchResult;
import lavs.business.repository.MainframeUnavailableException;
import lavs.business.service.ValidationError;
import lavs.business.service.ValidationService;
import lavs.infrastructure.mainframe.CustomerAddressAdapter;
import lavs.infrastructure.mainframe.CustomerAdapter;
import lavs.infrastructure.mainframe.CustomerEmailAdapter;
import lavs.infrastructure.mainframe.CustomerEmployerAdapter;
import lavs.infrastructure.mainframe.CustomerNoteAdapter;
import lavs.infrastructure.mainframe.CustomerPhoneNumberAdapter;
import mainframe.client.Connection;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Customer screens' data from the real mainframe, through the gateway adapters.
 * Mainframe pages count from 1; the screens count from 0.
 */
public class MainframeCustomerDirectory implements CustomerDirectory {

    private static final long SEARCH_PAGE_SIZE = 100;
    private static final int SEARCH_MAX_PAGES = 5;
    private static final long LIST_SIZE = 50;
    /** Organisation new customers are registered under until the team decides where it comes from. */
    private static final long ORGANISATION = 1;
    /** Types given to a phone or email added from the details screen, which has no type picker. */
    private static final String NEW_PHONE_TYPE = "Mobile";
    private static final String NEW_EMAIL_TYPE = "Personal";

    private final CustomerAdapter customers;
    private final CustomerPhoneNumberAdapter phones;
    private final CustomerEmailAdapter emails;
    private final CustomerAddressAdapter addresses;
    private final CustomerEmployerAdapter employers;
    private final CustomerNoteAdapter notes;
    private final MainframeLoanLookup loanLookup;
    private final ValidationService validation = new ValidationService();

    public MainframeCustomerDirectory(Connection connection) {
        Objects.requireNonNull(connection, "connection must not be null");
        this.customers = new CustomerAdapter(connection);
        this.phones = new CustomerPhoneNumberAdapter(connection);
        this.emails = new CustomerEmailAdapter(connection);
        this.addresses = new CustomerAddressAdapter(connection);
        this.employers = new CustomerEmployerAdapter(connection);
        this.notes = new CustomerNoteAdapter(connection);
        this.loanLookup = new MainframeLoanLookup(connection);
    }

    @Override
    public List<Match> search(String term) {
        if (term == null || term.isBlank()) {
            return List.of();
        }
        List<Match> found = new ArrayList<>();
        for (long page = 1; page <= SEARCH_MAX_PAGES; page++) {
            CustomerSearchResult<Customer> result = searchPage(page, term.trim(), SEARCH_PAGE_SIZE);
            for (Customer customer : result.getList()) {
                found.add(toMatch(customer));
            }
            if (result.getList().isEmpty() || found.size() >= result.getTotal()) {
                break;
            }
        }
        return found;
    }

    @Override
    public int count() {
        return (int) searchPage(1, null, 1).getTotal();
    }

    @Override
    public List<Match> page(int page, int size) {
        return searchPage(page + 1L, null, size).getList().stream()
                .map(MainframeCustomerDirectory::toMatch)
                .toList();
    }

    @Override
    public Details details(String customerNumber) {
        Optional<Customer> found = io(() -> customers.findByCustomerNumber(customerNumber));
        if (found.isEmpty()) {
            return null;
        }
        Customer customer = found.get();

        PhoneNumber phone = preferred(io(() -> phones.ListCustomerPhoneNumbers(1, customerNumber, LIST_SIZE)).getList(),
                PhoneNumber::is_isPrimary);
        Email email = preferred(io(() -> emails.ListCustomerEmails(1, customerNumber, LIST_SIZE)).getList(),
                Email::isPrimary);
        Address address = primaryAddress(customerNumber);

        List<Employer> employerList = employerList(customerNumber);

        return new Details(customerNumber, nz(customer.get_firstName()), nz(customer.get_familyName()),
                phone == null ? "" : phoneText(phone),
                email == null ? "" : nz(email.getAddress()),
                address == null ? "" : nz(address.getLine1()),
                address == null ? "" : addressTail(address),
                noteText(customerNumber), employerList, loanLookup.loansOf(customerNumber));
    }

    /**
     * Compares the screen with what the mainframe holds now and sends only what changed:
     * the name, the primary phone, email and address line 1, and employers removed on
     * screen. Everything is checked before anything is sent. If one write is refused,
     * the ones before it stay saved, and saving again only sends what is still different.
     */
    @Override
    public Map<String, String> save(Details edited) {
        String number = edited.customerNumber();
        Map<String, String> errors = new LinkedHashMap<>();

        Optional<Customer> found = io(() -> customers.findByCustomerNumber(number));
        if (found.isEmpty()) {
            errors.put("form", "That customer no longer exists.");
            return errors;
        }

        // Work out what changed, and check it, before sending anything.
        Customer customer = found.get();
        boolean nameChanged = !edited.firstName().equals(nz(customer.get_firstName()))
                || !edited.lastName().equals(nz(customer.get_familyName()));
        if (nameChanged) {
            customer.set_firstName(edited.firstName());
            customer.set_familyName(edited.lastName());
            if (edited.lastName().isBlank()) {
                errors.put("lastName", "Last name is required.");
            }
            addErrors(errors, validation.validateCustomer(customer), Map.of(
                    "firstName", "firstName", "familyName", "lastName"), "Name");
        }

        PhoneNumber phoneChange = phoneChange(number, edited.phone(), errors);
        Email emailChange = emailChange(number, edited.email(), errors);

        Address address = primaryAddress(number);
        Address addressChange = null;
        if (!edited.addressLine1().equals(address == null ? "" : nz(address.getLine1()))) {
            if (address == null) {
                errors.put("addressLine1", "Adding a new address is not available on this screen yet.");
            } else {
                address.setLine1(edited.addressLine1());
                addErrors(errors, validation.validateAddress(address), Map.of("line1", "addressLine1"), "Address");
                addressChange = address;
            }
        }
        if (!edited.addressLine2().equals(address == null ? "" : addressTail(address))) {
            errors.put("addressLine2", "Suburb, city and postcode cannot be changed on this screen yet.");
        }
        if (!edited.notes().equals(noteText(number))) {
            errors.put("notes", "Notes cannot be saved from this screen yet.");
        }

        List<String> keptEmployers = edited.employers().stream().map(Employer::id).toList();
        List<Long> removedEmployers = employerList(number).stream()
                .map(Employer::id)
                .filter(id -> !keptEmployers.contains(id))
                .map(MainframeCustomerDirectory::parseId)
                .filter(id -> id >= 0)
                .toList();

        if (!errors.isEmpty()) {
            return errors;
        }

        // Send the changes.
        if (nameChanged && io(() -> customers.updateCustomer(customer)).isEmpty()) {
            return refused("name");
        }
        if (phoneChange != null && io(() -> phoneChange.get_id() == 0
                ? phones.addCustomerPhoneNumber(phoneChange)
                : phones.updateCustomerNumber(phoneChange)).isEmpty()) {
            return refused("phone number");
        }
        if (emailChange != null && io(() -> emailChange.getId() == 0
                ? emails.addCustomerEmail(emailChange)
                : emails.updateCustomerEmail(emailChange)).isEmpty()) {
            return refused("email");
        }
        Address addressToSave = addressChange;
        if (addressToSave != null && io(() -> addresses.updateCustomerAddress(addressToSave)).isEmpty()) {
            return refused("address");
        }
        for (long id : removedEmployers) {
            if (!io(() -> employers.deleteEmployerByCustomerNumberAndId(id, number))) {
                return refused("employer removal");
            }
        }
        return errors;
    }

    /** @return the phone record to send, or null when the phone did not change or is invalid */
    private PhoneNumber phoneChange(String customerNumber, String text, Map<String, String> errors) {
        PhoneNumber current = preferred(io(() -> phones.ListCustomerPhoneNumbers(1, customerNumber, LIST_SIZE)).getList(),
                PhoneNumber::is_isPrimary);
        if (text.equals(current == null ? "" : phoneText(current))) {
            return null;
        }
        if (text.isBlank()) {
            if (current != null) {
                errors.put("phone", "A phone number cannot be removed from this screen.");
            }
            return null;
        }

        // "+64 21 555 0100" carries its country code; without a "+" the number keeps the current code.
        String code = null;
        String digits = text;
        if (text.startsWith("+")) {
            int space = text.indexOf(' ');
            code = space < 0 ? text : text.substring(0, space);
            digits = space < 0 ? "" : text.substring(space + 1).trim();
        }

        PhoneNumber phone;
        if (current == null) {
            if (code == null) {
                errors.put("phone", "Start a new phone number with its country code, such as +64 21 555 0100.");
                return null;
            }
            phone = new PhoneNumber(false, code, null, customerNumber, 0, true, null, NEW_PHONE_TYPE);
        } else {
            phone = io(() -> phones.findCustomerNumberByNumberAndId(current.get_id(), customerNumber)).orElse(current);
            phone.set_customerNumber(customerNumber);
            if (code != null) {
                phone.set_countryCode(code);
            }
        }
        phone.set_number(digits);
        addErrors(errors, validation.validatePhone(phone), Map.of("number", "phone", "countryCode", "phone"), "Phone number");
        return phone;
    }

    /** @return the email record to send, or null when the email did not change or is invalid */
    private Email emailChange(String customerNumber, String text, Map<String, String> errors) {
        Email current = preferred(io(() -> emails.ListCustomerEmails(1, customerNumber, LIST_SIZE)).getList(),
                Email::isPrimary);
        if (text.equals(current == null ? "" : nz(current.getAddress()))) {
            return null;
        }
        if (text.isBlank()) {
            if (current != null) {
                errors.put("email", "An email cannot be removed from this screen.");
            }
            return null;
        }

        Email email;
        if (current == null) {
            email = new Email(text, null, customerNumber, 0, true, null, NEW_EMAIL_TYPE);
        } else {
            email = io(() -> emails.findEmailByCustomerNumberAndId(current.getId(), customerNumber)).orElse(current);
            email.setCustomerNumber(customerNumber);
            email.setAddress(text);
        }
        addErrors(errors, validation.validateEmail(email), Map.of("address", "email"), "Email");
        return email;
    }

    /** The list leaves out the street lines, so the full record is fetched for the primary address. */
    private Address primaryAddress(String customerNumber) {
        Address summary = preferred(io(() -> addresses.ListCustomerAddresses(1, customerNumber, LIST_SIZE)).getList(),
                a -> Boolean.TRUE.equals(a.getIsPrimary()));
        if (summary == null) {
            return null;
        }
        Address full = io(() -> addresses.findAddressByCustomerNumberAndId(summary.getId(), customerNumber)).orElse(summary);
        full.setCustomerNumber(customerNumber);
        return full;
    }

    private List<Employer> employerList(String customerNumber) {
        return io(() -> employers.ListCustomerEmployers(1, customerNumber, LIST_SIZE)).getList()
                .stream()
                .map(e -> new Employer(orElse(e.get_name(), "Employer"), String.valueOf(e.get_id())))
                .toList();
    }

    private String noteText(String customerNumber) {
        return io(() -> notes.ListCustomerNotes(1, customerNumber, LIST_SIZE)).getList().stream()
                .map(Note::get_lines)
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining("\n"));
    }

    /**
     * Turns validator errors into the screen's field names and readable sentences.
     * Errors on fields the screen does not show go under "form".
     */
    private static void addErrors(Map<String, String> errors, List<ValidationError> found,
                                  Map<String, String> screenFields, String label) {
        for (ValidationError error : found) {
            String message = error.getMessage();
            if (message.startsWith("Must") || message.startsWith("Is")) {
                message = label + " " + Character.toLowerCase(message.charAt(0)) + message.substring(1);
            }
            errors.putIfAbsent(screenFields.getOrDefault(error.getField(), "form"), message + ".");
        }
    }

    private static Map<String, String> refused(String what) {
        return Map.of("form", "The mainframe did not accept the " + what
                + " change. Check the details and try again.");
    }

    @Override
    public Registration register(NewCustomer form) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (form.title().isBlank()) {
            errors.put("title", "Title is required.");
        }
        if (form.firstName().isBlank()) {
            errors.put("firstName", "First name is required.");
        }
        if (form.lastName().isBlank()) {
            errors.put("lastName", "Last name is required.");
        }
        try {
            if (LocalDate.parse(form.dateOfBirth()).isAfter(LocalDate.now())) {
                errors.put("dateOfBirth", "Date of birth cannot be in the future.");
            }
        } catch (DateTimeParseException e) {
            errors.put("dateOfBirth", "Date of birth must look like 1990-04-23.");
        }
        if (form.nationality().isBlank()) {
            errors.put("nationality", "Nationality is required.");
        }
        if (!errors.isEmpty()) {
            return new Registration(null, errors);
        }

        Customer toCreate = new Customer();
        toCreate.set_title(form.title());
        toCreate.set_firstName(form.firstName());
        toCreate.set_familyName(form.lastName());
        toCreate.set_dateOfBirth(form.dateOfBirth());
        toCreate.set_nationality(form.nationality());
        toCreate.setOrganisation(ORGANISATION);

        Optional<Customer> created = io(() -> customers.createCustomer(toCreate));
        if (created.isEmpty() || created.get().get_number() == null) {
            errors.put("form", "The mainframe did not accept this customer. Check the details and try again.");
            return new Registration(null, errors);
        }
        return new Registration(created.get().get_number(), Map.of());
    }

    @Override
    public EmployerDetails employer(String customerNumber, String employerId) {
        long id = parseId(employerId);
        if (id < 0) {
            return null;
        }
        return io(() -> employers.findEmployerByCustomerNumberAndId(id, customerNumber)).map(e -> new EmployerDetails(
                String.valueOf(e.get_id()), nz(e.get_name()), nz(e.get_position()),
                String.format(java.util.Locale.US, "%.2f", e.get_salary()))).orElse(null);
    }

    @Override
    public Map<String, String> saveEmployer(String customerNumber, EmployerDetails employer) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (employer.name().isBlank()) {
            errors.put("name", "Employer name is required.");
        }
        double salary = 0;
        try {
            salary = employer.salary().isBlank() ? 0 : Double.parseDouble(employer.salary().replace(",", ""));
            if (salary < 0) {
                errors.put("salary", "Salary cannot be negative.");
            }
        } catch (NumberFormatException e) {
            errors.put("salary", "Salary must be a number such as 85000.00.");
        }
        if (!errors.isEmpty()) {
            return errors;
        }

        long id = parseId(employer.id());
        Optional<lavs.business.model.Employer> existing = io(() -> employers.findEmployerByCustomerNumberAndId(id, customerNumber));
        if (existing.isEmpty()) {
            errors.put("form", "That employer no longer exists.");
            return errors;
        }
        lavs.business.model.Employer changed = existing.get();
        changed.set_customerNumber(customerNumber);
        changed.set_name(employer.name());
        changed.set_position(employer.position());
        changed.set_salary(salary);
        if (io(() -> employers.updateCustomerEmployer(changed)).isEmpty()) {
            errors.put("form", "The mainframe did not accept the change. Check the details and try again.");
        }
        return errors;
    }

    @Override
    public byte[] photo(String customerNumber) {
        return null;
    }

    @Override
    public void savePhoto(String customerNumber, byte[] image) {
        throw new NotSupportedYetException(
                "Saving a photo is not connected to the mainframe yet. Your changes are still on screen.");
    }

    private static long parseId(String text) {
        try {
            return Long.parseLong(text.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @SuppressWarnings("unchecked")
    private CustomerSearchResult<Customer> searchPage(long page, String term, long size) {
        return (CustomerSearchResult<Customer>) io(() -> customers.searchCustomer(page, term, size));
    }

    private static Match toMatch(Customer customer) {
        String name = (nz(customer.get_firstName()) + " " + nz(customer.get_familyName())).trim();
        return new Match(name.isEmpty() ? "(no name)" : name, nz(customer.get_number()));
    }

    private static <T> T preferred(List<T> items, Predicate<T> isPrimary) {
        return items.stream().filter(isPrimary).findFirst().orElse(items.isEmpty() ? null : items.get(0));
    }

    private static String phoneText(PhoneNumber phone) {
        String code = nz(phone.get_countryCode());
        return (code.isEmpty() ? "" : code + " ") + nz(phone.get_number());
    }

    /** The mainframe splits an address into more parts than the screen has room for. */
    private static String addressTail(Address address) {
        return java.util.stream.Stream.of(address.getLine2(), address.getSuburb(), address.getCity(), address.getPostCode())
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining(", "));
    }

    private static String nz(String value) {
        return value == null ? "" : value.trim();
    }

    private static String orElse(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private interface IoCall<T> {
        T call() throws IOException;
    }

    /** Gateways report network trouble as IOException; the screens expect it unchecked. */
    private static <T> T io(IoCall<T> call) {
        try {
            T result = call.call();
            if (result == null) {
                throw new MainframeUnavailableException("The mainframe did not answer.");
            }
            return result;
        } catch (IOException e) {
            throw new MainframeUnavailableException("The mainframe could not be reached.", e);
        }
    }
}
