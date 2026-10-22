package lavs.infrastructure.mainframe;

import com.sun.net.httpserver.HttpServer;
import mainframe.client.HttpConnection;
import mainframe.client.MessageRequest;
import mainframe.client.MessageResponse;
import mainframe.client.Status;
import mainframe.client.StatusMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A 401 or 403 while logged in means the session ended; it is not a network failure. */
class HttpConnectionSessionTest {

    private HttpServer server;
    private final AtomicInteger queryStatus = new AtomicInteger(200);
    private final AtomicInteger loginStatus = new AtomicInteger(200);

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/login", exchange -> reply(exchange.getResponseBody(), exchange, loginStatus.get(),
                "{\"token\":\"t-1\",\"userName\":\"tester\",\"lastNumber\":5}"));
        server.createContext("/api/query", exchange -> reply(exchange.getResponseBody(), exchange, queryStatus.get(),
                "{\"data\":\"{}\"}"));
        server.start();
    }

    private static void reply(OutputStream out, com.sun.net.httpserver.HttpExchange exchange, int status, String body)
            throws java.io.IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        out.write(bytes);
        out.close();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private HttpConnection connection() {
        return new HttpConnection("http://127.0.0.1:" + server.getAddress().getPort() + "/");
    }

    private static int codeOf(MessageResponse response) {
        return response.getStatus().getErrorCode();
    }

    @Test
    void unauthorisedWhileLoggedIn_isReportedAsAnEndedSession() throws Exception {
        HttpConnection connection = connection();
        assertTrue(connection.Login("tester", "pw").getWasSuccessful());

        queryStatus.set(401);
        assertEquals(StatusMessage.SESSION_EXPIRED.getCode(),
                codeOf(connection.send(new MessageRequest(MessageRequest.QUERY, "customer.search"))));

        queryStatus.set(403);
        assertEquals(StatusMessage.SESSION_EXPIRED.getCode(),
                codeOf(connection.send(new MessageRequest(MessageRequest.QUERY, "customer.search"))));
    }

    @Test
    void otherServerErrorsWhileLoggedIn_areStillNetworkFailures() throws Exception {
        HttpConnection connection = connection();
        assertTrue(connection.Login("tester", "pw").getWasSuccessful());

        queryStatus.set(502);
        assertEquals(StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode(),
                codeOf(connection.send(new MessageRequest(MessageRequest.QUERY, "customer.search"))));
    }

    @Test
    void unauthorisedBeforeLoggingIn_isUnchanged() throws Exception {
        HttpConnection connection = connection();
        loginStatus.set(401);

        Status login = connection.Login("tester", "wrong");

        assertEquals(StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode(), login.getErrorCode(),
                "the login request itself behaves as before");
    }
}
