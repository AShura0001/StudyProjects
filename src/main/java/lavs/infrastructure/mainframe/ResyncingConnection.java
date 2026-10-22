package lavs.infrastructure.mainframe;

import mainframe.client.Connection;
import mainframe.client.MessageRequest;
import mainframe.client.MessageResponse;
import mainframe.client.Status;
import mainframe.client.StatusMessage;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;

/**
 * Keeps the request numbers in step, so a login can be shared and nothing races.
 *
 * <p>The mainframe numbers every request of an account in order, and answers
 * "Invalid transaction number" to a request that carries the wrong one. Two things
 * put the numbers out of step. The first is two threads of this application sending
 * at the same moment, since the provided connection counts without any locking. The
 * second is two people using the same account: each one counts from the number they
 * logged in at, so whoever is behind is refused from then on.
 *
 * <p>This wrapper does two things. It lets only one request travel at a time. And
 * when the mainframe says the number is wrong, it logs in again, which hands back
 * the current number, and sends the same request once more. That is safe, because
 * the mainframe turns a wrongly numbered request away before it does anything with it,
 * so nothing is done twice.
 *
 * <p>To log in again it keeps the login name and password in memory until
 * {@link #Logout()}. They are never written anywhere.
 */
public class ResyncingConnection implements Connection {

    private static final String OUT_OF_STEP_TEXT = "invalid transaction number";

    private final Connection inner;
    private String userName;
    private String password;

    /**
     * Wraps a connection.
     *
     * @param inner the real connection
     * @throws NullPointerException if the connection is null
     */
    public ResyncingConnection(Connection inner) {
        this.inner = Objects.requireNonNull(inner, "inner must not be null");
    }

    /**
     * Sends a request, on its own, and once more after logging in again if the
     * mainframe says the request number was wrong.
     */
    @Override
    public synchronized MessageResponse send(MessageRequest request) {
        MessageResponse response = inner.send(request);
        if (userName == null || !isOutOfStep(response.getStatus())) {
            return response;
        }
        try {
            if (!inner.Login(userName, password).getWasSuccessful()) {
                return response;
            }
        } catch (IOException e) {
            return response;
        }
        return inner.send(request);
    }

    @Override
    public synchronized Status Login(String username, String password) throws IOException {
        Status status = inner.Login(username, password);
        if (status.getWasSuccessful()) {
            this.userName = username;
            this.password = password;
        }
        return status;
    }

    /** Logs out and forgets the login name and password. */
    @Override
    public synchronized void Logout() {
        userName = null;
        password = null;
        inner.Logout();
    }

    /** Asking whether the mainframe is there carries no request number, so it needs no turn. */
    @Override
    public boolean isConnected() {
        return inner.isConnected();
    }

    @Override
    public void close() throws IOException {
        inner.close();
    }

    private static boolean isOutOfStep(Status status) {
        if (status == null || status.getWasSuccessful()
                || !Objects.equals(status.getErrorCode(), StatusMessage.SERVER_FAILURE.getCode())) {
            return false;
        }
        String message = status.getErrorMessage();
        return message != null && message.toLowerCase(Locale.ROOT).contains(OUT_OF_STEP_TEXT);
    }
}
