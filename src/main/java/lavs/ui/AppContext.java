package lavs.ui;

import lavs.business.service.AuthService;
import mainframe.client.Connection;

import java.io.IOException;
import java.util.Objects;

public final class AppContext implements AutoCloseable {
    private final Connection connection;
    private final AuthService auth;

    public AppContext(Connection connection, AuthService auth) {
        this.connection = Objects.requireNonNull(connection, "connection must not be null");
        this.auth = Objects.requireNonNull(auth, "auth must not be null");
    }

    public AuthService auth() {
        return auth;
    }

    /** Called by the login screen after a successful login. */
    public void onLoggedIn() {
        // Start AppServices here later, once a CustomerProfileSource exists.
    }

    /** Ends the session. Fast: it only clears the token, so it is safe on the JavaFX thread. */
    public void logout() {
        auth.logout();
    }

    @Override
    public void close() throws IOException {
        logout();
        connection.close();
    }
}
