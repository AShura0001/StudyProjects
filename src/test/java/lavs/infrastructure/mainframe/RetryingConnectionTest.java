package lavs.infrastructure.mainframe;

import mainframe.client.MessageRequest;
import mainframe.client.MessageResponse;
import mainframe.client.StatusMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RetryingConnectionTest {

    private static MessageResponse networkFailure() {
        return StatusMessage.NETWORK_FAILURE_UNAVAILABLE.generateMessageResponse(0);
    }

    @Test
    void retriesQueryAfterNetworkFailure() {
        MainframeTestConnection fake = new MainframeTestConnection();
        fake.addResponse(networkFailure());
        fake.addResponse(networkFailure());
        fake.addResponse(MainframeTestConnection.successResponse("id", "1"));

        RetryingConnection connection = new RetryingConnection(fake, 4, 1);
        MessageResponse response =
                connection.send(new MessageRequest(MessageRequest.QUERY, "customer.retrieve"));

        assertTrue(response.getStatus().getWasSuccessful());
        assertEquals(3, fake.getRequestCount());
    }

    @Test
    void givesUpAfterMaxAttempts() {
        MainframeTestConnection fake = new MainframeTestConnection();
        for (int i = 0; i < 3; i++) {
            fake.addResponse(networkFailure());
        }

        RetryingConnection connection = new RetryingConnection(fake, 3, 1);
        MessageResponse response =
                connection.send(new MessageRequest(MessageRequest.QUERY, "customer.retrieve"));

        assertFalse(response.getStatus().getWasSuccessful());
        assertEquals(3, fake.getRequestCount());
    }

    @Test
    void doesNotRetryNonNetworkFailure() {
        MainframeTestConnection fake = new MainframeTestConnection();
        fake.addResponse(MainframeTestConnection.failedResponse());

        RetryingConnection connection = new RetryingConnection(fake, 4, 1);
        connection.send(new MessageRequest(MessageRequest.QUERY, "customer.retrieve"));

        assertEquals(1, fake.getRequestCount());
    }

    @Test
    void doesNotRetryUpdates() {
        MainframeTestConnection fake = new MainframeTestConnection();
        fake.addResponse(networkFailure());

        RetryingConnection connection = new RetryingConnection(fake, 4, 1);
        MessageResponse response =
                connection.send(new MessageRequest(MessageRequest.UPDATE, "customer.addNote"));

        assertFalse(response.getStatus().getWasSuccessful());
        assertEquals(1, fake.getRequestCount());
    }
}
