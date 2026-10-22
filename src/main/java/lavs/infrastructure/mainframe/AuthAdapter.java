package lavs.infrastructure.mainframe;

import lavs.business.repository.InvalidCredentialsException;
import lavs.business.repository.MainframeUnavailableException;
import mainframe.client.Connection;
import mainframe.client.Status;
import mainframe.client.StatusMessage;

import java.io.IOException;
import java.util.Objects;

/**
 * Logs staff in to the mainframe and out again, for {@link lavs.business.repository.AuthGateway}.
 */
public class AuthAdapter implements lavs.business.repository.AuthGateway {
    public static final String UNAVAILABLE_MESSAGE = "The mainframe could not be reached.";
    public static final String REFUSED_MESSAGE = "The mainframe refused the username or password.";

    private final Connection connection;

    public AuthAdapter(Connection connection) {
        this.connection = Objects.requireNonNull(connection, "connection must not be null");
    }

    @Override
    public String login(String username, String password) {
        Status status;
        try {
            status = connection.Login(username, password);
        } catch (IOException | RuntimeException e) {
            throw new MainframeUnavailableException(UNAVAILABLE_MESSAGE, e);
        }
        if (status.getWasSuccessful()) {
            return username;
        }
        if (Objects.equals(status.getErrorCode(), StatusMessage.SERVER_FAILURE.getCode())) {
            String reason = status.getErrorMessage();
            throw new InvalidCredentialsException(reason == null || reason.isBlank() ? REFUSED_MESSAGE : reason);
        }
        throw new MainframeUnavailableException(UNAVAILABLE_MESSAGE);
    }

    @Override
    public void logout() {
        connection.Logout();
    }
}
