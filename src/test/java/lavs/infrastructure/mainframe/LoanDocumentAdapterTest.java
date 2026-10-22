package lavs.infrastructure.mainframe;

import lavs.business.model.LoanDocument;
import lavs.business.repository.MainframeUnavailableException;
import lavs.business.repository.PageResult;
import lavs.business.repository.RecordRejectedException;
import mainframe.client.MessageRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The loan document adapter against a scripted connection, so no mainframe is needed. */
class LoanDocumentAdapterTest {

    private final ScriptedConnection connection = new ScriptedConnection();
    private final LoanDocumentAdapter adapter = new LoanDocumentAdapter(connection);

    private void retrieveReply() {
        connection.replySuccess("loanNumber", "LN-1", "customerName", "Jane Doe", "id", "7", "type", "Valuation",
                "fileName", "valuation.pdf", "fileType", "application/pdf", "size", "4096");
    }

    @Test
    void findDocument_readsEveryDetail() {
        retrieveReply();

        assertEquals(new LoanDocument(7, "LN-1", "valuation.pdf", "application/pdf", 4096L, "Valuation"),
                adapter.findDocument("LN-1", 7).orElseThrow());
        MessageRequest sent = connection.requests().get(0);
        assertEquals("loanDocument.retrieve", sent.getRequestType());
        assertEquals("LN-1", sent.getValue("number"));
    }

    @Test
    void findDocument_whenTheReplyLeavesOutTheLoanNumber_usesTheOneAskedFor() {
        connection.replySuccess("id", "7", "type", "Valuation", "fileName", "valuation.pdf", "size", "4096");

        assertEquals("LN-1", adapter.findDocument("LN-1", 7).orElseThrow().loanNumber());
    }

    @Test
    void findDocument_whenThereIsNone_isEmpty() {
        connection.replyRefused("No such document.");

        assertTrue(adapter.findDocument("LN-1", 7).isEmpty());
    }

    @Test
    void findDocument_whenTheMainframeCannotBeReached_saysSo() {
        connection.replyUnreachable();

        assertThrows(MainframeUnavailableException.class, () -> adapter.findDocument("LN-1", 7));
    }

    @Test
    void listDocuments_readsEachDocumentWithoutItsFileTypeOrSize() {
        connection.replySuccess("loanNumber", "LN-1", "page", "1", "total", "2", "items", "2",
                "1:id", "7", "1:type", "Valuation", "1:fileName", "valuation.pdf",
                "2:id", "8", "2:type", "Other", "2:fileName", "letter.pdf");

        PageResult<LoanDocument> page = adapter.listDocuments("LN-1", 1, 100);

        assertEquals(2, page.getTotal());
        LoanDocument second = page.getList().get(1);
        assertEquals("letter.pdf", second.fileName());
        assertNull(second.fileType());
        assertNull(second.size());
        assertEquals("loanDocument.list", connection.requests().get(0).getRequestType());
    }

    @Test
    void listDocuments_whenTheMainframeRefuses_isAnEmptyPage() {
        connection.replyRefused("No such loan.");

        assertTrue(adapter.listDocuments("LN-404", 1, 100).getList().isEmpty());
    }

    @Test
    void addDocument_sendsTheDetailsThenReadsThemBack() {
        connection.replySuccess("loanNumber", "LN-1", "id", "7", "size", "4096");
        retrieveReply();

        LoanDocument added = adapter.addDocument("LN-1", "valuation.pdf", "application/pdf", 4096, "Valuation");

        assertEquals(7, added.id());
        MessageRequest add = connection.requests().get(0);
        assertEquals("loan.addDocument", add.getRequestType());
        assertEquals("valuation.pdf", add.getValue("fileName"));
        assertEquals("LN-1", add.getValue("loanNumber"));
    }

    @Test
    void addDocument_whenItCannotBeReadBack_reportsWhatWasSent() {
        connection.replySuccess("loanNumber", "LN-1", "id", "7", "size", "4096");
        connection.replyRefused("Busy.");

        assertEquals(new LoanDocument(7, "LN-1", "valuation.pdf", "application/pdf", 4096L, "Valuation"),
                adapter.addDocument("LN-1", "valuation.pdf", "application/pdf", 4096, "Valuation"));
    }

    @Test
    void addDocument_whenTheMainframeRefuses_passesItsReasonOn() {
        connection.replyRefused("No such loan.");

        RecordRejectedException refused = assertThrows(RecordRejectedException.class,
                () -> adapter.addDocument("LN-404", "a.pdf", "application/pdf", 1, "Other"));
        assertEquals("No such loan.", refused.getMessage());
    }

    @Test
    void deleteDocument_sendsTheRemoval_andReportsARefusal() {
        connection.replySuccess("loanNumber", "LN-1", "id", "7");
        connection.replyRefused("No such document.");

        adapter.deleteDocument("LN-1", 7);
        assertEquals("loan.deleteDocument", connection.requests().get(0).getRequestType());

        assertThrows(RecordRejectedException.class, () -> adapter.deleteDocument("LN-1", 7));
    }
}
