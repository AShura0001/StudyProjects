package lavs.business.model;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Turning the mainframe's relationship type names back into types, and how each type reads. */
class RelationshipTypeLookupTest {

    @Test
    void fromMainframeName_findsEveryType() {
        for (RelationshipType type : RelationshipType.values()) {
            assertEquals(Optional.of(type), RelationshipType.fromMainframeName(type.mainframeName()));
        }
    }

    @Test
    void fromMainframeName_ignoresCaseAndSpaces() {
        assertEquals(Optional.of(RelationshipType.LEGAL_AUTHORITY), RelationshipType.fromMainframeName(" legalauthority "));
    }

    @Test
    void fromMainframeName_ofAnUnknownOrMissingName_isEmpty() {
        assertTrue(RelationshipType.fromMainframeName("Cousin").isEmpty());
        assertTrue(RelationshipType.fromMainframeName(null).isEmpty());
    }

    @Test
    void label_readsAsWords() {
        assertEquals("Legal authority", RelationshipType.LEGAL_AUTHORITY.label());
        assertEquals("Spouse", RelationshipType.SPOUSE.label());
    }
}
