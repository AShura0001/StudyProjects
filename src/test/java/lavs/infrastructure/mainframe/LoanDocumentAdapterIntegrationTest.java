package lavs.infrastructure.mainframe;

import lavs.business.model.LoanDocument;
import lavs.business.repository.RecordRejectedException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class LoanDocumentAdapterIntegrationTest extends MainframeIntegrationTestSupport {

    private static final String CUSTOMER_NUMBER =
            System.getProperty("mainframe.customerNumber", "01-0001-0000020");

    private LoanDocumentAdapter adapter;
    private String loanNumber;

    @BeforeAll
    void setUpLoan() throws Exception {
        adapter = new LoanDocumentAdapter(connection);
        loanNumber = findOrCreateTestLoan(CUSTOMER_NUMBER);
    }

    @Test
    void addFindListAndDeleteDocument() throws Exception {
        LoanDocument added = adapter.addDocument(
                loanNumber, "integration-test.pdf", "application/pdf", 2048, "Integration test");
        try {
            assertEquals(loanNumber, added.loanNumber());
            assertEquals("integration-test.pdf", added.fileName());

            LoanDocument found = adapter.findDocument(loanNumber, added.id()).orElseThrow();
            assertEquals("integration-test.pdf", found.fileName());
            assertEquals("application/pdf", found.fileType());
            assertEquals(2048L, found.size());
            assertEquals("Integration test", found.type());

            List<LoanDocument> listed = fetchAllPages(
                    (page, size) -> adapter.listDocuments(loanNumber, page, size));
            LoanDocument inList = listed.stream().filter(d -> d.id() == added.id()).findFirst()
                    .orElseThrow(() -> new AssertionError("Document " + added.id() + " was not in the list"));
            assertEquals("integration-test.pdf", inList.fileName());
            assertEquals("Integration test", inList.type());
        } finally {
            adapter.deleteDocument(loanNumber, added.id());
        }

        assertTrue(adapter.findDocument(loanNumber, added.id()).isEmpty(), "Document should be deleted");
    }

    @Test
    void findUnknownDocumentIsEmpty() {
        assertTrue(adapter.findDocument(loanNumber, Long.MAX_VALUE).isEmpty());
    }

    @Test
    void deleteUnknownDocumentIsRejected() {
        assertThrows(RecordRejectedException.class, () -> adapter.deleteDocument(loanNumber, Long.MAX_VALUE));
    }

    @Test
    void listDocumentsOfUnknownLoanIsEmpty() {
        assertTrue(adapter.listDocuments("99-9999-9999999-999", 1, 10).getList().isEmpty());
    }
}
