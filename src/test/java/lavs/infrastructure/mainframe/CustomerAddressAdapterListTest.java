package lavs.infrastructure.mainframe;

import lavs.business.model.Address;
import lavs.business.repository.CustomerSearchResult;
import mainframe.client.MessageRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Adding and listing addresses through the address adapter, against a scripted connection. */
class CustomerAddressAdapterListTest {

    private final ScriptedConnection connection = new ScriptedConnection();
    private final CustomerAddressAdapter adapter = new CustomerAddressAdapter(connection);

    private static Address home() {
        Address address = new Address();
        address.setCustomerNumber("CUS-9");
        address.setType("Residential");
        address.setLine1("1 Queen Street");
        address.setCity("Auckland");
        address.setPostCode("1010");
        address.setCountry("New Zealand");
        address.setIsMailing(true);
        address.setIsPrimary(true);
        return address;
    }

    private void retrieveReply() {
        connection.replySuccess("id", "50", "number", "CUS-9", "customerNumber", "CUS-9", "customerName", "Jane Doe",
                "type", "Residential", "otherType", "", "isPrimary", "y", "isMailing", "y", "line1", "1 Queen Street",
                "line2", "", "suburb", "CBD", "city", "Auckland", "country", "New Zealand", "postCode", "1010");
    }

    @Test
    void add_sendsTheAddressThenReadsItBack() throws Exception {
        connection.replySuccess("customer", "CUS-9", "id", "50", "isPrimary", "y", "isMailing", "y");
        retrieveReply();

        Address saved = adapter.addCustomerAddress(home()).orElseThrow();

        assertEquals("1 Queen Street", saved.getLine1());
        MessageRequest add = connection.requests().get(0);
        assertEquals("customer.addAddress", add.getRequestType());
        assertEquals("CUS-9", add.getValue("customer"));
        assertEquals("Auckland", add.getValue("city"));
        assertEquals("50", connection.requests().get(1).getValue("id"));
    }

    @Test
    void add_whenTheMainframeRefuses_givesNothingAndAsksNoMore() throws Exception {
        connection.replyRefused("Bad post code.");

        assertTrue(adapter.addCustomerAddress(home()).isEmpty());
        assertEquals(1, connection.requests().size());
    }

    @Test
    void add_whenItCannotBeReadBack_givesNothing() throws Exception {
        connection.replySuccess("customer", "CUS-9", "id", "50");
        connection.replyRefused("No such address.");

        assertTrue(adapter.addCustomerAddress(home()).isEmpty());
    }

    @Test
    void list_readsEachAddressWithItsFlags() throws Exception {
        connection.replySuccess("customerNumber", "CUS-9", "customerName", "Jane Doe", "page", "1", "total", "2",
                "items", "2",
                "1:id", "50", "1:type", "Residential", "1:isPrimary", "y", "1:isMailing", "n",
                "1:city", "Auckland", "1:country", "New Zealand",
                "2:id", "51", "2:type", "Postal", "2:isPrimary", "n", "2:isMailing", "y",
                "2:city", "Wellington", "2:country", "New Zealand");

        CustomerSearchResult<Address> result = adapter.ListCustomerAddresses(1, "CUS-9", 100);

        assertEquals(2, result.getTotal());
        List<Address> addresses = result.getList();
        assertTrue(addresses.get(0).getIsPrimary());
        assertFalse(addresses.get(0).getIsMailing());
        assertTrue(addresses.get(1).getIsMailing());
        assertEquals("Wellington", addresses.get(1).getCity());
        assertEquals("CUS-9", addresses.get(1).getCustomerNumber());
        assertEquals("customerAddress.list", connection.requests().get(0).getRequestType());
    }

    @Test
    void list_whenTheMainframeLeavesOutAFlag_readsItAsNo() throws Exception {
        connection.replySuccess("customerNumber", "CUS-9", "page", "1", "total", "1", "items", "1",
                "1:id", "50", "1:type", "Residential", "1:city", "Auckland", "1:country", "New Zealand");

        Address only = adapter.ListCustomerAddresses(1, "CUS-9", 100).getList().get(0);

        assertFalse(only.getIsPrimary());
        assertFalse(only.getIsMailing());
    }

    @Test
    void list_whenTheMainframeRefuses_isEmpty() throws Exception {
        connection.replyRefused("No such customer.");

        CustomerSearchResult<Address> result = adapter.ListCustomerAddresses(1, "CUS-404", 100);

        assertTrue(result.getList().isEmpty());
        assertEquals(0, result.getTotal());
    }
}
