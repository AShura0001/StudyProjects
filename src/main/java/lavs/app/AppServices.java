package lavs.app;

import lavs.business.repository.CustomerGateway;
import lavs.business.repository.CustomerRelationshipGateway;
import lavs.business.repository.DraftRepository;
import lavs.business.repository.DraftUploadGateway;
import lavs.business.repository.LoanGateway;
import lavs.business.service.BackgroundSync;
import lavs.business.service.CustomerProfileSource;
import lavs.business.service.CustomerRelationshipService;
import lavs.business.service.DraftSyncService;
import lavs.business.service.LoanService;
import lavs.business.service.LoanValidator;
import lavs.business.service.RetryPolicy;
import lavs.business.service.Scheduler;
import lavs.business.service.SyncListener;
import lavs.data.JsonDraftRepository;
import lavs.infrastructure.concurrent.ExecutorScheduler;
import lavs.infrastructure.mainframe.CustomerAdapter;
import lavs.infrastructure.mainframe.CustomerRelationshipAdapter;
import lavs.infrastructure.mainframe.LoanAdapter;
import lavs.infrastructure.mainframe.MainframeDraftUploader;
import mainframe.client.Connection;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Creates the services of the application and joins them together, in one place.
 *
 * <p>The screens ask this class for what they need and never create a service
 * themselves. Drafts are kept in a folder on disk, so they survive the application
 * being closed. Starting also asks for a pass over the saved drafts, so anything left
 * from last time is sent, and an upload that was cut short is marked as uncertain
 * instead of being sent again.
 *
 * <p>Call {@link #close()} when the application ends, to stop the background work.
 */
public final class AppServices implements AutoCloseable {

    private static final String APPLICATION_FOLDER = ".lavs";
    private static final String DRAFTS_FOLDER = "drafts";

    /**
     * The ways the application talks to the mainframe.
     *
     * @param upload sends saved drafts
     * @param loans asks for the figures the mainframe works out for a loan
     * @param customers looks customers up
     * @param relationships saves relationships between customers
     */
    public record Gateways(DraftUploadGateway upload, LoanGateway loans, CustomerGateway customers,
                           CustomerRelationshipGateway relationships) {

        /**
         * Gathers the gateways.
         *
         * @throws NullPointerException if any of them is null
         */
        public Gateways {
            Objects.requireNonNull(upload, "upload must not be null");
            Objects.requireNonNull(loans, "loans must not be null");
            Objects.requireNonNull(customers, "customers must not be null");
            Objects.requireNonNull(relationships, "relationships must not be null");
        }
    }

    private final BackgroundSync sync;
    private final LoanService loans;
    private final CustomerRelationshipService relationships;

    private AppServices(BackgroundSync sync, LoanService loans, CustomerRelationshipService relationships) {
        this.sync = sync;
        this.loans = loans;
        this.relationships = relationships;
    }

    /**
     * Builds the gateways that talk to the real mainframe through one connection.
     *
     * @param connection the connection to the mainframe, already logged in
     * @return the gateways
     * @throws NullPointerException if the connection is null
     */
    public static Gateways mainframeGateways(Connection connection) {
        Objects.requireNonNull(connection, "connection must not be null");
        return new Gateways(new MainframeDraftUploader(connection), new LoanAdapter(connection),
                new CustomerAdapter(connection), new CustomerRelationshipAdapter(connection));
    }

    /**
     * Starts the services against the real mainframe, with a real background worker.
     *
     * @param connection the connection to the mainframe, already logged in
     * @param draftsFolder where drafts are kept; created when the first draft is saved
     * @param profiles how the facts about a customer are found
     * @param validator how the values of a loan are checked
     * @param listener told what the background sync is doing, on the sync thread
     * @return the running services
     * @throws NullPointerException if any argument is null
     */
    public static AppServices forMainframe(Connection connection, Path draftsFolder, CustomerProfileSource profiles,
                                           LoanValidator validator, SyncListener listener) {
        return start(draftsFolder, mainframeGateways(connection), profiles, validator, listener);
    }

    /**
     * Gets the folder drafts are kept in unless another one is chosen: a
     * {@code .lavs/drafts} folder inside the user's home folder.
     *
     * @return the folder
     */
    public static Path defaultDraftsFolder() {
        return Path.of(System.getProperty("user.home"), APPLICATION_FOLDER, DRAFTS_FOLDER);
    }

    /**
     * Starts the services, using a real background worker and the standard retry delays.
     *
     * @param draftsFolder where drafts are kept; created when the first draft is saved
     * @param gateways the ways of talking to the mainframe
     * @param profiles how the facts about a customer are found
     * @param validator how the values of a loan are checked
     * @param listener told what the background sync is doing, on the sync thread
     * @return the running services
     * @throws NullPointerException if any argument is null
     */
    public static AppServices start(Path draftsFolder, Gateways gateways, CustomerProfileSource profiles,
                                    LoanValidator validator, SyncListener listener) {
        return start(draftsFolder, gateways, profiles, validator, listener,
                new ExecutorScheduler(), RetryPolicy.standard());
    }

    /**
     * Starts the services with a chosen worker and retry delays, so tests can control time.
     *
     * @param draftsFolder where drafts are kept; created when the first draft is saved
     * @param gateways the ways of talking to the mainframe
     * @param profiles how the facts about a customer are found
     * @param validator how the values of a loan are checked
     * @param listener told what the background sync is doing, on the sync thread
     * @param scheduler the single worker that runs the sync
     * @param retryPolicy how long to wait between attempts after a lost connection
     * @return the running services
     * @throws NullPointerException if any argument is null
     */
    public static AppServices start(Path draftsFolder, Gateways gateways, CustomerProfileSource profiles,
                                    LoanValidator validator, SyncListener listener,
                                    Scheduler scheduler, RetryPolicy retryPolicy) {
        Objects.requireNonNull(draftsFolder, "draftsFolder must not be null");
        Objects.requireNonNull(gateways, "gateways must not be null");
        Objects.requireNonNull(profiles, "profiles must not be null");
        Objects.requireNonNull(validator, "validator must not be null");
        Objects.requireNonNull(listener, "listener must not be null");
        DraftRepository repository = new JsonDraftRepository(draftsFolder);
        BackgroundSync sync = new BackgroundSync(new DraftSyncService(repository, gateways.upload()),
                Objects.requireNonNull(scheduler, "scheduler must not be null"),
                Objects.requireNonNull(retryPolicy, "retryPolicy must not be null"), listener);
        LoanService loans = new LoanService(gateways.loans(), profiles, validator, sync, repository);
        CustomerRelationshipService relationships =
                new CustomerRelationshipService(gateways.relationships(), gateways.customers());
        sync.requestSync();
        return new AppServices(sync, loans, relationships);
    }

    /**
     * Gets the background sync, for the "not yet sent" notice and for settling uncertain drafts.
     *
     * @return the background sync
     */
    public BackgroundSync sync() {
        return sync;
    }

    /**
     * Gets the loan service.
     *
     * @return the loan service
     */
    public LoanService loans() {
        return loans;
    }

    /**
     * Gets the relationship service.
     *
     * @return the relationship service
     */
    public CustomerRelationshipService relationships() {
        return relationships;
    }

    /** Stops the background work. Drafts already saved stay on disk. */
    @Override
    public void close() {
        sync.stop();
    }
}
