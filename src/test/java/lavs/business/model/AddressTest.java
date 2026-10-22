package lavs.business.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddressTest {

    @Test
    void newAddress_withAllValues_keepsThem() {
        Address address = new Address("Auckland", "New Zealand", "Jane Doe", "C-1", 5L, true, false,
                "1 Queen Street", "Flat 2", "A-1", "holiday", "1010", "CBD", "Other");

        assertEquals("Auckland", address.getCity());
        assertEquals("New Zealand", address.getCountry());
        assertEquals("Jane Doe", address.getCustomerName());
        assertEquals("C-1", address.getCustomerNumber());
        assertEquals(5L, address.getId());
        assertTrue(address.getIsMailing());
        assertFalse(address.getIsPrimary());
        assertEquals("1 Queen Street", address.getLine1());
        assertEquals("Flat 2", address.getLine2());
        assertEquals("A-1", address.getNumber());
        assertEquals("holiday", address.getOtherType());
        assertEquals("1010", address.getPostCode());
        assertEquals("CBD", address.getSuburb());
        assertEquals("Other", address.getType());
    }

    @Test
    void setters_changeEveryProperty() {
        Address address = new Address();

        address.setCity("Wellington");
        address.setCountry("New Zealand");
        address.setCustomerName("Sam Doe");
        address.setCustomerNumber("C-2");
        address.setId(9L);
        address.setIsMailing(true);
        address.setIsPrimary(true);
        address.setLine1("2 Lambton Quay");
        address.setLine2("Level 3");
        address.setNumber("A-2");
        address.setOtherType("work");
        address.setPostCode("6011");
        address.setSuburb("Central");
        address.setType("Residential");

        assertEquals("Wellington", address.getCity());
        assertEquals("New Zealand", address.getCountry());
        assertEquals("Sam Doe", address.getCustomerName());
        assertEquals("C-2", address.getCustomerNumber());
        assertEquals(9L, address.getId());
        assertTrue(address.getIsMailing());
        assertTrue(address.getIsPrimary());
        assertEquals("2 Lambton Quay", address.getLine1());
        assertEquals("Level 3", address.getLine2());
        assertEquals("A-2", address.getNumber());
        assertEquals("work", address.getOtherType());
        assertEquals("6011", address.getPostCode());
        assertEquals("Central", address.getSuburb());
        assertEquals("Residential", address.getType());
    }

    @Test
    void address_isACustomerType() {
        assertInstanceOf(CustomerType.class, new Address());
    }
}
