package lavs.business.service;

import lavs.business.model.Address;
import lavs.business.model.Customer;
import lavs.business.model.CustomerProfile;
import lavs.business.model.CustomerReference;
import lavs.business.model.Draft;
import lavs.business.model.DraftStatus;
import lavs.business.model.DraftType;
import lavs.business.model.Email;
import lavs.business.model.Employer;
import lavs.business.model.PhoneNumber;
import lavs.business.repository.CustomerAddressGateway;
import lavs.business.repository.CustomerEmailGateway;
import lavs.business.repository.CustomerEmployerGateway;
import lavs.business.repository.CustomerGateway;
import lavs.business.repository.CustomerPhoneNumberGateway;
import lavs.business.repository.CustomerSearchResult;
import lavs.business.repository.FakeDraftRepository;
import lavs.business.repository.FakeDraftUploadGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CustomerServiceTest {

    private CustomerGateway customerGateway;
    private CustomerAddressGateway addressGateway;
    private CustomerPhoneNumberGateway phoneGateway;
    private CustomerEmailGateway emailGateway;
    private CustomerEmployerGateway employerGateway;
    private lavs.business.repository.CustomerNoteGateway noteGateway;
    private FakeDraftRepository drafts;
    private BackgroundSync sync;
    private CustomerService service;

    @BeforeEach
    void setUp() {
        customerGateway = mock(CustomerGateway.class);
        addressGateway = mock(CustomerAddressGateway.class);
        phoneGateway = mock(CustomerPhoneNumberGateway.class);
        emailGateway = mock(CustomerEmailGateway.class);
        employerGateway = mock(CustomerEmployerGateway.class);
        noteGateway = mock(lavs.business.repository.CustomerNoteGateway.class);
        drafts = new FakeDraftRepository();
        sync = new BackgroundSync(new DraftSyncService(drafts, new FakeDraftUploadGateway()),
                new ManualScheduler(), RetryPolicy.standard());
        service = new CustomerService(customerGateway, addressGateway, phoneGateway,
                emailGateway, employerGateway, noteGateway, new ValidationService(), drafts, sync);
    }

    // ---- profileOf: ExistingCustomer ----

    @Test
    void profileOf_existingCustomer_withoutMailingAddress_reportsFalse() throws IOException {
        when(customerGateway.findByCustomerNumber("CUS-1")).thenReturn(Optional.of(new Customer()));
        onePageOf(addressGateway, nonMailingAddress());
        onePageOf(phoneGateway, somePhone());
        onePageOf(emailGateway);
        onePageOf(employerGateway);

        Optional<CustomerProfile> profile = service.profileOf(CustomerReference.existing("CUS-1"));

        assertTrue(profile.isPresent());
        assertFalse(profile.get().hasMailingAddress());
        assertTrue(profile.get().hasPhoneOrEmail());
    }

    @Test
    void profileOf_existingCustomer_withMailingAddressAndPhone_reportsTrue() throws IOException {
        when(customerGateway.findByCustomerNumber("CUS-1")).thenReturn(Optional.of(new Customer()));
        onePageOf(addressGateway, mailingAddress());
        onePageOf(phoneGateway, somePhone());
        onePageOf(emailGateway);
        onePageOf(employerGateway);

        CustomerProfile profile = service.profileOf(CustomerReference.existing("CUS-1")).orElseThrow();

        assertTrue(profile.hasMailingAddress());
        assertTrue(profile.hasPhoneOrEmail());
    }

    @Test
    void profileOf_unknownExistingCustomer_isEmpty() throws IOException {
        when(customerGateway.findByCustomerNumber("CUS-404")).thenReturn(Optional.empty());

        Optional<CustomerProfile> profile = service.profileOf(CustomerReference.existing("CUS-404"));

        assertTrue(profile.isEmpty());
        verifyNoInteractions(addressGateway, phoneGateway, emailGateway, employerGateway);
    }

    // ---- createCustomerDraft ----

    @Test
    void createCustomerDraft_withValidData_savesAPendingCustomerDraft() {
        Draft draft = service.createCustomerDraft(someCustomer(),
                List.of(mailingAddress()), List.of(somePhone()), List.of(), List.of());

        Draft stored = drafts.findById(draft.getLocalId()).orElseThrow();
        assertEquals(DraftType.CUSTOMER, stored.getType());
        assertEquals(DraftStatus.PENDING, stored.getStatus());
        assertTrue(stored.getDependsOn().isEmpty());
    }

    @Test
    void createCustomerDraft_profileOfTheSavedDraft_reflectsItsBundledRecords() {
        Draft draft = service.createCustomerDraft(someCustomer(),
                List.of(mailingAddress()), List.of(somePhone()), List.of(), List.of());

        CustomerProfile profile = service.profileOf(CustomerReference.draft(draft.getLocalId())).orElseThrow();

        assertTrue(profile.hasMailingAddress());
        assertTrue(profile.hasPhoneOrEmail());
        assertFalse(profile.hasEmployer());
    }

    @Test
    void createCustomerDraft_withEmployerBundled_profileReportsHasEmployer() {
        Draft draft = service.createCustomerDraft(someCustomer(),
                List.of(mailingAddress()), List.of(), List.of(someEmail()), List.of(someEmployer()));

        CustomerProfile profile = service.profileOf(CustomerReference.draft(draft.getLocalId())).orElseThrow();

        assertTrue(profile.hasEmployer());
    }

    @Test
    void createCustomerDraft_withoutFirstName_throwsValidationExceptionAndSavesNothing() {
        Customer invalid = new Customer();
        invalid.set_title("Mr");

        assertThrows(ValidationException.class,
                () -> service.createCustomerDraft(invalid, List.of(), List.of(), List.of(), List.of()));

        assertTrue(drafts.findPending().isEmpty());
    }

    @Test
    void createCustomerDraft_withAnInvalidBundledAddress_throwsValidationExceptionAndSavesNothing() {
        Address invalid = new Address();
        invalid.setCustomerNumber("ignored-for-drafts");
        // type, line1, country all left null

        ValidationException thrown = assertThrows(ValidationException.class,
                () -> service.createCustomerDraft(someCustomer(), List.of(invalid), List.of(), List.of(), List.of()));

        assertTrue(thrown.getErrors().size() >= 3);
        assertTrue(drafts.findPending().isEmpty());
    }

    // ---- profileOf: DraftCustomer edge cases ----

    @Test
    void profileOf_unknownDraft_isEmpty() {
        assertTrue(service.profileOf(CustomerReference.draft("no-such-draft")).isEmpty());
    }

    @Test
    void profileOf_draftThatIsNotACustomer_throwsIllegalArgument() {
        Draft loanDraft = sync.saveDraft(DraftType.LOAN, "{\"record\":\"loan\"}");

        assertThrows(IllegalArgumentException.class,
                () -> service.profileOf(CustomerReference.draft(loanDraft.getLocalId())));
    }

    // ---- Pagination ----

    @Test
    void listAddresses_walksEveryPage() throws IOException {
        Address first = mailingAddress();
        Address second = mailingAddress();
        when(addressGateway.ListCustomerAddresses(eq(1L), eq("CUS-1"), anyLong()))
                .thenReturn(new CustomerSearchResult<>(List.of(first), 2, 1));
        when(addressGateway.ListCustomerAddresses(eq(2L), eq("CUS-1"), anyLong()))
                .thenReturn(new CustomerSearchResult<>(List.of(second), 2, 2));

        List<Address> addresses = service.listAddresses("CUS-1");

        assertEquals(2, addresses.size());
    }

    // ---- Primary flag enforcement ----

    @Test
    void addAddress_markedPrimary_clearsPreviousPrimaryAddress() throws IOException {
        Address existingPrimary = mailingAddress();
        existingPrimary.setIsPrimary(true);
        existingPrimary.setId(5L);
        onePageOf(addressGateway, existingPrimary);
        when(addressGateway.findAddressByCustomerNumberAndId(5L, "CUS-1")).thenReturn(Optional.of(existingPrimary));
        when(addressGateway.updateCustomerAddress(any())).thenReturn(Optional.of(existingPrimary));

        Address newAddress = mailingAddress();
        newAddress.setIsPrimary(true);
        when(addressGateway.addCustomerAddress(newAddress)).thenReturn(Optional.of(newAddress));

        service.addAddress(newAddress);

        assertFalse(existingPrimary.getIsPrimary());
        verify(addressGateway).updateCustomerAddress(existingPrimary);
    }

    // ---- Validation on write (existing-customer path) ----

    @Test
    void addAddress_withMissingRequiredFields_throwsValidationException() {
        Address invalid = new Address();
        invalid.setCustomerNumber("CUS-1");

        ValidationException thrown = assertThrows(ValidationException.class, () -> service.addAddress(invalid));
        assertTrue(thrown.getErrors().size() >= 3);
    }

    @Test
    void createCustomer_withValidData_isSentToGateway() throws IOException {
        Customer customer = new Customer();
        customer.set_firstName("Jane");
        when(customerGateway.createCustomer(customer)).thenReturn(Optional.of(customer));

        Customer result = service.createCustomer(customer);

        assertSame(customer, result);
    }

    @Test
    void addEmployer_withNegativeSalary_throwsValidationException() {
        Employer invalid = new Employer("Jane Doe", "CUS-1", 0L, "Acme Ltd", "Dev", -100.0, "Full-time");

        assertThrows(ValidationException.class, () -> service.addEmployer(invalid));
    }

    // ---- Helpers ----

    private static void onePageOf(CustomerAddressGateway gateway, Address... items) throws IOException {
        when(gateway.ListCustomerAddresses(anyLong(), any(), anyLong()))
                .thenReturn(new CustomerSearchResult<>(List.of(items), items.length, 1));
    }

    private static void onePageOf(CustomerPhoneNumberGateway gateway, PhoneNumber... items) throws IOException {
        when(gateway.ListCustomerPhoneNumbers(anyLong(), any(), anyLong()))
                .thenReturn(new CustomerSearchResult<>(List.of(items), items.length, 1));
    }

    private static void onePageOf(CustomerEmailGateway gateway, Email... items) throws IOException {
        when(gateway.ListCustomerEmails(anyLong(), any(), anyLong()))
                .thenReturn(new CustomerSearchResult<>(List.of(items), items.length, 1));
    }

    private static void onePageOf(CustomerEmployerGateway gateway, Employer... items) throws IOException {
        when(gateway.ListCustomerEmployers(anyLong(), any(), anyLong()))
                .thenReturn(new CustomerSearchResult<>(List.of(items), items.length, 1));
    }

    private static Customer someCustomer() {
        Customer customer = new Customer();
        customer.set_firstName("Jane");
        customer.set_familyName("Doe");
        customer.set_title("Ms");
        customer.set_dateOfBirth("1990-01-01");
        customer.set_nationality("NZ");
        customer.setOrganisation(1L);
        return customer;
    }

    private static Address mailingAddress() {
        Address address = new Address();
        address.setCustomerNumber("CUS-1");
        address.setType("Home");
        address.setLine1("1 Queen Street");
        address.setCountry("New Zealand");
        address.setIsMailing(true);
        address.setIsPrimary(false);
        return address;
    }

    private static Address nonMailingAddress() {
        Address address = mailingAddress();
        address.setIsMailing(false);
        return address;
    }

    private static PhoneNumber somePhone() {
        PhoneNumber phone = new PhoneNumber(false, "64", "Jane Doe", "CUS-1", 1L, false, null, "Mobile");
        phone.set_number("211234567");
        return phone;
    }

    private static Email someEmail() {
        return new Email("jane@example.com", "Jane Doe", "CUS-1", 1L, false, null, "Personal");
    }

    private static Employer someEmployer() {
        return new Employer("Jane Doe", "CUS-1", 1L, "Acme Ltd", "Developer", 75000.0, "Full-time");
    }
}
