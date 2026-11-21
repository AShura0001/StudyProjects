package lavs.infrastructure.mainframe;

import lavs.business.model.Customer;
import mainframe.client.Connection;
import mainframe.client.MessageRequest;
import mainframe.client.MessageResponse;
import mainframe.client.Status;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CustomerAdapterTest {

    @Test
    void findByCustomerNumber_whenMainframeSucceeds_mapsCustomer() throws IOException {
        CapturingConnection connection = new CapturingConnection();
        connection.response = successResponse(new HashMap<>() {{
            put("dateOfBirth", "1990-01-01");
            put("familyName", "Doe");
            put("firstName", "Jane");
            put("nationality", "NZ");
            put("number", "CUS-1");
            put("status", "Active");
            put("title", "Ms");
        }});

        CustomerAdapter adapter = new CustomerAdapter(connection);

        Optional<Customer> result = adapter.findByCustomerNumber("CUS-1");

        assertTrue(result.isPresent());
        assertEquals("1990-01-01", result.orElseThrow().get_dateOfBirth());
        assertEquals("Doe", result.orElseThrow().get_familyName());
        assertEquals("Jane", result.orElseThrow().get_firstName());
        assertEquals("NZ", result.orElseThrow().get_nationality());
        assertEquals("CUS-1", result.orElseThrow().get_number());
        assertEquals("Active", result.orElseThrow().get_status());
        assertEquals("Ms", result.orElseThrow().get_title());
        assertEquals("customer.retrieve", connection.lastRequest.getRequestType());
        assertEquals("CUS-1", connection.lastRequest.getValue("number"));
    }

    @Test
    void findByCustomerNumber_whenMainframeFails_returnsEmpty() throws IOException {
        CapturingConnection connection = new CapturingConnection();
        connection.response = failedResponse();

        CustomerAdapter adapter = new CustomerAdapter(connection);

        assertTrue(adapter.findByCustomerNumber("CUS-1").isEmpty());
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
