package lavs.infrastructure.mainframe;

import lavs.business.model.Customer;
import lavs.business.repository.CustomerSearchResult;
import mainframe.client.messages.customer.Create;
import mainframe.client.messages.customer.Retrieve;
import mainframe.client.messages.customer.Search;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class CustomerAdapterIntegrationTest
        extends MainframeIntegrationTestSupport {

    private final String customerNumber = System.getProperty(
            "mainframe.customerNumber",
            "01-0001-0000020"
    );

    @Test
    void findByCustomerNumberRetrievesCustomerFromMainframe()
            throws Exception {

        CustomerAdapter adapter =
                new CustomerAdapter(connection);

        Optional<Customer> result =
                adapter.findByCustomerNumber(customerNumber);

        assertTrue(
                result.isPresent(),
                "Expected the mainframe to return customer "
                        + customerNumber
        );

        Customer customer = result.orElseThrow();

        assertEquals(
                customerNumber,
                customer.get_number()
        );

        assertNotNull(customer.get_firstName());
        assertFalse(customer.get_firstName().isBlank());

        assertNotNull(customer.get_status());
        assertFalse(customer.get_status().isBlank());

    }

    @Test
    void findByCustomerNumberReturnsEmptyForUnknownCustomer()
            throws Exception {

        CustomerAdapter adapter =
                new CustomerAdapter(connection);

        Optional<Customer> result =
                adapter.findByCustomerNumber(
                        "CUSTOMER-DOES-NOT-EXIST"
                );

        assertTrue(
                result.isEmpty(),
                "Expected an unknown customer to return Optional.empty()"
        );
    }

    @Test
    void searchCustomerByFamilyNameOrNameReturnNotNull() throws IOException {
        CustomerAdapter adapter =
                new CustomerAdapter(connection);
        CustomerSearchResult result = adapter.searchCustomer(1, "a", 25);
        assertNotNull(result);

    }

    @Test
    void createCustomerReturnsCreatedCustomer() throws IOException {
        CustomerAdapter adapter =
                new CustomerAdapter(connection);
        Customer customer = new Customer();
        customer.set_firstName("a");
        customer.set_title("Mr");
        customer.set_status("Active");
        customer.set_familyName("bcd");
        customer.set_dateOfBirth("2024-03-03");

        Optional<Customer> result = adapter.createCustomer(customer);
        assertTrue(result.isPresent());

        Optional<Customer> retrieved =  adapter.findByCustomerNumber(result.get().get_number());
        assertTrue(retrieved.isPresent());
        assertEquals(customer.get_firstName(), retrieved.get().get_firstName());

    }
}