package lavs.infrastructure.mainframe;

import mainframe.client.Connection;
import mainframe.client.MessageRequest;
import mainframe.client.MessageResponse;
import mainframe.client.Status;
import mainframe.client.StatusMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A stand-in for the connection to the mainframe, for tests only.
 *
 * <p>Replies are queued in advance and handed out in order, every request is
 * recorded, and whether the connection "is connected" can be switched. A request
 * with no reply queued fails the test, so a test cannot pass by accident when the
 * code sends more than it should.
 */
public class ScriptedConnection implements Connection {

    private final Queue<MessageResponse> replies = new ConcurrentLinkedQueue<>();
    private final List<MessageRequest> requests = new CopyOnWriteArrayList<>();
    private volatile boolean connected = true;
    private final AtomicInteger pings = new AtomicInteger();

    /**
     * Queues a successful reply with the given values.
     *
     * @param keyValuePairs the values as key, value, key, value...
     */
    public void replySuccess(String... keyValuePairs) {
        replies.add(new MessageResponse(new Status(1), values(keyValuePairs)));
    }

    /**
     * Queues a reply that says the mainframe refused the request.
     *
     * @param message the reason the mainframe gave
     */
    public void replyRefused(String message) {
        replies.add(new MessageResponse(
                new Status(StatusMessage.SERVER_FAILURE.getCode(), message, 1), new HashMap<>()));
    }

    /** Queues a reply that says the mainframe could not be reached. */
    public void replyUnreachable() {
        replies.add(StatusMessage.NETWORK_FAILURE_UNAVAILABLE.generateMessageResponse(1));
    }

    /** Queues a reply that says the answer could not be read. */
    public void replyUnreadable() {
        replies.add(StatusMessage.UNABLE_TO_PARSE.generateMessageResponse(1));
    }

    /** Queues a reply that says the mainframe no longer accepts this login session. */
    public void replySessionExpired() {
        replies.add(StatusMessage.SESSION_EXPIRED.generateMessageResponse(1));
    }

    /**
     * Sets what the connection answers when asked whether it is connected.
     *
     * @param connected true if the mainframe can be reached
     */
    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    /**
     * Gets every request sent so far, oldest first.
     *
     * @return the requests
     */
    public List<MessageRequest> requests() {
        return List.copyOf(requests);
    }

    /**
     * Gets how many times the connection was asked whether it is connected.
     *
     * @return the number of pings
     */
    public int pings() {
        return pings.get();
    }

    private static HashMap<String, String> values(String... keyValuePairs) {
        if (keyValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("values must be key and value pairs");
        }
        HashMap<String, String> values = new HashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            values.put(keyValuePairs[i], keyValuePairs[i + 1]);
        }
        return values;
    }

    @Override
    public MessageResponse send(MessageRequest request) {
        requests.add(request);
        MessageResponse reply = replies.poll();
        if (reply == null) {
            throw new AssertionError("no reply was queued for " + request.getRequestType());
        }
        return reply;
    }

    @Override
    public boolean isConnected() {
        pings.incrementAndGet();
        return connected;
    }

    @Override
    public void close() {
    }

    @Override
    public Status Login(String username, String password) {
        return new Status(1);
    }

    @Override
    public void Logout() {
    }
}
