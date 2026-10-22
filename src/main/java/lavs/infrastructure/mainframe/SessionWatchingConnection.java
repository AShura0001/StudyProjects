package lavs.infrastructure.mainframe;

import mainframe.client.Connection;
import mainframe.client.MessageRequest;
import mainframe.client.MessageResponse;
import mainframe.client.Status;
import mainframe.client.StatusMessage;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Wraps the connection to the mainframe and notices when the mainframe stops accepting
 * the login session (it answers {@link StatusMessage#SESSION_EXPIRED}). It then tells the
 * application once, so staff are sent back to log in instead of every screen failing with
 * "could not reach the mainframe". Logging in again re-arms it.
 */
public class SessionWatchingConnection implements Connection {

    private final Connection inner;
    private final AtomicBoolean reported = new AtomicBoolean(false);
    private volatile Runnable onSessionLost = () -> { };

    public SessionWatchingConnection(Connection inner) {
        this.inner = Objects.requireNonNull(inner, "inner must not be null");
    }

    /**
     * Sets what happens when the session is lost. It runs on whichever thread made the
     * request, at most once until the next successful login.
     */
    public void onSessionLost(Runnable action) {
        this.onSessionLost = Objects.requireNonNull(action, "action must not be null");
    }

    @Override
    public MessageResponse send(MessageRequest request) {
        MessageResponse response = inner.send(request);
        if (isSessionLost(response.getStatus()) && reported.compareAndSet(false, true)) {
            onSessionLost.run();
        }
        return response;
    }

    @Override
    public Status Login(String username, String password) throws IOException {
        Status status = inner.Login(username, password);
        if (status.getWasSuccessful()) {
            reported.set(false);
        }
        return status;
    }

    @Override
    public void Logout() {
        inner.Logout();
    }

    @Override
    public boolean isConnected() {
        return inner.isConnected();
    }

    @Override
    public void close() throws IOException {
        inner.close();
    }

    private static boolean isSessionLost(Status status) {
        return status != null && Objects.equals(status.getErrorCode(), StatusMessage.SESSION_EXPIRED.getCode());
    }
}
