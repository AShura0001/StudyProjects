package lavs.infrastructure.mainframe;

import lavs.business.model.Document;
import lavs.business.repository.CustomerSearchResult;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class CustomerDocumentApdaterIntegrationTest
        extends MainframeIntegrationTestSupport {

    private static final String CUSTOMER_NUMBER =
            System.getProperty(
                    "mainframe.customerNumber",
                    "01-0001-0000020"
            );

    private static final long DOCUMENT_ID =
            Long.parseLong(
                    System.getProperty(
                            "mainframe.documentId",
                            "1"
                    )
            );

    @Test
    void findDocumentByCustomerNumberAndIdRetrievesDocument()
            throws Exception {

        CustomerDocumentAdapter adapter =
                new CustomerDocumentAdapter(connection);

        Optional<Document> result =
                adapter.findDocumentByCustomerNumberAndId(
                        DOCUMENT_ID,
                        CUSTOMER_NUMBER
                );

        assertTrue(
                result.isPresent(),
                "Expected document " + DOCUMENT_ID
                        + " for customer " + CUSTOMER_NUMBER
        );

        Document document = result.orElseThrow();

        assertEquals(DOCUMENT_ID, document.getId());
        assertEquals(
                CUSTOMER_NUMBER,
                document.getCustomerNumber()
        );

        assertNotNull(document.getCustomerName());
        assertNotNull(document.getFileName());
        assertFalse(document.getFileName().isBlank());

        assertNotNull(document.getFileType());
        assertNotNull(document.getType());

        assertTrue(
                document.getSize() >= 0,
                "Document size should not be negative"
        );
    }

    @Test
    void findDocumentByCustomerNumberAndIdReturnsEmptyForUnknownDocument()
            throws Exception {

        CustomerDocumentAdapter adapter =
                new CustomerDocumentAdapter(connection);

        Optional<Document> result =
                adapter.findDocumentByCustomerNumberAndId(
                        Long.MAX_VALUE,
                        CUSTOMER_NUMBER
                );

        assertTrue(
                result.isEmpty(),
                "An unknown document should return Optional.empty()"
        );
    }

    @Test
    void listDocumentsByCustomerId() throws IOException {
        CustomerDocumentAdapter adapter =
                new CustomerDocumentAdapter(connection);

        CustomerSearchResult<Document> result = adapter.ListCustomerDocuments(1, CUSTOMER_NUMBER, 20);

        assertTrue(result.getList().size() >= 5);

        result = adapter.ListCustomerDocuments(1, CUSTOMER_NUMBER, 3);
        assertTrue(result.getList().size() <= 3);


    }
}