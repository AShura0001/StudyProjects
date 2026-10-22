package lavs.infrastructure.mainframe;

import lavs.business.model.Document;
import lavs.business.repository.CustomerSearchResult;
import mainframe.client.MessageRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The customer document adapter against a scripted connection, so no mainframe is needed. */
class CustomerDocumentAdapterTest {

    private final ScriptedConnection connection = new ScriptedConnection();
    private final CustomerDocumentAdapter adapter = new CustomerDocumentAdapter(connection);

    private static Document payslip() {
        Document document = new Document();
        document.setCustomerNumber("C-1");
        document.setFileName("payslip.pdf");
        document.setFileType("application/pdf");
        document.setSize(2048);
        document.setType("Proof of Income");
        return document;
    }

    private void retrieveReply() {
        connection.replySuccess("customerNumber", "C-1", "customerName", "Jane Doe", "id", "4", "type", "Proof of Income",
                "fileName", "payslip.pdf", "fileType", "application/pdf", "size", "2048");
    }

    @Test
    void find_readsEveryDetailOfTheDocument() throws Exception {
        retrieveReply();

        Document found = adapter.findDocumentByCustomerNumberAndId(4L, "C-1").orElseThrow();

        assertEquals("payslip.pdf", found.getFileName());
        assertEquals("application/pdf", found.getFileType());
        assertEquals(2048, found.getSize());
        assertEquals("Proof of Income", found.getType());
        MessageRequest sent = connection.requests().get(0);
        assertEquals("customerDocument.retrieve", sent.getRequestType());
        assertEquals("4", sent.getValue("id"));
    }

    @Test
    void find_whenThereIsNone_isEmpty() throws Exception {
        connection.replyRefused("No such document.");

        assertTrue(adapter.findDocumentByCustomerNumberAndId(4L, "C-1").isEmpty());
    }

    @Test
    void add_sendsTheDetailsThenReadsThemBack() throws Exception {
        connection.replySuccess("customer", "C-1", "id", "4", "size", "2048");
        retrieveReply();

        Document saved = adapter.addCustomerDocument(payslip()).orElseThrow();

        assertEquals(4, saved.getId());
        MessageRequest add = connection.requests().get(0);
        assertEquals("customer.addDocument", add.getRequestType());
        assertEquals("payslip.pdf", add.getValue("fileName"));
        assertEquals("2048", add.getValue("size"));
        assertEquals("C-1", add.getValue("customer"));
    }

    @Test
    void add_whenTheMainframeRefuses_givesNothingAndAsksNoMore() throws Exception {
        connection.replyRefused("No such customer.");

        assertTrue(adapter.addCustomerDocument(payslip()).isEmpty());
        assertEquals(1, connection.requests().size());
    }

    @Test
    void add_whenItCannotBeReadBack_givesNothing() throws Exception {
        connection.replySuccess("customer", "C-1", "id", "4", "size", "2048");
        connection.replyRefused("No such document.");

        assertTrue(adapter.addCustomerDocument(payslip()).isEmpty());
    }

    @Test
    void delete_isTrueOnlyWhenTheMainframeAccepts() throws Exception {
        connection.replySuccess("customer", "C-1", "id", "4");
        connection.replyRefused("No such document.");

        assertTrue(adapter.deleteCustomerDocument(4, "C-1"));
        assertFalse(adapter.deleteCustomerDocument(4, "C-1"));
        assertEquals("customer.deleteDocument", connection.requests().get(0).getRequestType());
    }

    @Test
    void list_readsEachDocumentAndTheTotal() throws Exception {
        connection.replySuccess("customerNumber", "C-1", "customerName", "Jane Doe", "page", "1", "total", "2",
                "items", "2",
                "1:id", "4", "1:type", "Proof of Income", "1:fileName", "payslip.pdf",
                "2:id", "5", "2:type", "Proof of Address", "2:fileName", "power-bill.pdf");

        CustomerSearchResult<Document> result = adapter.ListCustomerDocuments(1, "C-1", 100);

        assertEquals(2, result.getTotal());
        assertEquals("power-bill.pdf", result.getList().get(1).getFileName());
        assertEquals("Proof of Address", result.getList().get(1).getType());
        assertEquals("C-1", result.getList().get(1).getCustomerNumber());
    }

    @Test
    void list_whenTheMainframeRefuses_isEmpty() throws Exception {
        connection.replyRefused("No such customer.");

        CustomerSearchResult<Document> result = adapter.ListCustomerDocuments(1, "C-404", 100);

        assertTrue(result.getList().isEmpty());
        assertEquals(0, result.getTotal());
    }
}
