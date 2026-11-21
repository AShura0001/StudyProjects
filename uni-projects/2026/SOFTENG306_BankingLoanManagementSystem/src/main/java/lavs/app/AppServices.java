package lavs.app;

import lavs.business.repository.CustomerGateway;
import lavs.business.repository.CustomerRelationshipGateway;
import lavs.business.repository.DraftRepository;
import lavs.business.repository.DraftUploadGateway;
import lavs.business.repository.LoanGateway;
import lavs.business.service.BackgroundSync;
import lavs.business.service.CustomerProfileSource;
import lavs.business.service.CustomerRelationshipService;
import lavs.business.service.LoanAccountService;
import lavs.business.service.DocumentService;
import lavs.business.service.LoanNoteService;
import lavs.business.service.CustomerService;
import lavs.business.service.DraftSyncService;
import lavs.business.service.LoanService;
import lavs.business.service.LoanValidator;
import lavs.business.service.ReferenceDataService;
import lavs.business.service.RetryPolicy;
import lavs.business.service.Scheduler;
import lavs.business.service.SyncListener;
import lavs.business.service.ValidationService;
import lavs.data.JsonDraftRepository;
import lavs.infrastructure.concurrent.ExecutorScheduler;
import lavs.infrastructure.mainframe.CustomerAdapter;
import lavs.infrastructure.mainframe.CustomerDocumentAdapter;
import lavs.infrastructure.mainframe.CustomerAddressAdapter;
import lavs.infrastructure.mainframe.CustomerEmailAdapter;
import lavs.infrastructure.mainframe.CustomerEmployerAdapter;
import lavs.infrastructure.mainframe.CustomerNoteAdapter;
import lavs.infrastructure.mainframe.LoanCoBorrowerAdapter;
import lavs.infrastructure.mainframe.LoanDocumentAdapter;
import lavs.infrastructure.mainframe.LoanNoteAdapter;
import lavs.infrastructure.mainframe.LoanPaymentAdapter;
import lavs.infrastructure.mainframe.CustomerPhoneNumberAdapter;
import lavs.infrastructure.mainframe.CustomerRelationshipAdapter;
import lavs.infrastructure.mainframe.LoanAdapter;
import lavs.infrastructure.mainframe.MainframeDraftUploader;
import lavs.infrastructure.mainframe.ReferenceDataAdapter;
import mainframe.client.Connection;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

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
    private final CustomerService customers;
    private LoanAccountService loanAccounts;
    private LoanNoteService loanNotes;
    private DocumentService documents;
    private ReferenceDataService referenceData;

    private AppServices(BackgroundSync sync, LoanService loans, CustomerRelationshipService relationships,
                        CustomerService customers) {
        this.sync = sync;
        this.loans = loans;
        this.relationships = relationships;
        this.customers = customers;
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
     * Starts the services against the real mainframe, with a customer service that
     * also tells the loan service what it needs to know about a customer. The
     * customer service shares the drafts and background sync of the other services.
     *
     * @param connection the connection to the mainframe, already logged in
     * @param draftsFolder where drafts are kept; created when the first draft is saved
     * @param validator how the values of a loan are checked
     * @param listener told what the background sync is doing, on the sync thread
     * @return the running services
     * @throws NullPointerException if any argument is null
     */
    public static AppServices forMainframe(Connection connection, Path draftsFolder, LoanValidator validator,
                                           SyncListener listener) {
        Gateways gateways = mainframeGateways(connection);
        AppServices services = start(draftsFolder, gateways, (repository, sync) -> new CustomerService(gateways.customers(),
                        new CustomerAddressAdapter(connection), new CustomerPhoneNumberAdapter(connection),
                        new CustomerEmailAdapter(connection), new CustomerEmployerAdapter(connection),
                        new CustomerNoteAdapter(connection), new ValidationService(), repository, sync),
                validator, listener, new ExecutorScheduler(), RetryPolicy.standard());
        services.loanAccounts = new LoanAccountService(gateways.loans(), new LoanCoBorrowerAdapter(connection),
                new LoanPaymentAdapter(connection), validator, services.customers);
        services.loanNotes = new LoanNoteService(new LoanNoteAdapter(connection));
        services.documents = new DocumentService(new CustomerDocumentAdapter(connection),
                new LoanDocumentAdapter(connection));
        services.referenceData = new ReferenceDataService(new ReferenceDataAdapter(connection));
        return services;
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
     * Gets the folder an employee's drafts are kept in: one folder each inside
     * {@link #defaultDraftsFolder()}, so drafts saved by one employee are only ever
     * sent while that employee is logged in.
     *
     * @param employee the login name of the person logged in
     * @return the folder
     * @throws IllegalArgumentException if the name is blank
     */
    public static Path draftsFolderFor(String employee) {
        Objects.requireNonNull(employee, "employee must not be null");
        String safe = employee.strip().toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
        if (safe.isEmpty() || safe.chars().allMatch(c -> c == '.')) {
            throw new IllegalArgumentException("employee must not be blank");
        }
        return defaultDraftsFolder().resolve(safe);
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
        Objects.requireNonNull(profiles, "profiles must not be null");
        return start(draftsFolder, gateways, (repository, sync) -> profiles, validator, listener,
                scheduler, retryPolicy);
    }

    /**
     * Starts the services, building the source of customer facts once the drafts
     * and background sync exist, because a customer service needs both.
     */
    private static AppServices start(Path draftsFolder, Gateways gateways,
                                     BiFunction<DraftRepository, BackgroundSync, CustomerProfileSource> profilesFactory,
                                     LoanValidator validator, SyncListener listener,
                                     Scheduler scheduler, RetryPolicy retryPolicy) {
        Objects.requireNonNull(draftsFolder, "draftsFolder must not be null");
        Objects.requireNonNull(gateways, "gateways must not be null");
        Objects.requireNonNull(validator, "validator must not be null");
        Objects.requireNonNull(listener, "listener must not be null");
        DraftRepository repository = new JsonDraftRepository(draftsFolder);
        BackgroundSync sync = new BackgroundSync(new DraftSyncService(repository, gateways.upload()),
                Objects.requireNonNull(scheduler, "scheduler must not be null"),
                Objects.requireNonNull(retryPolicy, "retryPolicy must not be null"), listener);
        CustomerProfileSource profiles = Objects.requireNonNull(profilesFactory.apply(repository, sync),
                "profiles must not be null");
        LoanService loans = new LoanService(gateways.loans(), profiles, validator, sync, repository);
        CustomerRelationshipService relationships =
                new CustomerRelationshipService(gateways.relationships(), gateways.customers());
        CustomerService customers = profiles instanceof CustomerService service ? service : null;
        sync.requestSync();
        return new AppServices(sync, loans, relationships, customers);
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

    /**
     * Gets the customer service, when these services built one.
     *
     * @return the customer service, or empty when the facts about customers come from elsewhere
     */
    public Optional<CustomerService> customers() {
        return Optional.ofNullable(customers);
    }

    /**
     * Gets the service for loans already on the mainframe. It is built by
     * {@link #forMainframe(Connection, Path, LoanValidator, SyncListener)}.
     *
     * @return the service, or empty when these services were started another way
     */
    public Optional<LoanAccountService> loanAccounts() {
        return Optional.ofNullable(loanAccounts);
    }

    /**
     * Gets the service for the notes on a loan, when these services were started against the mainframe.
     *
     * @return the loan note service, or empty when it was not built
     */
    public Optional<LoanNoteService> loanNotes() {
        return Optional.ofNullable(loanNotes);
    }

    /**
     * Gets the service for the documents recorded against customers and loans, when
     * these services were started against the mainframe.
     *
     * @return the document service, or empty when it was not built
     */
    public Optional<DocumentService> documents() {
        return Optional.ofNullable(documents);
    }

    /**
     * Gets the service for the mainframe's constant values, such as address types.
     * It is built by {@link #forMainframe(Connection, Path, LoanValidator, SyncListener)}.
     *
     * @return the service, or empty when these services were started another way
     */
    public Optional<ReferenceDataService> referenceData() {
        return Optional.ofNullable(referenceData);
    }

    /** Stops the background work. Drafts already saved stay on disk. */
    @Override
    public void close() {
        sync.stop();
    }
}
