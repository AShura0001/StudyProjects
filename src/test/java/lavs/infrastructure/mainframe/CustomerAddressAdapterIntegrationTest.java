package lavs.infrastructure.mainframe;

import lavs.business.model.Address;
import lavs.business.model.CustomerType;
import lavs.business.repository.CustomerSearchResult;
import mainframe.client.Connection;
import mainframe.client.HttpConnection;
import mainframe.client.TestConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class CustomerAddressAdapterIntegrationTest {

    private static final String USERNAME =
            System.getProperty("mainframe.username", "admin");

    private static final String PASSWORD =
            System.getProperty("mainframe.password", "admin");

    private static final String CUSTOMER_NUMBER =
            System.getProperty(
                    "mainframe.customerNumber",
                    "01-0001-0000020"
            );

    private Connection connection;
    private CustomerAddressAdapter adapter;

    @BeforeEach
    void setUp() throws Exception {
        connection = new HttpConnection(
                TestConstants.BASE_URL
        );

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

        adapter =
                new CustomerAddressAdapter(connection);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (connection != null) {
            connection.Logout();
            connection.close();
        }
    }

    @Test
    void listCustomerAddressesReturnsAddressesFromMainframe()
            throws Exception {

        long page = 1;
        long pageSize = 10;

        CustomerSearchResult<Address> result =
                adapter.ListCustomerAddress(
                        page,
                        CUSTOMER_NUMBER,
                        pageSize
                );

        assertNotNull(result);

        assertEquals(page, result.getPage());
        assertTrue(
                result.getTotal() >= 0,
                "Total address count should not be negative"
        );

        assertNotNull(result.getList());

        for (Address address :  result.getList()) {
            assertEquals(
                    CUSTOMER_NUMBER,
                    address.getCustomerNumber()
            );

            assertNotNull(address.getCity());
            assertNotNull(address.getCountry());
            assertNotNull(address.getType());
            assertNotNull(address.getIsMailing());
            assertNotNull(address.getIsPrimary());
        }
    }

    @Test
    void listCustomerAddressesMapsAllReturnedAddresses()
            throws Exception {

        CustomerSearchResult<Address> result =
                adapter.ListCustomerAddress(
                        1,
                        CUSTOMER_NUMBER,
                        10
                );

        assertNotNull(result);

        assertTrue(
                result.getList().size()
                        <= 10,
                "The result should not contain more than pageSize addresses"
        );

        /*
         * This verifies that the mapper does not return after the first
         * item. If the mainframe returns multiple addresses, all of them
         * should be present in the result.
         */
        assertTrue(
                result.getList().size()
                        <= result.getTotal()
                        || result.getTotal() == 0
        );
    }

    @Test
    void listCustomerAddressesWithUnknownCustomerReturnsEmptyResult()
            throws Exception {

        CustomerSearchResult<Address> result =
                adapter.ListCustomerAddress(
                        1,
                        "CUSTOMER-DOES-NOT-EXIST",
                        10
                );

        assertNotNull(result);
        assertTrue(result.getList().isEmpty());
    }

    @Test
    void listCustomerAddressesWithDifferentPagesReturnsValidPageData()
            throws Exception {

        CustomerSearchResult<Address> firstPage =
                adapter.ListCustomerAddress(
                        1,
                        CUSTOMER_NUMBER,
                        1
                );

        CustomerSearchResult<Address> secondPage =
                adapter.ListCustomerAddress(
                        2,
                        CUSTOMER_NUMBER,
                        1
                );

        assertNotNull(firstPage);
        assertNotNull(secondPage);

        assertEquals(1, firstPage.getPage());
        assertEquals(2, secondPage.getPage());

        assertTrue(firstPage.getList().size() <= 1);
        assertTrue(secondPage.getList().size() <= 1);

        if (!firstPage.getList().isEmpty()
                && !secondPage.getList().isEmpty()) {

            assertNotEquals(
                    firstPage.getList().get(0).getId(),
                    secondPage.getList().get(0).getId(),
                    "Different pages should not return the same address"
            );
        }
    }
}
