package lavs.business.service;

import lavs.business.model.Document;
import lavs.business.model.LoanDocument;
import lavs.business.model.StoredDocument;
import lavs.business.repository.CustomerDocumentGateway;
import lavs.business.repository.CustomerSearchResult;
import lavs.business.repository.LoanDocumentGateway;
import lavs.business.repository.MainframeUnavailableException;
import lavs.business.repository.PageResult;
import lavs.business.repository.RecordRejectedException;
import lavs.business.repository.SessionExpiredException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The rules for documents recorded against customers and loans, with the mainframe played by mocks. */
class DocumentServiceTest {

    private CustomerDocumentGateway customerDocuments;
    private LoanDocumentGateway loanDocuments;
    private DocumentService service;

    @BeforeEach
    void setUp() {
        customerDocuments = mock(CustomerDocumentGateway.class);
        loanDocuments = mock(LoanDocumentGateway.class);
        service = new DocumentService(customerDocuments, loanDocuments);
    }

    private static Document customerDocument(long id, String fileType, long size) {
        Document document = new Document();
        document.setId(id);
        document.setCustomerNumber("C-1");
        document.setFileName("payslip.pdf");
        document.setFileType(fileType);
        document.setSize(size);
        document.setType("Proof of Income");
        return document;
    }

    private static Map<String, String> problemsOf(ValidationException e) {
        return e.getErrors().stream().collect(Collectors.toMap(ValidationError::getField, ValidationError::getMessage));
    }

    @Test
    void newService_withoutAGateway_isRejected() {
        assertThrows(NullPointerException.class, () -> new DocumentService(null, loanDocuments));
        assertThrows(NullPointerException.class, () -> new DocumentService(customerDocuments, null));
    }

    // ---- customers ----

    @Test
    void documentsOfCustomer_readsEachDocumentInFull() throws IOException {
        when(customerDocuments.ListCustomerDocuments(eq(1L), eq("C-1"), anyLong()))
                .thenReturn(new CustomerSearchResult<>(List.of(customerDocument(4, null, 0)), 1, 1));
        when(customerDocuments.findDocumentByCustomerNumberAndId(4L, "C-1"))
                .thenReturn(Optional.of(customerDocument(4, "application/pdf", 2048)));

        List<StoredDocument> found = service.documentsOfCustomer("C-1");

        assertEquals(List.of(new StoredDocument(4, "C-1", "payslip.pdf", "application/pdf", 2048L, "Proof of Income")),
                found);
    }

    @Test
    void documentsOfCustomer_keepsTheListVersionAndReadsNoSizeAsUnknown() throws IOException {
        Document listed = customerDocument(4, null, 0);
        listed.setCustomerNumber(null);
        when(customerDocuments.ListCustomerDocuments(eq(1L), eq("C-1"), anyLong()))
                .thenReturn(new CustomerSearchResult<>(List.of(listed), 1, 1));
        when(customerDocuments.findDocumentByCustomerNumberAndId(4L, "C-1")).thenReturn(Optional.empty());

        StoredDocument found = service.documentsOfCustomer("C-1").get(0);

        assertEquals("C-1", found.owner());
        assertNull(found.size());
    }

    @Test
    void documentsOfCustomer_readsEveryPage() throws IOException {
        when(customerDocuments.ListCustomerDocuments(eq(1L), eq("C-1"), anyLong()))
                .thenReturn(new CustomerSearchResult<>(List.of(customerDocument(4, null, 0)), 2, 1));
        when(customerDocuments.ListCustomerDocuments(eq(2L), eq("C-1"), anyLong()))
                .thenReturn(new CustomerSearchResult<>(List.of(customerDocument(5, null, 0)), 2, 2));
        when(customerDocuments.findDocumentByCustomerNumberAndId(any(), eq("C-1"))).thenReturn(Optional.empty());

        assertEquals(2, service.documentsOfCustomer("C-1").size());
    }

    @Test
    void documentsOfCustomer_aCustomerWithNone_isEmpty() throws IOException {
        when(customerDocuments.ListCustomerDocuments(eq(1L), eq("C-1"), anyLong()))
                .thenReturn(new CustomerSearchResult<>(List.of(), 0, 1));

        assertTrue(service.documentsOfCustomer("C-1").isEmpty());
    }

    @Test
    void addToCustomer_sendsTheTidiedDetails() throws IOException {
        when(customerDocuments.addCustomerDocument(any()))
                .thenReturn(Optional.of(customerDocument(9, "application/pdf", 2048)));

        StoredDocument added = service.addToCustomer("C-1", "  payslip.pdf ", "application/pdf", 2048, " Proof of Income ");

        ArgumentCaptor<Document> sent = ArgumentCaptor.forClass(Document.class);
        verify(customerDocuments).addCustomerDocument(sent.capture());
        assertEquals("payslip.pdf", sent.getValue().getFileName());
        assertEquals("Proof of Income", sent.getValue().getType());
        assertEquals("C-1", sent.getValue().getCustomerNumber());
        assertEquals(2048, sent.getValue().getSize());
        assertEquals(9, added.id());
    }

    @Test
    void addToCustomer_withoutAKnownFileType_recordsItAsUnknown() throws IOException {
        when(customerDocuments.addCustomerDocument(any())).thenReturn(Optional.of(customerDocument(9, null, 0)));

        service.addToCustomer("C-1", "scan", " ", 0, "Other");

        ArgumentCaptor<Document> sent = ArgumentCaptor.forClass(Document.class);
        verify(customerDocuments).addCustomerDocument(sent.capture());
        assertEquals(DocumentService.UNKNOWN_FILE_TYPE, sent.getValue().getFileType());
    }

    @Test
    void addToCustomer_withNoFileOrKind_reportsBothAndSendsNothing() throws IOException {
        ValidationException refused = assertThrows(ValidationException.class,
                () -> service.addToCustomer("C-1", " ", null, 0, null));

        Map<String, String> problems = problemsOf(refused);
        assertTrue(problems.containsKey(DocumentService.FILE_NAME_FIELD));
        assertTrue(problems.containsKey(DocumentService.TYPE_FIELD));
        verify(customerDocuments, never()).addCustomerDocument(any());
    }

    @Test
    void addToCustomer_withAFileNameTooLong_isRefused() {
        String longest = "a".repeat(DocumentService.MAX_FILE_NAME_LENGTH);
        assertThrows(ValidationException.class,
                () -> service.addToCustomer("C-1", longest + "b", "text/plain", 1, "Other"));
    }

    @Test
    void addToCustomer_withANegativeSize_isAProgrammingMistake() {
        assertThrows(IllegalArgumentException.class,
                () -> service.addToCustomer("C-1", "a.pdf", "application/pdf", -1, "Other"));
    }

    @Test
    void addToCustomer_whenTheMainframeDoesNotAccept_saysSo() throws IOException {
        when(customerDocuments.addCustomerDocument(any())).thenReturn(Optional.empty());

        RecordRejectedException refused = assertThrows(RecordRejectedException.class,
                () -> service.addToCustomer("C-1", "a.pdf", "application/pdf", 1, "Other"));
        assertEquals(DocumentService.NOT_ADDED_MESSAGE, refused.getMessage());
    }

    @Test
    void removeFromCustomer_asksTheMainframe_andReportsWhenItCannot() throws IOException {
        when(customerDocuments.deleteCustomerDocument(4, "C-1")).thenReturn(true, false);

        service.removeFromCustomer("C-1", 4);
        RecordRejectedException refused = assertThrows(RecordRejectedException.class,
                () -> service.removeFromCustomer("C-1", 4));
        assertEquals(DocumentService.NOT_REMOVED_MESSAGE, refused.getMessage());
    }

    @Test
    void customerDocuments_whenTheMainframeCannotBeReached_giveTheOfflineMessage() throws IOException {
        when(customerDocuments.ListCustomerDocuments(anyLong(), anyString(), anyLong()))
                .thenThrow(new IOException("socket closed"));

        MainframeUnavailableException offline = assertThrows(MainframeUnavailableException.class,
                () -> service.documentsOfCustomer("C-1"));
        assertEquals(DocumentService.OFFLINE_MESSAGE, offline.getMessage());
    }

    // ---- loans ----

    @Test
    void documentsOfLoan_readsEachDocumentInFull() {
        LoanDocument listed = new LoanDocument(7, "LN-1", "valuation.pdf", null, null, "Valuation");
        LoanDocument full = new LoanDocument(7, "LN-1", "valuation.pdf", "application/pdf", 4096L, "Valuation");
        when(loanDocuments.listDocuments(eq("LN-1"), eq(1L), anyLong())).thenReturn(new PageResult<>(List.of(listed), 1, 1));
        when(loanDocuments.findDocument("LN-1", 7)).thenReturn(Optional.of(full));

        assertEquals(List.of(new StoredDocument(7, "LN-1", "valuation.pdf", "application/pdf", 4096L, "Valuation")),
                service.documentsOfLoan("LN-1"));
    }

    @Test
    void documentsOfLoan_keepsTheListVersionWhenOneCannotBeRead_andReadsEveryPage() {
        when(loanDocuments.listDocuments(eq("LN-1"), eq(1L), anyLong())).thenReturn(new PageResult<>(
                List.of(new LoanDocument(7, "LN-1", "a.pdf", null, null, "Other")), 2, 1));
        when(loanDocuments.listDocuments(eq("LN-1"), eq(2L), anyLong())).thenReturn(new PageResult<>(
                List.of(new LoanDocument(8, "LN-1", "b.pdf", null, null, "Other")), 2, 2));
        when(loanDocuments.findDocument(eq("LN-1"), anyLong())).thenReturn(Optional.empty());

        List<StoredDocument> found = service.documentsOfLoan("LN-1");

        assertEquals(2, found.size());
        assertEquals("b.pdf", found.get(1).fileName());
    }

    @Test
    void addToLoan_sendsTheTidiedDetails() {
        LoanDocument saved = new LoanDocument(7, "LN-1", "valuation.pdf", "application/pdf", 4096L, "Valuation");
        when(loanDocuments.addDocument("LN-1", "valuation.pdf", "application/pdf", 4096, "Valuation")).thenReturn(saved);

        assertEquals(7, service.addToLoan("LN-1", " valuation.pdf", "application/pdf", 4096, "Valuation ").id());
    }

    @Test
    void addToLoan_withNoKind_isRefusedAndSendsNothing() {
        assertThrows(ValidationException.class, () -> service.addToLoan("LN-1", "a.pdf", "application/pdf", 1, " "));

        verify(loanDocuments, never()).addDocument(anyString(), anyString(), anyString(), anyLong(), anyString());
    }

    @Test
    void addToLoan_whenTheMainframeRefuses_passesItsReasonOn() {
        when(loanDocuments.addDocument(anyString(), anyString(), anyString(), anyLong(), anyString()))
                .thenThrow(new RecordRejectedException("No such loan.", Map.of()));

        RecordRejectedException refused = assertThrows(RecordRejectedException.class,
                () -> service.addToLoan("LN-404", "a.pdf", "application/pdf", 1, "Other"));
        assertEquals("No such loan.", refused.getMessage());
    }

    @Test
    void removeFromLoan_asksTheMainframe() {
        service.removeFromLoan("LN-1", 7);

        verify(loanDocuments).deleteDocument("LN-1", 7);
    }

    @Test
    void loanDocuments_whenTheMainframeCannotBeReached_giveTheOfflineMessage() {
        when(loanDocuments.listDocuments(anyString(), anyLong(), anyLong()))
                .thenThrow(new MainframeUnavailableException("socket closed"));

        MainframeUnavailableException offline = assertThrows(MainframeUnavailableException.class,
                () -> service.documentsOfLoan("LN-1"));
        assertEquals(DocumentService.OFFLINE_MESSAGE, offline.getMessage());
    }

    @Test
    void anEndedSession_keepsItsOwnMessage() {
        SessionExpiredException ended = new SessionExpiredException();
        doThrow(ended).when(loanDocuments).deleteDocument("LN-1", 7);

        assertSame(ended, assertThrows(SessionExpiredException.class, () -> service.removeFromLoan("LN-1", 7)));
    }

    @Test
    void aBlankOwner_isRejectedBeforeAskingTheMainframe() {
        assertThrows(IllegalArgumentException.class, () -> service.documentsOfCustomer(" "));
        assertThrows(NullPointerException.class, () -> service.documentsOfLoan(null));
        assertThrows(IllegalArgumentException.class, () -> service.addToCustomer("", "a", "b", 1, "c"));
        assertThrows(IllegalArgumentException.class, () -> service.addToLoan(" ", "a", "b", 1, "c"));
        assertThrows(IllegalArgumentException.class, () -> service.removeFromCustomer("", 1));
        assertThrows(IllegalArgumentException.class, () -> service.removeFromLoan("", 1));
    }
}
