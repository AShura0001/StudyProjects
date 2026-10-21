package lavs.business.service;

import lavs.business.model.Address;
import lavs.business.model.ContactChanges;
import lavs.business.model.Customer;
import lavs.business.model.Email;
import lavs.business.model.Note;
import lavs.business.model.PhoneNumber;
import lavs.business.repository.CustomerAddressGateway;
import lavs.business.repository.CustomerEmailGateway;
import lavs.business.repository.CustomerEmployerGateway;
import lavs.business.repository.CustomerGateway;
import lavs.business.repository.CustomerNoteGateway;
import lavs.business.repository.CustomerPhoneNumberGateway;
import lavs.business.repository.CustomerSearchResult;
import lavs.business.repository.FakeDraftRepository;
import lavs.business.repository.FakeDraftUploadGateway;
import lavs.business.repository.RecordRejectedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Saving the Customer Details screen, and registering a customer, through CustomerService. */
class CustomerServiceContactTest {

    private static final String NUMBER = "CUS-9";

    private CustomerGateway customers;
    private CustomerAddressGateway addresses;
    private CustomerPhoneNumberGateway phones;
    private CustomerEmailGateway emails;
    private CustomerEmployerGateway employers;
    private CustomerNoteGateway noteGateway;
    private CustomerService service;

    @BeforeEach
    void setUp() throws IOException {
        customers = mock(CustomerGateway.class);
        addresses = mock(CustomerAddressGateway.class);
        phones = mock(CustomerPhoneNumberGateway.class);
        emails = mock(CustomerEmailGateway.class);
        employers = mock(CustomerEmployerGateway.class);
        FakeDraftRepository drafts = new FakeDraftRepository();
        BackgroundSync sync = new BackgroundSync(new DraftSyncService(drafts, new FakeDraftUploadGateway()),
                new ManualScheduler(), RetryPolicy.standard());
        noteGateway = mock(CustomerNoteGateway.class);
        service = new CustomerService(customers, addresses, phones, emails, employers,
                noteGateway, new ValidationService(), drafts, sync);

        Customer jane = new Customer("1990-04-23", "Doe", "Jane", NUMBER, "Active", "Ms");
        jane.set_nationality("NZ");
        when(customers.findByCustomerNumber(NUMBER)).thenReturn(Optional.of(jane));
        when(customers.updateCustomer(any())).thenAnswer(a -> Optional.of(a.getArgument(0)));

        PhoneNumber phone = new PhoneNumber(true, "+64", "Jane Doe", NUMBER, 11, true, null, "Mobile");
        phone.set_number("21 555 0100");
        when(phones.ListCustomerPhoneNumbers(anyLong(), eq(NUMBER), anyLong()))
                .thenReturn(new CustomerSearchResult<>(List.of(phone), 1, 1));
        when(phones.findCustomerNumberByNumberAndId(11, NUMBER)).thenReturn(Optional.of(copy(phone)));
        when(phones.updateCustomerNumber(any())).thenAnswer(a -> Optional.of(a.getArgument(0)));
        when(phones.addCustomerPhoneNumber(any())).thenAnswer(a -> Optional.of(a.getArgument(0)));

        Email email = new Email("jane@example.com", "Jane Doe", NUMBER, 12, true, null, "Personal");
        when(emails.ListCustomerEmails(anyLong(), eq(NUMBER), anyLong()))
                .thenReturn(new CustomerSearchResult<>(List.of(email), 1, 1));
        when(emails.findEmailByCustomerNumberAndId(12L, NUMBER)).thenReturn(Optional.of(email));
        when(emails.updateCustomerEmail(any())).thenAnswer(a -> Optional.of(a.getArgument(0)));

        Address summary = new Address();
        summary.setId(13L);
        summary.setIsPrimary(true);
        when(addresses.ListCustomerAddresses(anyLong(), eq(NUMBER), anyLong()))
                .thenReturn(new CustomerSearchResult<>(List.of(summary), 1, 1));
        when(addresses.findAddressByCustomerNumberAndId(13L, NUMBER)).thenAnswer(a -> Optional.of(fullAddress()));
        when(addresses.updateCustomerAddress(any())).thenAnswer(a -> Optional.of(a.getArgument(0)));

        when(employers.deleteEmployerByCustomerNumberAndId(anyLong(), eq(NUMBER))).thenReturn(true);
    }

    private static Address fullAddress() {
        Address address = new Address();
        address.setId(13L);
        address.setType("Residential");
        address.setLine1("1 Queen Street");
        address.setSuburb("CBD");
        address.setCity("Auckland");
        address.setPostCode("1010");
        address.setCountry("New Zealand");
        address.setIsPrimary(true);
        address.setIsMailing(true);
        return address;
    }

    private static PhoneNumber copy(PhoneNumber p) {
        PhoneNumber c = new PhoneNumber(p.is_canTxt(), p.get_countryCode(), p.get_customerName(), p.get_customerNumber(),
                p.get_id(), p.is_isPrimary(), p.get_otherType(), p.get_type());
        c.set_number(p.get_number());
        return c;
    }

    private static ContactChanges unchanged() {
        return new ContactChanges(NUMBER, "Jane", "Doe", "+64", "21 555 0100", "jane@example.com", List.of(), List.of(), List.of());
    }

    private void verifyNoWrites() throws IOException {
        verify(customers, never()).updateCustomer(any());
        verify(phones, never()).updateCustomerNumber(any());
        verify(phones, never()).addCustomerPhoneNumber(any());
        verify(emails, never()).updateCustomerEmail(any());
        verify(addresses, never()).updateCustomerAddress(any());
        verify(employers, never()).deleteEmployerByCustomerNumberAndId(anyLong(), any());
    }

    @Test
    void nothingChanged_sendsNothing() throws IOException {
        service.saveContactChanges(unchanged());
        verifyNoWrites();
    }

    @Test
    void changedValues_areSent_andEverythingElseIsKept() throws IOException {
        service.saveContactChanges(new ContactChanges(NUMBER, "Janet", "Doe-Smith", "+61", "400 111 222",
                "janet@example.com", List.of(22L), List.of(), List.of()));

        ArgumentCaptor<Customer> customer = ArgumentCaptor.forClass(Customer.class);
        verify(customers).updateCustomer(customer.capture());
        assertEquals("Janet", customer.getValue().get_firstName());
        assertEquals("Doe-Smith", customer.getValue().get_familyName());
        assertEquals("Ms", customer.getValue().get_title());
        assertEquals("1990-04-23", customer.getValue().get_dateOfBirth());

        ArgumentCaptor<PhoneNumber> phone = ArgumentCaptor.forClass(PhoneNumber.class);
        verify(phones).updateCustomerNumber(phone.capture());
        assertEquals("+61", phone.getValue().get_countryCode());
        assertEquals("400 111 222", phone.getValue().get_number());
        assertTrue(phone.getValue().is_canTxt(), "the stored text setting is kept");

        ArgumentCaptor<Email> email = ArgumentCaptor.forClass(Email.class);
        verify(emails).updateCustomerEmail(email.capture());
        assertEquals("janet@example.com", email.getValue().getAddress());

        verify(addresses, never()).updateCustomerAddress(any());
        verify(employers).deleteEmployerByCustomerNumberAndId(22L, NUMBER);
    }

    // ---- Removing addresses ----

    private void twoAddresses() throws IOException {
        Address primary = new Address();
        primary.setId(13L);
        primary.setIsPrimary(true);
        Address other = new Address();
        other.setId(14L);
        other.setIsPrimary(false);
        when(addresses.ListCustomerAddresses(anyLong(), eq(NUMBER), anyLong()))
                .thenReturn(new CustomerSearchResult<>(List.of(primary, other), 2, 1));
        when(addresses.deleteCustomerAddress(eq(NUMBER), anyLong())).thenReturn(true);
    }

    @Test
    void removingAnAddressThatIsNotPrimary_deletesIt() throws IOException {
        twoAddresses();
        service.saveContactChanges(new ContactChanges(NUMBER, "Jane", "Doe", "+64", "21 555 0100",
                "jane@example.com", List.of(), List.of(14L), List.of()));
        verify(addresses).deleteCustomerAddress(NUMBER, 14L);
        verify(addresses, never()).deleteCustomerAddress(NUMBER, 13L);
    }

    @Test
    void removingThePrimaryWhileOthersRemain_isRefused_andNothingIsSent() throws IOException {
        twoAddresses();
        ValidationException invalid = assertThrows(ValidationException.class, () ->
                service.saveContactChanges(new ContactChanges(NUMBER, "Janet", "Doe", "+64", "21 555 0100",
                        "jane@example.com", List.of(), List.of(13L), List.of())));
        assertEquals("addresses", invalid.getErrors().get(0).getField());
        verify(addresses, never()).deleteCustomerAddress(any(), anyLong());
        verify(customers, never()).updateCustomer(any());
    }

    @Test
    void removingEveryAddress_isAllowed() throws IOException {
        twoAddresses();
        service.saveContactChanges(new ContactChanges(NUMBER, "Jane", "Doe", "+64", "21 555 0100",
                "jane@example.com", List.of(), List.of(13L, 14L), List.of()));
        verify(addresses).deleteCustomerAddress(NUMBER, 13L);
        verify(addresses).deleteCustomerAddress(NUMBER, 14L);
    }

    @Test
    void refusedAddressRemoval_isReported() throws IOException {
        twoAddresses();
        when(addresses.deleteCustomerAddress(NUMBER, 14L)).thenReturn(false);
        RecordRejectedException refused = assertThrows(RecordRejectedException.class, () ->
                service.saveContactChanges(new ContactChanges(NUMBER, "Jane", "Doe", "+64", "21 555 0100",
                        "jane@example.com", List.of(), List.of(14L), List.of())));
        assertTrue(refused.getMessage().contains("address removal"));
    }

    // ---- Adding and changing addresses ----

    private static Address newAddress(String type, boolean primary) {
        Address address = new Address();
        address.setCustomerNumber(NUMBER);
        address.setType(type);
        address.setLine1("9 Ponsonby Road");
        address.setCity("Auckland");
        address.setCountry("New Zealand");
        address.setIsPrimary(primary);
        address.setIsMailing(true);
        return address;
    }

    @Test
    void firstAddress_alwaysBecomesPrimary() throws IOException {
        when(addresses.ListCustomerAddresses(anyLong(), eq(NUMBER), anyLong()))
                .thenReturn(new CustomerSearchResult<>(List.of(), 0, 1));
        when(addresses.addCustomerAddress(any())).thenAnswer(a -> Optional.of(a.getArgument(0)));

        Address saved = service.addAddress(newAddress("Postal", false));

        assertTrue(saved.getIsPrimary());
    }

    @Test
    void newPrimary_takesTheFlagOffTheOldOne_withoutLosingItsStreetLines() throws IOException {
        when(addresses.addCustomerAddress(any())).thenAnswer(a -> Optional.of(a.getArgument(0)));

        service.addAddress(newAddress("Postal", true));

        ArgumentCaptor<Address> cleared = ArgumentCaptor.forClass(Address.class);
        verify(addresses).updateCustomerAddress(cleared.capture());
        assertFalse(cleared.getValue().getIsPrimary());
        assertEquals("1 Queen Street", cleared.getValue().getLine1(), "the full record is sent, not the list summary");
        assertEquals("1010", cleared.getValue().getPostCode());
    }

    @Test
    void oldPrimaryThatCannotBeRead_isNotOverwritten() throws IOException {
        when(addresses.findAddressByCustomerNumberAndId(13L, NUMBER)).thenReturn(Optional.empty());

        assertThrows(RecordRejectedException.class, () -> service.addAddress(newAddress("Postal", true)));
        verify(addresses, never()).updateCustomerAddress(any());
        verify(addresses, never()).addCustomerAddress(any());
    }

    @Test
    void keepingAnAddressPrimary_doesNotClearItself() throws IOException {
        Address edited = fullAddress();
        edited.setCustomerNumber(NUMBER);
        edited.setLine1("2 Queen Street");

        service.updateAddress(edited);

        verify(addresses, times(1)).updateCustomerAddress(any());
    }

    @Test
    void otherAddress_needsADescription() throws IOException {
        ValidationException invalid = assertThrows(ValidationException.class,
                () -> service.addAddress(newAddress("Other", false)));
        assertEquals("otherType", invalid.getErrors().get(0).getField());
        verify(addresses, never()).addCustomerAddress(any());
    }

    @Test
    void listAddressesInFull_readsEachOne_primaryFirst() throws IOException {
        Address postal = new Address();
        postal.setId(14L);
        postal.setIsPrimary(false);
        Address primarySummary = new Address();
        primarySummary.setId(13L);
        primarySummary.setIsPrimary(true);
        when(addresses.ListCustomerAddresses(anyLong(), eq(NUMBER), anyLong()))
                .thenReturn(new CustomerSearchResult<>(List.of(postal, primarySummary), 2, 1));
        Address fullPostal = newAddress("Postal", false);
        fullPostal.setId(14L);
        when(addresses.findAddressByCustomerNumberAndId(14L, NUMBER)).thenReturn(Optional.of(fullPostal));

        List<Address> all = service.listAddressesInFull(NUMBER);

        assertEquals(List.of(13L, 14L), all.stream().map(Address::getId).toList());
        assertEquals("1 Queen Street", all.get(0).getLine1());
        assertEquals("9 Ponsonby Road", all.get(1).getLine1());
    }

    @Test
    void phoneWithoutACode_keepsTheCurrentCode() throws IOException {
        service.saveContactChanges(new ContactChanges(NUMBER, "Jane", "Doe", null, "21 999 0000",
                "jane@example.com", List.of(), List.of(), List.of()));

        ArgumentCaptor<PhoneNumber> phone = ArgumentCaptor.forClass(PhoneNumber.class);
        verify(phones).updateCustomerNumber(phone.capture());
        assertEquals("+64", phone.getValue().get_countryCode());
        assertEquals("21 999 0000", phone.getValue().get_number());
    }

    @Test
    void invalidValues_areAllReported_andNothingIsSent() throws IOException {
        ValidationException invalid = assertThrows(ValidationException.class, () ->
                service.saveContactChanges(new ContactChanges(NUMBER, "", "", "+64", "abc",
                        "not-an-email", List.of(), List.of(), List.of())));

        Set<String> fields = invalid.getErrors().stream().map(ValidationError::getField).collect(Collectors.toSet());
        assertEquals(Set.of("firstName", "familyName", "phone", "email"), fields);
        verifyNoWrites();
    }

    @Test
    void newPhone_needsACountryCode() throws IOException {
        when(phones.ListCustomerPhoneNumbers(anyLong(), eq(NUMBER), anyLong()))
                .thenReturn(new CustomerSearchResult<>(List.of(), 0, 1));

        ValidationException invalid = assertThrows(ValidationException.class, () ->
                service.saveContactChanges(new ContactChanges(NUMBER, "Jane", "Doe", null, "21 555 0100",
                        "jane@example.com", List.of(), List.of(), List.of())));
        assertEquals("phone", invalid.getErrors().get(0).getField());

        service.saveContactChanges(new ContactChanges(NUMBER, "Jane", "Doe", "+64", "21 555 0100",
                "jane@example.com", List.of(), List.of(), List.of()));
        ArgumentCaptor<PhoneNumber> added = ArgumentCaptor.forClass(PhoneNumber.class);
        verify(phones).addCustomerPhoneNumber(added.capture());
        assertTrue(added.getValue().is_isPrimary());
        assertEquals(CustomerService.NEW_PHONE_TYPE, added.getValue().get_type());
    }

    @Test
    void refusedWrite_isReported_andLaterWritesAreNotSent() throws IOException {
        doReturn(Optional.empty()).when(customers).updateCustomer(any());

        RecordRejectedException refused = assertThrows(RecordRejectedException.class, () ->
                service.saveContactChanges(new ContactChanges(NUMBER, "Janet", "Doe", "+64", "21 000 0000",
                        "jane@example.com", List.of(), List.of(), List.of())));

        assertTrue(refused.getMessage().contains("name"));
        verify(phones, never()).updateCustomerNumber(any());
    }

    @Test
    void registerCustomer_needsTheNewCustomerFields_andSetsTheOrganisation() throws IOException {
        ValidationException invalid = assertThrows(ValidationException.class,
                () -> service.registerCustomer(new Customer("2999-01-01", "", "Sam", null, null, "")));
        Set<String> fields = invalid.getErrors().stream().map(ValidationError::getField).collect(Collectors.toSet());
        assertEquals(Set.of("title", "familyName", "nationality", "dateOfBirth"), fields);
        verify(customers, never()).createCustomer(any());

        Customer sam = new Customer("1990-01-01", "Lee", "Sam", null, null, "Mr");
        sam.set_nationality("New Zealand");
        when(customers.createCustomer(any())).thenAnswer(a -> Optional.of(a.getArgument(0)));
        assertEquals(CustomerService.DEFAULT_ORGANISATION, service.registerCustomer(sam).getOrganisation());
    }

    // ---- Notes ----

    @Test
    void removedNotes_areDeletedLast() throws IOException {
        CustomerNoteGateway notes = noteGateway;
        when(notes.deleteNoteByCustomerNumberAndId(31L, NUMBER)).thenReturn(true);
        service.saveContactChanges(new ContactChanges(NUMBER, "Jane", "Doe", "+64", "21 555 0100",
                "jane@example.com", List.of(), List.of(), List.of(31L)));
        verify(notes).deleteNoteByCustomerNumberAndId(31L, NUMBER);
    }

    @Test
    void refusedNoteRemoval_isReported() throws IOException {
        when(noteGateway.deleteNoteByCustomerNumberAndId(31L, NUMBER)).thenReturn(false);
        RecordRejectedException refused = assertThrows(RecordRejectedException.class, () ->
                service.saveContactChanges(new ContactChanges(NUMBER, "Jane", "Doe", "+64", "21 555 0100",
                        "jane@example.com", List.of(), List.of(), List.of(31L))));
        assertTrue(refused.getMessage().contains("note removal"));
    }

    @Test
    void addNote_withoutATitle_isCalledGeneral_andTrimmed() throws IOException {
        when(noteGateway.addCustomerNote(any())).thenAnswer(a -> Optional.of(a.getArgument(0)));
        Note note = new Note();
        note.set_customerNumber(NUMBER);
        note.set_lines("  Called about a top-up." + System.lineSeparator() + "  ");

        Note saved = service.addNote(note);

        assertEquals(CustomerService.DEFAULT_NOTE_TYPE, saved.get_type());
        assertEquals("Called about a top-up.", saved.get_lines());
    }

    @Test
    void emptyOrOverlongNotes_areRefused_andNothingIsSent() throws IOException {
        Note empty = new Note();
        empty.set_customerNumber(NUMBER);
        empty.set_lines("   ");
        assertEquals("lines", assertThrows(ValidationException.class, () -> service.addNote(empty))
                .getErrors().get(0).getField());

        Note tooLong = new Note();
        tooLong.set_customerNumber(NUMBER);
        tooLong.set_id(31);
        tooLong.set_lines("x".repeat(CustomerService.MAX_NOTE_LENGTH + 1));
        assertThrows(ValidationException.class, () -> service.updateNote(tooLong));

        verify(noteGateway, never()).addCustomerNote(any());
        verify(noteGateway, never()).updateCustomerNote(any());
    }

    @Test
    void updateNote_sendsTheChange() throws IOException {
        when(noteGateway.updateCustomerNote(any())).thenAnswer(a -> Optional.of(a.getArgument(0)));
        Note note = new Note();
        note.set_customerNumber(NUMBER);
        note.set_id(31);
        note.set_type("Call log");
        note.set_lines("Rang back on Tuesday.");

        service.updateNote(note);

        verify(noteGateway).updateCustomerNote(note);
    }

    // ---- Employers ----

    private static lavs.business.model.Employer employer(String name, String type) {
        lavs.business.model.Employer e = new lavs.business.model.Employer();
        e.set_customerNumber(NUMBER);
        e.set_name(name);
        e.set_type(type);
        e.set_position("Analyst");
        e.set_salary(85000);
        return e;
    }

    @Test
    void addEmployer_needsANameAndType_andNothingIsSentOtherwise() throws IOException {
        ValidationException invalid = assertThrows(ValidationException.class,
                () -> service.addEmployer(employer("", "")));
        assertEquals(Set.of("name", "type"),
                invalid.getErrors().stream().map(ValidationError::getField).collect(Collectors.toSet()));
        verify(employers, never()).addCustomerEmployer(any());
    }

    @Test
    void addEmployer_sendsIt_andARefusalIsReportedReadably() throws IOException {
        when(employers.addCustomerEmployer(any())).thenAnswer(a -> Optional.of(a.getArgument(0)));
        assertEquals("Acme", service.addEmployer(employer("Acme", "Primary")).get_name());

        doReturn(Optional.empty()).when(employers).addCustomerEmployer(any());
        RecordRejectedException refused = assertThrows(RecordRejectedException.class,
                () -> service.addEmployer(employer("Globex", "Secondary")));
        assertTrue(refused.getMessage().contains("new employer"));
    }

    @Test
    void updateEmployer_refused_isReportedReadably() throws IOException {
        when(employers.updateCustomerEmployer(any())).thenReturn(Optional.empty());
        RecordRejectedException refused = assertThrows(RecordRejectedException.class,
                () -> service.updateEmployer(employer("Acme", "Primary")));
        assertTrue(refused.getMessage().contains("employer change"));
    }
}
