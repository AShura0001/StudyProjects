package lavs.business.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Tests for the customer records: each constructor keeps its values and each setter changes one. */
class CustomerModelsTest {

    private static final String CUSTOMER_NUMBER = "01-0001-0000020";
    private static final String CUSTOMER_NAME = "Jane Smith";

    @Test
    void newAddress_keepsValues() {
        Address address = new Address("Auckland", "New Zealand", CUSTOMER_NAME, CUSTOMER_NUMBER, 7L,
                true, false, "14 Karaka Street", "Unit 2", "N-1", "Holiday", "1023", "Newmarket", "Postal");

        assertEquals("Auckland", address.getCity());
        assertEquals("New Zealand", address.getCountry());
        assertEquals(CUSTOMER_NAME, address.getCustomerName());
        assertEquals(CUSTOMER_NUMBER, address.getCustomerNumber());
        assertEquals(7L, address.getId());
        assertTrue(address.getIsMailing());
        assertFalse(address.getIsPrimary());
        assertEquals("14 Karaka Street", address.getLine1());
        assertEquals("Unit 2", address.getLine2());
        assertEquals("N-1", address.getNumber());
        assertEquals("Holiday", address.getOtherType());
        assertEquals("1023", address.getPostCode());
        assertEquals("Newmarket", address.getSuburb());
        assertEquals("Postal", address.getType());
    }

    @Test
    void addressSetters_changeValues() {
        Address address = new Address();
        address.setCity("Wellington");
        address.setCountry("NZ");
        address.setCustomerName(CUSTOMER_NAME);
        address.setCustomerNumber(CUSTOMER_NUMBER);
        address.setId(8L);
        address.setIsMailing(false);
        address.setIsPrimary(true);
        address.setLine1("1 Queen Street");
        address.setLine2("Level 3");
        address.setNumber("N-2");
        address.setOtherType("Work");
        address.setPostCode("6011");
        address.setSuburb("Te Aro");
        address.setType("Residential");

        assertEquals("Wellington", address.getCity());
        assertEquals("NZ", address.getCountry());
        assertEquals(CUSTOMER_NAME, address.getCustomerName());
        assertEquals(CUSTOMER_NUMBER, address.getCustomerNumber());
        assertEquals(8L, address.getId());
        assertFalse(address.getIsMailing());
        assertTrue(address.getIsPrimary());
        assertEquals("1 Queen Street", address.getLine1());
        assertEquals("Level 3", address.getLine2());
        assertEquals("N-2", address.getNumber());
        assertEquals("Work", address.getOtherType());
        assertEquals("6011", address.getPostCode());
        assertEquals("Te Aro", address.getSuburb());
        assertEquals("Residential", address.getType());
    }

    @Test
    void newPhoneNumber_keepsValues() {
        PhoneNumber phone = new PhoneNumber(true, "+64", CUSTOMER_NAME, CUSTOMER_NUMBER, 3, false, "Pager", "Other");

        assertTrue(phone.is_canTxt());
        assertEquals("+64", phone.get_countryCode());
        assertEquals(CUSTOMER_NAME, phone.get_customerName());
        assertEquals(CUSTOMER_NUMBER, phone.get_customerNumber());
        assertEquals(3, phone.get_id());
        assertFalse(phone.is_isPrimary());
        assertEquals("Pager", phone.get_otherType());
        assertEquals("Other", phone.get_type());
        assertNull(phone.get_number(), "The constructor does not take the phone number itself");
    }

    @Test
    void phoneNumberSetters_changeValues() {
        PhoneNumber phone = new PhoneNumber(false, null, null, null, 0, false, null, null);
        phone.set_canTxt(true);
        phone.set_countryCode("+61");
        phone.set_customerName(CUSTOMER_NAME);
        phone.set_customerNumber(CUSTOMER_NUMBER);
        phone.set_id(4);
        phone.set_isPrimary(true);
        phone.set_number("21-222-2222");
        phone.set_otherType("Fax");
        phone.set_type("Mobile");

        assertTrue(phone.is_canTxt());
        assertEquals("+61", phone.get_countryCode());
        assertEquals(CUSTOMER_NAME, phone.get_customerName());
        assertEquals(CUSTOMER_NUMBER, phone.get_customerNumber());
        assertEquals(4, phone.get_id());
        assertTrue(phone.is_isPrimary());
        assertEquals("21-222-2222", phone.get_number());
        assertEquals("Fax", phone.get_otherType());
        assertEquals("Mobile", phone.get_type());
    }

    @Test
    void newRelationship_keepsValues() {
        Relationship relationship = new Relationship(CUSTOMER_NAME, CUSTOMER_NUMBER, 5, "Friend",
                "John Smith", "01-0001-0000015", "Other");

        assertEquals(CUSTOMER_NAME, relationship.get_customerName());
        assertEquals(CUSTOMER_NUMBER, relationship.get_customerNumber());
        assertEquals(5, relationship.get_id());
        assertEquals("Friend", relationship.get_otherType());
        assertEquals("John Smith", relationship.get_secondaryCustomerName());
        assertEquals("01-0001-0000015", relationship.get_secondaryCustomerNumber());
        assertEquals("Other", relationship.get_type());
    }

    @Test
    void relationshipSetters_changeValues() {
        Relationship relationship = new Relationship();
        relationship.set_customerName(CUSTOMER_NAME);
        relationship.set_customerNumber(CUSTOMER_NUMBER);
        relationship.set_id(6);
        relationship.set_number("R-6");
        relationship.set_otherType("Cousin");
        relationship.set_secondaryCustomerName("John Smith");
        relationship.set_secondaryCustomerNumber("01-0001-0000015");
        relationship.set_type("Spouse");

        assertEquals(CUSTOMER_NAME, relationship.get_customerName());
        assertEquals(CUSTOMER_NUMBER, relationship.get_customerNumber());
        assertEquals(6, relationship.get_id());
        assertEquals("R-6", relationship.get_number());
        assertEquals("Cousin", relationship.get_otherType());
        assertEquals("John Smith", relationship.get_secondaryCustomerName());
        assertEquals("01-0001-0000015", relationship.get_secondaryCustomerNumber());
        assertEquals("Spouse", relationship.get_type());
    }

    @Test
    void newDocument_keepsValues() {
        Document document = new Document(CUSTOMER_NAME, CUSTOMER_NUMBER, "id.pdf", "application/pdf", 9, 2048, "Identity");

        assertEquals(CUSTOMER_NAME, document.getCustomerName());
        assertEquals(CUSTOMER_NUMBER, document.getCustomerNumber());
        assertEquals("id.pdf", document.getFileName());
        assertEquals("application/pdf", document.getFileType());
        assertEquals(9, document.getId());
        assertEquals(2048, document.getSize());
        assertEquals("Identity", document.getType());
    }

    @Test
    void documentSetters_changeValues() {
        Document document = new Document();
        document.setCustomerName(CUSTOMER_NAME);
        document.setCustomerNumber(CUSTOMER_NUMBER);
        document.setFileName("bill.png");
        document.setFileType("image/png");
        document.setId(10);
        document.setNumber("DOC-10");
        document.setSize(512);
        document.setType("ProofOfAddress");

        assertEquals(CUSTOMER_NAME, document.getCustomerName());
        assertEquals(CUSTOMER_NUMBER, document.getCustomerNumber());
        assertEquals("bill.png", document.getFileName());
        assertEquals("image/png", document.getFileType());
        assertEquals(10, document.getId());
        assertEquals("DOC-10", document.getNumber());
        assertEquals(512, document.getSize());
        assertEquals("ProofOfAddress", document.getType());
    }

    @Test
    void newNote_keepsValues() {
        Note note = new Note("test", CUSTOMER_NAME, CUSTOMER_NUMBER, "2026-10-01", 11, "Line 1", "1", "General");

        assertEquals("test", note.get_author());
        assertEquals(CUSTOMER_NAME, note.get_customerName());
        assertEquals(CUSTOMER_NUMBER, note.get_customerNumber());
        assertEquals("2026-10-01", note.get_dateAdded());
        assertEquals(11, note.get_id());
        assertEquals("Line 1", note.get_lines());
        assertEquals("1", note.get_numberOfLines());
        assertEquals("General", note.get_type());
    }

    @Test
    void noteSetters_changeValues() {
        Note note = new Note();
        note.set_author("admin");
        note.set_customerName(CUSTOMER_NAME);
        note.set_customerNumber(CUSTOMER_NUMBER);
        note.set_dateAdded("2026-10-02");
        note.set_id(12);
        note.set_lines("Line 1\nLine 2");
        note.set_number("NOTE-12");
        note.set_numberOfLines("2");
        note.set_type("Review");

        assertEquals("admin", note.get_author());
        assertEquals(CUSTOMER_NAME, note.get_customerName());
        assertEquals(CUSTOMER_NUMBER, note.get_customerNumber());
        assertEquals("2026-10-02", note.get_dateAdded());
        assertEquals(12, note.get_id());
        assertEquals("Line 1\nLine 2", note.get_lines());
        assertEquals("NOTE-12", note.get_number());
        assertEquals("2", note.get_numberOfLines());
        assertEquals("Review", note.get_type());
    }

    @Test
    void newEmployer_keepsValues() {
        Employer employer = new Employer(CUSTOMER_NAME, CUSTOMER_NUMBER, 13, "Widgets Ltd", "Tester", 55000.0, "Primary");

        assertEquals(CUSTOMER_NAME, employer.get_customerName());
        assertEquals(CUSTOMER_NUMBER, employer.get_customerNumber());
        assertEquals(13, employer.get_id());
        assertEquals("Widgets Ltd", employer.get_name());
        assertEquals("Tester", employer.get_position());
        assertEquals(55000.0, employer.get_salary());
        assertEquals("Primary", employer.get_type());
    }

    @Test
    void employerSetters_changeValues() {
        Employer employer = new Employer();
        employer.set_customerName(CUSTOMER_NAME);
        employer.set_customerNumber(CUSTOMER_NUMBER);
        employer.set_id(14);
        employer.set_name("Gadgets Ltd");
        employer.set_number("EMP-14");
        employer.set_position("Manager");
        employer.set_salary(80000.0);
        employer.set_type("Secondary");

        assertEquals(CUSTOMER_NAME, employer.get_customerName());
        assertEquals(CUSTOMER_NUMBER, employer.get_customerNumber());
        assertEquals(14, employer.get_id());
        assertEquals("Gadgets Ltd", employer.get_name());
        assertEquals("EMP-14", employer.get_number());
        assertEquals("Manager", employer.get_position());
        assertEquals(80000.0, employer.get_salary());
        assertEquals("Secondary", employer.get_type());
    }

    @Test
    void newEmail_keepsValues() {
        Email email = new Email("jane@example.com", CUSTOMER_NAME, CUSTOMER_NUMBER, 15, true, "School", "Other");

        assertEquals("jane@example.com", email.getAddress());
        assertEquals(CUSTOMER_NAME, email.getCustomerName());
        assertEquals(CUSTOMER_NUMBER, email.getCustomerNumber());
        assertEquals(15, email.getId());
        assertTrue(email.isPrimary());
        assertEquals("School", email.getOtherType());
        assertEquals("Other", email.getType());
    }

    @Test
    void emailSetters_changeValues() {
        Email email = new Email();
        email.setAddress("work@example.com");
        email.setCustomerName(CUSTOMER_NAME);
        email.setCustomerNumber(CUSTOMER_NUMBER);
        email.setId(16);
        email.setPrimary(true);
        email.setNumber("EM-16");
        email.setOtherType("Club");
        email.setType("Work");

        assertEquals("work@example.com", email.getAddress());
        assertEquals(CUSTOMER_NAME, email.getCustomerName());
        assertEquals(CUSTOMER_NUMBER, email.getCustomerNumber());
        assertEquals(16, email.getId());
        assertTrue(email.isPrimary());
        assertEquals("EM-16", email.getNumber());
        assertEquals("Club", email.getOtherType());
        assertEquals("Work", email.getType());
    }

    @Test
    void newCustomer_keepsValues() {
        Customer customer = new Customer("1990-05-01", "Smith", "Jane", CUSTOMER_NUMBER, "Active", "Ms");

        assertEquals("1990-05-01", customer.get_dateOfBirth());
        assertEquals("Smith", customer.get_familyName());
        assertEquals("Jane", customer.get_firstName());
        assertEquals(CUSTOMER_NUMBER, customer.get_number());
        assertEquals("Active", customer.get_status());
        assertEquals("Ms", customer.get_title());
    }

    @Test
    void customerSetters_changeValues() {
        Customer customer = new Customer();
        customer.set_dateOfBirth("1985-12-31");
        customer.set_familyName("Brown");
        customer.set_firstName("Sam");
        customer.set_nationality("New Zealander");
        customer.set_number(CUSTOMER_NUMBER);
        customer.set_status("Inactive");
        customer.set_title("Dr");
        customer.setOrganisation(2);

        assertEquals("1985-12-31", customer.get_dateOfBirth());
        assertEquals("Brown", customer.get_familyName());
        assertEquals("Sam", customer.get_firstName());
        assertEquals("New Zealander", customer.get_nationality());
        assertEquals(CUSTOMER_NUMBER, customer.get_number());
        assertEquals("Inactive", customer.get_status());
        assertEquals("Dr", customer.get_title());
        assertEquals(2, customer.getOrganisation());
    }
}
