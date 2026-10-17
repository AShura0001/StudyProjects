package lavs.infrastructure.mainframe;

import lavs.business.model.Customer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

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
}