package lavs.business.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class DocumentTest {

    @Test
    void newDocument_withAllValues_keepsThem() {
        Document document = new Document("Jane Doe", "C-1", "passport.pdf", "pdf", 6L, 2048L, "Identity");

        assertEquals("Jane Doe", document.getCustomerName());
        assertEquals("C-1", document.getCustomerNumber());
        assertEquals("passport.pdf", document.getFileName());
        assertEquals("pdf", document.getFileType());
        assertEquals(6L, document.getId());
        assertEquals(2048L, document.getSize());
        assertEquals("Identity", document.getType());
    }

    @Test
    void setters_changeEveryProperty() {
        Document document = new Document();

        document.setCustomerName("Sam Doe");
        document.setCustomerNumber("C-2");
        document.setFileName("licence.png");
        document.setFileType("png");
        document.setId(11L);
        document.setNumber("D-1");
        document.setSize(4096L);
        document.setType("Licence");

        assertEquals("Sam Doe", document.getCustomerName());
        assertEquals("C-2", document.getCustomerNumber());
        assertEquals("licence.png", document.getFileName());
        assertEquals("png", document.getFileType());
        assertEquals(11L, document.getId());
        assertEquals("D-1", document.getNumber());
        assertEquals(4096L, document.getSize());
        assertEquals("Licence", document.getType());
    }

    @Test
    void document_isACustomerType() {
        assertInstanceOf(CustomerType.class, new Document());
    }
}
