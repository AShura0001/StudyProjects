package lavs.ui.controller;

import lavs.business.model.Relationship;
import lavs.business.model.StoredDocument;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** How the relationships and documents screens word what they show. */
class ScreenHelpersTest {

    private static Relationship relationship(String type, String otherType, String name, String number) {
        return new Relationship("Jane Doe", "C-1", 5, otherType, name, number, type);
    }

    @Test
    void kindOf_readsTheTypeAsWords() {
        assertEquals("Legal authority", RelationshipsController.kindOf(relationship("LegalAuthority", "", null, "C-2")));
        assertEquals("Spouse", RelationshipsController.kindOf(relationship("Spouse", "ignored", null, "C-2")));
    }

    @Test
    void kindOf_anOtherRelationship_addsItsDescription() {
        assertEquals("Other: Godparent", RelationshipsController.kindOf(relationship("Other", "Godparent", null, "C-2")));
        assertEquals("Other", RelationshipsController.kindOf(relationship("Other", "", null, "C-2")));
    }

    @Test
    void kindOf_aTypeTheAppDoesNotKnow_isShownAsItIs() {
        assertEquals("Cousin", RelationshipsController.kindOf(relationship("Cousin", "", null, "C-2")));
    }

    @Test
    void otherCustomerOf_showsTheNameWhenThereIsOne() {
        assertEquals("Sam Lee  •  C-2", RelationshipsController.otherCustomerOf(relationship("Spouse", "", "Sam Lee", "C-2")));
        assertEquals("C-2", RelationshipsController.otherCustomerOf(relationship("Spouse", "", null, "C-2")));
    }

    @Test
    void sizeOf_picksASensibleUnit() {
        assertEquals("", DocumentsController.sizeOf(null));
        assertEquals("", DocumentsController.sizeOf(0L));
        assertEquals("860 bytes", DocumentsController.sizeOf(860L));
        assertEquals("120 KB", DocumentsController.sizeOf(120L * 1024));
        assertEquals("1.5 MB", DocumentsController.sizeOf(3L * 512 * 1024));
    }

    @Test
    void detailsOf_leavesOutWhatWasNotSent() {
        assertEquals("payslip.pdf  •  2 KB  •  application/pdf", DocumentsController.detailsOf(
                new StoredDocument(4, "C-1", "payslip.pdf", "application/pdf", 2048L, "Proof of Income")));
        assertEquals("payslip.pdf", DocumentsController.detailsOf(
                new StoredDocument(4, "C-1", "payslip.pdf", null, null, "Proof of Income")));
    }
}
