package lavs.infrastructure.mainframe;

import lavs.business.model.Email;
import lavs.business.repository.CustomerSearchResult;
import mainframe.client.messages.customer.AddEmail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class CustomerEmailAdapterIntegrationTest extends MainframeIntegrationTestSupport {

    private static final String CUSTOMER_NUMBER =
            System.getProperty("mainframe.customerNumber", "01-0001-0000020");

    private CustomerEmailAdapter adapter;

    @BeforeEach
    void createAdapter() {
        adapter = new CustomerEmailAdapter(connection);
    }

    @Test
    void listCustomerEmailsReturnsAddedEmails() throws Exception {
        List<Long> createdIds = new ArrayList<>();
        try {
            Email first = addEmail("list-test-1@example.com", AddEmail.TYPE_PERSONAL);
            Email second = addEmail("list-test-2@example.com", AddEmail.TYPE_WORK);
            createdIds.add(first.getId());
            createdIds.add(second.getId());

            List<Email> listed = fetchAllPages(
                    (page, size) -> adapter.ListCustomerEmails(page, CUSTOMER_NUMBER, size));

            assertListed(listed, first);
            assertListed(listed, second);
            for (Email email : listed) {
                assertEquals(CUSTOMER_NUMBER, email.getCustomerNumber());
                assertNotNull(email.getCustomerName());
            }
        } finally {
            for (long id : createdIds) {
                adapter.deleteCustomerEmail(CUSTOMER_NUMBER, id);
            }
        }
    }

    @Test
    void listCustomerEmailsWithUnknownCustomerReturnsEmptyResult() throws Exception {
        CustomerSearchResult<Email> result =
                adapter.ListCustomerEmails(1, "CUSTOMER-DOES-NOT-EXIST", 10);

        assertNotNull(result);
        assertTrue(result.getList().isEmpty());
    }

    @Test
    void listCustomerEmailsRespectsPageSize() throws Exception {
        CustomerSearchResult<Email> firstPage = adapter.ListCustomerEmails(1, CUSTOMER_NUMBER, 1);
        CustomerSearchResult<Email> secondPage = adapter.ListCustomerEmails(2, CUSTOMER_NUMBER, 1);

        assertEquals(1, firstPage.getPage());
        assertEquals(2, secondPage.getPage());
        assertTrue(firstPage.getList().size() <= 1);
        assertTrue(secondPage.getList().size() <= 1);

        if (!firstPage.getList().isEmpty() && !secondPage.getList().isEmpty()) {
            assertNotEquals(
                    firstPage.getList().get(0).getId(),
                    secondPage.getList().get(0).getId(),
                    "Different pages should not return the same email");
        }
    }

    private Email addEmail(String address, String type) throws Exception {
        Email email = new Email();
        email.setAddress(address);
        email.setCustomerNumber(CUSTOMER_NUMBER);
        email.setPrimary(false);
        email.setType(type);

        Optional<Email> added = adapter.addCustomerEmail(email);
        assertTrue(added.isPresent(), "Could not add email " + address);
        return added.get();
    }

    private static void assertListed(List<Email> listed, Email expected) {
        Email actual = listed.stream()
                .filter(e -> e.getId() == expected.getId())
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Email " + expected.getId() + " was not in the list"));

        assertEquals(expected.getAddress(), actual.getAddress());
        assertEquals(expected.getType(), actual.getType());
        assertEquals(expected.isPrimary(), actual.isPrimary());
    }
}
