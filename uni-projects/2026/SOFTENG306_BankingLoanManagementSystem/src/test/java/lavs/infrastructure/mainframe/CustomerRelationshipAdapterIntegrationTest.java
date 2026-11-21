package lavs.infrastructure.mainframe;

import lavs.business.model.Relationship;
import lavs.business.repository.CustomerSearchResult;
import mainframe.client.messages.customer.AddRelationship;
import mainframe.client.messages.customer.Search;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class CustomerRelationshipAdapterIntegrationTest extends MainframeIntegrationTestSupport {

    private static final String CUSTOMER_NUMBER =
            System.getProperty("mainframe.customerNumber", "01-0001-0000020");

    private CustomerRelationshipAdapter adapter;

    @BeforeEach
    void createAdapter() {
        adapter = new CustomerRelationshipAdapter(connection);
    }

    @Test
    void listCustomerRelationshipsReturnsAddedRelationship() throws Exception {
        String secondaryNumber = findOtherCustomerNumber();
        Long createdId = null;
        try {
            Relationship relationship = new Relationship();
            relationship.set_customerNumber(CUSTOMER_NUMBER);
            relationship.set_secondaryCustomerNumber(secondaryNumber);
            relationship.set_type(AddRelationship.TYPE_OTHER);
            relationship.set_otherType("Integration test");

            Optional<Relationship> added = adapter.addCustomerRelationship(relationship);
            assertTrue(added.isPresent(), "Could not add relationship to " + secondaryNumber);
            createdId = added.get().get_id();
            long id = createdId;

            List<Relationship> listed = fetchAllPages(
                    (page, size) -> adapter.ListCustomerRelationships(page, CUSTOMER_NUMBER, size));

            Relationship actual = listed.stream()
                    .filter(r -> r.get_id() == id)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(
                            "Relationship " + id + " was not in the list"));

            assertEquals(secondaryNumber, actual.get_secondaryCustomerNumber());
            assertEquals(AddRelationship.TYPE_OTHER, actual.get_type());
            for (Relationship r : listed) {
                assertEquals(CUSTOMER_NUMBER, r.get_customerNumber());
                assertNotNull(r.get_customerName());
            }
        } finally {
            if (createdId != null) {
                adapter.deleteRelationshipByCustomerNumberAndId(createdId, CUSTOMER_NUMBER);
            }
        }
    }

    @Test
    void listCustomerRelationshipsWithUnknownCustomerReturnsEmptyResult() throws Exception {
        CustomerSearchResult<Relationship> result =
                adapter.ListCustomerRelationships(1, "CUSTOMER-DOES-NOT-EXIST", 10);

        assertNotNull(result);
        assertTrue(result.getList().isEmpty());
    }

    @Test
    void listCustomerRelationshipsRespectsPageSize() throws Exception {
        CustomerSearchResult<Relationship> firstPage = adapter.ListCustomerRelationships(1, CUSTOMER_NUMBER, 1);
        CustomerSearchResult<Relationship> secondPage = adapter.ListCustomerRelationships(2, CUSTOMER_NUMBER, 1);

        assertEquals(1, firstPage.getPage());
        assertEquals(2, secondPage.getPage());
        assertTrue(firstPage.getList().size() <= 1);
        assertTrue(secondPage.getList().size() <= 1);

        if (!firstPage.getList().isEmpty() && !secondPage.getList().isEmpty()) {
            assertNotEquals(
                    firstPage.getList().get(0).get_id(),
                    secondPage.getList().get(0).get_id(),
                    "Different pages should not return the same relationship");
        }
    }

    /** A relationship needs a second, existing customer to point at. */
    private String findOtherCustomerNumber() throws Exception {
        String configured = System.getProperty("mainframe.secondaryCustomerNumber");
        if (configured != null && !configured.isBlank()) {
            return configured;
        }

        Search search = new Search(1, "Smith", 25);
        assertTrue(search.send(connection).getWasSuccessful(), "Customer search failed");

        return Arrays.stream(search.getItems())
                .map(Search.Item::getNumber)
                .filter(number -> !CUSTOMER_NUMBER.equals(number))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No second customer found; set -Dmainframe.secondaryCustomerNumber"));
    }
}
