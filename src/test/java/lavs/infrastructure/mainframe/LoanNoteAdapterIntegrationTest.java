package lavs.infrastructure.mainframe;

import lavs.business.model.LoanNote;
import lavs.business.repository.RecordRejectedException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class LoanNoteAdapterIntegrationTest extends MainframeIntegrationTestSupport {

    private static final String CUSTOMER_NUMBER =
            System.getProperty("mainframe.customerNumber", "01-0001-0000020");

    private LoanNoteAdapter adapter;
    private String loanNumber;

    @BeforeAll
    void setUpLoan() throws Exception {
        adapter = new LoanNoteAdapter(connection);
        loanNumber = findOrCreateTestLoan(CUSTOMER_NUMBER);
    }

    @Test
    void addFindUpdateListAndDeleteNote() throws Exception {
        LoanNote added = adapter.addNote(loanNumber, "Integration test", "Line 1\nLine 2");
        try {
            assertEquals(loanNumber, added.loanNumber());
            assertEquals("Integration test", added.type());

            LoanNote found = adapter.findNote(loanNumber, added.id()).orElseThrow();
            assertEquals("Line 1\nLine 2", found.lines());
            assertNotNull(found.author());

            LoanNote updated = adapter.updateNote(loanNumber, added.id(), "Integration test updated", "Changed");
            assertEquals("Integration test updated", updated.type());
            assertEquals("Changed", updated.lines());

            List<LoanNote> listed = fetchAllPages(
                    (page, size) -> adapter.listNotes(loanNumber, page, size));
            LoanNote inList = listed.stream().filter(n -> n.id() == added.id()).findFirst()
                    .orElseThrow(() -> new AssertionError("Note " + added.id() + " was not in the list"));
            assertEquals("Integration test updated", inList.type());
            assertEquals(found.author(), inList.author());
        } finally {
            adapter.deleteNote(loanNumber, added.id());
        }

        assertTrue(adapter.findNote(loanNumber, added.id()).isEmpty(), "Note should be deleted");
    }

    @Test
    void findUnknownNoteIsEmpty() {
        assertTrue(adapter.findNote(loanNumber, Long.MAX_VALUE).isEmpty());
    }

    @Test
    void deleteUnknownNoteIsRejected() {
        assertThrows(RecordRejectedException.class, () -> adapter.deleteNote(loanNumber, Long.MAX_VALUE));
    }

    @Test
    void listNotesOfUnknownLoanIsEmpty() {
        assertTrue(adapter.listNotes("99-9999-9999999-999", 1, 10).getList().isEmpty());
    }
}
