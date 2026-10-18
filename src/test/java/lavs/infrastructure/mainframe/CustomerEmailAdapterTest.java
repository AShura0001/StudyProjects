package lavs.infrastructure.mainframe;

import lavs.business.model.Email;
import mainframe.client.Connection;
import mainframe.client.MessageRequest;
import mainframe.client.MessageResponse;
import mainframe.client.Status;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class CustomerEmailAdapterTest {

    @Test
    void findEmailByCustomerNumberAndId() throws IOException {
        MainframeTestConnection connection = new MainframeTestConnection();
        connection.addResponse(MainframeTestConnection
                .successResponse(new HashMap<>() {
                                     {
                                         put("customer", "CUS-9");
                                         put("id", "12345");
                                         put("type", "Personal");
                                         put("otherType", "");
                                         put("address", "johnsmith123@hotmail.com");
                                         put("isPrimary", "y");
                                         put("customerNumber", "CUS-9");
                                     }
                                 }
                )

        );

        CustomerEmailAdapter adapter = new CustomerEmailAdapter(connection);

        Optional<Email> email = adapter.findEmailByCustomerNumberAndId(12345L, "CUS-9");
        assertTrue(email.isPresent());
        assertEquals(12345L, email.orElseThrow().getId());
        assertEquals("CUS-9", email.orElseThrow().getCustomerNumber());
        assertEquals("johnsmith123@hotmail.com", email.orElseThrow().getAddress());
        assertEquals("Personal", email.orElseThrow().getType());
        assertTrue(email.orElseThrow().isPrimary());
    }

    @Test
    void findEmailByCustomerNumberAndIdWithStatus() throws IOException {
        MainframeTestConnection connection = new MainframeTestConnection();
        connection.addResponse(MainframeTestConnection.failedResponse());

        CustomerEmailAdapter adapter = new CustomerEmailAdapter(connection);

        assertTrue(adapter.findEmailByCustomerNumberAndId(12345L, "CUS-9").isEmpty());

    }

    @Test
    void updateEmail_sendsUpdateAndReturnsMappedAddress() throws IOException {
        MainframeTestConnection connection = new MainframeTestConnection();

        connection.addResponse(MainframeTestConnection
                .successResponse(new HashMap<>() {
                                     {
                                         put("customer", "CUS-9");
                                         put("id", "12345");
                                         put("type", "Personal");
                                         put("otherType", "");
                                         put("address", "johnsmith123@hotmail.com");
                                         put("isPrimary", "y");
                                         put("customerNumber", "CUS-9");
                                     }
                                 }
                )

        );

        CustomerEmailAdapter adapter = new CustomerEmailAdapter(connection);

        Email input = new Email();
        input.setAddress("johnsmith123@hotmail.com");
        input.setType("Personal");
        input.setPrimary(true);
        input.setCustomerNumber("CUS-9");
        input.setOtherType("someOtherType");
        input.setId(12345L);
        Optional<Email> result = adapter.updateCustomerEmail(input);

        assertTrue(result.isPresent());
        assertEquals(12345L, result.orElseThrow().getId());
        assertEquals("CUS-9", result.orElseThrow().getCustomerNumber());
        assertEquals("johnsmith123@hotmail.com", result.orElseThrow().getAddress());
        assertEquals("Personal", result.orElseThrow().getType());
        assertTrue(result.orElseThrow().isPrimary());

    }

    @Test
    void deleteEmail_sendsDeleteRequest() throws IOException {
        MainframeTestConnection connection = new MainframeTestConnection();
        connection.addResponse(MainframeTestConnection.successResponse(new HashMap<>() {
        }));
        CustomerEmailAdapter adapter = new CustomerEmailAdapter(connection);
        adapter.deleteCustomerEmail("CSU-0", 12345L);

        assertEquals("customer.deleteEmail", connection.getLastRequest().getRequestType());
        assertEquals("CSU-0", connection.getLastRequest().getValue("customer"));
        assertEquals("12345", connection.getLastRequest().getValue("id"));

    }


}
