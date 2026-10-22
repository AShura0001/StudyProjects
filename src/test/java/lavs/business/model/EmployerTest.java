package lavs.business.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class EmployerTest {

    @Test
    void newEmployer_withAllValues_keepsThem() {
        Employer employer = new Employer("Jane Doe", "C-1", 4L, "Acme Ltd", "Engineer", 85000.5, "Full time");

        assertEquals("Jane Doe", employer.get_customerName());
        assertEquals("C-1", employer.get_customerNumber());
        assertEquals(4L, employer.get_id());
        assertEquals("Acme Ltd", employer.get_name());
        assertEquals("Engineer", employer.get_position());
        assertEquals(85000.5, employer.get_salary());
        assertEquals("Full time", employer.get_type());
    }

    @Test
    void setters_changeEveryProperty() {
        Employer employer = new Employer();

        employer.set_customerName("Sam Doe");
        employer.set_customerNumber("C-2");
        employer.set_id(5L);
        employer.set_name("Globex");
        employer.set_number("EM-1");
        employer.set_position("Manager");
        employer.set_salary(100000.0);
        employer.set_type("Part time");

        assertEquals("Sam Doe", employer.get_customerName());
        assertEquals("C-2", employer.get_customerNumber());
        assertEquals(5L, employer.get_id());
        assertEquals("Globex", employer.get_name());
        assertEquals("EM-1", employer.get_number());
        assertEquals("Manager", employer.get_position());
        assertEquals(100000.0, employer.get_salary());
        assertEquals("Part time", employer.get_type());
    }

    @Test
    void employer_isACustomerType() {
        assertInstanceOf(CustomerType.class, new Employer());
    }
}
