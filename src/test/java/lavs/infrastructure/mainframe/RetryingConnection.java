package lavs.infrastructure.mainframe;

import mainframe.client.Connection;
import mainframe.client.MessageRequest;
import mainframe.client.MessageResponse;
import mainframe.client.Status;
import mainframe.client.StatusMessage;

import java.io.IOException;

/**
 * Test-only wrapper that retries requests which failed because the mainframe
 * could not be reached (e.g. the ngrok tunnel dropped out).
 *
 * Only read-only (query) requests are retried. Retrying an add or update is not
 * safe: if the request reached the server but the reply was lost, a retry would
 * create a duplicate record. Blips on those are left to Surefire's rerun.
 */
public class RetryingConnection implements Connection {

    private static final int NETWORK_FAILURE_CODE =
            StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode();

    private final Connection delegate;
    private final int maxAttempts;
    private final long initialDelayMillis;

    public RetryingConnection(Connection delegate, int maxAttempts, long initialDelayMillis) {
        this.delegate = delegate;
        this.maxAttempts = maxAttempts;
        this.initialDelayMillis = initialDelayMillis;
    }

    @Override
    public MessageResponse send(MessageRequest request) {
        if (!MessageRequest.QUERY.equals(request.getMessageType())) {
            return delegate.send(request);
        }

        MessageResponse response = delegate.send(request);
        long delay = initialDelayMillis;
        for (int attempt = 2; attempt <= maxAttempts && isNetworkFailure(response); attempt++) {
            sleep(delay);
            delay *= 2;
            response = delegate.send(request);
        }
        return response;
    }

    static boolean isNetworkFailure(MessageResponse response) {
        return isNetworkFailure(response.getStatus());
    }

    static boolean isNetworkFailure(Status status) {
        return !status.getWasSuccessful()
                && Integer.valueOf(NETWORK_FAILURE_CODE).equals(status.getErrorCode());
    }

    static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to retry", e);
        }
    }

    @Override
    public boolean isConnected() {
        return delegate.isConnected();
    }

    @Override
    public Status Login(String username, String password) throws IOException {
        return delegate.Login(username, password);
    }

    @Override
    public void Logout() {
        delegate.Logout();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
