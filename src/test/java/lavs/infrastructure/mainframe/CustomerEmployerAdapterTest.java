package lavs.infrastructure.mainframe;

import lavs.business.model.Employer;
import lavs.business.repository.CustomerSearchResult;
import mainframe.client.MessageRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The employer adapter against a scripted connection, so no mainframe is needed. */
class CustomerEmployerAdapterTest {

    private final ScriptedConnection connection = new ScriptedConnection();
    private final CustomerEmployerAdapter adapter = new CustomerEmployerAdapter(connection);

    private static Employer acme(long id) {
        return new Employer("Jane Doe", "CUS-1", id, "Acme Ltd", "Developer", 75000.0, "Full-time");
    }

    private void retrieveReply(String salary) {
        connection.replySuccess("customerNumber", "CUS-1", "customerName", "Jane Doe", "id", "3", "type", "Full-time",
                "name", "Acme Ltd", "position", "Developer", "salary", salary);
    }

    @Test
    void find_readsEveryFieldOfTheEmployer() throws Exception {
        retrieveReply("75000.0");

        Employer found = adapter.findEmployerByCustomerNumberAndId(3, "CUS-1").orElseThrow();

        assertEquals("Acme Ltd", found.get_name());
        assertEquals("Developer", found.get_position());
        assertEquals(75000.0, found.get_salary());
        assertEquals("Full-time", found.get_type());
        assertEquals(3, found.get_id());
        assertEquals("customerEmployer.retrieve", connection.requests().get(0).getRequestType());
    }

    @Test
    void find_whenThereIsNone_isEmpty() throws Exception {
        connection.replyRefused("No such employer.");

        assertTrue(adapter.findEmployerByCustomerNumberAndId(3, "CUS-1").isEmpty());
    }

    @Test
    void add_sendsTheEmployerThenReadsItBack() throws Exception {
        connection.replySuccess("customer", "CUS-1", "id", "3", "salary", "75000.0");
        retrieveReply("75000.0");

        Employer saved = adapter.addCustomerEmployer(acme(0)).orElseThrow();

        assertEquals(3, saved.get_id());
        MessageRequest add = connection.requests().get(0);
        assertEquals("customer.addEmployer", add.getRequestType());
        assertEquals("Acme Ltd", add.getValue("name"));
        assertEquals("CUS-1", add.getValue("customer"));
    }

    @Test
    void add_whenTheMainframeRefuses_givesNothingAndAsksNoMore() throws Exception {
        connection.replyRefused("Bad salary.");

        assertTrue(adapter.addCustomerEmployer(acme(0)).isEmpty());
        assertEquals(1, connection.requests().size());
    }

    @Test
    void add_whenItCannotBeReadBack_givesNothing() throws Exception {
        connection.replySuccess("customer", "CUS-1", "id", "3", "salary", "75000.0");
        connection.replyRefused("No such employer.");

        assertTrue(adapter.addCustomerEmployer(acme(0)).isEmpty());
    }

    @Test
    void update_sendsTheChangeThenReadsItBack() throws Exception {
        connection.replySuccess("customer", "CUS-1", "id", "3", "salary", "75000.0");
        retrieveReply("80000.0");

        Employer saved = adapter.updateCustomerEmployer(acme(3)).orElseThrow();

        assertEquals(80000.0, saved.get_salary());
        assertEquals("customer.updateEmployer", connection.requests().get(0).getRequestType());
        assertEquals("3", connection.requests().get(0).getValue("id"));
    }

    @Test
    void update_whenTheMainframeRefuses_givesNothing() throws Exception {
        connection.replyRefused("Not allowed.");

        assertTrue(adapter.updateCustomerEmployer(acme(3)).isEmpty());
        assertEquals(1, connection.requests().size());
    }

    @Test
    void update_whenItCannotBeReadBack_givesNothing() throws Exception {
        connection.replySuccess("customer", "CUS-1", "id", "3", "salary", "75000.0");
        connection.replyRefused("No such employer.");

        assertTrue(adapter.updateCustomerEmployer(acme(3)).isEmpty());
    }

    @Test
    void delete_isTrueOnlyWhenTheMainframeAccepts() throws Exception {
        connection.replySuccess("customer", "CUS-1", "id", "3", "salary", "75000.0");
        connection.replyRefused("No such employer.");

        assertTrue(adapter.deleteEmployerByCustomerNumberAndId(3, "CUS-1"));
        assertFalse(adapter.deleteEmployerByCustomerNumberAndId(3, "CUS-1"));
        assertEquals("customer.deleteEmployer", connection.requests().get(0).getRequestType());
    }

    @Test
    void list_readsEachEmployerAndTheTotal() throws Exception {
        connection.replySuccess("customerNumber", "CUS-1", "customerName", "Jane Doe", "page", "1", "total", "2",
                "items", "2",
                "1:id", "3", "1:type", "Full-time", "1:name", "Acme Ltd",
                "2:id", "4", "2:type", "Part-time", "2:name", "Corner Shop");

        CustomerSearchResult<Employer> result = adapter.ListCustomerEmployers(1, "CUS-1", 100);

        assertEquals(2, result.getTotal());
        List<Employer> employers = result.getList();
        assertEquals("Corner Shop", employers.get(1).get_name());
        assertEquals("Part-time", employers.get(1).get_type());
        assertEquals("CUS-1", employers.get(1).get_customerNumber());
    }

    @Test
    void list_whenTheMainframeRefuses_isEmpty() throws Exception {
        connection.replyRefused("No such customer.");

        assertTrue(adapter.ListCustomerEmployers(1, "CUS-404", 100).getList().isEmpty());
    }
}
