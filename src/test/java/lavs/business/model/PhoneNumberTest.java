package lavs.business.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhoneNumberTest {

    @Test
    void newPhoneNumber_withAllValues_keepsThem() {
        PhoneNumber phone = new PhoneNumber(true, "+64", "Jane Doe", "C-1", 3L, true, "work", "Other");

        assertTrue(phone.is_canTxt());
        assertEquals("+64", phone.get_countryCode());
        assertEquals("Jane Doe", phone.get_customerName());
        assertEquals("C-1", phone.get_customerNumber());
        assertEquals(3L, phone.get_id());
        assertTrue(phone.is_isPrimary());
        assertEquals("work", phone.get_otherType());
        assertEquals("Other", phone.get_type());
    }

    @Test
    void setters_changeEveryProperty() {
        PhoneNumber phone = new PhoneNumber(false, "", "", "", 0L, false, "", "");

        phone.set_canTxt(true);
        phone.set_countryCode("+61");
        phone.set_customerName("Sam Doe");
        phone.set_customerNumber("C-2");
        phone.set_id(8L);
        phone.set_isPrimary(true);
        phone.set_number("0211234567");
        phone.set_otherType("home");
        phone.set_type("Mobile");

        assertTrue(phone.is_canTxt());
        assertEquals("+61", phone.get_countryCode());
        assertEquals("Sam Doe", phone.get_customerName());
        assertEquals("C-2", phone.get_customerNumber());
        assertEquals(8L, phone.get_id());
        assertTrue(phone.is_isPrimary());
        assertEquals("0211234567", phone.get_number());
        assertEquals("home", phone.get_otherType());
        assertEquals("Mobile", phone.get_type());
    }

    @Test
    void newPhoneNumber_withFlagsOff_reportsThemOff() {
        PhoneNumber phone = new PhoneNumber(false, "+64", "Jane Doe", "C-1", 3L, false, "", "Mobile");

        assertFalse(phone.is_canTxt());
        assertFalse(phone.is_isPrimary());
    }

    @Test
    void phoneNumber_isACustomerType() {
        assertInstanceOf(CustomerType.class, new PhoneNumber(false, "", "", "", 0L, false, "", ""));
    }
}
