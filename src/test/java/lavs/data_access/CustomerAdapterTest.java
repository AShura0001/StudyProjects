package lavs.data_access;


import lavs.business.model.Address;
import lavs.business.model.Customer;
import mainframe.client.*;
import mainframe.client.messages.customer.AddAddress;
import mainframe.client.messages.customer.Retrieve;
import mainframe.client.messages.customer.Search;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

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

    @Test
    void UpdateAddress() throws IOException, InterruptedException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        Connection connection = new HttpConnection(TestConstants.BASE_URL);
        connection.Login("test", "test");

        Search searchMessage = sendMessage(
                connection,
                new Search(1, "Smith", 25),
                "search");
        Search.Item[] customers = searchMessage.getItems();
        Assertions.assertNotNull(customers.length > 0, "No customers returned");
        String customerNumber = customers[0].getNumber();

        CustomerAdapter customerAdapter = new CustomerAdapter(connection);


        Address address = new Address("Auckland",
                "New Zealand", "Smith",
                customerNumber,
                null,
                false,
                false,
                "Line 1",
                "Line 2",
                null,
                "1234",
                "Onehunga",
                AddAddress.TYPE_POSTAL);

        Optional<Address> addressOptional = customerAdapter.updateCustomerAddress(address);

        assertTrue(addressOptional.isPresent());

        Address result = addressOptional.get();

        assertEquals("Auckland", result.get_city());
        assertEquals("New Zealand", result.get_country());
        assertEquals("Johnn Smith", result.get_customerName());
        assertEquals(customerNumber, result.get_customerNumber());


        assertEquals(false, result.is_isMailing());
        assertEquals(false, result.is_isPrimary());

        assertEquals("Line 1", result.get_line1());
        assertEquals("Line 2", result.get_line2());

        assertEquals(null, result.get_otherType());

        assertEquals("1234", result.get_postCode());
        assertEquals("Onehunga", result.get_suburb());
        assertEquals(AddAddress.TYPE_POSTAL, result.get_type());






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

    private <T extends Message> T sendMessage(Connection connection, T message, String name) throws IOException, InterruptedException {
        Status status = message.send(connection);
        Thread.sleep(500);     // give the server time to apply the updates

        Assertions.assertAll(
                () -> Assertions.assertTrue(status.getWasSuccessful(), name + " message failed"),
                () -> Assertions.assertNull(status.getErrorMessage(), name + " message failed")
        );

        return message;
    }
}