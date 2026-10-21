package lavs.infrastructure.mainframe;

import lavs.business.model.ReferenceCategory;
import lavs.business.model.ReferenceItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class ReferenceDataAdapterIntegrationTest extends MainframeIntegrationTestSupport {

    private static final String ADDRESS_TYPES = "CustomerAddressType";

    private ReferenceDataAdapter adapter;

    @BeforeAll
    void setUpAdapter() {
        adapter = new ReferenceDataAdapter(connection);
    }

    @Test
    void listCategoriesReturnsEveryCategoryOnce() {
        List<ReferenceCategory> categories = adapter.listCategories();

        assertFalse(categories.isEmpty(), "The mainframe should have constant categories");
        assertEquals(categories.size(), categories.stream().map(ReferenceCategory::id).distinct().count(),
                "No category should be listed twice");
    }

    @Test
    void listItemsReturnsTheAddressTypes() {
        ReferenceCategory addressTypes = adapter.listCategories().stream()
                .filter(c -> ADDRESS_TYPES.equals(c.name()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No " + ADDRESS_TYPES + " category on the mainframe"));

        List<String> names = adapter.listItems(addressTypes.id()).stream()
                .map(ReferenceItem::name)
                .collect(Collectors.toList());

        assertTrue(names.contains("Residential"), "Address types were " + names);
    }
}
