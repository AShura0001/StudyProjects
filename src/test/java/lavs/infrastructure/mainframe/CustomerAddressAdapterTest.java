package lavs.infrastructure.mainframe;

import lavs.business.model.Address;
import mainframe.client.Connection;
import mainframe.client.MessageRequest;
import mainframe.client.MessageResponse;
import mainframe.client.Status;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CustomerAddressAdapterTest {

    @Test
    void findAddressByCustomerNumberAndId_whenMainframeSucceeds_mapsAddress() throws IOException {
        CapturingConnection connection = new CapturingConnection();
        connection.response = successResponse(new HashMap<>() {{
            put("id", "50");
            put("number", "CUS-9");
            put("customerNumber", "CUS-9");
            put("customerName", "Jane Doe");
            put("type", "Postal");
            put("otherType", "");
            put("isPrimary", "y");
            put("isMailing", "n");
            put("line1", "14 Karaka Street");
            put("line2", "Newmarket");
            put("suburb", "Auckland");
            put("city", "Auckland");
            put("country", "New Zealand");
            put("postCode", "1023");
        }});

        CustomerAddressAdapter adapter = new CustomerAddressAdapter(connection);

        Optional<Address> result = adapter.findAddressByCustomerNumberAndId(50L, "CUS-9");

        assertTrue(result.isPresent());
        assertEquals(50L, result.orElseThrow().getId());
        assertEquals("CUS-9", result.orElseThrow().getCustomerNumber());
        assertEquals("Jane Doe", result.orElseThrow().getCustomerName());
        assertEquals("Postal", result.orElseThrow().getType());
        assertEquals(true, result.orElseThrow().getIsPrimary());
        assertEquals(false, result.orElseThrow().getIsMailing());
        assertEquals("customerAddress.retrieve", connection.lastRequest.getRequestType());
        assertEquals("50", connection.lastRequest.getValue("id"));
        assertEquals("CUS-9", connection.lastRequest.getValue("number"));
    }

    @Test
    void findAddressByCustomerNumberAndId_whenMainframeFails_returnsEmpty() throws IOException {
        CapturingConnection connection = new CapturingConnection();
        connection.response = failedResponse();

        CustomerAddressAdapter adapter = new CustomerAddressAdapter(connection);

        assertTrue(adapter.findAddressByCustomerNumberAndId(50L, "CUS-9").isEmpty());
    }

    @Test
    void updateCustomerAddress_sendsUpdateAndReturnsMappedAddress() throws IOException {
        CapturingConnection connection = new CapturingConnection();
        connection.response = successResponse(new HashMap<>() {{
            put("id", "50");
            put("customer", "CUS-9");
            put("type", "Other");
            put("otherType", "Farm");
            put("isPrimary", "n");
            put("isMailing", "y");
            put("line1", "1 New Street");
            put("line2", "Suite 2");
            put("suburb", "Takapuna");
            put("city", "Auckland");
            put("country", "New Zealand");
            put("postCode", "0622");
        }});

        CustomerAddressAdapter adapter = new CustomerAddressAdapter(connection);
        Address input = new Address();
        input.setId(50L);
        input.setCustomerNumber("CUS-9");
        input.setType("Other");
        input.setOtherType("Farm");
        input.setIsPrimary(false);
        input.setIsMailing(true);
        input.setLine1("1 New Street");
        input.setLine2("Suite 2");
        input.setSuburb("Takapuna");
        input.setCity("Auckland");
        input.setCountry("New Zealand");
        input.setPostCode("0622");



        Optional<Address> result = adapter.updateCustomerAddress(input);
        assertTrue(result.isPresent());
        Address updatedAddress = result.get();

        assertEquals("customer.updateAddress", connection.lastRequest.getRequestType());
        assertEquals("CUS-9", connection.lastRequest.getValue("customer"));
        assertEquals("50", connection.lastRequest.getValue("id"));
        assertEquals("y", connection.lastRequest.getValue("isMailing"));
        assertEquals("n", connection.lastRequest.getValue("isPrimary"));
        assertEquals("CUS-9", updatedAddress.getCustomerNumber());
        assertEquals("Other", updatedAddress.getType());
        assertEquals("Farm", updatedAddress.getOtherType());
    }

    @Test
    void deleteCustomerAddress_sendsDeleteRequest() throws IOException {
        CapturingConnection connection = new CapturingConnection();
        connection.response = successResponse(new HashMap<>());

        CustomerAddressAdapter adapter = new CustomerAddressAdapter(connection);
        adapter.deleteCustomerAddress("CUS-9", 50l);

        assertEquals("customer.deleteAddress", connection.lastRequest.getRequestType());
        assertEquals("CUS-9", connection.lastRequest.getValue("customer"));
        assertEquals("50", connection.lastRequest.getValue("id"));
    }

    private static MessageResponse successResponse(HashMap<String, String> values) {
        return new MessageResponse(new Status(1), values);
    }

    private static MessageResponse failedResponse() {
        return new MessageResponse(new Status(1, "failed", 1), new HashMap<>());
    }

    private static final class CapturingConnection implements Connection {
        private MessageRequest lastRequest;
        private MessageResponse response;

        @Override
        public void close() {
        }

        @Override
        public boolean isConnected() {
            return true;
        }

        @Override
        public MessageResponse send(MessageRequest request) {
            this.lastRequest = request;
            return response;
        }

        @Override
        public Status Login(String username, String password) {
            return new Status(1);
        }

        @Override
        public void Logout() {
        }
    }
}
