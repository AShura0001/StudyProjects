package lavs.business.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailTest {

    @Test
    void newEmail_withAllValues_keepsThem() {
        Email email = new Email("jane@example.com", "Jane Doe", "C-1", 7L, true, "work", "Other");

        assertEquals("jane@example.com", email.getAddress());
        assertEquals("Jane Doe", email.getCustomerName());
        assertEquals("C-1", email.getCustomerNumber());
        assertEquals(7L, email.getId());
        assertTrue(email.isPrimary());
        assertEquals("work", email.getOtherType());
        assertEquals("Other", email.getType());
    }

    @Test
    void setters_changeEveryProperty() {
        Email email = new Email();

        email.setAddress("sam@example.com");
        email.setCustomerName("Sam Doe");
        email.setCustomerNumber("C-2");
        email.setId(9L);
        email.setPrimary(true);
        email.setNumber("E-1");
        email.setOtherType("home");
        email.setType("Personal");

        assertEquals("sam@example.com", email.getAddress());
        assertEquals("Sam Doe", email.getCustomerName());
        assertEquals("C-2", email.getCustomerNumber());
        assertEquals(9L, email.getId());
        assertTrue(email.isPrimary());
        assertEquals("E-1", email.getNumber());
        assertEquals("home", email.getOtherType());
        assertEquals("Personal", email.getType());
    }

    @Test
    void newEmail_withNothingSet_isNotPrimary() {
        assertFalse(new Email().isPrimary());
    }

    @Test
    void email_isACustomerType() {
        assertInstanceOf(CustomerType.class, new Email());
    }
}
