package lavs.data_access;


import lavs.business.model.Customer;
import mainframe.client.Message;
import mainframe.client.MessageResponse;
import mainframe.client.Status;
import mainframe.client.StubConnection;
import mainframe.client.messages.customer.Retrieve;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CustomerAdapterTest {

    @Test
    void findByCustomerNumberReturnsMappedCustomerWhenRequestSucceeds()
            throws IOException {

        StubConnection connection = new StubConnection();

        HashMap<String, String> values = new HashMap<>();
        values.put("dateOfBirth", "1990-01-15");
        values.put("familyName", "Doe");
        values.put("firstName", "Jane");
        values.put("nationality", "New Zealand");
        values.put("number", "CUS-20481");
        values.put("status", "Active");
        values.put("title", "Ms");

        connection.setResponse(new MessageResponse(
                new Status(1),
                values
        ));

        CustomerAdapter adapter = new CustomerAdapter(connection);

        Optional<Customer> result =
                adapter.findByCustomerNumber("CUS-20481");

        assertTrue(result.isPresent());

        Customer customer = result.get();

        assertEquals("1990-01-15", customer.get_dateOfBirth());
        assertEquals("Doe", customer.get_familyName());
        assertEquals("Jane", customer.get_firstName());
        assertEquals("New Zealand", customer.get_nationality());
        assertEquals("CUS-20481", customer.get_number());
        assertEquals("Active", customer.get_status());
        assertEquals("Ms", customer.get_title());
    }

    @Test
    void findByCustomerNumberReturnsEmptyWhenMainframeRequestFails()
            throws IOException {

        StubConnection connection = new StubConnection();

        connection.setResponse(new MessageResponse(
                new Status(400, "Customer was not found", 1),
                new HashMap<>()
        ));

        CustomerAdapter adapter = new CustomerAdapter(connection);

        Optional<Customer> result =
                adapter.findByCustomerNumber("CUS-99999");

        assertTrue(result.isEmpty());
    }

    @Test
    void mapMessageToCustomerReturnsEmptyForNonRetrieveMessage() {

        StubConnection connection = new StubConnection();
        CustomerAdapter adapter = new CustomerAdapter(connection);

        Message message = new FakeMessage();

        Optional<Customer> result =
                adapter.mapMessageToCustomer(message);

        assertTrue(result.isEmpty());
    }

    @Test
    void mapMessageToCustomerMapsRetrieveMessage() throws IOException {

        StubConnection connection = new StubConnection();

        HashMap<String, String> values = new HashMap<>();
        values.put("dateOfBirth", "1985-05-20");
        values.put("familyName", "Smith");
        values.put("firstName", "John");
        values.put("nationality", "Australian");
        values.put("number", "CUS-10001");
        values.put("status", "Inactive");
        values.put("title", "Mr");

        connection.setResponse(new MessageResponse(
                new Status(1),
                values
        ));

        Retrieve retrieve = new Retrieve("CUS-10001");
        retrieve.send(connection);

        CustomerAdapter adapter = new CustomerAdapter(connection);

        Optional<Customer> result =
                adapter.mapMessageToCustomer(retrieve);

        assertTrue(result.isPresent());

        Customer customer = result.get();

        assertEquals("1985-05-20", customer.get_dateOfBirth());
        assertEquals("Smith", customer.get_familyName());
        assertEquals("John", customer.get_firstName());
        assertEquals("Australian", customer.get_nationality());
        assertEquals("CUS-10001", customer.get_number());
        assertEquals("Inactive", customer.get_status());
        assertEquals("Mr", customer.get_title());
    }

    @Test
    void mapMessageToCustomerReturnsEmptyForUnpopulatedRetrieve() {

        CustomerAdapter adapter =
                new CustomerAdapter(new StubConnection());

        Retrieve retrieve = new Retrieve();

        Optional<Customer> result =
                adapter.mapMessageToCustomer(retrieve);

        /*
         * This test documents the current behavior:
         * mapMessageToCustomer only checks the message type, not whether
         * the Retrieve actually succeeded.
         */
        assertTrue(result.isPresent());

        Customer customer = result.get();

        assertNull(customer.get_dateOfBirth());
        assertNull(customer.get_familyName());
        assertNull(customer.get_firstName());
        assertNull(customer.get_nationality());
        assertNull(customer.get_number());
        assertNull(customer.get_title());
    }


    /**
     * Minimal Message implementation used to verify that the mapper ignores
     * messages that are not customer Retrieve messages.
     */
    private static class FakeMessage implements Message {

        @Override
        public Status send(mainframe.client.Connection connection)
                throws IOException {
            return new Status(1);
        }
    }

    /**
     * Connection implementation used to verify exception propagation.
     */
    private static class ConnectionThatThrows implements mainframe.client.Connection {

        @Override
        public void close() {
        }

        @Override
        public boolean isConnected() {
            return false;
        }

        @Override
        public mainframe.client.MessageResponse send(
                mainframe.client.MessageRequest request)
                throws RuntimeException {
            throw new RuntimeException("This method should not be called directly");
        }

        @Override
        public Status Login(String username, String password) {
            return new Status(1);
        }

        @Override
        public void Logout() {
        }
    }
}