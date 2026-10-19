package lavs.ui.service;

import lavs.business.model.Address;
import lavs.business.model.Customer;
import lavs.business.model.Email;
import lavs.business.model.Note;
import lavs.business.model.PhoneNumber;
import lavs.business.repository.CustomerSearchResult;
import lavs.business.repository.MainframeUnavailableException;
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

    private final CustomerAdapter customers;
    private final CustomerPhoneNumberAdapter phones;
    private final CustomerEmailAdapter emails;
    private final CustomerAddressAdapter addresses;
    private final CustomerEmployerAdapter employers;
    private final CustomerNoteAdapter notes;
    private final MainframeLoanLookup loanLookup;

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
        Address address = preferred(io(() -> addresses.ListCustomerAddresses(1, customerNumber, LIST_SIZE)).getList(),
                a -> Boolean.TRUE.equals(a.getIsPrimary()));

        List<Employer> employerList = io(() -> employers.ListCustomerEmployers(1, customerNumber, LIST_SIZE)).getList()
                .stream()
                .map(e -> new Employer(orElse(e.get_name(), "Employer"), String.valueOf(e.get_id())))
                .toList();

        String noteText = io(() -> notes.ListCustomerNotes(1, customerNumber, LIST_SIZE)).getList().stream()
                .map(Note::get_lines)
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining("\n"));

        return new Details(customerNumber, nz(customer.get_firstName()), nz(customer.get_familyName()),
                phone == null ? "" : phoneText(phone),
                email == null ? "" : nz(email.getAddress()),
                address == null ? "" : nz(address.getLine1()),
                address == null ? "" : addressTail(address),
                noteText, employerList, loanLookup.loansOf(customerNumber));
    }

    @Override
    public Map<String, String> save(Details details) {
        throw new NotSupportedYetException(
                "Saving customer changes is not connected to the mainframe yet. Your changes are still on screen.");
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
