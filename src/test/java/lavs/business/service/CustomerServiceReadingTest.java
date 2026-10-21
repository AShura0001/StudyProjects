package lavs.business.service;

import lavs.business.model.Address;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Looking customers, their primary contact details and their notes up through CustomerService. */
class CustomerServiceReadingTest {

    private static final String NUMBER = "CUS-9";

    private CustomerGateway customers;
    private CustomerAddressGateway addresses;
    private CustomerPhoneNumberGateway phones;
    private CustomerEmailGateway emails;
    private CustomerNoteGateway notes;
    private CustomerService service;

    @BeforeEach
    void setUp() {
        customers = mock(CustomerGateway.class);
        addresses = mock(CustomerAddressGateway.class);
        phones = mock(CustomerPhoneNumberGateway.class);
        emails = mock(CustomerEmailGateway.class);
        notes = mock(CustomerNoteGateway.class);
        FakeDraftRepository drafts = new FakeDraftRepository();
        BackgroundSync sync = new BackgroundSync(new DraftSyncService(drafts, new FakeDraftUploadGateway()),
                new ManualScheduler(), RetryPolicy.standard());
        service = new CustomerService(customers, addresses, phones, emails, mock(CustomerEmployerGateway.class),
                notes, new ValidationService(), drafts, sync);
    }

    private static Customer customer(String number) {
        return new Customer("1990-04-23", "Doe", "Jane", number, "Active", "Ms");
    }

    // ---- finding customers ----

    @Test
    void getCustomer_returnsWhatTheMainframeHolds() throws IOException {
        Customer jane = customer(NUMBER);
        when(customers.findByCustomerNumber(NUMBER)).thenReturn(Optional.of(jane));

        assertEquals(Optional.of(jane), service.getCustomer(NUMBER));
    }

    @Test
    void search_withABlankTerm_listsEveryone() throws IOException {
        CustomerSearchResult<Customer> everyone = new CustomerSearchResult<>(List.of(customer("A")), 1, 1);
        when(customers.searchCustomer(1L, null, 50L)).thenReturn(everyone);

        assertEquals(everyone, service.search("   ", 1, 50));
    }

    @Test
    void search_trimsTheTerm() throws IOException {
        CustomerSearchResult<Customer> result = new CustomerSearchResult<>(List.of(customer("A")), 1, 1);
        when(customers.searchCustomer(1L, "doe", 50L)).thenReturn(result);

        assertEquals(result, service.search("  doe ", 1, 50));
    }

    @Test
    void search_whenTheMainframeGivesNoAnswer_saysSo() throws IOException {
        when(customers.searchCustomer(anyLong(), any(), anyLong())).thenReturn(null);

        assertThrows(IOException.class, () -> service.search("doe", 1, 50));
    }

    @Test
    void searchAll_readsPagesUntilItHasEveryoneTheMainframeReports() throws IOException {
        Customer a = customer("A");
        Customer b = customer("B");
        when(customers.searchCustomer(eq(1L), any(), anyLong())).thenReturn(new CustomerSearchResult<>(List.of(a), 2, 1));
        when(customers.searchCustomer(eq(2L), any(), anyLong())).thenReturn(new CustomerSearchResult<>(List.of(b), 2, 2));

        assertEquals(List.of(a, b), service.searchAll("doe", 5));
    }

    @Test
    void searchAll_stopsAtTheLimitOfPages() throws IOException {
        when(customers.searchCustomer(anyLong(), any(), anyLong()))
                .thenReturn(new CustomerSearchResult<>(List.of(customer("A")), 1000, 1));

        assertEquals(2, service.searchAll("doe", 2).size());
    }

    @Test
    void searchAll_stopsWhenAPageComesBackEmpty() throws IOException {
        when(customers.searchCustomer(anyLong(), any(), anyLong()))
                .thenReturn(new CustomerSearchResult<>(List.of(), 1000, 1));

        assertTrue(service.searchAll("doe", 5).isEmpty());
    }

    // ---- primary contact details ----

    @Test
    void primaryPhone_isThePhoneMarkedPrimary() throws IOException {
        PhoneNumber other = new PhoneNumber(false, "+64", "Jane", NUMBER, 1, false, null, "Home");
        PhoneNumber primary = new PhoneNumber(true, "+64", "Jane", NUMBER, 2, true, null, "Mobile");
        when(phones.ListCustomerPhoneNumbers(anyLong(), eq(NUMBER), anyLong()))
                .thenReturn(new CustomerSearchResult<>(List.of(other, primary), 2, 1));

        assertEquals(Optional.of(primary), service.primaryPhone(NUMBER));
    }

    @Test
    void primaryPhone_whenThereAreNone_isEmpty() throws IOException {
        when(phones.ListCustomerPhoneNumbers(anyLong(), eq(NUMBER), anyLong()))
                .thenReturn(new CustomerSearchResult<>(List.of(), 0, 1));

        assertTrue(service.primaryPhone(NUMBER).isEmpty());
    }

    @Test
    void primaryEmail_fallsBackToTheFirstWhenNoneIsMarked() throws IOException {
        Email first = new Email("a@example.com", "Jane", NUMBER, 1, false, null, "Personal");
        Email second = new Email("b@example.com", "Jane", NUMBER, 2, false, null, "Work");
        when(emails.ListCustomerEmails(anyLong(), eq(NUMBER), anyLong()))
                .thenReturn(new CustomerSearchResult<>(List.of(first, second), 2, 1));

        assertEquals(Optional.of(first), service.primaryEmail(NUMBER));
    }

    @Test
    void primaryAddress_isReadInFullAndKnowsItsCustomer() throws IOException {
        Address summary = new Address();
        summary.setId(13L);
        summary.setIsPrimary(true);
        Address full = new Address();
        full.setId(13L);
        full.setLine1("1 Queen Street");
        when(addresses.ListCustomerAddresses(anyLong(), eq(NUMBER), anyLong()))
                .thenReturn(new CustomerSearchResult<>(List.of(summary), 1, 1));
        when(addresses.findAddressByCustomerNumberAndId(13L, NUMBER)).thenReturn(Optional.of(full));

        Address found = service.primaryAddress(NUMBER).orElseThrow();

        assertEquals("1 Queen Street", found.getLine1());
        assertEquals(NUMBER, found.getCustomerNumber());
    }

    @Test
    void primaryAddress_whenTheFullRecordIsMissing_usesTheSummary() throws IOException {
        Address summary = new Address();
        summary.setId(13L);
        summary.setIsPrimary(true);
        when(addresses.ListCustomerAddresses(anyLong(), eq(NUMBER), anyLong()))
                .thenReturn(new CustomerSearchResult<>(List.of(summary), 1, 1));
        when(addresses.findAddressByCustomerNumberAndId(13L, NUMBER)).thenReturn(Optional.empty());

        assertEquals(13L, service.primaryAddress(NUMBER).orElseThrow().getId());
    }

    @Test
    void primaryAddress_whenThereAreNone_isEmpty() throws IOException {
        when(addresses.ListCustomerAddresses(anyLong(), eq(NUMBER), anyLong()))
                .thenReturn(new CustomerSearchResult<>(List.of(), 0, 1));

        assertTrue(service.primaryAddress(NUMBER).isEmpty());
    }

    // ---- notes ----

    @Test
    void listNotes_readsEachNoteInFull() throws IOException {
        Note summary = new Note();
        summary.set_id(5L);
        Note full = new Note();
        full.set_id(5L);
        when(notes.ListCustomerNotes(anyLong(), eq(NUMBER), anyLong()))
                .thenReturn(new CustomerSearchResult<>(List.of(summary), 1, 1));
        when(notes.findNoteByCustomerNumberAndId(5L, NUMBER)).thenReturn(Optional.of(full));

        assertEquals(List.of(full), service.listNotes(NUMBER));
    }

    @Test
    void listNotes_keepsTheSummaryWhenTheFullNoteCannotBeRead() throws IOException {
        Note summary = new Note();
        summary.set_id(5L);
        when(notes.ListCustomerNotes(anyLong(), eq(NUMBER), anyLong()))
                .thenReturn(new CustomerSearchResult<>(List.of(summary), 1, 1));
        when(notes.findNoteByCustomerNumberAndId(5L, NUMBER)).thenReturn(Optional.empty());

        assertEquals(List.of(summary), service.listNotes(NUMBER));
    }

    @Test
    void findNote_tellsTheNoteWhichCustomerItBelongsTo() throws IOException {
        Note note = new Note();
        note.set_id(5L);
        when(notes.findNoteByCustomerNumberAndId(5L, NUMBER)).thenReturn(Optional.of(note));

        Note found = service.findNote(NUMBER, 5L).orElseThrow();

        assertEquals(NUMBER, found.get_customerNumber());
        verify(notes).findNoteByCustomerNumberAndId(5L, NUMBER);
    }

    @Test
    void findNote_ofAnUnknownNote_isEmpty() throws IOException {
        when(notes.findNoteByCustomerNumberAndId(404L, NUMBER)).thenReturn(Optional.empty());

        assertTrue(service.findNote(NUMBER, 404L).isEmpty());
    }
}
