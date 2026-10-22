package lavs.infrastructure.mainframe;

import lavs.business.model.LoanNote;
import lavs.business.repository.MainframeUnavailableException;
import lavs.business.repository.PageResult;
import lavs.business.repository.RecordRejectedException;
import mainframe.client.MessageRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The loan note adapter against a scripted connection, so no mainframe is needed. */
class LoanNoteAdapterTest {

    private final ScriptedConnection connection = new ScriptedConnection();
    private final LoanNoteAdapter adapter = new LoanNoteAdapter(connection);

    private void retrieveReply(String lines) {
        connection.replySuccess("loanNumber", "LN-1", "customerName", "Jane Doe", "id", "9", "type", "Visit",
                "dateAdded", "2026-10-22", "lines", lines, "numberOfLines", "1", "author", "Group_2");
    }

    @Test
    void findNote_readsEveryFieldOfTheNote() {
        retrieveReply("Checked the payslips.");

        LoanNote note = adapter.findNote("LN-1", 9).orElseThrow();

        assertEquals(new LoanNote(9, "LN-1", "Visit", "Group_2", "2026-10-22", "Checked the payslips."), note);
        MessageRequest sent = connection.requests().get(0);
        assertEquals("loanNote.retrieve", sent.getRequestType());
        assertEquals("9", sent.getValue("id"));
        assertEquals("LN-1", sent.getValue("number"));
    }

    @Test
    void findNote_whenTheReplyLeavesOutTheLoanNumber_usesTheOneAskedFor() {
        connection.replySuccess("id", "9", "type", "Visit", "lines", "Text");

        assertEquals("LN-1", adapter.findNote("LN-1", 9).orElseThrow().loanNumber());
    }

    @Test
    void findNote_whenThereIsNone_isEmpty() {
        connection.replyRefused("No such note.");

        assertTrue(adapter.findNote("LN-1", 9).isEmpty());
    }

    @Test
    void findNote_whenTheMainframeCannotBeReached_saysSo() {
        connection.replyUnreachable();

        assertThrows(MainframeUnavailableException.class, () -> adapter.findNote("LN-1", 9));
    }

    @Test
    void listNotes_readsEachNoteWithoutItsText() {
        connection.replySuccess("loanNumber", "LN-1", "page", "1", "total", "2", "items", "2",
                "1:id", "9", "1:type", "Visit", "1:dateAdded", "2026-10-21", "1:author", "A",
                "2:id", "10", "2:type", "Call", "2:dateAdded", "2026-10-22", "2:author", "B");

        PageResult<LoanNote> page = adapter.listNotes("LN-1", 1, 100);

        assertEquals(2, page.getTotal());
        LoanNote second = page.getList().get(1);
        assertEquals("Call", second.type());
        assertEquals("B", second.author());
        assertNull(second.lines());
        assertEquals("loanNote.list", connection.requests().get(0).getRequestType());
    }

    @Test
    void listNotes_whenTheMainframeRefuses_isAnEmptyPage() {
        connection.replyRefused("No such loan.");

        assertTrue(adapter.listNotes("LN-404", 1, 100).getList().isEmpty());
    }

    @Test
    void addNote_sendsTheNoteThenReadsItBack() {
        connection.replySuccess("loanNumber", "LN-1", "id", "9", "type", "Visit", "lines", "Text");
        retrieveReply("Text");

        LoanNote added = adapter.addNote("LN-1", "Visit", "Text");

        assertEquals(9, added.id());
        assertEquals("Group_2", added.author());
        MessageRequest add = connection.requests().get(0);
        assertEquals("loan.addNote", add.getRequestType());
        assertEquals("Text", add.getValue("lines"));
        assertEquals("Visit", add.getValue("type"));
        assertEquals("LN-1", add.getValue("loanNumber"));
    }

    @Test
    void addNote_whenItCannotBeReadBack_reportsWhatWasSent() {
        connection.replySuccess("loanNumber", "LN-1", "id", "9", "author", "Group_2");
        connection.replyRefused("Busy.");

        assertEquals(new LoanNote(9, "LN-1", "Visit", "Group_2", null, "Text"), adapter.addNote("LN-1", "Visit", "Text"));
    }

    @Test
    void addNote_whenTheMainframeRefuses_passesItsReasonOn() {
        connection.replyRefused("No such loan.");

        RecordRejectedException refused = assertThrows(RecordRejectedException.class,
                () -> adapter.addNote("LN-404", "Visit", "Text"));
        assertEquals("No such loan.", refused.getMessage());
    }

    @Test
    void updateNote_sendsTheChangeThenReadsItBack() {
        connection.replySuccess("loanNumber", "LN-1", "id", "9");
        retrieveReply("New text");

        assertEquals("New text", adapter.updateNote("LN-1", 9, "Visit", "New text").lines());
        MessageRequest update = connection.requests().get(0);
        assertEquals("loan.updateNote", update.getRequestType());
        assertEquals("9", update.getValue("id"));
    }

    @Test
    void updateNote_whenItCannotBeReadBack_reportsWhatWasSent() {
        connection.replySuccess("loanNumber", "LN-1", "id", "9");
        connection.replyRefused("Busy.");

        assertEquals("New text", adapter.updateNote("LN-1", 9, "Visit", "New text").lines());
    }

    @Test
    void deleteNote_sendsTheRemoval_andReportsARefusal() {
        connection.replySuccess("loanNumber", "LN-1", "id", "9");
        connection.replyRefused("No such note.");

        adapter.deleteNote("LN-1", 9);
        assertEquals("loan.deleteNote", connection.requests().get(0).getRequestType());

        assertThrows(RecordRejectedException.class, () -> adapter.deleteNote("LN-1", 9));
    }
}
