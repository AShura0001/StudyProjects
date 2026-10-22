package lavs.infrastructure.mainframe;

import lavs.business.model.ReferenceCategory;
import lavs.business.model.ReferenceItem;
import lavs.business.repository.MainframeUnavailableException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReferenceDataAdapterTest {

    private final ScriptedConnection connection = new ScriptedConnection();
    private final ReferenceDataAdapter adapter = new ReferenceDataAdapter(connection);

    private void categoriesReply(int total, Object... idsAndNames) {
        List<String> pairs = new ArrayList<>(List.of(
                "page", "1", "total", String.valueOf(total), "items", String.valueOf(idsAndNames.length / 2)));
        for (int i = 0; i < idsAndNames.length; i += 2) {
            String prefix = (i / 2 + 1) + ":";
            pairs.addAll(List.of(prefix + "id", String.valueOf(idsAndNames[i]), prefix + "name", (String) idsAndNames[i + 1]));
        }
        connection.replySuccess(pairs.toArray(new String[0]));
    }

    private void itemsReply(int total, Object... namesAndOrders) {
        List<String> pairs = new ArrayList<>(List.of("categoryId", "7", "categoryName", "CustomerAddressType",
                "page", "1", "total", String.valueOf(total), "items", String.valueOf(namesAndOrders.length / 2)));
        for (int i = 0; i < namesAndOrders.length; i += 2) {
            String prefix = (i / 2 + 1) + ":";
            pairs.addAll(List.of(prefix + "name", (String) namesAndOrders[i], prefix + "order", String.valueOf(namesAndOrders[i + 1])));
        }
        connection.replySuccess(pairs.toArray(new String[0]));
    }

    @Test
    void newAdapter_withANullConnection_isRejected() {
        assertThrows(NullPointerException.class, () -> new ReferenceDataAdapter(null));
    }

    @Test
    void listCategories_returnsEveryCategory() {
        categoriesReply(2, 7, "CustomerAddressType", 8, "LoanRateType");

        assertEquals(List.of(new ReferenceCategory(7, "CustomerAddressType"), new ReferenceCategory(8, "LoanRateType")),
                adapter.listCategories());
        assertEquals("constantCategory.list", connection.requests().get(0).getRequestType());
        assertEquals("1", connection.requests().get(0).getValue("page"));
    }

    @Test
    void listCategories_overSeveralPages_fetchesThemAll() {
        categoriesReply(3, 7, "CustomerAddressType", 8, "LoanRateType");
        categoriesReply(3, 9, "LoanPaymentFrequency");

        assertEquals(3, adapter.listCategories().size());
        assertEquals("2", connection.requests().get(1).getValue("page"));
    }

    @Test
    void listCategories_whenAPageIsEmpty_stops() {
        categoriesReply(5, 7, "CustomerAddressType");
        categoriesReply(5);

        assertEquals(List.of(new ReferenceCategory(7, "CustomerAddressType")), adapter.listCategories());
        assertEquals(2, connection.requests().size());
    }

    @Test
    void listCategories_whenTheMainframeCannotBeReached_saysSo() {
        connection.replyUnreachable();

        assertThrows(MainframeUnavailableException.class, adapter::listCategories);
    }

    @Test
    void listCategories_whenTheMainframeSaysNo_saysItIsUnavailable() {
        connection.replyRefused("Not allowed.");

        assertThrows(MainframeUnavailableException.class, adapter::listCategories);
    }

    @Test
    void listCategories_whenTheReplyCannotBeRead_saysItIsUnavailable() {
        connection.replySuccess("page", "1", "total", "1");

        assertThrows(MainframeUnavailableException.class, adapter::listCategories);
    }

    @Test
    void listItems_returnsEveryValueInTheCategory() {
        itemsReply(2, "Residential", 1, "Postal", 2);

        assertEquals(List.of(new ReferenceItem("Residential", 1), new ReferenceItem("Postal", 2)),
                adapter.listItems(7));
        assertEquals("constantItem.list", connection.requests().get(0).getRequestType());
        assertEquals("7", connection.requests().get(0).getValue("category"));
    }

    @Test
    void listItems_overSeveralPages_fetchesThemAll() {
        itemsReply(3, "Residential", 1, "Postal", 2);
        itemsReply(3, "Other", 5);

        assertEquals(3, adapter.listItems(7).size());
        assertEquals("2", connection.requests().get(1).getValue("page"));
    }

    @Test
    void listItems_whenAPageIsEmpty_stops() {
        itemsReply(4, "Residential", 1);
        itemsReply(4);

        assertEquals(List.of(new ReferenceItem("Residential", 1)), adapter.listItems(7));
    }

    @Test
    void listItems_whenTheMainframeCannotBeReached_saysSo() {
        connection.replyUnreachable();

        assertThrows(MainframeUnavailableException.class, () -> adapter.listItems(7));
    }

    @Test
    void listItems_whenTheMainframeSaysNo_saysItIsUnavailable() {
        connection.replyRefused("No such category.");

        assertThrows(MainframeUnavailableException.class, () -> adapter.listItems(99));
    }

    @Test
    void listItems_whenTheReplyCannotBeRead_saysItIsUnavailable() {
        connection.replyUnreadable();

        assertThrows(MainframeUnavailableException.class, () -> adapter.listItems(7));
    }
}
