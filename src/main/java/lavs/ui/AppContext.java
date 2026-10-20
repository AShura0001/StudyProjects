package lavs.ui;

import lavs.business.service.AuthService;
import lavs.ui.service.CustomerDirectory;
import lavs.ui.service.LoanRecords;
import lavs.ui.sync.SyncStatus;
import mainframe.client.Connection;

import java.io.IOException;
import java.util.Objects;

public final class AppContext implements AutoCloseable {
    private final Connection connection;
    private final AuthService auth;
    private final CustomerDirectory customers;
    private final LoanRecords loans;
    private final SyncStatus syncStatus = new SyncStatus();

    public AppContext(Connection connection, AuthService auth, CustomerDirectory customers, LoanRecords loans) {
        this.connection = Objects.requireNonNull(connection, "connection must not be null");
        this.auth = Objects.requireNonNull(auth, "auth must not be null");
        this.customers = Objects.requireNonNull(customers, "customers must not be null");
        this.loans = Objects.requireNonNull(loans, "loans must not be null");
    }

    public CustomerDirectory customers() {
        return customers;
    }

    public LoanRecords loans() {
        return loans;
    }

    /** What the sync indicator on every screen shows; feed it with a SyncStatusListener. */
    public SyncStatus syncStatus() {
        return syncStatus;
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
