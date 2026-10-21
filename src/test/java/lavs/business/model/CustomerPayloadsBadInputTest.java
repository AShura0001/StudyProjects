package lavs.business.model;

import lavs.business.model.CustomerPayloads.CustomerContent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** How {@link CustomerPayloads} treats saved text that is incomplete or damaged. */
class CustomerPayloadsBadInputTest {

    private static final String MINIMAL = "{\"record\":\"customer\",\"firstName\":\"Jane\"";

    @Test
    void readCustomer_ofSomethingThatIsNotAnObject_isRefused() {
        assertThrows(IllegalArgumentException.class, () -> CustomerPayloads.readCustomer("[1, 2]"));
    }

    @Test
    void readCustomer_withoutAFirstName_isRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> CustomerPayloads.readCustomer("{\"record\":\"customer\"}"));
    }

    @Test
    void readCustomer_withOnlyAFirstName_fillsInTheRest() {
        CustomerContent content = CustomerPayloads.readCustomer(MINIMAL + "}");

        assertEquals("Jane", content.customer().get_firstName());
        assertEquals(0L, content.customer().getOrganisation());
        assertTrue(content.addresses().isEmpty());
        assertTrue(content.employers().isEmpty());
    }

    @Test
    void readCustomer_whenAListIsNotAList_treatsItAsEmpty() {
        CustomerContent content = CustomerPayloads.readCustomer(MINIMAL + ",\"addresses\":\"none\"}");

        assertTrue(content.addresses().isEmpty());
    }

    @Test
    void readCustomer_withAnOrganisationThatIsNotANumber_isRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> CustomerPayloads.readCustomer(MINIMAL + ",\"organisation\":\"abc\"}"));
    }

    @Test
    void readCustomer_withAnEmployerWithoutASalary_readsZero() {
        CustomerContent content = CustomerPayloads.readCustomer(
                MINIMAL + ",\"employers\":[{\"type\":\"Full-time\",\"name\":\"Acme\"}]}");

        assertEquals(0.0, content.employers().get(0).get_salary());
    }

    @Test
    void readCustomer_withASalaryThatIsNotANumber_isRefused() {
        assertThrows(IllegalArgumentException.class, () -> CustomerPayloads.readCustomer(
                MINIMAL + ",\"employers\":[{\"type\":\"Full-time\",\"name\":\"Acme\",\"salary\":\"lots\"}]}"));
    }
}
