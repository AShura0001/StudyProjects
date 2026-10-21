package lavs.infrastructure.mainframe;

import lavs.business.model.Employer;
import lavs.business.repository.CustomerSearchResult;
import mainframe.client.messages.customer.AddEmployer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class CustomerEmployerAdapterIntegrationTest extends MainframeIntegrationTestSupport {

    private static final String CUSTOMER_NUMBER =
            System.getProperty("mainframe.customerNumber", "01-0001-0000020");

    private CustomerEmployerAdapter adapter;

    @BeforeEach
    void createAdapter() {
        adapter = new CustomerEmployerAdapter(connection);
    }

    @Test
    void listCustomerEmployersReturnsAddedEmployers() throws Exception {
        List<Long> createdIds = new ArrayList<>();
        try {
            Employer first = addEmployer("List Test Widgets", AddEmployer.TYPE_PRIMARY);
            Employer second = addEmployer("List Test Gadgets", AddEmployer.TYPE_SECONDARY);
            createdIds.add(first.get_id());
            createdIds.add(second.get_id());

            List<Employer> listed = fetchAllPages(
                    (page, size) -> adapter.ListCustomerEmployers(page, CUSTOMER_NUMBER, size));

            assertListed(listed, first);
            assertListed(listed, second);
            for (Employer employer : listed) {
                assertEquals(CUSTOMER_NUMBER, employer.get_customerNumber());
                assertNotNull(employer.get_customerName());
            }
        } finally {
            for (long id : createdIds) {
                adapter.deleteEmployerByCustomerNumberAndId(id, CUSTOMER_NUMBER);
            }
        }
    }

    @Test
    void listCustomerEmployersWithUnknownCustomerReturnsEmptyResult() throws Exception {
        CustomerSearchResult<Employer> result =
                adapter.ListCustomerEmployers(1, "CUSTOMER-DOES-NOT-EXIST", 10);

        assertNotNull(result);
        assertTrue(result.getList().isEmpty());
    }

    @Test
    void listCustomerEmployersRespectsPageSize() throws Exception {
        CustomerSearchResult<Employer> firstPage = adapter.ListCustomerEmployers(1, CUSTOMER_NUMBER, 1);
        CustomerSearchResult<Employer> secondPage = adapter.ListCustomerEmployers(2, CUSTOMER_NUMBER, 1);

        assertEquals(1, firstPage.getPage());
        assertEquals(2, secondPage.getPage());
        assertTrue(firstPage.getList().size() <= 1);
        assertTrue(secondPage.getList().size() <= 1);

        if (!firstPage.getList().isEmpty() && !secondPage.getList().isEmpty()) {
            assertNotEquals(
                    firstPage.getList().get(0).get_id(),
                    secondPage.getList().get(0).get_id(),
                    "Different pages should not return the same employer");
        }
    }

    private Employer addEmployer(String name, String type) throws Exception {
        Employer employer = new Employer();
        employer.set_customerNumber(CUSTOMER_NUMBER);
        employer.set_name(name);
        employer.set_position("Tester");
        employer.set_salary(50000.00);
        employer.set_type(type);

        Optional<Employer> added = adapter.addCustomerEmployer(employer);
        assertTrue(added.isPresent(), "Could not add employer " + name);
        return added.get();
    }

    private static void assertListed(List<Employer> listed, Employer expected) {
        Employer actual = listed.stream()
                .filter(e -> e.get_id() == expected.get_id())
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Employer " + expected.get_id() + " was not in the list"));

        assertEquals(expected.get_name(), actual.get_name());
        assertEquals(expected.get_type(), actual.get_type());
    }
}
