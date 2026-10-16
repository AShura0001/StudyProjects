package lavs.business.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReferenceDataModelTest {

    @Test
    void newCategory_whenCreated_keepsIdAndName() {
        ReferenceCategory category = new ReferenceCategory(3, "Address Type");

        assertEquals(3, category.id());
        assertEquals("Address Type", category.name());
    }

    @Test
    void newCategory_withNullName_isRejected() {
        assertThrows(NullPointerException.class, () -> new ReferenceCategory(3, null));
    }

    @Test
    void newItem_whenCreated_keepsNameAndOrder() {
        ReferenceItem item = new ReferenceItem("Home", 1);

        assertEquals("Home", item.name());
        assertEquals(1, item.order());
    }

    @Test
    void newItem_withNullName_isRejected() {
        assertThrows(NullPointerException.class, () -> new ReferenceItem(null, 1));
    }
}
