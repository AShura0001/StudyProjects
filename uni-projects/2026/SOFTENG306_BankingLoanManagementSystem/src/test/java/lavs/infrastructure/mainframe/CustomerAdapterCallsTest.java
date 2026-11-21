package lavs.infrastructure.mainframe;

import lavs.business.model.Customer;
import lavs.business.repository.CustomerSearchResult;
import mainframe.client.MessageRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Creating, changing, activating and searching customers through the customer adapter. */
class CustomerAdapterCallsTest {

    private final ScriptedConnection connection = new ScriptedConnection();
    private final CustomerAdapter adapter = new CustomerAdapter(connection);

    private static Customer jane() {
        Customer customer = new Customer("1990-04-23", "Doe", "Jane", null, null, "Ms");
        customer.set_nationality("New Zealand");
        customer.setOrganisation(1);
        return customer;
    }

    private void retrieveReply(String status) {
        connection.replySuccess("number", "CUS-9", "familyName", "Doe", "firstName", "Jane", "title", "Ms",
                "dateOfBirth", "1990-04-23", "nationality", "New Zealand", "status", status);
    }

    @Test
    void createCustomer_sendsTheCustomerThenReadsThemBack() throws Exception {
        connection.replySuccess("number", "CUS-9", "familyName", "Doe", "firstName", "Jane");
        retrieveReply("Active");

        Customer created = adapter.createCustomer(jane()).orElseThrow();

        assertEquals("CUS-9", created.get_number());
        assertEquals("Active", created.get_status());
        MessageRequest create = connection.requests().get(0);
        assertEquals("customer.create", create.getRequestType());
        assertEquals("Doe", create.getValue("familyName"));
        assertEquals("CUS-9", connection.requests().get(1).getValue("number"));
    }

    @Test
    void createCustomer_thatWasSavedButCannotBeReadBack_stillGivesTheNewNumber() throws Exception {
        connection.replySuccess("number", "CUS-9", "familyName", "Doe", "firstName", "Jane", "title", "Ms",
                "dateOfBirth", "1990-04-23", "nationality", "New Zealand", "status", "Active");
        connection.replyRefused("Busy.");

        Customer created = adapter.createCustomer(jane()).orElseThrow();

        assertEquals("CUS-9", created.get_number());
        assertEquals("Doe", created.get_familyName());
        assertEquals("New Zealand", created.get_nationality());
    }

    @Test
    void createCustomer_whenTheMainframeRefuses_givesNothing() throws Exception {
        connection.replyRefused("Bad date of birth.");

        assertTrue(adapter.createCustomer(jane()).isEmpty());
        assertEquals(1, connection.requests().size());
    }

    @Test
    void updateCustomer_sendsTheChangeThenReadsItBack() throws Exception {
        connection.replySuccess("number", "CUS-9");
        retrieveReply("Active");
        Customer changed = jane();
        changed.set_number("CUS-9");

        assertEquals("Jane", adapter.updateCustomer(changed).orElseThrow().get_firstName());
        assertEquals("customer.update", connection.requests().get(0).getRequestType());
        assertEquals("CUS-9", connection.requests().get(0).getValue("number"));
    }

    @Test
    void updateCustomer_thatWasSavedButCannotBeReadBack_stillReportsTheCustomer() throws Exception {
        connection.replySuccess("number", "CUS-9");
        connection.replyRefused("Busy.");
        Customer changed = jane();
        changed.set_number("CUS-9");

        assertSame(changed, adapter.updateCustomer(changed).orElseThrow());
    }

    @Test
    void updateCustomer_whenTheMainframeRefuses_givesNothing() throws Exception {
        connection.replyRefused("Not allowed.");
        Customer changed = jane();
        changed.set_number("CUS-9");

        assertTrue(adapter.updateCustomer(changed).isEmpty());
    }

    @Test
    void updateStatus_turnsAnActiveCustomerInactive() throws Exception {
        retrieveReply("Active");
        connection.replySuccess("customer", "CUS-9", "status", "Inactive");

        assertTrue(adapter.updateStatus("CUS-9"));

        MessageRequest change = connection.requests().get(1);
        assertEquals("customer.updateStatus", change.getRequestType());
        assertEquals("Inactive", change.getValue("status"));
    }

    @Test
    void updateStatus_turnsAnInactiveCustomerActive() throws Exception {
        retrieveReply("Inactive");
        connection.replySuccess("customer", "CUS-9", "status", "Active");

        assertTrue(adapter.updateStatus("CUS-9"));
        assertEquals("Active", connection.requests().get(1).getValue("status"));
    }

    @Test
    void updateStatus_whenTheMainframeLeftTheStatusOut_makesTheCustomerActiveInsteadOfFailing() throws Exception {
        connection.replySuccess("number", "CUS-9", "firstName", "Jane");
        connection.replySuccess("customer", "CUS-9", "status", "Active");

        assertTrue(adapter.updateStatus("CUS-9"));
        assertEquals("Active", connection.requests().get(1).getValue("status"));
    }

    @Test
    void updateStatus_ofACustomerThatCannotBeFound_changesNothing() throws Exception {
        connection.replyRefused("No such customer.");

        assertFalse(adapter.updateStatus("CUS-404"));
        assertEquals(1, connection.requests().size());
    }

    @Test
    void updateStatus_whenTheChangeIsRefused_saysSo() throws Exception {
        retrieveReply("Active");
        connection.replyRefused("Not allowed.");

        assertFalse(adapter.updateStatus("CUS-9"));
    }

    @Test
    void searchCustomer_readsEachMatchAndTheTotal() throws Exception {
        connection.replySuccess("page", "1", "total", "2", "items", "2",
                "1:number", "CUS-9", "1:firstName", "Jane", "1:familyName", "Doe", "1:title", "Ms",
                "1:dateOfBirth", "1990-04-23", "1:status", "Active",
                "2:number", "CUS-10", "2:firstName", "Indiana", "2:familyName", "Jones", "2:title", "Mr",
                "2:dateOfBirth", "1899-07-01", "2:status", "Inactive");

        CustomerSearchResult<Customer> result = adapter.searchCustomer(1, "Jones", 10);

        assertEquals(2, result.getTotal());
        assertEquals("Jones", result.getList().get(1).get_familyName());
        assertEquals("Inactive", result.getList().get(1).get_status());
        assertEquals("customer.search", connection.requests().get(0).getRequestType());
        assertEquals("Jones", connection.requests().get(0).getValue("search"));
    }

    @Test
    void searchCustomer_whenTheMainframeRefuses_givesNoAnswer() throws Exception {
        connection.replyRefused("Bad search.");

        assertNull(adapter.searchCustomer(1, "Jones", 10));
    }
}
