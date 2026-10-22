package lavs.infrastructure.mainframe;

import lavs.business.model.Email;
import lavs.business.repository.CustomerSearchResult;
import mainframe.client.MessageRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The email adapter against a scripted connection: every operation, its replies and its refusals. */
class CustomerEmailAdapterScriptedTest {

    private final ScriptedConnection connection = new ScriptedConnection();
    private final CustomerEmailAdapter adapter = new CustomerEmailAdapter(connection);

    private static Email email(long id) {
        return new Email("jane@example.com", "Jane Doe", "CUS-1", id, true, null, "Personal");
    }

    private void retrieveReply() {
        connection.replySuccess("customerNumber", "CUS-1", "customerName", "Jane Doe", "id", "12", "type", "Personal",
                "otherType", "", "address", "jane@example.com", "isPrimary", "y");
    }

    @Test
    void find_readsEveryFieldOfTheEmail() throws Exception {
        retrieveReply();

        Email found = adapter.findEmailByCustomerNumberAndId(12L, "CUS-1").orElseThrow();

        assertEquals("jane@example.com", found.getAddress());
        assertEquals("Personal", found.getType());
        assertEquals(12, found.getId());
        assertTrue(found.isPrimary());
        assertEquals("customerEmail.retrieve", connection.requests().get(0).getRequestType());
    }

    @Test
    void find_whenThereIsNone_isEmpty() throws Exception {
        connection.replyRefused("No such email.");

        assertTrue(adapter.findEmailByCustomerNumberAndId(12L, "CUS-1").isEmpty());
    }

    @Test
    void add_sendsTheEmailThenReadsItBack() throws Exception {
        connection.replySuccess("customer", "CUS-1", "id", "12", "address", "jane@example.com", "isPrimary", "y");
        retrieveReply();

        Email saved = adapter.addCustomerEmail(email(0)).orElseThrow();

        assertEquals(12, saved.getId());
        MessageRequest add = connection.requests().get(0);
        assertEquals("customer.addEmail", add.getRequestType());
        assertEquals("jane@example.com", add.getValue("address"));
        assertEquals("CUS-1", add.getValue("customer"));
        assertEquals("y", add.getValue("isPrimary"));
    }

    @Test
    void add_whenTheMainframeRefuses_givesNothingAndAsksNoMore() throws Exception {
        connection.replyRefused("Bad address.");

        assertTrue(adapter.addCustomerEmail(email(0)).isEmpty());
        assertEquals(1, connection.requests().size());
    }

    @Test
    void add_whenItCannotBeReadBack_givesNothing() throws Exception {
        connection.replySuccess("customer", "CUS-1", "id", "12");
        connection.replyRefused("No such email.");

        assertTrue(adapter.addCustomerEmail(email(0)).isEmpty());
    }

    @Test
    void update_sendsTheChangeAndGivesTheEmailBack() throws Exception {
        connection.replySuccess("customer", "CUS-1", "id", "12");
        Email changed = email(12);

        assertSame(changed, adapter.updateCustomerEmail(changed).orElseThrow());

        MessageRequest update = connection.requests().get(0);
        assertEquals("customer.updateEmail", update.getRequestType());
        assertEquals("12", update.getValue("id"));
    }

    @Test
    void update_whenTheMainframeRefuses_givesNothing() throws Exception {
        connection.replyRefused("Not allowed.");

        assertTrue(adapter.updateCustomerEmail(email(12)).isEmpty());
    }

    @Test
    void delete_isTrueOnlyWhenTheMainframeAccepts() throws Exception {
        connection.replySuccess("customer", "CUS-1", "id", "12");
        connection.replyRefused("No such email.");

        assertTrue(adapter.deleteCustomerEmail("CUS-1", 12));
        assertFalse(adapter.deleteCustomerEmail("CUS-1", 12));
        assertEquals("customer.deleteEmail", connection.requests().get(0).getRequestType());
    }

    @Test
    void list_readsEachEmailAndTheTotal() throws Exception {
        connection.replySuccess("customerNumber", "CUS-1", "customerName", "Jane Doe", "page", "1", "total", "2",
                "items", "2",
                "1:id", "12", "1:type", "Personal", "1:address", "a@example.com", "1:isPrimary", "y",
                "2:id", "13", "2:type", "Work", "2:address", "b@example.com", "2:isPrimary", "n");

        CustomerSearchResult<Email> result = adapter.ListCustomerEmails(1, "CUS-1", 100);

        assertEquals(2, result.getTotal());
        List<Email> emails = result.getList();
        assertTrue(emails.get(0).isPrimary());
        assertFalse(emails.get(1).isPrimary());
        assertEquals("b@example.com", emails.get(1).getAddress());
        assertEquals("Jane Doe", emails.get(1).getCustomerName());
    }

    @Test
    void list_whenTheMainframeRefuses_isEmpty() throws Exception {
        connection.replyRefused("No such customer.");

        assertTrue(adapter.ListCustomerEmails(1, "CUS-404", 100).getList().isEmpty());
    }
}
