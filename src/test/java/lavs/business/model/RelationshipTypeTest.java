package lavs.business.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RelationshipTypeTest {

    @Test
    void mainframeName_forEachType_isTheNameTheMainframeAccepts() {
        assertEquals("Parent", RelationshipType.PARENT.mainframeName());
        assertEquals("Spouse", RelationshipType.SPOUSE.mainframeName());
        assertEquals("LegalAuthority", RelationshipType.LEGAL_AUTHORITY.mainframeName());
        assertEquals("Other", RelationshipType.OTHER.mainframeName());
    }

    @Test
    void values_areExactlyTheFourTypesTheMainframeKnows() {
        assertEquals(List.of("Parent", "Spouse", "LegalAuthority", "Other"),
                Arrays.stream(RelationshipType.values()).map(RelationshipType::mainframeName).toList());
    }
}
