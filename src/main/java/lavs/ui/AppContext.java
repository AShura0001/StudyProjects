package lavs.ui;

import lavs.app.AppServices;
import lavs.business.service.AuthService;
import lavs.business.service.BasicLoanValidator;
import lavs.business.service.CustomerService;
import lavs.business.service.LoanAccountService;
import lavs.business.service.LoanService;
import lavs.business.service.ReferenceDataService;
import lavs.business.service.SyncListener;
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
    private final SyncStatus syncStatus = new SyncStatus();
    private volatile boolean syncPreview;
    private volatile AppServices services;

    public AppContext(Connection connection, AuthService auth) {
        this.connection = Objects.requireNonNull(connection, "connection must not be null");
        this.auth = Objects.requireNonNull(auth, "auth must not be null");
    }

    /** Customers on the mainframe, and new customers saved as drafts. Available once logged in. */
    public CustomerService customers() {
        return started().customers().orElseThrow(() -> new IllegalStateException("customer service not built"));
    }

    /** Starting new loans, which are saved as drafts and sent in the background. Available once logged in. */
    public LoanService loans() {
        return started().loans();
    }

    /** The saved drafts and their background sending. Available once logged in. */
    public lavs.business.service.BackgroundSync drafts() {
        return started().sync();
    }

    /** Loans already on the mainframe. Available once logged in. */
    public LoanAccountService loanAccounts() {
        return started().loanAccounts().orElseThrow(() -> new IllegalStateException("loan account service not built"));
    }

    /** The mainframe's constant values, such as address types, for the drop-down lists. Available once logged in. */
    public ReferenceDataService referenceData() {
        return started().referenceData().orElseThrow(() -> new IllegalStateException("reference data service not built"));
    }

    private AppServices started() {
        AppServices running = services;
        if (running == null) {
            throw new IllegalStateException("Log in first: the services start after login.");
        }
        return running;
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
        String employee = auth.currentSession()
                .orElseThrow(() -> new IllegalStateException("onLoggedIn called without a session"))
                .username();
        services = AppServices.forMainframe(connection, AppServices.draftsFolderFor(employee),
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
