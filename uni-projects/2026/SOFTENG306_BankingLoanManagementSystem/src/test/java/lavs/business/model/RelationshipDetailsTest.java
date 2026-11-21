package lavs.business.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RelationshipDetailsTest {

    @Test
    void newDetails_withAllValues_keepsThem() {
        RelationshipDetails details = new RelationshipDetails(RelationshipType.OTHER, "family friend", "C-2");

        assertEquals(RelationshipType.OTHER, details.type());
        assertEquals("family friend", details.description());
        assertEquals("C-2", details.relatedCustomerNumber());
    }

    @Test
    void newDetails_withANullDescription_keepsAnEmptyOne() {
        assertEquals("", new RelationshipDetails(RelationshipType.SPOUSE, null, "C-2").description());
    }

    @Test
    void newDetails_withSpacesAroundTheDescription_removesThem() {
        assertEquals("friend", new RelationshipDetails(RelationshipType.OTHER, "  friend ", "C-2").description());
    }

    @Test
    void newDetails_withANullTypeOrNullRelatedCustomer_isRejected() {
        assertThrows(NullPointerException.class, () -> new RelationshipDetails(null, "", "C-2"));
        assertThrows(NullPointerException.class, () -> new RelationshipDetails(RelationshipType.PARENT, "", null));
    }

    @Test
    void newDetails_withABlankRelatedCustomer_isRejected() {
        assertThrows(IllegalArgumentException.class, () -> new RelationshipDetails(RelationshipType.PARENT, "", " "));
    }
}
