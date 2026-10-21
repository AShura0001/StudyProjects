package lavs.demo;

import lavs.infrastructure.mainframe.ScriptedConnection;
import mainframe.client.MessageRequest;
import mainframe.client.MessageResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwitchableConnectionTest {

    private final ScriptedConnection real = new ScriptedConnection();
    private final SwitchableConnection connection = new SwitchableConnection(real);

    @Test
    void newConnection_withANullDelegate_isRejected() {
        assertThrows(NullPointerException.class, () -> new SwitchableConnection(null));
    }

    @Test
    void whileOnline_itPassesEverythingToTheRealConnection() {
        real.replySuccess("answer", "yes");

        MessageResponse reply = connection.send(new MessageRequest("query", "customer.search"));

        assertTrue(connection.isConnected());
        assertTrue(reply.getStatus().getWasSuccessful());
        assertEquals(1, real.requests().size());
    }

    @Test
    void whileOffline_itSaysItIsNotConnectedAndSendsNothing() {
        connection.goOffline();

        MessageResponse reply = connection.send(new MessageRequest("query", "customer.search"));

        assertTrue(connection.isOffline());
        assertFalse(connection.isConnected());
        assertFalse(reply.getStatus().getWasSuccessful());
        assertTrue(real.requests().isEmpty());
    }

    @Test
    void whileOffline_itDoesNotEvenAskTheRealConnection() {
        connection.goOffline();

        connection.isConnected();

        assertEquals(0, real.pings());
    }

    @Test
    void goOnline_afterGoingOffline_worksAgain() {
        connection.goOffline();
        connection.goOnline();
        real.replySuccess("answer", "yes");

        assertFalse(connection.isOffline());
        assertTrue(connection.isConnected());
        assertTrue(connection.send(new MessageRequest("query", "customer.search")).getStatus().getWasSuccessful());
    }

    @Test
    void whenTheRealConnectionIsNotConnected_itReportsThatToo() {
        real.setConnected(false);

        assertFalse(connection.isConnected());
    }

    @Test
    void loginLogoutAndClose_goToTheRealConnection() throws Exception {
        assertTrue(connection.Login("user", "pass").getWasSuccessful());
        connection.Logout();
        connection.close();
    }
}
