package lavs.business.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReferenceDataModelTest {

    @Test
    void newCategory_whenCreated_keepsIdAndName() {
        ReferenceCategory category = new ReferenceCategory(2, "CustomerAddressType");

        assertEquals(2, category.id());
        assertEquals("CustomerAddressType", category.name());
    }

    @Test
    void newCategory_withNullName_isRejected() {
        assertThrows(NullPointerException.class, () -> new ReferenceCategory(2, null));
    }

    @Test
    void newItem_whenCreated_keepsNameAndOrder() {
        ReferenceItem item = new ReferenceItem("Residential", 1);

        assertEquals("Residential", item.name());
        assertEquals(1, item.order());
    }

    @Test
    void newItem_withNullName_isRejected() {
        assertThrows(NullPointerException.class, () -> new ReferenceItem(null, 1));
    }
}
