package lavs.infrastructure.mainframe;

import lavs.business.model.Note;
import lavs.business.repository.CustomerSearchResult;
import mainframe.client.MessageRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The customer note adapter against a scripted connection, so no mainframe is needed. */
class CustomerNoteAdapterTest {

    private final ScriptedConnection connection = new ScriptedConnection();
    private final CustomerNoteAdapter adapter = new CustomerNoteAdapter(connection);

    private static Note note(long id) {
        Note note = new Note();
        note.set_id(id);
        note.set_customerNumber("CUS-1");
        note.set_type("Visit");
        note.set_lines("Met the customer.");
        return note;
    }

    private void retrieveReply() {
        connection.replySuccess("customerNumber", "CUS-1", "customerName", "Jane Doe", "id", "7", "type", "Visit",
                "dateAdded", "2026-10-22", "lines", "Met the customer.", "numberOfLines", "1", "author", "Group_2");
    }

    @Test
    void find_readsEveryFieldOfTheNote() throws Exception {
        retrieveReply();

        Note found = adapter.findNoteByCustomerNumberAndId(7, "CUS-1").orElseThrow();

        assertEquals("Met the customer.", found.get_lines());
        assertEquals("Visit", found.get_type());
        assertEquals("Group_2", found.get_author());
        assertEquals("2026-10-22", found.get_dateAdded());
        assertEquals(7, found.get_id());
        MessageRequest sent = connection.requests().get(0);
        assertEquals("customerNote.retrieve", sent.getRequestType());
        assertEquals("7", sent.getValue("id"));
    }

    @Test
    void find_whenThereIsNone_isEmpty() throws Exception {
        connection.replyRefused("No such note.");

        assertTrue(adapter.findNoteByCustomerNumberAndId(7, "CUS-1").isEmpty());
    }

    @Test
    void add_sendsTheNoteThenReadsItBack() throws Exception {
        connection.replySuccess("customer", "CUS-1", "id", "7");
        retrieveReply();

        Note saved = adapter.addCustomerNote(note(0)).orElseThrow();

        assertEquals(7, saved.get_id());
        MessageRequest add = connection.requests().get(0);
        assertEquals("customer.addNote", add.getRequestType());
        assertEquals("Met the customer.", add.getValue("lines"));
        assertEquals("Visit", add.getValue("type"));
        assertEquals("CUS-1", add.getValue("customer"));
    }

    @Test
    void add_whenTheMainframeRefuses_givesNothingAndAsksNoMore() throws Exception {
        connection.replyRefused("Too long.");

        assertTrue(adapter.addCustomerNote(note(0)).isEmpty());
        assertEquals(1, connection.requests().size());
    }

    @Test
    void add_whenItCannotBeReadBack_givesNothing() throws Exception {
        connection.replySuccess("customer", "CUS-1", "id", "7");
        connection.replyRefused("No such note.");

        assertTrue(adapter.addCustomerNote(note(0)).isEmpty());
    }

    @Test
    void update_sendsTheChangeThenReadsItBack() throws Exception {
        connection.replySuccess("customer", "CUS-1", "id", "7");
        retrieveReply();

        Note saved = adapter.updateCustomerNote(note(7)).orElseThrow();

        assertEquals("Group_2", saved.get_author());
        assertEquals("customer.updateNote", connection.requests().get(0).getRequestType());
        assertEquals("7", connection.requests().get(0).getValue("id"));
    }

    @Test
    void update_thatWasSavedButCannotBeReadBack_stillReportsTheNote() throws Exception {
        connection.replySuccess("customer", "CUS-1", "id", "7");
        connection.replyRefused("Busy.");
        Note changed = note(7);

        assertSame(changed, adapter.updateCustomerNote(changed).orElseThrow());
    }

    @Test
    void update_whenTheMainframeRefuses_givesNothing() throws Exception {
        connection.replyRefused("Not allowed.");

        assertTrue(adapter.updateCustomerNote(note(7)).isEmpty());
        assertEquals(1, connection.requests().size());
    }

    @Test
    void delete_isTrueOnlyWhenTheMainframeAccepts() throws Exception {
        connection.replySuccess("customer", "CUS-1", "id", "7");
        connection.replyRefused("No such note.");

        assertTrue(adapter.deleteNoteByCustomerNumberAndId(7, "CUS-1"));
        assertFalse(adapter.deleteNoteByCustomerNumberAndId(7, "CUS-1"));
        assertEquals("customer.deleteNote", connection.requests().get(0).getRequestType());
    }

    @Test
    void list_readsEachNoteWithoutItsText() throws Exception {
        connection.replySuccess("customerNumber", "CUS-1", "customerName", "Jane Doe", "page", "1", "total", "2",
                "items", "2",
                "1:id", "7", "1:type", "Visit", "1:dateAdded", "2026-10-21", "1:numberOfLines", "1", "1:author", "A",
                "2:id", "8", "2:type", "Call", "2:dateAdded", "2026-10-22", "2:numberOfLines", "3", "2:author", "B");

        CustomerSearchResult<Note> result = adapter.ListCustomerNotes(1, "CUS-1", 100);

        assertEquals(2, result.getTotal());
        List<Note> notes = result.getList();
        assertEquals("Call", notes.get(1).get_type());
        assertEquals("B", notes.get(1).get_author());
        assertEquals("3", notes.get(1).get_numberOfLines());
        assertEquals("CUS-1", notes.get(1).get_customerNumber());
        assertEquals(null, notes.get(1).get_lines());
    }

    @Test
    void list_whenTheMainframeRefuses_isEmpty() throws Exception {
        connection.replyRefused("No such customer.");

        CustomerSearchResult<Note> result = adapter.ListCustomerNotes(1, "CUS-404", 100);

        assertTrue(result.getList().isEmpty());
        assertEquals(0, result.getTotal());
    }
}
