package lavs.data_access;

import lavs.business.model.Address;
import lavs.business.model.Customer;
import mainframe.client.Connection;
import mainframe.client.HttpConnection;
import mainframe.client.TestConstants;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class CustomerAdapterIntegrationTest {

    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin";

    /*
     * This customer number comes from the example mainframe access
     * documented in the repository README.
     */
    private static final String CUSTOMER_NUMBER = "01-0001-0000020";

    @Test
    void findByCustomerNumberRetrievesAndMapsCustomerFromMainframe()
            throws IOException {

        Connection connection =
                new HttpConnection(TestConstants.BASE_URL);

        try {
            // Verify that the mainframe is reachable.
            assertTrue(
                    connection.isConnected(),
                    "The mainframe is not reachable"
            );

            // Authenticate before making the customer request.
            var loginStatus =
                    connection.Login(USERNAME, PASSWORD);

            assertTrue(
                    loginStatus.getWasSuccessful(),
                    "Login failed: " + loginStatus.getErrorMessage()
            );

            // Use the real adapter with the real HTTP connection.
            CustomerGateway gateway =
                    new CustomerAdapter(connection);

            Optional<Customer> result =
                    gateway.findByCustomerNumber(CUSTOMER_NUMBER);

            assertTrue(
                    result.isPresent(),
                    "Expected the mainframe to return a customer"
            );

            Customer customer = result.get();

            assertEquals(
                    CUSTOMER_NUMBER,
                    customer.get_number()
            );

            assertNotNull(customer.get_firstName());
            assertFalse(customer.get_firstName().isBlank());


            assertNotNull(customer.get_status());
            assertFalse(customer.get_status().isBlank());


        } finally {
            /*
             * Logout clears the token stored inside HttpConnection.
             * close() shuts down OkHttp resources.
             */
            connection.Logout();
            connection.close();
        }
    }

    @Test
    void updateCustomerAddressUpdatesAndRetrievesAddressFromMainframe()
            throws Exception {

        Connection connection =
                new HttpConnection(TestConstants.BASE_URL);

        try {
            assertTrue(
                    connection.isConnected(),
                    "The mainframe is not reachable"
            );

            var loginStatus =
                    connection.Login(USERNAME, PASSWORD);

            assertTrue(
                    loginStatus.getWasSuccessful(),
                    "Login failed: "
                            + loginStatus.getErrorMessage()
            );

            CustomerAdapter adapter =
                    new CustomerAdapter(connection);

            Address input = createAddress(
                    CUSTOMER_NUMBER,
                    "Auckland",
                    "New Zealand",
                    true,
                    true,
                    "14 Karaka Street",
                    "Apartment 3",
                    "",
                    "1023",
                    "Newmarket",
                    "Residential"
            );

            Optional<Address> result =
                    adapter.updateCustomerAddress(input);

            assertTrue(
                    result.isPresent(),
                    "Expected the updated address to be returned"
            );

            Address updated = result.get();

            assertEquals(
                    CUSTOMER_NUMBER,
                    updated.get_customerNumber()
            );

            assertEquals(
                    "Auckland",
                    updated.get_city()
            );

            assertEquals(
                    "New Zealand",
                    updated.get_country()
            );

            assertEquals(
                    "14 Karaka Street",
                    updated.get_line1()
            );

            assertEquals(
                    "Apartment 3",
                    updated.get_line2()
            );

            assertEquals(
                    "1023",
                    updated.get_postCode()
            );

            assertEquals(
                    "Newmarket",
                    updated.get_suburb()
            );

            assertEquals(
                    "Residential",
                    updated.get_type()
            );

            assertTrue(updated.is_isMailing());
            assertTrue(updated.is_isPrimary());

            assertTrue(adapter.deleteCustomerAddress(updated.get_customerNumber(), updated.get_id()));

            result = adapter.findAddressByCustomerNumberAndId(updated.get_id(), updated.get_customerNumber());
            assertFalse(result.isPresent());


        } finally {
            connection.Logout();
            connection.close();
        }
    }


    private static Address createAddress(
            String customerNumber,
            String city,
            String country,
            boolean isMailing,
            boolean isPrimary,
            String line1,
            String line2,
            String otherType,
            String postCode,
            String suburb,
            String type
    ) {
        Address address = new Address();

        address.set_customerNumber(customerNumber);
        address.set_city(city);
        address.set_country(country);
        address.set_isMailing(isMailing);
        address.set_isPrimary(isPrimary);
        address.set_line1(line1);
        address.set_line2(line2);
        address.set_otherType(otherType);
        address.set_postCode(postCode);
        address.set_suburb(suburb);
        address.set_type(type);

        return address;
    }
}
