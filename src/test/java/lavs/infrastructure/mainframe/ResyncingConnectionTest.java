package lavs.infrastructure.mainframe;

import mainframe.client.Connection;
import mainframe.client.MessageRequest;
import mainframe.client.MessageResponse;
import mainframe.client.Status;
import mainframe.client.StatusMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The request numbers of the mainframe: one request at a time, and a quiet re-login when the count is wrong. */
class ResyncingConnectionTest {

    private static final MessageResponse OK = new MessageResponse(new Status(1), new HashMap<>());
    private static final MessageResponse OUT_OF_STEP = refused("Invalid transaction number");

    private Connection inner;
    private ResyncingConnection connection;
    private final MessageRequest request = new MessageRequest(MessageRequest.QUERY, "customer.search");

    private static MessageResponse refused(String message) {
        return new MessageResponse(new Status(StatusMessage.SERVER_FAILURE.getCode(), message, 1), new HashMap<>());
    }

    @BeforeEach
    void setUp() throws IOException {
        inner = mock(Connection.class);
        connection = new ResyncingConnection(inner);
        when(inner.Login(anyString(), anyString())).thenReturn(new Status(1));
    }

    @Test
    void send_whenAllIsWell_passesTheReplyThroughAndDoesNotLogInAgain() throws IOException {
        when(inner.send(request)).thenReturn(OK);
        connection.Login("Group_2", "bug");

        assertSame(OK, connection.send(request));

        verify(inner, times(1)).Login(anyString(), anyString());
    }

    @Test
    void send_whenTheNumberIsOutOfStep_logsInAgainAndSendsTheSameRequestOnce() throws IOException {
        when(inner.send(request)).thenReturn(OUT_OF_STEP, OK);
        connection.Login("Group_2", "bug");

        MessageResponse reply = connection.send(request);

        assertSame(OK, reply);
        InOrder order = inOrder(inner);
        order.verify(inner).Login("Group_2", "bug");
        order.verify(inner).send(request);
        order.verify(inner).Login("Group_2", "bug");
        order.verify(inner).send(request);
    }

    @Test
    void send_whenTheMessageHasOtherWordingAroundIt_stillRecognisesAnOutOfStepNumber() throws IOException {
        when(inner.send(request)).thenReturn(refused("Error: INVALID TRANSACTION NUMBER 41"), OK);
        connection.Login("Group_2", "bug");

        assertSame(OK, connection.send(request));
    }

    @Test
    void send_triesOnlyOnce_andGivesTheMainframesAnswerIfItIsStillWrong() throws IOException {
        when(inner.send(request)).thenReturn(OUT_OF_STEP);
        connection.Login("Group_2", "bug");

        assertSame(OUT_OF_STEP, connection.send(request));

        verify(inner, times(2)).send(request);
        verify(inner, times(2)).Login(anyString(), anyString());
    }

    @Test
    void send_aDifferentRefusal_isLeftAlone() throws IOException {
        MessageResponse forbidden = refused("Forbidden: you do not have permission to execute this query.");
        when(inner.send(request)).thenReturn(forbidden);
        connection.Login("Group_2", "bug");

        assertSame(forbidden, connection.send(request));

        verify(inner, times(1)).send(request);
        verify(inner, times(1)).Login(anyString(), anyString());
    }

    @Test
    void send_aRefusalWithNoMessage_isLeftAlone() throws IOException {
        MessageResponse bare = refused(null);
        when(inner.send(request)).thenReturn(bare);
        connection.Login("Group_2", "bug");

        assertSame(bare, connection.send(request));

        verify(inner, times(1)).send(request);
    }

    @Test
    void send_aFailureThatIsNotARefusal_isLeftAlone() throws IOException {
        MessageResponse unreachable = StatusMessage.NETWORK_FAILURE_UNAVAILABLE.generateMessageResponse(1);
        when(inner.send(request)).thenReturn(unreachable);
        connection.Login("Group_2", "bug");

        assertSame(unreachable, connection.send(request));

        verify(inner, times(1)).send(request);
    }

    @Test
    void send_beforeAnyLogin_cannotLogInAndReturnsTheReply() throws IOException {
        when(inner.send(request)).thenReturn(OUT_OF_STEP);

        assertSame(OUT_OF_STEP, connection.send(request));

        verify(inner, never()).Login(anyString(), anyString());
    }

    @Test
    void send_afterLogout_hasForgottenTheLogin() throws IOException {
        when(inner.send(request)).thenReturn(OUT_OF_STEP);
        connection.Login("Group_2", "bug");
        connection.Logout();

        assertSame(OUT_OF_STEP, connection.send(request));

        verify(inner).Logout();
        verify(inner, times(1)).Login(anyString(), anyString());
    }

    @Test
    void login_thatFails_isNotRemembered() throws IOException {
        when(inner.Login(anyString(), anyString())).thenReturn(
                new Status(StatusMessage.SERVER_FAILURE.getCode(), "Wrong password", 1));
        when(inner.send(request)).thenReturn(OUT_OF_STEP);

        assertFalse(connection.Login("Group_2", "wrong").getWasSuccessful());

        assertSame(OUT_OF_STEP, connection.send(request));
        verify(inner, times(1)).Login(anyString(), anyString());
    }

    @Test
    void send_whenLoggingInAgainIsRefused_givesTheOriginalAnswer() throws IOException {
        when(inner.send(request)).thenReturn(OUT_OF_STEP);
        connection.Login("Group_2", "bug");
        when(inner.Login(anyString(), anyString())).thenReturn(
                new Status(StatusMessage.SERVER_FAILURE.getCode(), "Wrong password", 1));

        assertSame(OUT_OF_STEP, connection.send(request));

        verify(inner, times(1)).send(request);
    }

    @Test
    void send_whenLoggingInAgainFailsToReachTheMainframe_givesTheOriginalAnswer() throws IOException {
        when(inner.send(request)).thenReturn(OUT_OF_STEP);
        connection.Login("Group_2", "bug");
        when(inner.Login(anyString(), anyString())).thenThrow(new IOException("connection reset"));

        assertSame(OUT_OF_STEP, connection.send(request));

        verify(inner, times(1)).send(request);
    }

    @Test
    void send_fromSeveralThreads_letsOnlyOneRequestTravelAtATime() throws Exception {
        AtomicInteger inside = new AtomicInteger();
        AtomicInteger mostAtOnce = new AtomicInteger();
        when(inner.send(any())).thenAnswer(call -> {
            int now = inside.incrementAndGet();
            mostAtOnce.accumulateAndGet(now, Math::max);
            Thread.sleep(1);
            inside.decrementAndGet();
            return OK;
        });
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<?>> done = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            done.add(pool.submit(() -> {
                go.await();
                for (int i = 0; i < 25; i++) {
                    connection.send(request);
                }
                return null;
            }));
        }
        go.countDown();
        for (Future<?> f : done) {
            f.get(30, TimeUnit.SECONDS);
        }
        pool.shutdownNow();

        assertEquals(1, mostAtOnce.get());
        verify(inner, times(threads * 25)).send(any());
    }

    @Test
    void isConnected_asksTheRealConnection() {
        when(inner.isConnected()).thenReturn(true, false);

        assertTrue(connection.isConnected());
        assertFalse(connection.isConnected());
    }

    @Test
    void close_closesTheRealConnection() throws IOException {
        connection.close();

        verify(inner).close();
    }

    @Test
    void newConnection_withoutARealConnection_isRejected() {
        assertThrows(NullPointerException.class, () -> new ResyncingConnection(null));
    }
}
