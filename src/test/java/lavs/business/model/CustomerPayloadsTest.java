package lavs.business.model;

import lavs.business.model.CustomerPayloads.CustomerContent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CustomerPayloadsTest {

    @Test
    void writeAndRead_roundTripsCustomerFields() {
        Customer customer = new Customer();
        customer.set_firstName("Jane");
        customer.set_familyName("Doe");
        customer.set_title("Ms");
        customer.set_dateOfBirth("1990-01-01");
        customer.set_nationality("NZ");
        customer.setOrganisation(1L);

        String payload = CustomerPayloads.customerPayload(customer, List.of(), List.of(), List.of(), List.of());
        CustomerContent content = CustomerPayloads.readCustomer(payload);

        assertEquals("Jane", content.customer().get_firstName());
        assertEquals("Doe", content.customer().get_familyName());
        assertEquals("Ms", content.customer().get_title());
        assertEquals("1990-01-01", content.customer().get_dateOfBirth());
        assertEquals("NZ", content.customer().get_nationality());
        assertEquals(1L, content.customer().getOrganisation());
    }

    @Test
    void writeAndRead_roundTripsAnAddress() {
        Customer customer = new Customer();
        customer.set_firstName("Jane");

        Address address = new Address();
        address.setType("Home");
        address.setLine1("1 Queen Street");
        address.setLine2("Unit 2");
        address.setSuburb("CBD");
        address.setCity("Auckland");
        address.setPostCode("1010");
        address.setCountry("New Zealand");
        address.setIsMailing(true);
        address.setIsPrimary(true);

        String payload = CustomerPayloads.customerPayload(customer, List.of(address), List.of(), List.of(), List.of());
        CustomerContent content = CustomerPayloads.readCustomer(payload);

        Address read = content.addresses().get(0);
        assertEquals("Home", read.getType());
        assertEquals("1 Queen Street", read.getLine1());
        assertEquals("Auckland", read.getCity());
        assertEquals("1010", read.getPostCode());
        assertTrue(read.getIsMailing());
        assertTrue(read.getIsPrimary());
    }

    @Test
    void writeAndRead_roundTripsAPhoneNumber() {
        Customer customer = new Customer();
        customer.set_firstName("Jane");

        PhoneNumber phone = new PhoneNumber(true, "64", null, null, 0L, true, null, "Mobile");
        phone.set_number("211234567");

        String payload = CustomerPayloads.customerPayload(customer, List.of(), List.of(phone), List.of(), List.of());
        CustomerContent content = CustomerPayloads.readCustomer(payload);

        PhoneNumber read = content.phoneNumbers().get(0);
        assertEquals("Mobile", read.get_type());
        assertEquals("64", read.get_countryCode());
        assertEquals("211234567", read.get_number());
        assertTrue(read.is_isPrimary());
        assertTrue(read.is_canTxt());
    }

    @Test
    void writeAndRead_roundTripsAnEmail() {
        Customer customer = new Customer();
        customer.set_firstName("Jane");

        Email email = new Email("jane@example.com", null, null, 0L, true, null, "Personal");

        String payload = CustomerPayloads.customerPayload(customer, List.of(), List.of(), List.of(email), List.of());
        CustomerContent content = CustomerPayloads.readCustomer(payload);

        Email read = content.emails().get(0);
        assertEquals("jane@example.com", read.getAddress());
        assertEquals("Personal", read.getType());
        assertTrue(read.isPrimary());
    }

    @Test
    void writeAndRead_roundTripsAnEmployer() {
        Customer customer = new Customer();
        customer.set_firstName("Jane");

        Employer employer = new Employer(null, null, 0L, "Acme Ltd", "Developer", 75000.0, "Full-time");

        String payload = CustomerPayloads.customerPayload(customer, List.of(), List.of(), List.of(), List.of(employer));
        CustomerContent content = CustomerPayloads.readCustomer(payload);

        Employer read = content.employers().get(0);
        assertEquals("Acme Ltd", read.get_name());
        assertEquals("Developer", read.get_position());
        assertEquals(75000.0, read.get_salary());
        assertEquals("Full-time", read.get_type());
    }

    @Test
    void writeAndRead_withEmptyLists_roundTripsCleanly() {
        Customer customer = new Customer();
        customer.set_firstName("Jane");

        String payload = CustomerPayloads.customerPayload(customer, List.of(), List.of(), List.of(), List.of());
        CustomerContent content = CustomerPayloads.readCustomer(payload);

        assertTrue(content.addresses().isEmpty());
        assertTrue(content.phoneNumbers().isEmpty());
        assertTrue(content.emails().isEmpty());
        assertTrue(content.employers().isEmpty());
    }

    @Test
    void readCustomer_withInvalidJson_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> CustomerPayloads.readCustomer("not json"));
    }

    @Test
    void readCustomer_withWrongRecordType_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> CustomerPayloads.readCustomer("{\"record\":\"loan\"}"));
    }

    @Test
    void readCustomer_withNullPayload_throwsNullPointer() {
        assertThrows(NullPointerException.class, () -> CustomerPayloads.readCustomer(null));
    }
}
