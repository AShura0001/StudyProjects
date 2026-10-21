package lavs.ui;

import lavs.app.AppServices;
import lavs.business.service.AuthService;
import lavs.business.service.BasicLoanValidator;
import lavs.business.service.SyncListener;
import lavs.ui.service.CustomerDirectory;
import lavs.ui.service.LoanRecords;
import lavs.ui.sync.SyncState;
import lavs.ui.sync.SyncStatus;
import lavs.ui.sync.SyncStatusListener;
import mainframe.client.Connection;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

public final class AppContext implements AutoCloseable {
    private final Connection connection;
    private final AuthService auth;
    private final CustomerDirectory customers;
    private final LoanRecords loans;
    private final SyncStatus syncStatus = new SyncStatus();
    private volatile boolean syncPreview;
    private volatile AppServices services;

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

    /**
     * Shows a fixed state on the sync indicator instead of what the background
     * sync reports, so a state can be looked at without making it happen.
     */
    public void previewSync(SyncState state, int count) {
        syncPreview = true;
        syncStatus.set(state, count);
    }

    /** The running services, or empty when nobody is logged in. */
    public Optional<AppServices> services() {
        return Optional.ofNullable(services);
    }

    /**
     * Called by the login screen after a successful login. Starts the services,
     * which also sends any drafts left from last time. Fast: no mainframe call is
     * made here, so it is safe on the JavaFX thread.
     */
    public synchronized void onLoggedIn() {
        stopServices();
        SyncListener listener = syncPreview ? new SyncListener() { } : new SyncStatusListener(syncStatus);
        services = AppServices.forMainframe(connection, AppServices.defaultDraftsFolder(),
                new BasicLoanValidator(), listener);
    }

    /**
     * Ends the session and stops the background sync. Fast, so it is safe on the
     * JavaFX thread. Drafts not yet sent stay on disk and go at the next login.
     */
    public synchronized void logout() {
        stopServices();
        auth.logout();
    }

    private void stopServices() {
        if (services != null) {
            services.close();
            services = null;
        }
    }

    @Override
    public void close() throws IOException {
        logout();
        connection.close();
    }
}
