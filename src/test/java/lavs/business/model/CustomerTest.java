package lavs.business.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class CustomerTest {

    @Test
    void newCustomer_withValues_keepsThem() {
        Customer customer = new Customer("1990-05-04", "Doe", "Jane", "C-1", "Active", "Ms");

        assertEquals("1990-05-04", customer.get_dateOfBirth());
        assertEquals("Doe", customer.get_familyName());
        assertEquals("Jane", customer.get_firstName());
        assertEquals("C-1", customer.get_number());
        assertEquals("Active", customer.get_status());
        assertEquals("Ms", customer.get_title());
    }

    @Test
    void setters_changeEveryProperty() {
        Customer customer = new Customer();

        customer.set_dateOfBirth("1985-01-02");
        customer.set_familyName("Smith");
        customer.set_firstName("Sam");
        customer.set_nationality("New Zealand");
        customer.set_number("C-2");
        customer.setOrganisation(3L);
        customer.set_status("Inactive");
        customer.set_title("Mr");

        assertEquals("1985-01-02", customer.get_dateOfBirth());
        assertEquals("Smith", customer.get_familyName());
        assertEquals("Sam", customer.get_firstName());
        assertEquals("New Zealand", customer.get_nationality());
        assertEquals("C-2", customer.get_number());
        assertEquals(3L, customer.getOrganisation());
        assertEquals("Inactive", customer.get_status());
        assertEquals("Mr", customer.get_title());
    }

    @Test
    void customer_isACustomerType() {
        assertInstanceOf(CustomerType.class, new Customer());
    }
}
