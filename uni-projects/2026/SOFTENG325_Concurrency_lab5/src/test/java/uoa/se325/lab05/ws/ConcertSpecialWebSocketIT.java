package uoa.se325.lab05.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;
import uoa.se325.lab05.controller.ConcertTestUtils;
import uoa.se325.lab05.dto.ConcertSpecialNotification;

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ConcertSpecialWebSocketIT {

    // This will be set to the port our app is listening on.
    @LocalServerPort
    private int port;

    @Autowired
    private ConcertTestUtils testUtils;

    private ConcertTestUtils.Ids ids;


    // These three are our STOMP / WebSocket client.
    private static WebSocketStompClient stompClient;
    private StompSession stompSession;

    /**
     * Creates our STOMP client. We only need to do this once, before we run any tests.
     */
    @BeforeAll
    public static void createStompClient() {
        stompClient = new WebSocketStompClient(new SockJsClient(
                List.of(new WebSocketTransport(new StandardWebSocketClient()))
        ));

        // Allow the client to parse JSON, including Java date libraries.
        // This functionality isn't enabled by default for this client.
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(mapper);
        stompClient.setMessageConverter(converter);
    }

    @BeforeEach
    public void createTestData() {
        this.ids = testUtils.setUp();
    }

    /**
     * Creates a new STOMP / WebSocket session for each test.
     */
    @BeforeEach
    public void createStompSession() throws Exception {
        // Connect to our server
        // The URL path /ws is coming from WebSocketConfig line 21
        var futureSession = stompClient.connectAsync(
                "ws://localhost:" + port + "/ws",
                new StompSessionHandlerAdapter() {
                    @Override
                    public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
                        System.err.println("WebSocket error: " + exception.getMessage());
                        exception.printStackTrace();
                    }

                    @Override
                    public void handleTransportError(StompSession session, Throwable exception) {
                        System.err.println("Transport error: " + exception.getMessage());
                        exception.printStackTrace();
                    }
                }

        );
        stompSession = futureSession.join();
    }

    /**
     * Disconnect from the STOMP session after each test.
     */
    @AfterEach
    public void tearDown() {
        stompSession.disconnect();
    }

    @Test
    public void testReceiveSpecialNotifications() throws InterruptedException {

        // Somewhere to store our incoming notifications
        final var q = new LinkedBlockingQueue<ConcertSpecialNotification>();

        // Subscribe to /topic/specials; add any received messages to our
        // receivedMessages queue to examine later.
        stompSession.subscribe("/topic/specials", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ConcertSpecialNotification.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                System.out.println("Received WebSocket message: " + payload);
                q.add((ConcertSpecialNotification) payload);
            }
        });

        // PUT special ticket price; should be received into the queue above
        TestRestTemplate restTemplate = new TestRestTemplate();
        restTemplate.put("http://localhost:" + port + "/concerts/" + ids.ptxId + "/special-ticket-price", 50);

        ConcertSpecialNotification notification = q.poll(5, TimeUnit.SECONDS);
        assertNotNull(notification);
        assertEquals(ids.ptxId, notification.getConcertId());
        assertEquals("PTX: The World Tour", notification.getTitle());
        assertEquals(50, notification.getSpecialTicketPrice());
    }
}
