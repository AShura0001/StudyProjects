package lavs.infrastructure.mainframe;

import lavs.business.model.CoBorrower;
import lavs.business.model.CoBorrowerType;
import lavs.business.repository.PageResult;
import lavs.business.repository.RecordRejectedException;
import mainframe.client.MessageRequest;
import mainframe.client.messages.loan.AddCoborrower;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Finding, listing, adding and removing co-borrowers through the co-borrower adapter. */
class LoanCoBorrowerAdapterCallsTest {

    private final ScriptedConnection connection = new ScriptedConnection();
    private final LoanCoBorrowerAdapter adapter = new LoanCoBorrowerAdapter(connection);

    private void retrieveReply(String type) {
        connection.replySuccess("loanNumber", "LN-1", "customerName", "Jane Doe", "id", "7", "type", type,
                "coborrowerNumber", "CUS-2", "coborrowerName", "Sam Lee");
    }

    @Test
    void findCoBorrower_readsEveryDetail() {
        retrieveReply(AddCoborrower.TYPE_GUARANTOR);

        assertEquals(new CoBorrower(7, "LN-1", "CUS-2", "Sam Lee", CoBorrowerType.GUARANTOR),
                adapter.findCoBorrower("LN-1", 7).orElseThrow());
        MessageRequest sent = connection.requests().get(0);
        assertEquals("loanCoborrower.retrieve", sent.getRequestType());
        assertEquals("7", sent.getValue("id"));
    }

    @Test
    void findCoBorrower_whenTheReplyLeavesOutTheLoanNumber_usesTheOneAskedFor() {
        connection.replySuccess("id", "7", "type", AddCoborrower.TYPE_COBORROWER, "coborrowerNumber", "CUS-2");

        assertEquals("LN-1", adapter.findCoBorrower("LN-1", 7).orElseThrow().loanNumber());
    }

    @Test
    void findCoBorrower_whenThereIsNone_isEmpty() {
        connection.replyRefused("No such co-borrower.");

        assertTrue(adapter.findCoBorrower("LN-1", 7).isEmpty());
    }

    @Test
    void findCoBorrower_withATypeTheAppDoesNotKnow_isRefusedClearly() {
        retrieveReply("Cosigner");

        RecordRejectedException refused = assertThrows(RecordRejectedException.class,
                () -> adapter.findCoBorrower("LN-1", 7));
        assertTrue(refused.getMessage().contains("Cosigner"));
    }

    @Test
    void listCoBorrowers_readsEachPersonAndTheTotal() {
        connection.replySuccess("loanNumber", "LN-1", "page", "1", "total", "2", "items", "2",
                "1:id", "7", "1:type", AddCoborrower.TYPE_COBORROWER, "1:coborrowerNumber", "CUS-2",
                "1:coborrowerName", "Sam Lee",
                "2:id", "8", "2:type", AddCoborrower.TYPE_GUARANTOR, "2:coborrowerNumber", "CUS-3",
                "2:coborrowerName", "Ana Ruiz");

        PageResult<CoBorrower> page = adapter.listCoBorrowers("LN-1", 1, 100);

        assertEquals(2, page.getTotal());
        assertEquals(new CoBorrower(8, "LN-1", "CUS-3", "Ana Ruiz", CoBorrowerType.GUARANTOR), page.getList().get(1));
        assertEquals("loanCoborrower.list", connection.requests().get(0).getRequestType());
    }

    @Test
    void listCoBorrowers_whenTheReplyLeavesOutTheLoanNumber_usesTheOneAskedFor() {
        connection.replySuccess("page", "1", "total", "1", "items", "1",
                "1:id", "7", "1:type", AddCoborrower.TYPE_COBORROWER, "1:coborrowerNumber", "CUS-2");

        assertEquals("LN-1", adapter.listCoBorrowers("LN-1", 1, 100).getList().get(0).loanNumber());
    }

    @Test
    void listCoBorrowers_whenTheMainframeRefuses_isAnEmptyPage() {
        connection.replyRefused("No such loan.");

        assertTrue(adapter.listCoBorrowers("LN-404", 1, 100).getList().isEmpty());
    }

    @Test
    void addCoBorrower_sendsThePersonThenReadsThemBack() {
        connection.replySuccess("loanNumber", "LN-1", "type", AddCoborrower.TYPE_GUARANTOR,
                "coborrowerNumber", "CUS-2", "id", "7");
        retrieveReply(AddCoborrower.TYPE_GUARANTOR);

        CoBorrower added = adapter.addCoBorrower("LN-1", "CUS-2", CoBorrowerType.GUARANTOR);

        assertEquals("Sam Lee", added.coBorrowerName());
        MessageRequest add = connection.requests().get(0);
        assertEquals("loan.addCoborrower", add.getRequestType());
        assertEquals(AddCoborrower.TYPE_GUARANTOR, add.getValue("type"));
        assertEquals("CUS-2", add.getValue("coborrowerNumber"));
    }

    @Test
    void addCoBorrower_whenTheyCannotBeReadBack_reportsWhatWasSent() {
        connection.replySuccess("loanNumber", "LN-1", "type", AddCoborrower.TYPE_COBORROWER,
                "coborrowerNumber", "CUS-2", "id", "7");
        connection.replyRefused("Busy.");

        assertEquals(new CoBorrower(7, "LN-1", "CUS-2", null, CoBorrowerType.COBORROWER),
                adapter.addCoBorrower("LN-1", "CUS-2", CoBorrowerType.COBORROWER));
    }

    @Test
    void addCoBorrower_whenTheMainframeRefuses_passesItsReasonOn() {
        connection.replyRefused("No such customer.");

        RecordRejectedException refused = assertThrows(RecordRejectedException.class,
                () -> adapter.addCoBorrower("LN-1", "CUS-404", CoBorrowerType.COBORROWER));
        assertEquals("No such customer.", refused.getMessage());
    }

    @Test
    void deleteCoBorrower_sendsTheRemoval_andReportsARefusal() {
        connection.replySuccess("loanNumber", "LN-1", "id", "7");
        connection.replyRefused("No such co-borrower.");

        adapter.deleteCoBorrower("LN-1", 7);
        assertEquals("loan.deleteCoborrower", connection.requests().get(0).getRequestType());

        assertThrows(RecordRejectedException.class, () -> adapter.deleteCoBorrower("LN-1", 7));
    }
}
