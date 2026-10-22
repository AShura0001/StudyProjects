package lavs.infrastructure.mainframe;

import lavs.business.model.Relationship;
import lavs.business.repository.CustomerSearchResult;
import mainframe.client.MessageRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The relationship adapter against a scripted connection, so no mainframe is needed. */
class CustomerRelationshipAdapterTest {

    private final ScriptedConnection connection = new ScriptedConnection();
    private final CustomerRelationshipAdapter adapter = new CustomerRelationshipAdapter(connection);

    private static Relationship toSave() {
        return new Relationship(null, "CUS-1", 0, "", null, "CUS-2", "Spouse");
    }

    /** What the mainframe answers when a single relationship is retrieved. */
    private void retrieveReply(String type) {
        connection.replySuccess("customerNumber", "CUS-1", "customerName", "Jane Doe", "id", "5",
                "type", type, "secondaryCustomerNumber", "CUS-2", "secondaryCustomerName", "Sam Lee",
                "otherType", "");
    }

    @Test
    void add_sendsTheRelationshipThenReadsItBack() throws Exception {
        connection.replySuccess("customer", "CUS-1", "type", "Spouse", "otherType", "",
                "secondaryCustomerNumber", "CUS-2", "id", "5");
        retrieveReply("Spouse");

        Optional<Relationship> saved = adapter.addCustomerRelationship(toSave());

        assertTrue(saved.isPresent());
        assertEquals(5L, saved.get().get_id());
        assertEquals("CUS-2", saved.get().get_secondaryCustomerNumber());
        assertEquals("Sam Lee", saved.get().get_secondaryCustomerName());
        assertEquals("Spouse", saved.get().get_type());
        MessageRequest add = connection.requests().get(0);
        assertEquals("customer.addRelationship", add.getRequestType());
        assertEquals("CUS-1", add.getValue("customer"));
        assertEquals("CUS-2", add.getValue("secondaryCustomerNumber"));
        assertEquals("customerRelationship.retrieve", connection.requests().get(1).getRequestType());
        assertEquals("5", connection.requests().get(1).getValue("id"));
    }

    @Test
    void add_whenTheMainframeRefuses_givesNothingAndAsksNoMore() throws Exception {
        connection.replyRefused("That customer does not exist.");

        assertTrue(adapter.addCustomerRelationship(toSave()).isEmpty());
        assertEquals(1, connection.requests().size());
    }

    @Test
    void add_whenItCannotBeReadBack_givesNothing() throws Exception {
        connection.replySuccess("customer", "CUS-1", "type", "Spouse", "otherType", "",
                "secondaryCustomerNumber", "CUS-2", "id", "5");
        connection.replyRefused("No such relationship.");

        assertTrue(adapter.addCustomerRelationship(toSave()).isEmpty());
    }

    @Test
    void update_sendsTheChangeThenReadsItBack() throws Exception {
        connection.replySuccess("customer", "CUS-1", "id", "5", "type", "Parent", "otherType", "",
                "secondaryCustomerNumber", "CUS-2");
        retrieveReply("Parent");
        Relationship changed = new Relationship("Jane Doe", "CUS-1", 5, "", null, "CUS-2", "Parent");

        Optional<Relationship> saved = adapter.updateCustomerRelationship(changed);

        assertEquals("Parent", saved.orElseThrow().get_type());
        MessageRequest update = connection.requests().get(0);
        assertEquals("customer.updateRelationship", update.getRequestType());
        assertEquals("5", update.getValue("id"));
        assertEquals("Parent", update.getValue("type"));
    }

    @Test
    void update_whenTheMainframeRefuses_givesNothingAndAsksNoMore() throws Exception {
        connection.replyRefused("Not allowed.");

        assertTrue(adapter.updateCustomerRelationship(
                new Relationship("Jane Doe", "CUS-1", 5, "", null, "CUS-2", "Parent")).isEmpty());
        assertEquals(1, connection.requests().size());
    }

    @Test
    void update_whenItCannotBeReadBack_givesNothing() throws Exception {
        connection.replySuccess("customer", "CUS-1", "id", "5", "type", "Parent", "otherType", "",
                "secondaryCustomerNumber", "CUS-2");
        connection.replyRefused("No such relationship.");

        assertTrue(adapter.updateCustomerRelationship(
                new Relationship("Jane Doe", "CUS-1", 5, "", null, "CUS-2", "Parent")).isEmpty());
    }

    @Test
    void delete_isTrueWhenTheMainframeAcceptsIt() throws Exception {
        connection.replySuccess("customer", "CUS-1", "id", "5");

        assertTrue(adapter.deleteRelationshipByCustomerNumberAndId(5, "CUS-1"));
        assertEquals("customer.deleteRelationship", connection.requests().get(0).getRequestType());
        assertEquals("5", connection.requests().get(0).getValue("id"));
    }

    @Test
    void delete_isFalseWhenTheMainframeRefuses() throws Exception {
        connection.replyRefused("No such relationship.");

        assertFalse(adapter.deleteRelationshipByCustomerNumberAndId(5, "CUS-1"));
    }

    @Test
    void find_returnsTheRelationshipTheMainframeHolds() throws Exception {
        retrieveReply("Spouse");

        Relationship found = adapter.findRelationshipByCustomerNumberAndId(5, "CUS-1").orElseThrow();

        assertEquals("Jane Doe", found.get_customerName());
        assertEquals("CUS-1", found.get_customerNumber());
        assertEquals("customerRelationship.retrieve", connection.requests().get(0).getRequestType());
    }

    @Test
    void find_whenThereIsNone_isEmpty() throws Exception {
        connection.replyRefused("No such relationship.");

        assertTrue(adapter.findRelationshipByCustomerNumberAndId(5, "CUS-1").isEmpty());
    }

    @Test
    void list_returnsEachRelationshipWithItsCustomerAndTheTotal() throws Exception {
        connection.replySuccess("customerNumber", "CUS-1", "customerName", "Jane Doe", "page", "1", "total", "2",
                "items", "2",
                "1:id", "5", "1:type", "Spouse", "1:secondaryCustomerNumber", "CUS-2",
                "2:id", "6", "2:type", "Parent", "2:secondaryCustomerNumber", "CUS-3");

        CustomerSearchResult<Relationship> result = adapter.ListCustomerRelationships(1, "CUS-1", 100);

        assertEquals(2, result.getTotal());
        List<Relationship> items = result.getList();
        assertEquals(2, items.size());
        assertEquals(5L, items.get(0).get_id());
        assertEquals("CUS-3", items.get(1).get_secondaryCustomerNumber());
        assertEquals("Parent", items.get(1).get_type());
        assertEquals("CUS-1", items.get(1).get_customerNumber());
        assertEquals("Jane Doe", items.get(1).get_customerName());
        assertEquals("100", connection.requests().get(0).getValue("size"));
    }

    @Test
    void list_whenTheMainframeRefuses_givesAnEmptyResult() throws Exception {
        connection.replyRefused("No such customer.");

        CustomerSearchResult<Relationship> result = adapter.ListCustomerRelationships(1, "CUS-404", 100);

        assertTrue(result.getList().isEmpty());
        assertEquals(0, result.getTotal());
    }
}
