package lavs.demo;

import mainframe.client.Connection;
import mainframe.client.MessageRequest;
import mainframe.client.MessageResponse;
import mainframe.client.Status;
import mainframe.client.StatusMessage;

import java.io.IOException;
import java.util.Objects;

/**
 * A connection that can be told to lose its signal, for showing what the
 * application does when the connection drops.
 *
 * <p>While it is offline it says the mainframe cannot be reached and sends
 * nothing at all. When it is online it simply passes everything on to the real
 * connection.
 */
public class SwitchableConnection implements Connection {

    private final Connection delegate;
    private volatile boolean offline;

    /**
     * Wraps a connection.
     *
     * @param delegate the real connection
     * @throws NullPointerException if the connection is null
     */
    public SwitchableConnection(Connection delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    /** Makes the mainframe look unreachable, and stops anything from being sent. */
    public void goOffline() {
        offline = true;
    }

    /** Makes the real connection available again. */
    public void goOnline() {
        offline = false;
    }

    /**
     * Tells whether the signal is switched off.
     *
     * @return true while offline
     */
    public boolean isOffline() {
        return offline;
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    @Override
    public boolean isConnected() {
        return !offline && delegate.isConnected();
    }

    @Override
    public MessageResponse send(MessageRequest request) {
        if (offline) {
            return StatusMessage.NETWORK_FAILURE_UNAVAILABLE.generateMessageResponse(0);
        }
        return delegate.send(request);
    }

    @Override
    public Status Login(String username, String password) throws IOException {
        return delegate.Login(username, password);
    }

    @Override
    public void Logout() {
        delegate.Logout();
    }
}
